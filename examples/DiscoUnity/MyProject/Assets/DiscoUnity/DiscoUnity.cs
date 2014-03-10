using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System;
using System.Threading;

using TextWindow;
using System.Windows.Forms;
using edu.wpi.disco;
using edu.wpi.disco.lang;
using edu.wpi.disco.plugin;
using edu.wpi.cetask;

public class DiscoUnity : MonoBehaviour
{	
	public const String VERSION = "1.0.6";
	
	// Note: Sleep below is a quick and easy way to prevent Disco from using 100% of processor
	// A more sophisticated approach would would adjust to how much was used in each update
	
	// fields accessible from Inspector
	public int Sleep = 1000; 
	public System.Threading.ThreadPriority Priority = System.Threading.ThreadPriority.BelowNormal;
	public bool Tracing = false;
	public bool Console = false; 
	public bool Menu = false;
	public String Models; //TODO thread-safe dynamic loading
	
	private bool discoRunning = true;
	private bool formStarted, formLoaded;
	private bool sensing;
	private bool onWindows = (Environment.OSVersion.Platform == PlatformID.Win32NT);
	
	// for synchronizing execution of NPC actions (including utterances)
    private Task task; 
	
	// for synchronizing presentation and choice of player menu
	private String[] menu, lastMenu;
	private bool newMenu; // because menu can be null
	private java.util.List items; // corresponding to lastMenu
	private Agenda.Plugin.Item chosen;
	private String formatted; // text for chosen utterance
	
	private edu.wpi.disco.Console console;
    private bool firstPrompt = true;
	
	// following are all readonly (see initializations in Awake and Start)
	private EventWaitHandle taskHandle, menuHandle, sensingHandle;
	private Program program;
	private TextForm form; 
	private Player player;
	private Agent npc;
	private Thread discoThread, formThread;
	private Interaction interaction;
	private W32ClassBatchUnregisterer w32Classes;
	
	// for overriding to present NPC utterance output
	// called on Unity thread
	protected virtual void Say (String utterance) 
	{ 
		Debug.Log("SAY "+utterance); 
	} 
	
	// for overriding to present player utterance choices
	// called on Unity thread
	// note that we do *not* assume game is locked until player responds (see MenuChoice)
	// NB: this may be called with null to clear the menu!
	protected virtual void UpdateMenu (String[] choices)
	{ 
		if ( choices != null ) {
			String menu = choices[0];
			for (int i = 1; i < choices.Length; i++) menu += Environment.NewLine+choices[i];
	    	Debug.Log(menu);
		}
	}
	
	protected String[] GetLastMenu () { return lastMenu; }
	
	// call to communicate player menu choice back to Disco
	// called on Unity (GUI) thread
	// note zero refers to first choice (normal array indexing)
	// note we pass back in the original choices array to guarantee that is same menu
	// returns false iff there if choice out of bounds or new menu has been posted
	public bool MenuChoice (String[] choices, int choice)
	{
		if ( newMenu || choices != lastMenu || choice < 0 || lastMenu == null || choice >= lastMenu.Length )
			return false;
		chosen = (Agenda.Plugin.Item) items.get(choice);
		formatted = lastMenu[choice]; // need to remember actual string due to alternatives
		lastMenu = null; // prevent selecting twice from same menu
		return true;
	}
	
	// return discourse state
	public edu.wpi.disco.Disco getDisco () { return interaction.getDisco(); }
	
	// note Awake, Start, Update, etc. made public virtual so can override in extensions 
	
	public virtual void Awake () // do non-primitive initializations for MonoBehavior's here
	{
		taskHandle = new EventWaitHandle(false, EventResetMode.ManualReset);
		menuHandle = new EventWaitHandle(false, EventResetMode.ManualReset);
		sensingHandle = new EventWaitHandle(false, EventResetMode.ManualReset);
	    w32Classes = new W32ClassBatchUnregisterer();
		program = new Program();
		player = new Player("Player");
		npc = new NPC(this, "NPC");
		interaction = new Interaction(npc, player);
		interaction.setOk(false);  // suppress default Ok's
		// support calling Debug.Log from scripts
		getDisco().eval(
             "Debug = { Log : function (obj) { TextWindow.Program.Log(obj); }}", "DiscoUnity");	
		
		discoThread = new Thread ((ThreadStart)delegate {
			try {
				while (discoRunning) {
					// start console if requested
					if ( Console && !formStarted ) {
						formStarted = true;
						//second parameter is called when the console Form is loaded
						program.StartWindow (formThread, (sender, evt) => {
							if(onWindows) w32Classes.AddWindowClassesRecursively ((Control)sender);
							formLoaded = true;								
							form.Writer.WriteLine("    DiscoUnity "+VERSION);
							console = new edu.wpi.disco.Console(null, interaction);
							interaction.setConsole(console);
							console.init(getDisco());
							console.setReader(new Program.ConsoleReader(form));
							form.shell.shellTextBox.GotFocus += (s, e) => { // improve window readability
								if ( !firstPrompt && form.shell.shellTextBox.IsCaretJustBeforePrompt() ) {
									firstPrompt = false;
								    form.Writer.Write(console.getPrompt()); 
								}
							};
							interaction.start(true); // start console thread
						});
					}
					// do Sensing on Unity thread
					sensing = true;
					sensingHandle.WaitOne();
					sensingHandle.Reset();
					UpdateDisco(getDisco()); // manage toplevel goals
	 				getDisco().tick(); // update conditions
					// process player menu choice, if any
					if ( chosen != null ) {
						getDisco().doneUtterance(chosen.task, formatted);
						done(true, chosen.task, chosen.contributes);
						chosen = null;
						formatted = null;
					}
					// process NPC response, if any
					Agenda.Plugin.Item item = npc.respondIf(interaction, true);
					if ( item != null && !item.task.Equals(npc.getLastUtterance()) ) 
						npc.done(interaction, item);
					if ( Menu ) {
						// update player menu options (including empty)
						java.util.List newItems = player.generate(interaction);
						if ( !newItems.equals(items) ) {
							String [] choices = null;
							if ( !newItems.isEmpty() ) {
								choices = new String[newItems.size()];
								int i = 0;
								java.util.Iterator l = newItems.iterator();
								while ( l.hasNext() ) 
									choices[i++] = translate(((Agenda.Plugin.Item) l.next()).task);
							}
							lastMenu = null; // block choosing from old menu
							menu = choices;
							items = newItems;
							newMenu = true;
							menuHandle.WaitOne(); // execute on Unity thread
							menuHandle.Reset();
							lastMenu = choices;
						}
					}
					// sleep for a while
					Thread.Sleep(Sleep);
				}
			} catch (Exception e) { 
				Debug.Log("Disco thread terminating: "+e); 
				if ( Tracing ) Debug.Log(e.ToString());
			}
		});
		discoThread.IsBackground = true;
		discoThread.Priority = Priority;
		
		formThread = new Thread ((ThreadStart)delegate {
			try {
				/*
					while (true) { // for testing
						program.WriteLine("Echo: " + program.ReadLine());
					}
				*/			
				form = program.GetForm();
				java.lang.System.setOut (new java.io.PrintStream (new Program.ConsoleOut (form.Writer), true));
				java.lang.System.setErr (java.lang.System.@out);
			} catch (Exception e) { Debug.Log("Console form thread terminating: "+e); }	
			});
		formThread.IsBackground = true;
	}
	
	public virtual void Start()
	{  
		if(onWindows)
		{
			AppDomain.CurrentDomain.DomainUnload += delegate {
				EnsureFormIsClosed ();
				w32Classes.TryUnregisterAll ();
			};
		}
		if ( Models.Length != 0 ) {
			String[] modelArray = Models.Split(new char[] {','});
			for (int i = 0; i < modelArray.Length; i++) {
				String name = modelArray[i];
				TextAsset model =(TextAsset) Resources.Load(name);
				if ( model == null ) Debug.LogWarning("Cannot find task model: "+name);
				else {
					if ( Tracing ) Debug.Log("LOADING "+name); 
                	TextAsset properties = (TextAsset) Resources.Load(name+".properties");
					TextAsset translate = (TextAsset) Resources.Load(name+".translate.properties");
					getDisco().load(name, model.text,
				    	            properties == null ? null : properties.text,
				        	        translate == null ? null : translate.text);
				}
			}
		}
		discoThread.Start();
	}
	
	// called on Disco or Console thread
	private void done (bool external, Task occurrence, Plan contributes) 
	{
        occurrence.done(external);
		if ( !external || occurrence is Utterance ) {
			// execute on Unity Thread
			task = occurrence;
       	 	taskHandle.WaitOne(); 
			taskHandle.Reset();
		}
		if ( contributes == null ) contributes = getDisco().explainBest(occurrence, true); 
		getDisco().done(occurrence, contributes);
		if ( console != null ) {
			// improve readability
			if ( !form.shell.shellTextBox.IsCaretJustBeforePrompt() ) form.Writer.WriteLine(); 
			firstPrompt = false;
			console.done(occurrence);
		}
	}

	class NPC : Agent 
	{
		private readonly DiscoUnity disco;
		
	    public NPC (DiscoUnity disco, String name) : base(name) { this.disco = disco; }
		
		override public void done (Interaction interaction, Agenda.Plugin.Item item) {
			disco.done(false, item.task, item.contributes);
			if ( item.task is Utterance ) 
			   lastUtterance = (Utterance) item.task;
			retry(interaction.getDisco());
		}
	}
	
	class Player : edu.wpi.disco.User
	{
		public Player (String name) : base(name) { 
			// tweak TTSay plugins
			agenda.remove(java.lang.Class.forName("edu.wpi.disco.plugin.ProposeHowPlugin"));
            new ProposeHowPlugin(agenda, 30);
            agenda.remove(java.lang.Class.forName("edu.wpi.disco.plugin.ProposeShouldSelfPlugin"));
            new ProposeShouldSelfPlugin(agenda, 90, true); //suppressExternal
		}
	}
	
	// this method is called on Unity thread once before each Disco update
	protected virtual void Sensing () {}
	
	// this method is called on Disco thread once after Sensing and
	// before each Disco update, e.g., to manage toplevel goals
	protected virtual void UpdateDisco (Disco disco) {}
	
	public virtual void Update () // called on Unity thread
	{  
		if ( form != null && formLoaded ) {
			if ( !Console && form.Visible ) form.Hide();
			if ( Console && !form.Visible ) form.Show();
		} 
		if ( sensing ) {
			Sensing();
			sensing = false;
			sensingHandle.Set();
		}
		if ( task != null ) {
			if ( Tracing ) Debug.Log("EXECUTING "+task);
			execute(task); 
			if ( task is Utterance && task.isSystem() ) 
				Say(translate(task));
			task = null;
			taskHandle.Set();
		}
		if ( newMenu ) {
			UpdateMenu(menu);
		    menu = null;
			newMenu = false;
			menuHandle.Set();
		}
	}
	
	private static void execute (Task task) 
	{
	   Script script = task.getScript();
	   if ( script != null ) script.eval(task);
	}
		
	private String translate (Task task)
	{
		java.lang.StringBuffer buffer = new java.lang.StringBuffer(
		       Utils.capitalize(getDisco().translate((Utterance) task)));		
		Utils.endSentence(buffer);
		return buffer.toString();
	}
	
	public virtual void OnDestroy ()
	{
		if ( interaction != null ) interaction.exit();
		discoRunning = false;
	    if ( sensingHandle != null ) sensingHandle.Set();
		if ( taskHandle != null ) taskHandle.Set();
	    if ( menuHandle != null ) menuHandle.Set();
		EnsureFormIsClosed ();
	}

	void EnsureFormIsClosed ()
	{
		if (form != null) {
			form.Close ();
			form.Dispose ();
		}
	}

}
