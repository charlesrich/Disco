<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.cs.wpi.edu/~rich/cetask/cea-2018-ext"
                xmlns:t="http://www.cs.wpi.edu/~rich/cetask/cea-2018-ext"
                xmlns:g="http://www.cs.wpi.edu/~rich/d4g">
  <!--
      These are the XSLT rules for translating Disco for Games (D4g) files
      to standard ANSI/CEA-2018 files.
  -->

  <!-- Include the templates and such from the first pass -->
  <xsl:include href="d4g-fp.xslt"/>

  <!-- access a task through it's id - 
       used in transferring the data from spurious nodes -->
  <xsl:key name="access-task" match="t:task" use="@id"/>

  <!-- access a task that referred to the current task
       (reverse of above key) -->
  <xsl:key name="rev-access-task" match="t:task"
           use="t:subtasks/t:step[ends-with(@name, '_ref')]/@task"/>

  <!-- first pass changes it from d4g to cea2018,
       second pass fixes spurious nodes -->  
  <xsl:template match="/">
    <xsl:variable name="first-pass">
      <xsl:apply-templates select="g:model"/>
    </xsl:variable>

    <!-- $first-pass is a node-set in 2.0, 
         needs node-set() extension if 1.0 -->
    <xsl:apply-templates select="$first-pass/t:taskModel" mode="rem-spur"/> 
  </xsl:template>

  <!-- Note to me (and you): I use 'reference' and 'manually entered'
       semi-ambiguously:
       
       'reference' can mean a <task> in a chain of <task>s that refers to the next
       <task>, or it can mean an explicit @ref/@task in a <say>/<do> that refers
       to another node. The latter use is only used when labeling what is spurious
       in the first pass 

       'manually entered' can mean a top level <task> whose ID does not start
       with an underscore, which means the ID was manually entered, or, more
       broadly, it can mean a group of nodes that were human-transformed into
       the proper cea2018 and embedded in yet-to-be-machine-transformed d4g code
    -->

  <!-- start templates for second pass -->
  <xsl:template match="t:taskModel" mode="rem-spur">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates select="t:task" mode="rem-spur"/>

      <!-- let scripts and toplevel subtasks pass through unscathed -->
      <xsl:apply-templates select="t:script|t:subtasks" mode="disco"/>
    </xsl:copy>
  </xsl:template>

  <!-- allow manually entered tasks to pass through unscathed -->
  <xsl:template match="t:task[not(@is-generated='true')]" mode="rem-spur">
    <xsl:copy-of select="."/>
  </xsl:template>

  <!-- if referrer was manually entered then display the referree
       even if it is spurious (because the manually entered task
       won't be cleansed of spurious nodes). This is used for the
       <say>/<do> nodes directly under a toplevel <say>/<do> -->
  <xsl:template match="t:task[@is-spurious='true'][@is-generated='true']" mode="rem-spur">
    <xsl:variable name="rev-ref-task" select="key('rev-access-task', @id)"/>
   
    <xsl:if test="not($rev-ref-task/@is-generated='true')">
      <xsl:copy>
        <xsl:attribute name="id">
          <xsl:value-of select="@id"/>
        </xsl:attribute>
        <xsl:copy-of select="t:precondition"/>

        <xsl:apply-templates select="t:subtasks" mode="copy-spur"/>
      </xsl:copy>
    </xsl:if>
  </xsl:template>

  <xsl:template match="t:task[not(@is-spurious='true')][@is-generated='true']" mode="rem-spur"> 
    <xsl:copy>
      <xsl:attribute name="id">
        <xsl:value-of select="@id"/>
      </xsl:attribute>
      <xsl:copy-of select="t:precondition"/>

      <xsl:apply-templates select="t:subtasks" mode="copy-spur"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="t:subtasks" mode="copy-spur">
    <xsl:copy>
      <xsl:attribute name="id">
        <xsl:value-of select="@id"/>
      </xsl:attribute>

      <!-- pass transformation to the other t:subtasks template --> 
      <xsl:apply-templates select="." mode="recurse-spur"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="t:subtasks" mode="recurse-spur">
    <xsl:param name="appl" select="/.."/> <!-- defaults to empty node set -->
    <xsl:param name="comments" select="/.."/>
    <xsl:param name="bindings" select="/.."/> 
    <xsl:variable name="ref-task" select="key('access-task', t:step[ends-with(@name, '_ref')]/@task)"/>

    <!-- when ref task is spurious, call this template on it and 
         pass the applicable, bindings, and comments as parameters
	 to keep their order -->
    <xsl:if test="$ref-task/@is-spurious = 'true'">
      <xsl:copy-of select="t:step[1]"/>
      <xsl:apply-templates select="$ref-task/t:subtasks" mode="recurse-spur">
	<xsl:with-param name="appl">
	  <xsl:copy-of select="$appl"/>
	  <xsl:copy-of select="t:applicable"/>
	</xsl:with-param>
 	<xsl:with-param name="comments">
	  <xsl:copy-of select="$comments"/>
	  <xsl:apply-templates select="comment()" mode="warnings"/>
	</xsl:with-param>
        <xsl:with-param name="bindings">
          <xsl:copy-of select="$bindings"/>
          <xsl:copy-of select="t:binding"/>
        </xsl:with-param>
      </xsl:apply-templates> 
    </xsl:if>

    <!-- when it is not spurious, print out everything collected so far in order -->
    <xsl:if test="not($ref-task/@is-spurious = 'true')">
      <xsl:copy-of select="t:step"/>
      <xsl:copy-of select="$appl"/>
      <xsl:copy-of select="t:applicable"/>
      <xsl:copy-of select="$comments"/>
      <xsl:apply-templates select="comment()" mode="warnings"/> 
      <xsl:copy-of select="$bindings"/>
      <xsl:copy-of select="t:binding"/>
    </xsl:if>
  </xsl:template>

  <!-- to keep any warning comments -->
  <xsl:template match="comment()" mode="warnings">
    <xsl:comment><xsl:copy-of select="."/></xsl:comment>
    <xsl:text>&#10;</xsl:text> <!-- insert newline -->
  </xsl:template>
</xsl:stylesheet>
