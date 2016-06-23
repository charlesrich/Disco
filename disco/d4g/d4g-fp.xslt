<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.cs.wpi.edu/~rich/cetask/cea-2018-ext"
                xmlns:t="http://www.cs.wpi.edu/~rich/cetask/cea-2018-ext"
                xmlns:g="http://www.cs.wpi.edu/~rich/d4g">

  <!--
     These are the XSLT rules for translating Disco for Games (D4g) files
     to standard ANSI/CEA-2018 files. This is only the first pass; it
     is separated from the second pass to allow unit tests to use this one
     directly.
    -->

  <xsl:output method="xml" indent="yes"/>
  <xsl:strip-space elements="*"/>

  <xsl:param name="external" select="'user'"/>

  <!-- pre-pass to transform <user>/<agent> into <say> -->
  <xsl:template match="g:model">
    <xsl:variable name="pre-pass">
      <xsl:copy>
	<xsl:copy-of select="@*"/>
	<xsl:apply-templates mode="pre-pass"/>
      </xsl:copy>
    </xsl:variable>

    <xsl:apply-templates select="$pre-pass/g:model" mode="first-pass"/>
  </xsl:template>

  <!-- identity transformation - we want everything the same except <user>/<agent> -->
  <xsl:template match="@*|node()" mode="pre-pass">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" mode="pre-pass"/>
    </xsl:copy>
  </xsl:template>

  <!-- replace <user> with <say> -->
  <xsl:template match="g:user" mode="pre-pass">
    <xsl:element name="g:say">
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="actor">
	<xsl:value-of select="$external"/>
      </xsl:attribute>

      <xsl:apply-templates select="@*|node()" mode="pre-pass"/>
    </xsl:element>
  </xsl:template>

  <!-- replace <agent> with <say> -->
  <xsl:template match="g:agent" mode="pre-pass">
    <xsl:element name="g:say">
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="actor">
	<xsl:text>agent</xsl:text>
      </xsl:attribute>

      <xsl:apply-templates select="@*|node()" mode="pre-pass"/>
    </xsl:element>
  </xsl:template>

  <!-- start templates for first pass -->

  <xsl:template match="g:model" mode="first-pass">
    <xsl:element name="taskModel">

      <!-- note namespace element only supported in XSLT 2.0 -->

      <!-- force inclusion of disco namespace -->
      <xsl:namespace name="disco" select="'urn:disco.wpi.edu:Disco'"/>
      
      <!-- include other input prefixes, since might be used inside of task id's -->
      <xsl:variable name="model" select="."/>
      <xsl:for-each select="in-scope-prefixes(.)">
        <xsl:variable name="prefix" select="."/>
        <xsl:if test="not(string-length($prefix)=0 or 
                    namespace-uri-for-prefix($prefix,$model)='http://www.cs.wpi.edu/~rich/d4g' or
                    namespace-uri-for-prefix($prefix,$model)='http://ce.org/cea-2018' or
                    namespace-uri-for-prefix($prefix,$model)='http://www.cs.wpi.edu/~rich/cetask/cea-2018-ext')">
          <xsl:namespace name="{$prefix}" select="namespace-uri-for-prefix($prefix,$model)"/>
        </xsl:if>
      </xsl:for-each>

      <!-- take care of 'about' URN to make unique -->
      <xsl:attribute name="about">
        <xsl:value-of select="@about"/>
      </xsl:attribute>
      
      <!-- make top-level 'do' and 'say' tasks to get things started -->
      <xsl:for-each select="g:do|g:say">
        <xsl:element name="task">
          <xsl:attribute name="id">
            <xsl:value-of select="@id"/>
            <xsl:if test="not(@id)">
              <xsl:text>_</xsl:text>
              <xsl:value-of select="generate-id()"/>
            </xsl:if>
          </xsl:attribute>
          <!-- it's only generated if the id is not present -->
          <xsl:if test="not(@id)">
            <xsl:attribute name="is-generated">
              <xsl:text>true</xsl:text>
            </xsl:attribute>
          </xsl:if>

          <!-- preconditions only allowed at top level -->
          <xsl:if test="@precondition">
            <xsl:element name="precondition">
              <xsl:value-of select="@precondition"/>
            </xsl:element>
          </xsl:if>
          
          <xsl:apply-templates select="current()" mode="subtasks"/>
        </xsl:element>
      </xsl:for-each>

      <!-- make required 2nd, 3rd, and so on level tasks to build tree -->
      <xsl:apply-templates select="//g:do" mode="task"/>
      <xsl:apply-templates select="//g:say" mode="task"/>

      <!-- collect the disco elements -->
      <xsl:apply-templates select="t:task|t:script|t:subtasks" mode="disco"/>
    </xsl:element>
  </xsl:template>

  <!-- allow disco tags to pass through unscathed -->
  <xsl:template match="*" mode="disco">
    <xsl:element name="{local-name()}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="disco"/>
    </xsl:element>
  </xsl:template>

  <!-- tasks for 'do' nodes -->
  <xsl:template match="g:do" mode="task">
    <!-- select all nodes that reference this one
	 (these are the only ways a node can be referenced I have found)
	 note: this is a different type of 'reference' than how <subtasks>
	 link to <task>s via a <step> - this is an explicit @ref/@task -->
    <xsl:variable name="refs" 
		  select="//g:say[@ref=current()/@id] |
			  //g:do[@task=concat('_',current()/@id,'_tree')]"/>

    <!-- only generate task if there are child elements or if this <do>
         is being referenced by a node somewhere else (which would be weird,
         but legal) -->
    <xsl:if test="./* | $refs">
      <xsl:element name="task">
        <!-- use id attribute if present -->
        <xsl:attribute name="id">
          <xsl:choose>
            <xsl:when test="@id">
              <xsl:text>_</xsl:text>
              <xsl:value-of select="@id"/>
              <xsl:text>_tree</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>_</xsl:text>
              <xsl:value-of select="generate-id()"/>
              <xsl:text>_tree</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:attribute name="is-generated">
          <xsl:text>true</xsl:text>
        </xsl:attribute>

        <!-- test to see if this task is spurious (for use in second pass).
             it is spurious if it only has one immediate child and no referrers 
             (not counting the parent) -->
        <xsl:if test="count(g:say | g:do) = 1">
          <xsl:if test="count($refs) = 0">
            <xsl:attribute name="is-spurious">
              <xsl:text>true</xsl:text>
            </xsl:attribute>
          </xsl:if>
        </xsl:if>

        <!-- apply to children -->
        <xsl:apply-templates select="g:do|g:say" mode="subtasks"/>
      </xsl:element>
    </xsl:if>
    
    <!-- now test to see if the only child is a comment and send a warning
         (because it is probably a typo). The comment is for testing purposes-->
    <xsl:if test="count(node()) = 1 and comment()">
      <xsl:message>
	Warning, 'do' element with a lone comment as a subchild.
        Do @task="<xsl:value-of select="@task"/>"
      </xsl:message>
      <xsl:comment>
	Warning, 'do' element with a lone comment as a subchild.
      </xsl:comment>
    </xsl:if>
  </xsl:template>

  <!-- tasks for 'say' nodes -->
  <xsl:template match="g:say" mode="task">
    <!-- select all nodes that reference this one
	 (these are the only ways a node can be referenced I have found)
	 note: this is a different type of 'reference' than how <subtasks>
	 link to <task>s via a <step> - this is an explicit @ref/@task -->
    <xsl:variable name="refs"
		  select="//g:say[@ref=current()/@id] |
			  //g:do[@task=concat('_',current()/@id,'_tree')]"/>

    <!-- only generate task if there are child elements or if this <say>
         is being referenced by a node somewhere else (which would be werid,
         but legal) -->
    <xsl:if test="./* | $refs">
      <!-- use id attribute if present -->
      <xsl:element name="task">
        <xsl:attribute name="id">
          <xsl:choose>
            <xsl:when test="@id">
              <xsl:text>_</xsl:text>
              <xsl:value-of select="@id"/>
              <xsl:text>_tree</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>_</xsl:text>
              <xsl:value-of select="generate-id()"/>
              <xsl:text>_tree</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:attribute name="is-generated">
          <xsl:text>true</xsl:text>
        </xsl:attribute>       

        <!-- test to see if this task is spurious (for use in second pass)
             it is spurious if it only has one immediate child and no referrers 
             (not counting the parent) -->
	<xsl:if test="count(g:say | g:do) = 1">
          <xsl:if test="count($refs) = 0">
            <xsl:attribute name="is-spurious">
              <xsl:text>true</xsl:text>
            </xsl:attribute>
          </xsl:if>
        </xsl:if>

        <!-- apply to children -->
        <xsl:apply-templates select="g:do|g:say" mode="subtasks"/>
      </xsl:element>
    </xsl:if>

    <!-- now test to see if the only child is a comment and send a warning
         (because it is probably a typo). The comment is for testing purposes-->
    <xsl:if test="count(node()) = 1 and comment()">
      <xsl:message>
	Warning, 'say' element with a lone comment as a subchild.
        Say @text="<xsl:value-of select="@text"/>"
      </xsl:message>
      <xsl:comment>
	Warning, 'say' element with a lone comment as a subchild.
      </xsl:comment>
    </xsl:if>

  </xsl:template>

  <!-- subtasks for 'do' nodes-->
  <xsl:template match="g:do" mode="subtasks">
    <xsl:variable name="id">
      <xsl:choose>
        <xsl:when test="@id">
          <xsl:text>_</xsl:text>
          <xsl:value-of select="@id"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>_</xsl:text>
          <xsl:value-of select="generate-id()"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:element name="subtasks">
      <xsl:attribute name="id">
        <xsl:value-of select="$id"/>
        <xsl:text>_subtasks</xsl:text>
      </xsl:attribute>

      <!-- link to referenced task -->
      <xsl:element name="step">
        <xsl:attribute name="name">
          <xsl:value-of select="$id"/>
          <xsl:text>_step</xsl:text>
        </xsl:attribute>
        <xsl:attribute name="task">
          <xsl:value-of select="@task"/>
        </xsl:attribute>

        <!-- pass through min/maxOccurs if present -->
        <xsl:if test="@minOccurs">
          <xsl:attribute name="minOccurs">
            <xsl:value-of select="@minOccurs"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:if test="@maxOccurs">
          <xsl:attribute name="maxOccurs">
            <xsl:value-of select="@maxOccurs"/>
          </xsl:attribute>
        </xsl:if>

      </xsl:element>     

      <!-- include rest of tree if appropriate -->
      <xsl:if test="./*">
        <xsl:element name="step">
          <xsl:attribute name="name">
            <xsl:value-of select="$id"/>
            <xsl:text>_ref</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="task">
            <xsl:value-of select="$id"/>
            <xsl:text>_tree</xsl:text>
          </xsl:attribute>
        </xsl:element>
      </xsl:if>

      <!-- include applicable if appropriate -->
      <xsl:if test="@applicable">
	<xsl:if test="preceding-sibling::* | following-sibling::*">
	  <xsl:element name="applicable">
	    <xsl:value-of select="@applicable"/>
	  </xsl:element>
	</xsl:if>
	<!-- issue warning/don't create <applicable> if there are no siblings -->
	<xsl:if test="not(preceding-sibling::* | following-sibling::*)">
	  <xsl:message>
	    Warning, @applicable in 'do' element with no siblings. 
	    Do @task="<xsl:value-of select="@task"/>"
	  </xsl:message>
	  <xsl:comment>Warning, @applicable in a &lt;do&gt; with no siblings. Do @task="<xsl:value-of select="@task"/>"</xsl:comment> 	       <xsl:text>&#10;</xsl:text> <!-- insert newline -->
	</xsl:if>
      </xsl:if>

      <!-- apply external modifiers if actor specified -->
      <xsl:if test="@actor">
        <xsl:element name="binding">
          <xsl:attribute name="slot">
            <xsl:text>$</xsl:text>
            <xsl:value-of select="$id"/>
            <xsl:text>_step.external</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="value">
            <xsl:if test="@actor=$external"> 
              <xsl:text>true</xsl:text>
            </xsl:if>
            <xsl:if test="@actor!=$external">
              <xsl:text>false</xsl:text>
            </xsl:if>
          </xsl:attribute>
        </xsl:element>
      </xsl:if>

      <!-- interpret remaining attributes as input bindings -->
      <xsl:for-each select="@*">
        <xsl:if test="not(name()='id' or name()='actor' or name()='task' or name()='minOccurs'
                      or name()='maxOccurs' or name()='applicable')">
          <xsl:element name="binding">
            <xsl:attribute name="slot">
              <xsl:text>$</xsl:text>
              <xsl:value-of select="$id"/>
              <xsl:text>_step.</xsl:text>
              <xsl:value-of select="name()"/>
            </xsl:attribute>
            <xsl:attribute name="value">
              <xsl:value-of select="."/>
            </xsl:attribute>
          </xsl:element>
        </xsl:if>
      </xsl:for-each>
      
    </xsl:element>
  </xsl:template>

  <!-- subtasks for 'say' nodes -->
  <xsl:template match="g:say" mode="subtasks">
    <xsl:variable name="id">
      <xsl:choose>
        <xsl:when test="@id">
          <xsl:text>_</xsl:text>
          <xsl:value-of select="@id"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>_</xsl:text>
          <xsl:value-of select="generate-id()"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- Say$Expression needs extra level of quotation -->
    <xsl:variable name="preprepre-text">
      <xsl:choose>
	<xsl:when test="matches(@text,'(^\{)|[^\\]\{')">
	  <xsl:value-of select="concat('&quot;\&quot;', @text, '\&quot;&quot;')"/>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:value-of select="concat('&quot;', @text, '&quot;')"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- Modify text to transform embedded javascript -->
    <xsl:variable name="prepre-text">
      <!-- preliminary search to combine back-to-back embedded js snippets.
	   this is needed because the xslt regex engine does not support lookaround
	   which would be used to determine if the second embedded js that is
	   back-to-back is actually back-to-back.
	   Note that this will match any number of back to back regexes, and
	   that all consecutive back to back regexes that are empty will be
	   replaced as if there was only one.
	-->

      <xsl:value-of select="replace($preprepre-text,
			    '
			    ([^\\]
			    (\\{2})*)
			    \}\{(\}\{)*',
			    '$1)+''''+(',
			    'x')"/>
      
    </xsl:variable>
    <xsl:variable name="pre-text">
      <!-- regex explanation: the first two lines match an even number of
	   backslashes (including 0) so the regex won't match an escaped opening
	   bracket. Then the opening bracket and everything up until a closing
	   bracket that is not escaped (i.e., has an even number, including 0, of
	   backslashes preceding it) will be matched. This is to allow curlies
	   within the js as long as they are escaped. Because of this, make sure
	   that there are no non-escaped closing curlies within any of the js,
	   including in strings and comments - all of them should be escaped.
	-->
      <xsl:analyze-string select="$prepre-text"
			  regex="([^\\]
				 (\\{{2}})*)
				 \{{
				 (.*?[^\\](\\{{2}})*)?
				 \}}"
			  flags="x">
	<xsl:matching-substring>
	  <xsl:value-of select="regex-group(1)"/>
	  <xsl:text>\&quot;+(</xsl:text>

	  <!-- Replace '\{' and '\}' with '{' and '}' in js code -->
	  <xsl:value-of select="replace(
				replace(regex-group(3), '\\\}', '}'),
				'\\\{',
				'{')"/>

	  <xsl:text>)+\&quot;</xsl:text>
	</xsl:matching-substring>

	<xsl:non-matching-substring>
	  <!-- replace \{ and \} with { and } in the non-js code -->
	  <xsl:value-of select="replace(
				replace(., '\\\}', '}'),
				'\\\{',
				'{')"/>
	</xsl:non-matching-substring>
      </xsl:analyze-string>
    </xsl:variable>
    <!-- remove redundancies after javascript expansion, specifically:
	 - replace ^"\"\"+(... with "(...
	 - replace ...)+\"\""$ with ...)"
	 - replace ^"()"$ with ""
      -->    
    <xsl:variable name="text">
      <xsl:value-of select="replace(
			       replace(
			          replace($pre-text, '^&quot;\\&quot;\\&quot;\+', '&quot;'),
			          '\+\\&quot;\\&quot;&quot;$',
			          '&quot;'),
			       '^&quot;\(\)&quot;$',
			       '&quot;&quot;')"/>
    </xsl:variable>

    <xsl:element name="subtasks">
      <xsl:attribute name="id">
        <xsl:value-of select="$id"/>
        <xsl:text>_subtasks</xsl:text>
      </xsl:attribute>

      <xsl:if test="@ref">
        <!-- referencing another node -->
        <xsl:element name="step">
          <xsl:attribute name="name">
            <xsl:value-of select="$id"/>
            <xsl:text>_step</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="task">
            <xsl:text>_</xsl:text>
            <xsl:value-of select="@ref"/>
            <xsl:text>_tree</xsl:text>
          </xsl:attribute>
        </xsl:element>
      </xsl:if>

      <xsl:if test="not(@ref)">
        <xsl:element name="step">
          <xsl:attribute name="name">
            <xsl:value-of select="$id"/>
            <xsl:text>_step</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="task">
	    <xsl:text>disco:Say</xsl:text>
	    <!-- need to test here (and elsewhere) whether text has any variables -CR -->
	    <xsl:if test="matches($prepre-text,'[^\\]\{')">  
              <xsl:text>$Expression</xsl:text>
	    </xsl:if>
	    <xsl:if test="@eval">
              <xsl:text>$Eval</xsl:text>
	    </xsl:if>
          </xsl:attribute>
        </xsl:element>
      </xsl:if>

      <!-- include rest of tree if appropriate -->
      <xsl:if test="./*">
        <xsl:element name="step">
          <xsl:attribute name="name">
            <xsl:value-of select="$id"/>
            <xsl:text>_ref</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="task">
            <xsl:value-of select="$id"/>
            <xsl:text>_tree</xsl:text>
          </xsl:attribute>
        </xsl:element>
      </xsl:if>

      <!-- include applicable if appropriate -->
      <xsl:if test="@applicable">
	<xsl:if test="preceding-sibling::* | following-sibling::*">
	  <xsl:element name="applicable">
	    <xsl:value-of select="@applicable"/>
	  </xsl:element>
	</xsl:if>
	<!-- issue warning/don't create <applicable> if there are no siblings -->
	<xsl:if test="not(preceding-sibling::* | following-sibling::*)">
	  <xsl:message>
	    Warning, @applicable in 'say' element with no siblings. 
	    Say @text="<xsl:value-of select="@text"/>"
	  </xsl:message>
	  <xsl:comment>Warning, @applicable in a &lt;say&gt; with no siblings. Say @text="<xsl:value-of select="@text"/>"</xsl:comment>
	  <xsl:text>&#10;</xsl:text> <!-- insert newline -->
	</xsl:if>
      </xsl:if>

      <!-- i'm considering implementing a template to remove the following
	   duplicated code (all the bindings) -->

      <xsl:if test="not(@ref)">

        <!-- include text -->
        <xsl:element name="binding">
          <xsl:attribute name="slot">
            <xsl:text>$</xsl:text>
            <xsl:value-of select="$id"/>
            <xsl:text>_step.text</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="value">
            <xsl:value-of select="$text"/>
          </xsl:attribute>
        </xsl:element>

        <!-- apply external modifiers if actor specified -->
        <xsl:if test="@actor">
          <xsl:element name="binding">
            <xsl:attribute name="slot">
              <xsl:text>$</xsl:text>
              <xsl:value-of select="$id"/>
              <xsl:text>_step.external</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="value">
              <xsl:if test="@actor=$external">
                <xsl:text>true</xsl:text>
              </xsl:if>
              <xsl:if test="@actor!=$external">
                <xsl:text>false</xsl:text>
              </xsl:if>
            </xsl:attribute>
          </xsl:element>
          <!-- issue warning if not same actor as siblings -->
          <xsl:if test="../name()!='model' and preceding-sibling::*/@actor and @actor!=preceding-sibling::*/@actor[1]">
            <xsl:message>
              Warning, 'say' element with different actor than sibling. 
              Say @text="<xsl:value-of select="@text"/>"
            </xsl:message>
            <xsl:comment>Warning, &lt;say&gt; with different actor than sibling. Say @text="<xsl:value-of select="@text"/>"</xsl:comment>
            <xsl:text>&#10;</xsl:text> <!-- insert newline -->
          </xsl:if>
        </xsl:if>

	<!-- include eval if appropriate -->
	<xsl:if test="@eval">
          <xsl:element name="binding">
            <xsl:attribute name="slot">
              <xsl:text>$</xsl:text>
              <xsl:value-of select="$id"/>
              <xsl:text>_step.eval</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="value">
              <xsl:text>&quot;</xsl:text>
              <xsl:value-of select="@eval"/>
              <xsl:text>&quot;</xsl:text>
            </xsl:attribute>
          </xsl:element>
	</xsl:if>

      </xsl:if>

    </xsl:element>
  </xsl:template>
  <!-- end templates for first pass -->
</xsl:stylesheet>
