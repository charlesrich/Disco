using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.IO;

namespace TextWindow
{
	public partial class TextForm : Form
	{
		public delegate void Action ();
		
		private StringWriterWithEvent _writer;
		private DynamicStringReader _reader;

		public TextWriter Writer { get { return _writer; } }

		public TextReader Reader { get { return _reader; } }
		
		public void Clear () { shell.Clear(); }
		
		public TextForm ()
		{
			InitializeComponent ();

			shell.CommandEntered += new UILibrary.EventCommandEntered (shellControl1_CommandEntered);

			_writer = new StringWriterWithEvent ();

			_writer.LineEntered += delegate(object sender, CharEntered e)
			{
				Action uiwork = (Action) delegate()
				    {
						shell.WriteText (e.Char.ToString ());
					};
				
				if (shell.InvokeRequired) {
					shell.BeginInvoke (uiwork);
				} else
				{
					uiwork();
				}

			};

			_reader = new DynamicStringReader ();
		}

		void shellControl1_CommandEntered (object sender, UILibrary.CommandEnteredEventArgs e)
		{
			_reader.WriteLine (e.Command);
		}
		
		// should only be hidden via Console checkbox -- never closed
		private const int CP_NOCLOSE_BUTTON = 0x200;
 		protected override CreateParams CreateParams
 		{
     		get
     		{
        		CreateParams myCp = base.CreateParams;
        		myCp.ClassStyle = myCp.ClassStyle | CP_NOCLOSE_BUTTON ;
        		return myCp;
     		}
 		} 

	}
}
