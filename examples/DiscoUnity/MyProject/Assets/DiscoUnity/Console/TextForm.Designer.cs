using System.Windows.Forms;
namespace TextWindow
{
    partial class TextForm
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }
		
		// prevent stealing focus from Unity
		protected override bool ShowWithoutActivation {
          get { return true; }
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.shell = new UILibrary.ShellControl();
            this.SuspendLayout();
            // 
            // shell
            // 
            this.shell.Dock = System.Windows.Forms.DockStyle.Fill;
            this.shell.Location = new System.Drawing.Point(0, 0);
            this.shell.Name = "shell";
            this.shell.ShellTextBackColor = System.Drawing.Color.White;
            this.shell.ShellTextFont = new System.Drawing.Font("Courier New", 9F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.shell.ShellTextForeColor = System.Drawing.Color.Black;
            this.shell.Size = new System.Drawing.Size(284, 262);
            this.shell.TabIndex = 0;
            // 
            // TextForm
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(284, 262);
            this.Controls.Add(this.shell);
            this.Name = "TextForm";
            this.Text = "Disco";
            this.ResumeLayout(false);
        }

        #endregion

        internal UILibrary.ShellControl shell;


    }
}

