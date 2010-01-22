/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

//eventDoc contains a reference to the XML document which is built through various AJAX callbacks.
var eventDoc = null;
//WorkflowID is parsed from the URL parameters and is the ID of the workflow used to gather the required metadata.
var workflowID = null;

var debugDoc = null;
/**
 *  handleWorkflow parses the workflow xml, grabs the URLs of the additional metadata catalogs
 *  and makes additional ajax calls to bring in the metadata.
 *
 *  @param {XML Document}
 */
function handleWorkflow(workflowDoc){
  //console.log(workflowDoc);
  eventDoc = createDoc();
  var dcURL = $(workflowDoc.documentElement).find("metadata:first > catalog[type='metadata/dublincore'] url").text();
  //TODO: get extra metadata catalog URL
  //var caURL = $("ns2\\:workflow-instance", testDoc).find("metadata:first > catalog[type='metadata/extra'] url").text();
  //console.log($("ns2\\:workflow-instance", workflowDoc));
  
  eventid = eventDoc.createElement('event-id');
  eventid.appendChild(eventDoc.createTextNode($(workflowDoc.documentElement).attr('id')));
  eventDoc.documentElement.appendChild(eventid);
  //console.log(eventid);
  
  startdate = eventDoc.createElement('startdate');
  start = parseDateTime($(workflowDoc.documentElement).find('mediapackage').attr('start'));
  startdate.appendChild(eventDoc.createTextNode(start));
  
  eventDoc.documentElement.appendChild(startdate);
  //console.log(startdate);
  
  duration = eventDoc.createElement('duration');
  duration.appendChild(eventDoc.createTextNode(parseDuration(parseInt($(workflowDoc.documentElement).find('mediapackage').attr('duration')))));
  eventDoc.documentElement.appendChild(duration);
  //console.log(duration);
  
  $.get(dcURL, handleDCMetadata);
  //TODO: Get the extra metadata catalog, we need the Agent's id and the inputs selected.
  //$.get(caURL, handleCatalogMetadata);
}

/**
 *  handleDCMetadata handles the parsing of Dublin Core metadata catalog, before transforming and appending
 *  the data to the page.
 *  
 *  @param {XML Document}
 */
function handleDCMetadata(metadataDoc){
  debugDoc = metadataDoc;
  //TODO: This is a fast to code, but poor method of loading our values. Refactor.
  title = eventDoc.createElement('title');
  title.appendChild(eventDoc.createTextNode($('dcterms\\:title', metadataDoc).text()));
  eventDoc.documentElement.appendChild(title);
  //console.log(title);
  
  creator = eventDoc.createElement('creator');
  creator.appendChild(eventDoc.createTextNode($('dcterms\\:creator', metadataDoc).text()))
  eventDoc.documentElement.appendChild(creator);
  //console.log(creator);
  
  contributor = eventDoc.createElement('contributor');
  contributor.appendChild(eventDoc.createTextNode($('dcterms\\:contributor', metadataDoc).text()))
  eventDoc.documentElement.appendChild(contributor);
  //console.log(contributor);
  
  description = eventDoc.createElement('description');
  description.appendChild(eventDoc.createTextNode($('dcterms\\:description', metadataDoc).text()))
  eventDoc.documentElement.appendChild(description);
  //console.log(description);
  
  series = eventDoc.createElement('series');
  series.appendChild(eventDoc.createTextNode($('dcterms\\:isPartOf', metadataDoc).text()))
  eventDoc.documentElement.appendChild(series);
  //console.log(series);
  
  license = eventDoc.createElement('license');
  license.appendChild(eventDoc.createTextNode($('dcterms\\:license', metadataDoc).text()))
  eventDoc.documentElement.appendChild(license);
  //console.log(license);
  
  language = eventDoc.createElement('language');
  language.appendChild(eventDoc.createTextNode($('dcterms\\:language', metadataDoc).text()))
  eventDoc.documentElement.appendChild(language);
  //console.log(language);
  
  subject = eventDoc.createElement('subject');
  subject.appendChild(eventDoc.createTextNode($('dcterms\\:subject', metadataDoc).text()))
  eventDoc.documentElement.appendChild(subject);
  //console.log(subject);
  
  //Hopefully we've loaded an xml document with the values we want, transform and append this.
  doc = serialize(eventDoc);
  //console.log(doc);
  if(doc){
    $('#stage').xslt(doc, "xsl/viewevent.xsl", callback);
  }
}

function handleCatalogMetadata(metadataDoc){
  inputs = eventDoc.createElement('inputs');
  inputs.appendChild(eventDoc.createTextNode($(metadataDoc.documentElement).attr('id')));
  eventDoc.documentElement.appendChild(inputs);
  
  agent = eventDoc.createElement('agent');
  agent.appendChild(eventDoc.createTextNode($(metadataDoc.documentElement).attr('id')));
  eventDoc.documentElement.appendChild(agent);
}

/**
 *  callback is called after view metadata is transformed and attached to the page.
 *  it does some display setup.
 */
function callback(){
  $('.folder-head').click(
                          function(){
                          $(this).children('.fl-icon').toggleClass('icon-arrow-right');
                          $(this).children('.fl-icon').toggleClass('icon-arrow-down');
                          $(this).next().toggle('fast');
                          return false;
                          }
                          );
}

/**
 *  Returns a new xml document for FF or IE
 *
 *  @return {XML Document}
 */
function createDoc(){
  var doc = null;
  //Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
  if(document.implementation && document.implementation.createDocument){ //Firefox, Opera, Safari, Chrome, etc.
    doc = document.implementation.createDocument("", "event", null);
  }else{ // IE
    doc = new ActiveXObject('MSXML2.DOMDocument');
    doc.loadXML('<event></event>');
  }
  return doc;
}

/**
 *  Returns a string of a serialized XML Document
 *
 *  @param {XML Document}
 *  @return {String}
 */
function serialize(doc){
  if(typeof XMLSerializer != 'undefined'){
    return (new XMLSerializer()).serializeToString(doc);
  }else if(doc.xml){
    return doc.xml;
  }
  return false;
}

function parseDuration(dur){
  dur = dur / 1000;
  var hours = Math.floor(dur / 3600);
  var min   = Math.floor( ( dur /60 ) % 60 );
  return hours + " hours, " + min + " minutes";
}

function parseDateTime(datetime){
  if(datetime){
    //expects date in ical format of YYYY-MM-DDTHH:MM:SS e.g. 2007-12-05T13:40:00
    datetime = datetime.split("T");
    var date = datetime[0].split("-");
    var time = datetime[1].split(":");
    return new Date(date[0], (date[1] - 1), date[2], time[0], time[1], time[2]).toLocaleString();
  }
  return new Date(0);
}
