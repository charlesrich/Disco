README file for task/models directory.

This directory contains example task models for use with the CE Task
Engine.  These are XML files in the CEA-2018 formalism.  Models
ABC.xml, AnnexB.xml and TailRecursion.xml are examples from the
CEA-2018 standard document.

See task/README.txt  for task/test directory for information about
test scripts that use these models.

Some models may have associated speech grammar files (for input), 
such as ABC.gram, and properties files (for output), such as
ABC.properties.  These should eventually be internationalized using
resource bundles.  

The file schemas.xml is not a task model, but a support file for the
nxml Emacs mode for editing XML with RNC schema checking, which is
included in Aquamacs 22.1 and above.  See
http://www.thaiopensource.com/nxml-mode for other versions of Emacs.

If you read in any .xml file from this directory, it will
automatically use the cea-2018 schema.  Either put your models in this
directory, or copy the schemas.xml file to the directory where you
edit your models (and change the local reference to cea-2018.rnc in
schemas.xml).

NOTE: If you get an error message about "Striding character sets..."
when Emacs tries to validate your .xml file, then you have a slightly
buggy version of nXML, which can be fixed by changing the customization
for the 'Utf Translate Cjk Mode' option in the Editing > I18n > Mule
group to nil.

If you don't use Emacs, just Google "Free XML editor" to find many
other free XML editors, most of which will use the XML Schema file
(see lib/cea-2018.xsd) instead of the RelaxNG file (the xsd checking
is weaker).  For example, Microsoft distributes something called "XML
Notepad".

rich@wpi.edu
1/10/08
