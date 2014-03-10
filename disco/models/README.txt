README file for disco/models directory (see also task/models/README.txt).

This directory contains example task models for use with the Disco.
These are XML files in the CEA-2018 formalism.  Library.xml is a
slightly modified version of the library example in the task project.
CEA-2018 standard document, with an associated dataflow picture
Library.pdf.

Also see Meeting.xml with associated dataflow picture Meeting.pdf.

See disco/test directory for information about test scripts that use
these models.

Some models may have associated .properties and .translate.properties
files.  The former are used to annotate model elements with extra
information to control how they are used in the Disco engine, such as
formatted printing (@format property).  The latter are for customizing
utterances.

The file schemas.xml is not a task model, but a support file for the
nxml Emacs mode for editing XML with RNC schema checking, which is
included in Aquamacs 22.1 and above.  See
http://www.thaiopensource.com/nxml-mode for other versions of Emacs.

If you read in any .xml file from this directory, it will
automatically use the (regular and extended) cea-2018 schemas.  Either
put your models in this directory, or copy the schemas.xml file to the
directory where you edit your models (and change the local reference
to *.rnc in schemas.xml).

NOTE: If you get an error message about "Striding character sets..."
when Emacs tries to validate your .xml file, then you have a slightly
buggy version of nXML, which can be fixed by changing the customization
for the 'Utf Translate Cjk Mode' option in the Editing > I18n > Mule
group to nil.

If you don't use Emacs, just Google "Free XML editor" to find many
other free XML editors, most of which will use the XML Schema file
(see disco/lib/cea-2018-ext.xsd in this directory) instead of the
RelaxNG file (the xsd checking is weaker).  For example, Microsoft
distributes something called "XML Notepad".

rich@wpi.edu

