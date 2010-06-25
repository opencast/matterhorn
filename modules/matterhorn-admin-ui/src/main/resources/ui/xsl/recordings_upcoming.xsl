<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : recordings_upcoming.xsl
    Created on : November 19, 2009, 9:18 PM
    Author     : wulff
    Description:
        Transforms output of /admin/rest/recordings/upcoming to HTML table
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://adminui.opencastproject.org/" version="1.0">
  <xsl:output method="html"/>

  <xsl:template match="ns1:recordingLists">
    <div style="border: 1px solid #cccccc;font-size:0.9em;text-align:center;width:900px;margin-left:auto;margin-right:auto;margin-bottom:0.5em;padding:0.5em;">
      <img title="Information" alt="Information" src="shared_img/icons/information.png" style="vertical-align: bottom;"></img>
      All recordings scheduled for the future appear below. (Future versions of Matterhorn will allow you to filter this list to show specific days.)
    </div>
    <table id="recordingsTable" class="fl-theme-coal wu-table-list" width="100%" style="float:left;">
      <thead>
        <tr>
          <th width="25%" class="sortable">Title</th>
          <th width="17%" class="sortable">Presenter</th>
          <th width="20%" class="sortable">Course/Series</th>
          <th width="15%" class="sortable date-column">Recording Date &amp; Time</th>
          <th width="13%" class="sortable">Capture Agent</th>
          <th width="10%" class="sortable">Status</th>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates />
      </tbody>
    </table>
  </xsl:template>

  <xsl:template match="ns1:recording">
    <tr class="highlightable">
      <td>
        <a title="Edit Recording">
          <xsl:attribute name="href">/admin/scheduler.html?eventId=<xsl:value-of select="id" />&amp;edit</xsl:attribute>
          <xsl:value-of select="title" />
        </a>
      </td>
      <td>
        <xsl:value-of select="presenter" />
      </td>
      <td>
        <xsl:value-of select="seriestitle" />
      </td>
      <td class="td-TimeDate">
        <span class="date-start">
          <xsl:value-of select="startTime" />
        </span>
        <span class="date-end">
          <xsl:value-of select="endTime" />
        </span>
      </td>
      <td>
        <xsl:value-of select="captureAgent" />
      </td>
      <td>
        Scheduled for automatic capture
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
