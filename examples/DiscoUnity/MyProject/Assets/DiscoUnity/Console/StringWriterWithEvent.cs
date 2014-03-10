using System;
using System.Collections.Generic;
using System.Text;
using System.IO;

namespace TextWindow
{
    class StringWriterWithEvent : TextWriter
    {
        public event EventHandler<CharEntered> LineEntered = delegate { };

        public override void Write(char value)
        {   
            LineEntered(this, new CharEntered(value));

            base.Write(value);
        }


        public override Encoding Encoding
        {
            get { return Encoding.UTF8; }
        }
    }

    public class CharEntered : EventArgs
    {
		char _char;
        public char Char { get {return _char;} }

        public CharEntered(char line)
        {
            _char = line;
        }
    }
}
