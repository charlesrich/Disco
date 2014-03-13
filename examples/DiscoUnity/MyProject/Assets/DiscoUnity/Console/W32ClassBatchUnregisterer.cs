using System;
using System.Windows.Forms;
using System.Collections.Generic;


namespace TextWindow
{
	public class W32ClassBatchUnregisterer
	{
		Dictionary<string, W32ClassUnregisterer> windowClasses = new Dictionary<string, W32ClassUnregisterer>();

		public void AddWindowClassesRecursively(Control control)
		{
			var wc = new W32ClassUnregisterer(control.Handle);
			string name = wc.GetName ();
			
			if (!windowClasses.ContainsKey (name)) {
				windowClasses[name] = wc;
			}
			
			foreach(Control c in control.Controls)
				AddWindowClassesRecursively(c);
		}
		
		public void TryUnregisterAll()
		{
			foreach(var wc in windowClasses.Values)
				wc.TryUnregister();
		}
	}
}

