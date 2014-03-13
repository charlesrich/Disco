using System;
using System.Collections.Generic;
using System.Text;
using System.IO;
using System.Threading;

namespace TextWindow
{
    class DynamicStringReader : TextReader
    {
        Queue<string> _innerList = new Queue<string>();
        StringReader _curReader;
        EventWaitHandle waitHandle = new EventWaitHandle(false, EventResetMode.AutoReset);

        public override int Read()
        {
            AssignCurrentReader();
            if (_curReader == null)
                return -1;

            return _curReader.Read();
        }

        public override int Read(char[] buffer, int index, int count)
        {
            AssignCurrentReader();
            if (_curReader == null)
                base.Read(buffer, index, count);

            return _curReader.Read(buffer, index, count);
        }

        public override int ReadBlock(char[] buffer, int index, int count)
        {
            AssignCurrentReader();
            if(_curReader == null)
                return base.ReadBlock(buffer, index, count);

            return _curReader.ReadBlock(buffer, index, count);
        }

        public override string ReadLine()
        {   
            AssignCurrentReader();
            if(_curReader != null)
                return _curReader.ReadLine();

            waitHandle.WaitOne();
            return ReadLine();
        }

        public override string ReadToEnd()
        {
            AssignCurrentReader();
            if (_curReader == null)
                return this._curReader.ReadToEnd();

            return _curReader.ReadToEnd();
        }

        void AssignCurrentReader()
        {
            if ((_curReader == null || _curReader.Peek() != -1) && _innerList.Count > 0)
            {
                string s = _innerList.Dequeue();

                _curReader = new StringReader(s);
            }
            
            if(_curReader != null && _curReader.Peek() == -1)
            {
                _curReader = null;
            }
        }

        public override int Peek()
        {   
            AssignCurrentReader();

            if(_curReader == null)
                return base.Peek();

            return _curReader.Peek();
        }

        public void WriteLine(string value)
        {
            _innerList.Enqueue(value);
            waitHandle.Set();
        }
    }
}
