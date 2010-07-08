<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : recordings_failed.xsl
    Created on : November 19, 2009, 9:18 PM
    Author     : wulff
    Description:
        Transforms output of /admin/rest/recordings/finished to HTML table
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://adminui.opencastproject.org/" version="1.0">
  <xsl:output method="html"/>

  <xsl:template match="ns1:recordingLists">
    <table id="recordingsTable" class="fl-theme-coal wu-table-list" width="100%" style="float:left;">
      <thead>
        <tr>
          <th width="30%" id="th-Title" class="sortable recording-Table-head">Title</th>
          <th width="15%" id="th-Presenter" class="sortable recording-Table-head">Presenter</th>
          <th width="20%" id="th-Series" class="sortable recording-Table-head">Course/Series</th>
          <th width="20%" id="th-StartDate" class="sortable date-column recording-Table-head">Recording Date &amp; Time</th>
          <!-- <th width="10%" class="sortable">Capture Agent</th> -->
          <th width="15%" id="th-ProcessingStatus" class="sortable recording-Table-head">Status</th>
          <th width="15%">Actions</th>
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
        <a title="View Recording Info">
          <xsl:attribute name="href">/admin/viewevent.html?workflow=
            <xsl:value-of select="id" />
          </xsl:attribute>
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
      <!-- <td>
        <xsl:value-of select="captureAgent" />
      </td> -->
      <td class="processingStatus">
        <xsl:value-of select="processingStatus" />
      </td>
      <td class="td-Action">
        <!-- a>
          <xsl:attribute name="onclick">
            <xsl:text>Recordings.retryRecording('</xsl:text>
            <xsl:value-of select="id" />
            <xsl:text>');</xsl:text>
          </xsl:attribute>
          Re-try
        </a -->
        <xsl:choose>
          <xsl:when test="itemType='SCHEDULER_EVENT'">
            <a>
              <xsl:attribute name="onclick">
                <xsl:text>Recordings.removeSchedulerEvent('</xsl:text>
                <xsl:value-of select="id" />
                <xsl:text>');</xsl:text>
              </xsl:attribute>
          Delete
            </a>
          </xsl:when>
          <xsl:when test="itemType='WORKFLOW'">
            <a>
              <xsl:attribute name="onclick">
                <xsl:text>Recordings.removeRecording('</xsl:text>
                <xsl:value-of select="id" />
                <xsl:text>');</xsl:text>
              </xsl:attribute>
          Delete
            </a>
          </xsl:when>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
