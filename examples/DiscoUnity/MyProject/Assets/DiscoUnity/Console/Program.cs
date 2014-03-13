using System;
using System.IO;
using System.Collections.Generic;
using System.Windows.Forms;
using System.Threading;
using System.Diagnostics;

namespace TextWindow
{
	public class Program
	{
			
	    // for convenience to avoid Jint error for void methods
		// note this is here because it must be in a namespace
	    public static object Log (object message) {
			UnityEngine.Debug.Log(message);
			return message;
		}
		
		private TextForm form; // do not create form here!

		public TextForm GetForm ()
		{
			return form;
		}
		public void WriteLine (String line)
		{
			form.Writer.WriteLine (line);
		}
		public String ReadLine ()
		{
			return form.Reader.ReadLine ();
		}

		// for Mono executable
		public static void Main ()
		{
			Program program = new TextWindow.Program();
			TextForm form = program.GetForm();
			
			Thread appThread = new Thread ((ThreadStart)delegate {
				/*
				while (true) { // for testing
					WriteLine ("Echo: " + ReadLine());
				}
				*/				
				java.lang.System.setOut (new java.io.PrintStream 
				                           (new Program.ConsoleOut (form.Writer), true));
				java.lang.System.setErr (java.lang.System.@out);

				edu.wpi.disco.Disco.main (new String[0]);
				((edu.wpi.disco.Disco) edu.wpi.cetask.TaskEngine.ENGINE).getInteraction().getConsole()
					.setReader (new Program.ConsoleReader (form));
			});
			appThread.IsBackground = true;
			program.RunWindow (appThread, (sender, e) => { });
		}

		// for Unity library
		public void StartWindow (Thread appThread, Action<object, EventArgs> onFormLoad)
		{
			Thread winThread = new Thread ((ThreadStart)delegate { RunWindow (appThread, onFormLoad); });
			winThread.SetApartmentState (ApartmentState.STA);
			winThread.IsBackground = true;
			winThread.Start ();
		}

		private void RunWindow (Thread appThread, Action<object, EventArgs> onFormLoad)
		{
			form = new TextForm ();

			form.Load += (sender, e) =>
			{
				appThread.Start ();
				onFormLoad (sender, e);
			};
			

			form.Height = 600;
			form.Width = 800;
			
            // makes form visible
			Application.Run(form);
		}
		
		public class ConsoleReader : edu.wpi.cetask.Shell.Reader
		{
			private readonly TextForm form;
			
			public ConsoleReader (TextForm form) { this.form = form; }
			
			public String readLine ()
			{   
				return form.Reader.ReadLine ();
			}
		}

		public class ConsoleOut : java.io.OutputStream
		{

			private TextWriter writer;
			private System.Text.Decoder decoder = System.Text.Encoding.ASCII.GetDecoder ();

			public ConsoleOut (TextWriter writer)
			{
				this.writer = writer;
			}

			private char[] Decode (byte[] buffer)
			{
				char[] chars = new char[decoder.GetCharCount (buffer, 0, buffer.Length)];
				decoder.GetChars (buffer, 0, buffer.Length, chars, 0);
				return chars;
			}

			public override void write (int oneByte)
			{
				writer.Write (Decode (new byte[] { (byte)oneByte }));
			}

			public override void write (byte[] buffer)
			{
				writer.Write (Decode (buffer));
			}

			public override void write (byte[] buffer, int offset, int count)
			{
				writer.Write (Decode (buffer), offset, count);
			}

			public override void flush ()
			{
				writer.Flush ();
			}
			
		}
	}
}
