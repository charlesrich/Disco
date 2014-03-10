using System;
using System.Windows.Forms;
using System.Runtime.InteropServices;
using System.Text;

namespace TextWindow
{
	public class W32ClassUnregisterer
	{
		IntPtr _hwnd;
		string _name;
		
        [DllImport("user32.dll")]
        private static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);
		
		[DllImport("user32.dll")]
		static extern bool UnregisterClass(string lpClassName, IntPtr hInstance);
		
		public W32ClassUnregisterer (IntPtr handle)
		{
			_hwnd = handle;
		}

		private string CallW32GetClassName()
		{
            StringBuilder sb = new StringBuilder(256);
            GetClassName(_hwnd, sb, sb.Capacity);
            return sb.ToString();
		}
		
		public string GetName()
        {
			if(_name == null)
			{
				_name = CallW32GetClassName();
			}
			
			return _name;
        }
		
		public bool TryUnregister()
		{
			if(!UnregisterClass(GetName(), IntPtr.Zero))
			{
				UnityEngine.Debug.Log("Unregister failed: " + GetName());
				return false;
			}
			
			return true;
		}
	}
}

