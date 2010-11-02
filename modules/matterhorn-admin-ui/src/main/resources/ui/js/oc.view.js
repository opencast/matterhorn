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
 
var ocView = ocView || {};

var SCHEDULER_SERVICE_URL = '/scheduler/rest';
var WORKFLOW_SERVICE_URL  = '/workflow/rest';

ocView.init = function(){
  var id = ocUtils.getURLParam('id');
  var type = ocUtils.getURLParam('type');
  if(id && type === 'event'){
    $('head').append('<script id="eventJsonp" src="' + SCHEDULER_SERVICE_URL + '/event/' + id + '.json?jsonp=ocView.displayEvent"></script>');
  } else if(id && type === 'workflow') {
    $('head').append('<script id="workflowJsonp" src="' + WORKFLOW_SERVICE_URL + '/' + id + '.json?jsonp=ocView.displayWorkflow"></script>');
  }
};

ocView.internationalize = function(type){


};

ocView.displayEvent = function(event) {
  var eventView   = {};
  eventView.event = {};
  eventView.type  = 'scheduled event';
  for(i in event.event.metadataList.metadata){
    var metadata = event.event.metadataList.metadata[i]
    eventView.event[metadata.key] = metadata.value;
  }
  ocUtils.getTemplate('viewevent', function(template){
    try {
    var result = template.process(eventView);
    $('.layout-page-content').html(result);
    } catch(e) {
      console.log(e);
    }
  });
};

ocView.displayWorkflow = function(workflow) {
  /* TODO: Get mediapackage's episode.xml Dubline Core metadata and display it here. Should probably use XSLT */
  ocUtils.getTemplate('viewworkflow', function(template){
    template.process(event);
  });
};