<?xml version="1.0" encoding="utf-16"?>

<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:import href="base.xslt" />

  <xsl:template match="/">
    <html>
      <xsl:call-template name="head" />
      <body>
        <table>
          <thead>
            <tr>
              <th>Font Name</th>
              <th>Scripts</th>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates select="//Font" >
              <xsl:sort select="@FaceName"/>
            </xsl:apply-templates>
          </tbody>
        </table>
      </body>
    </html>
  </xsl:template>

<xsl:template match="Font">
  <tr class="Font">
    <td><xsl:value-of select="@FaceName"/></td>
    <td>
      <xsl:for-each select="Script">
        <xsl:value-of select="@Tag"/>: <xsl:for-each select="Language">
          <xsl:value-of select="@Tag"/><xsl:if test="count(../Language)=last()">, <xsl:value-of select="last()"/>
        </xsl:if>
        </xsl:for-each><br/>
      </xsl:for-each></td>
  </tr>
</xsl:template>

</xsl:stylesheet> 
