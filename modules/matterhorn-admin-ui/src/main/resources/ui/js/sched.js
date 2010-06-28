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

// REST endpoints
var SCHEDULER_URL     = '/scheduler/rest';
var WORKFLOW_URL      = '/workflow/rest';
var CAPTURE_ADMIN_URL = '/capture-admin/rest';
var SERIES_URL        = '/series/rest';

// Constants
var CREATE_MODE       = 1;
var EDIT_MODE         = 2;
var SINGLE_EVENT      = 3;
var MULTIPLE_EVENTS   = 4;

// XML Namespaces
var SINGLE_EVENT_ROOT_ELM   = "event"
var MULTIPLE_EVENT_ROOT_ELM = "recurringEvent";


/* @namespace Scheduluer Form Namespace */
var Scheduler = Scheduler || {};
Scheduler.mode = CREATE_MODE;
Scheduler.type = SINGLE_EVENT;
Scheduler.selectedInputs = '';
/* @namespace Scheduluer UI Namespace */
var UI        = UI || {};
var Agent     = Agent || {};

Agent.tzDiff  = 0;

UI.Init = function(){
  ocWorkflow.init($('#workflow-selector'), $('#workflow-config-container'));
  UI.Internationalize();
  UI.RegisterComponents();
  UI.RegisterEventHandlers();

  Scheduler.FormManager = new AdminForm.Manager(SINGLE_EVENT_ROOT_ELM, '', Scheduler.components)
  
  if(Scheduler.type === SINGLE_EVENT){
    UI.agentList = '#agent';
    UI.inputList = '#input-list';
    $('#singleRecording').click(); //Initiates Page event cycle
  }else{
    UI.agentList = '#recurAgent';
    UI.inputList = '#recur-input-list';
    $('#multipleRecordings').click();
  }
  
  //Editing setup
  var eventId = AdminUI.getURLParams('eventId');
  if(eventId && AdminUI.getURLParams('edit')){
    document.title = i18n.window.prefix + " " + i18n.window.edit;
    $('#i18n_page_title').text(i18n.page.title.edit);
    $('#eventId').val(eventId);
    $('#recording-type').hide();
    $('#deleteButton').click(UI.DeleteForm);
    $('#delete-recording').show();
    $('#agent').change(
      function() {
        $('#notice-container').hide();
        $.get(CAPTURE_ADMIN_URL + '/agents/' + $('#agent option:selected').val(), UI.CheckAgentStatus);
      });
  }else{
    $.get(SCHEDULER_URL + '/uuid', function(data){
      AdminUI.log(data);
      $('#eventId').val(data.id);
    });
  }

};

UI.Internationalize = function(){
  //Do internationalization of text
  jQuery.i18n.properties({
    name:'scheduler',
    path:'i18n/'
  });
  AdminUI.internationalize(i18n, 'i18n');
  //Handle special cases like the window title.
  document.title = i18n.window.prefix + " " + i18n.window.schedule;
  $('#i18n_page_title').text(i18n.page.title.sched);
};

UI.RegisterEventHandlers = function(){
  var initializerDate, agent_list;
  initializerDate = new Date();
  initializerDate.setHours(initializerDate.getHours() + 1); //increment an hour.
  initializerDate.setMinutes(0);
  
  //UI Functional elements
  $('#singleRecording').click(function(){
    UI.ChangeRecordingType(SINGLE_EVENT);
  });
  $('#multipleRecordings').click(function(){
    UI.ChangeRecordingType(MULTIPLE_EVENTS);
  });
  
  $('.folder-head').click(
    function() {
      $(this).children('.fl-icon').toggleClass('icon-arrow-right');
      $(this).children('.fl-icon').toggleClass('icon-arrow-down');
      $(this).next().toggle('fast');
      return false;
    });
  
  $('.icon-help').click(
    function(event) {
      popupHelp.displayHelp($(this), event);
      return false;
    });
  
  $('body').click(
    function() {
      if ($('#helpbox').css('display') != 'none') {
        popupHelp.resetHelp();
      }
      return true;
    });
  
  $('#series_select').autocomplete({
    source: SERIES_URL + '/search',
    select: function(event, ui){
      $("#series").val(ui.item.id);
    },
    minLength: 3
  });
  
  $('#submitButton').click(UI.SubmitForm);
  $('#cancelButton').click(UI.CancelForm);

  //single recording specific elements
  $('#startTimeHour').val(initializerDate.getHours());
  $('#startDate').datepicker({
    showOn: 'both',
    buttonImage: 'shared_img/icons/calendar.gif',
    buttonImageOnly: true
  });
  $('#startDate').datepicker('setDate', initializerDate);
  $('#endDate').datepicker({
    showOn: 'both',
    buttonImage: 'shared_img/icons/calendar.gif',
    buttonImageOnly: true
  });
  
  $('#agent').change(UI.HandleAgentChange);
  
  //multiple recording specific elements
  $('#recurStart').datepicker({
    showOn: 'both',
    buttonImage: 'shared_img/icons/calendar.gif',
    buttonImageOnly: true
  });
  
  $('#recurEnd').datepicker({
    showOn: 'both',
    buttonImage: 'shared_img/icons/calendar.gif',
    buttonImageOnly: true
  });

  $('#recurAgent').change(UI.HandleAgentChange);
}

UI.ChangeRecordingType = function(recType){
  Scheduler.type = recType;
  
  UI.RegisterComponents();
  Scheduler.FormManager.components = Scheduler.components;
  
  $('.error').removeClass('error');
  $('#missingFields-container').hide();
  
  var d = new Date()
  d.setHours(d.getHours() + 1); //increment an hour.
  d.setMinutes(0);
  
  if(Scheduler.type == SINGLE_EVENT){
    $('#title-note').hide();  
    $('#recurring_recording').hide();
    $('#single_recording').show();
    UI.agentList = '#agent';
    UI.inputList = '#input-list';
    $(UI.inputList).empty();
    $('#series_required').remove(); //Remove series required indicator.
    Scheduler.components.timeStart.setValue(d.getTime().toString());
    Scheduler.FormManager.rootElm = SINGLE_EVENT_ROOT_ELM;
  }else{
    // Multiple recordings have some differnt fields and different behaviors
    //show recurring_recording panel, hide single.
    $('#title-note').show();
    $('#recurring_recording').show();
    $('#single_recording').hide();
    UI.agentList = '#recurAgent';
    UI.inputList = '#recur-input-list';
    $(UI.inputList).empty();
    $('#series_container > label').prepend('<span id="series_required" style="color: red;">*</span>'); //series is required, indicate as such.
    Scheduler.components.recurrenceStart.setValue(d.getTime().toString());
    Scheduler.FormManager.rootElm = MULTIPLE_EVENT_ROOT_ELM;
  }
  UI.LoadKnownAgents();
};

UI.SubmitForm = function(){
  var eventXML = null;
  eventXML = Scheduler.FormManager.serialize();
  if(eventXML){
    if(Scheduler.type === SINGLE_EVENT){
      if(AdminUI.getURLParams('edit')){
        $.post( SCHEDULER_URL + '/event', {
               event: eventXML
               }, UI.EventSubmitComplete );
      }else{
        $.ajax({
               type: "PUT",
               url: SCHEDULER_URL + '/event',
               data: {
               event: eventXML
               },
               success: UI.EventSubmitComplete
               });
      }
    }else{
      if(AdminUI.getURLParams('edit')){
        $.post( SCHEDULER_URL + '/event', {
               recurringEvent: eventXML
               }, UI.EventSubmitComplete );
      }else{
        $.ajax({
               type: "PUT",
               url: SCHEDULER_URL + '/recurrence',
               data: {
               recurringEvent: eventXML
               },
               success: UI.EventSubmitComplete
               });
      }
    }
  }
  return true;
};

UI.CancelForm = function(){
  document.location = 'recordings.html';
};

UI.DeleteForm = function(){
  var title, series, creator;
  if(confirm(i18n.del.confirm)){
    $.get(SCHEDULER_URL + '/removeEvent/' + $('#eventId').val(), function(){
      title = Scheduler.components.title.toString() || 'No Title';
      series = Scheduler.components.seriesId.toString() || 'No Series';
      creator = Scheduler.components.creator.toString() || 'No Creator';
      $('#i18n_del_msg').text(i18n.del.msg(title, series, '(' + creator + ')'));
      $('#stage').hide();
      $('#deleteBox').show();
    });
  }
};

UI.HandleAgentChange = function(elm){
  var agent = elm.target.value;
  $(UI.inputList).empty();
  if(agent){
    $.get('/capture-admin/rest/agents/' + agent + '/capabilities',
      function(doc){
        var capabilities = [];
        $.each($('entry', doc), function(a, i){
          var s = $(i).attr('key');
          if(s.indexOf('.src') != -1){
            var name = s.split('.');
            capabilities.push(name[2]);
          } else if(s == 'capture.device.timezone.offset') {
            var agent_tz = parseInt($(i).text());
            if(agent_tz !== 'NaN'){
              UI.HandleAgentTZ(agent_tz);
            }else{
              AdminUI.log("Couldn't parse TZ");
            }
          }
        });
        if(capabilities.length){
          UI.DisplayCapabilities(capabilities);
        }else{
          Agent.tzDiff = 0; //No agent timezone could be found, assume local time.
          $('#input-list').append('Agent defaults will be used.');
        }
      });
  }else{
    // no valid agent, change time to local form what ever it was before.
    var time = Scheduler.components.timeStart.getValue();
    Agent.tzDiff = 0;
    Scheduler.components.timeStart.setValue(time);
  }
};

UI.DisplayCapabilities = function(capabilities){
  $.each(capabilities, function(i, v){
    $(UI.inputList).append('<input type="checkbox" id="' + v + '" value="' + v + '" checked="checked"><label for="' + v +'">' + v.charAt(0).toUpperCase() + v.slice(1).toLowerCase() + '</label>');
  });
  Scheduler.components.resources.setFields(capabilities);
  if(Scheduler.selectedInputs && AdminUI.getURLParams('edit')){
    Scheduler.components.resources.setValue(Scheduler.selectedInputs);
  }
};

UI.HandleAgentTZ = function(tz){
  var agentLocalTime = null;
  var localTZ = -(new Date()).getTimezoneOffset(); //offsets in minutes
  Agent.tzDiff = 0;
  if(tz != localTZ){
    //Display note of agent TZ difference, all times local to capture agent.
    //update time picker to agent time
    Agent.tzDiff = tz - localTZ;
    agentLocalTime = Scheduler.components.timeStart.getValue() + (Agent.tzDiff * 60 * 1000);
    Scheduler.components.timeStart.setValue(agentLocalTime);
    diff = Math.round((Agent.tzDiff/60)*100)/100;
    if(diff < 0){
      postfix = " hours earlier";
    }else if(diff > 0){
      postfix = " hours later"; 
    }
    $('#notice-container').show();
    $('#notice-tzdiff').show();
    $('#tzdiff').replaceWith(Math.abs(diff) + postfix);
  }
};

UI.CheckAgentStatus = function(doc){
  var state = $('state', doc).text();
  if(state == '' || state == 'unknown' || state == 'offline') {
    $('#notice-container').show();
    $('#notice-offline').show();
  }
};

/**
 *  loadKnownAgents calls the capture-admin service to get a list of known agents.
 *  Calls handleAgentList to populate the dropdown.
 */
UI.LoadKnownAgents = function() {
  $(UI.agentList).empty();
  $(UI.agentList).append($('<option></option>').val('').html('Choose one:'));
  $.get(CAPTURE_ADMIN_URL + '/agents', UI.HandleAgentList, 'xml');
};

/**
 *  Popluates dropdown with known agents
 *
 *  @param {XML Document}
 */
UI.HandleAgentList = function(data) {
  $.each($('name', data),
    function(i, agent) {
      $(UI.agentList).append($('<option></option>').val($(agent).text()).html($(agent).text())); 
    });
  var eventId = AdminUI.getURLParams('eventId');
  if(eventId && AdminUI.getURLParams('edit')) {
    $.get(SCHEDULER_URL + '/event/' + eventId, UI.LoadEvent);
  }
};

UI.LoadEvent = function(doc){
  var metadata = {};
  $.each($('metadataList > metadata',doc), function(i,v){
    metadata[$('key', v).text()] = $('value', v).text();
  });
  $.each($('completeMetadata > metadata',doc), function(i,v){
    if(metadata[$('key', v).text()] == undefined){
      //feild not in list, add it.
      metadata[$('key', v).text()] = $('value', v).text();
    }
  });
  if(metadata['resources']){
    //store the selected inputs for use when getting the capabilities.
    Scheduler.selectedInputs = metadata['resources'];
  }
  if(metadata['seriesId']){
    $.get(SERIES_URL + '/search?term=' + metadata['seriesId'], function(data){
      Scheduler.components.seriesId.setValue(data[0]);
    });
  }
  if(metadata['recurrenceId'] && metadata['recurrencePosition']){
    Scheduler.components.recurrenceId = new AdminForm.Component(['recurrenceId']);
    Scheduler.components.recurrencePosition = new AdminForm.Component(['recurrencePosition']);
  }
  Scheduler.FormManager.populate(metadata)
  $('#agent').change(); //update the selected agent's capabilities
}

UI.EventSubmitComplete = function(){
  for(var k in Scheduler.components){
    AdminUI.log("#data-" + k)
    $("#data-" + k).show();
    //$("#data-" + k + " > .data-label").text(i18n[k].label + ":");
    $("#data-" + k + " > .data-value").text(Scheduler.components[k].toString());
  }
  $("#submission_success").siblings().hide();
  $("#submission_success").show();
}

UI.RegisterComponents = function(){
  Scheduler.components = {};
  
  Scheduler.components.title = new AdminForm.Component(['title'], {label: 'label-title', required: true});
  Scheduler.components.creator = new AdminForm.Component(['creator'], {label: 'label-creator'});
  Scheduler.components.contributor = new AdminForm.Component(['contributor'], {label: 'label-contributor'});
  Scheduler.components.seriesId = new AdminForm.Component(['series', 'series_select'],
    { label: 'label-series', errorField: 'missing-series', nodeKey: 'seriesId' },
    { getValue: function(){ 
        if(this.fields.series){
          this.value = this.fields.series.val();
        }
        return this.value;
      },
      setValue: function(value){
        this.fields.series.val(value.id);
        this.fields.series_select.val(value.label)
      },
      toString: function(){
        if(this.fields.series_select){
          return this.fields.series_select.val();
        }
        return this.getValue();
      }
    });
  Scheduler.components.subject = new AdminForm.Component(['subject'], {label: 'label-subject'});
  Scheduler.components.language = new AdminForm.Component(['language'], {label: 'label-subject'});
  Scheduler.components.description = new AdminForm.Component(['description'], {label: 'label-description'});
  Scheduler.components.license = new AdminForm.Component(['license'], {label: 'label-license'});
  Scheduler.components.resources = new AdminForm.Component([],
    { label: 'label-inputs', errorField: 'none', nodeKey: 'resources' },
    { getValue: function(){
        var selected = [];
        for(var el in this.fields){
          var e = this.fields[el];
          if(e[0] && e[0].checked){
            selected.push(e.val());
          }
        }
        this.value = selected.toString();
        return this.value;
      },
      setValue: function(value){
        if(typeof value == 'string'){
          value = { resources: value };
        }
        for(var el in this.fields){
          var e = this.fields[el];
          if(e[0] && value.resources.toLowerCase().indexOf(e.val().toLowerCase()) != -1){
            e[0].checked = true;
          }else{
            e[0].checked = false;
          }
        }
      },
      validate: function(){
        var checked = false;
        for(var el in this.fields){
          if(this.fields[el][0].checked){
            checked = true;
            break;
          }
        }
        return checked;
      }
    });
  if(Scheduler.type === MULTIPLE_EVENTS){
    //Scheduler.components.recurringEventId = new AdminForm.Component(['eventId']);
    Scheduler.components.seriesId.required = true; //Series are required for groups of recordings.
    Scheduler.components.recurrenceStart = new AdminForm.Component(['recurStart', 'recurStartTimeHour', 'recurStartTimeMin'],
      { label: 'label-recurrstart', errorField: 'missing-startdate', required: true, nodeKey: 'recurrenceStart' },
      { getValue: function(){
          var date, start;
          if(this.validate()){
            date = this.fields.recurStart.datepicker('getDate');
            if(date && date.constructor == Date){
              start = date / 1000; // Get date in milliseconds, convert to seconds.
              start += this.fields.recurStartTimeHour.val() * 3600; // convert hour to seconds, add to date.
              start += this.fields.recurStartTimeMin.val() * 60; //convert minutes to seconds, add to date.
              start -= Agent.tzDiff * 60; //Agent TZ offset
              start = start * 1000; //back to milliseconds
              this.value = start;
            }
          }
          return this.value;
        },
        setValue: function(value){
          var date;
          if(typeof value === 'string'){
            value = { startdate: value };
          }
          date = parseInt(value.startdate);
          if(date != 'NaN') {
            AdminUI.log('date: ' + date);
            date = new Date(date + (Agent.tzDiff * 60 * 1000));
            AdminUI.log('date + offset: ' + date);
          } else {
            AdminUI.log('Could not parse date.');
          }
          if(this.fields.recurStart && this.fields.recurStartTimeHour && this.fields.recurStartTimeMin){
            this.fields.recurStartTimeHour.val(date.getHours());
            this.fields.recurStartTimeMin.val(date.getMinutes());
            this.fields.recurStart.datepicker('setDate', date); //datepicker modifies the date object removing the time.
          }
        },
        validate: function(){
          var date, now, startdatetime;
          if(this.fields.recurStart.datepicker){
            date = this.fields.recurStart.datepicker('getDate');
            now = (new Date()).getTime();
            now += Agent.tzDiff  * 60 * 1000; //Offset by the difference between local and client.
            now = new Date(now);
            if(date && this.fields.recurStartTimeHour && this.fields.recurStartTimeMin){
              startdatetime = new Date(date.getFullYear(), date.getMonth(), date.getDate(), this.fields.recurStartTimeHour.val(), this.fields.recurStartTimeMin.val());
              if(startdatetime.getTime() >= now.getTime()) {
                return true;
              }
            }
          }
          return false;
        },
        toString: function(){
          return (new Date(this.getValue())).toLocaleString();
        }
      });
    
    Scheduler.components.recurrenceDuration = new AdminForm.Component(['recurDurationHour', 'recurDurationMin'],
      { label: 'label-recurduration', errorField: 'missing-duration', required: true, nodeKey: 'recurrenceDuration' },
      { getValue: function(){
          if(this.validate()){
            duration = this.fields.recurDurationHour.val() * 3600; // seconds per hour
            duration += this.fields.recurDurationMin.val() * 60; // seconds per min
            this.value = (duration * 1000);
          }
          return this.value;
        },
        setValue: function(value){
          var val, hour, min;
          if(typeof value === 'string'){
            value = { duration: value };
          }
          val = parseInt(value.duration);
          if(val === 'NaN') {
            AdminUI.log('Could not parse duration.');
          }
          if(this.fields.recurDurationHour && this.fields.recurDurationMin){
            val   = val/1000; //milliseconds -> seconds
            hour  = Math.floor(val/3600);
            min   = Math.floor((val/60) % 60);
            this.fields.recurDurationHour.val(hour);
            this.fields.recurDurationMin.val(min);
          }
        },
        validate: function(){
          if(this.fields.recurDurationHour && this.fields.recurDurationMin && (this.fields.recurDurationHour.val() != '0' || this.fields.recurDurationMin.val() != '0')){
            return true;
          }
          return false;
        },
        toString: function(){
          var dur = this.getValue() / 1000;
          var hours = Math.floor(dur / 3600);
          var min   = Math.floor( ( dur /60 ) % 60 );
          return hours + ' hours, ' + min + ' minutes';
        }
      });

    Scheduler.components.recurrenceEnd = new AdminForm.Component(['recurEnd', 'recurStart', 'recurStartTimeHour', 'recurStartTimeMin'],
      { label: 'label-recurend', errorField: 'error-recurstart-end', required: true, nodeKey: 'recurrenceEnd' },
      { getValue: function(){
          var date, end;
          if(this.validate()){
            date = this.fields.recurEnd.datepicker('getDate');
            if(date && date.constructor === Date){
              end = date.getTime() / 1000; // Get date in milliseconds, convert to seconds.
              end += this.fields.recurStartTimeHour.val() * 3600; // convert hour to seconds, add to date.
              end += this.fields.recurStartTimeMin.val() * 60; //convert minutes to seconds, add to date.
              end -= Agent.tzDiff * 60; //Agent TZ offset
              end = end * 1000; //back to milliseconds
              end += Scheduler.components.recurrenceDuration.getValue(); //Add to duration start time for end time.
              this.value = end;
            }
          }
          return this.value;
        },
        setValue: function(value){
          var val = parseInt(value);
          if(val == 'NaN'){
            this.fields.recurEnd.datepicker('setDate', new Date(val));
          }
        },
        validate: function(){
          if(this.fields.recurEnd.datepicker && this.fields.recurStart.datepicker && Scheduler.components.recurrenceDuration.validate() &&
             this.fields.recurStartTimeHour && this.fields.recurStartTimeMin &&
             this.fields.recurEnd.datepicker('getDate') > this.fields.recurStart.datepicker('getDate')){
            return true;
          }
          return false;
        },
        toString: function(){
          return (new Date(this.getValue())).toLocaleString();
        }
      });

    Scheduler.components.device = new AdminForm.Component(['recurAgent'],
      { label: 'label-recurAgent', errorField: 'missing-agent', required: true, nodeKey: 'device' },
      { getValue: function(){
          if(this.fields.recurAgent) {
            this.value = this.fields.recurAgent.val();
          }
          return this.value;
        },
        setValue: function(value){
          var opts, agentId, found;
          if(typeof value === 'string'){
            value = { agent: value };
          }
          opts = this.fields.recurAgent.children();
          agentId = value.agent;
          if(opts.length > 0) {
            found = false;
            for(var i = 0; i < opts.length; i++) {
              if(opts[i].value == agentId) {
                found = true;
                opts[i].selected = true;
                break;
              }
            }
            if(!found){ //Couldn't find the previsouly selected agent, add to list and notifiy user.
              this.fields.recurAgent.append($('<option selected="selected">' + agentId + '</option>').val(agentId));
              $('#recurAgent').change();
            }
            this.fields.recurAgent.val(agentId);
          }
        },
        validate: function(){
          if(this.getValue()) {
            return true;
          }
          return false;
        }
      });

    Scheduler.components.recurrence = new AdminForm.Component(['schedule_repeat', 'repeat_sun', 'repeat_mon', 'repeat_tue', 'repeat_wed', 'repeat_thu', 'repeat_fri', 'repeat_sat'],
      { label: 'i18n_sched_days', errorField: 'error-recurrence', required: true, nodeKey: 'recurrence' },
      { getValue: function(){
          var rrule, dotw, days, date, hour, min, dayOffset;
          if(this.validate()){
            if(this.fields.schedule_repeat.val() == 'weekly'){
              rrule     = "FREQ=WEEKLY;BYDAY=";
              dotw      = ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA'];
              days      = [];
              date      = new Date(Scheduler.components.recurrenceStart.getValue());
              hour      = date.getUTCHours();
              min       = date.getUTCMinutes();
              dayOffset = 0;
              if(date.getDay() != date.getUTCDay()){
                dayOffset = date.getDay() < date.getUTCDay() ? 1 : -1;
              }
              AdminUI.log(dayOffset);
              if(this.fields.repeat_sun[0].checked){
                days.push(dotw[(0 + dayOffset) % 7]);
              }
              if(this.fields.repeat_mon[0].checked){
                days.push(dotw[(1 + dayOffset) % 7]);
              }
              if(this.fields.repeat_tue[0].checked){
                days.push(dotw[(2 + dayOffset) % 7]);
              }
              if(this.fields.repeat_wed[0].checked){
                days.push(dotw[(3 + dayOffset) % 7]);
              }
              if(this.fields.repeat_thu[0].checked){
                days.push(dotw[(4 + dayOffset) % 7]);
              }
              if(this.fields.repeat_fri[0].checked){
                days.push(dotw[(5 + dayOffset) % 7]);
              }
              if(this.fields.repeat_sat[0].checked){
                days.push(dotw[(6 + dayOffset) % 7]);
              }
              this.value = rrule + days.toString() + ";BYHOUR=" + hour + ";BYMINUTE=" + min;
            }
          }
          return this.value;
        },
        setValue: function(value){
          //to do, handle day offset.
          if(typeof value == 'string'){
            value = { rrule: value };
          }
          if(value.rrule.indexOf('FREQ=WEEKLY') != -1){
            this.fields.schedule_repeat.val('weekly');
            var days = value.rrule.split('BYDAY=');
            if(days[1].length > 0){
              days = days[1].split(',');
              this.fields.repeat_sun[0].checked = this.fields.repeat_mon[0].checked = this.fields.repeat_tue[0].checked = this.fields.repeat_wed[0].checked = this.fields.repeat_thu[0].checked = this.fields.repeat_fri[0].checked = this.fields.repeat_sat[0].checked = false;
              for(d in days){
                switch(days[d]){
                  case 'SU':
                    this.fields.repeat_sun[0].checked = true;
                    break;
                  case 'MO':
                    this.fields.repeat_mon[0].checked = true;
                    break;
                  case 'TU':
                    this.fields.repeat_tue[0].checked = true;
                    break;
                  case 'WE':
                    this.fields.repeat_wed[0].checked = true;
                    break;
                  case 'TH':
                    this.fields.repeat_thu[0].checked = true;
                    break;
                  case 'FR':
                    this.fields.repeat_fri[0].checked = true;
                    break;
                  case 'SA':
                    this.fields.repeat_sat[0].checked = true;
                    break;
                }
              }
            }
          }
        },
        validate: function(){
          if(this.fields.schedule_repeat.val() != 'norepeat'){
            if(this.fields.repeat_sun[0].checked ||
               this.fields.repeat_mon[0].checked ||
               this.fields.repeat_tue[0].checked ||
               this.fields.repeat_wed[0].checked ||
               this.fields.repeat_thu[0].checked ||
               this.fields.repeat_fri[0].checked ||
               this.fields.repeat_sat[0].checked ){
              if(Scheduler.components.recurrenceStart.validate() &&
                 Scheduler.components.recurrenceDuration.validate() &&
                 Scheduler.components.recurrenceEnd.validate()){
                return true;
              }
            }
          }
          return false;
        },
        toNode: function(parent){
          for(var el in this.fields){
            var container = parent.ownerDocument.createElement(this.nodeKey);
            container.appendChild(parent.ownerDocument.createTextNode(this.getValue()));
          }
          if(parent && parent.nodeType){
            parent.appendChild(container);
          }
          return container;
        }
      });
                                                                        
  }else{ //Single Event
    
    Scheduler.components.eventId = new AdminForm.Component(['eventId'],
      { nodeKey: 'eventId' },
      { toNode: function(parent) {
          for(var el in this.fields){
            var container = parent.ownerDocument.createElement(this.nodeKey);
            container.appendChild(parent.ownerDocument.createTextNode(this.getValue()));
          }
          if(parent && parent.nodeType){
            parent.appendChild(container);
          }
          return container;
        }
      });
    Scheduler.components.timeStart = new AdminForm.Component(['startDate', 'startTimeHour', 'startTimeMin'],
      { label: 'label-startdate', errorField: 'missing-startdate', required: true, nodeKey: 'timeStart' },
      { getValue: function(){
          var date = 0;
          date = this.fields.startDate.datepicker('getDate').getTime() / 1000; // Get date in milliseconds, convert to seconds.
          date += this.fields.startTimeHour.val() * 3600; // convert hour to seconds, add to date.
          date += this.fields.startTimeMin.val() * 60; //convert minutes to seconds, add to date.
          date -= Agent.tzDiff * 60; //Agent TZ offset
          date = date * 1000; //back to milliseconds
          return (new Date(date)).getTime();
        },
        setValue: function(value){
          var date, hour;
          date = parseInt(value);
          
          if(date != 'NaN') {
            date = new Date(date + (Agent.tzDiff * 60 * 1000));
          } else {
            AdminUI.log('Could not parse date.');
          }
          if(this.fields.startDate && this.fields.startTimeHour && this.fields.startTimeMin){
            hour = date.getHours();
            this.fields.startTimeHour.val(hour);
            this.fields.startTimeMin.val(date.getMinutes());
            this.fields.startDate.datepicker('setDate', date);//datepicker modifies the date object removing the time.
          }
        },
        validate: function(){
          var date, now, startdatetime;
          date = this.fields.startDate.datepicker('getDate');
          now = (new Date()).getTime();
          now += Agent.tzDiff  * 60 * 1000; //Offset by the difference between local and client.
          now = new Date(now);
          if(this.fields.startDate && date && this.fields.startTimeHour && this.fields.startTimeMin){
            startdatetime = new Date(date.getFullYear(), 
                                     date.getMonth(),
                                     date.getDate(),
                                     this.fields.startTimeHour.val(),
                                     this.fields.startTimeMin.val());
            if(startdatetime.getTime() >= now.getTime()){
              return true;
            }
            return false;
          }
        },
        toString: function(){
          return (new Date(this.getValue())).toLocaleString();
        }
      });

    Scheduler.components.timeDuration = new AdminForm.Component(['durationHour', 'durationMin'],
      { label: 'label-duration', errorField: 'missing-duration', required: true, nodeKey: 'timeEnd' },
      { getValue: function(){
          if(this.validate()){
            duration = this.fields.durationHour.val() * 3600; // seconds per hour
            duration += this.fields.durationMin.val() * 60; // seconds per min
            this.value = Scheduler.components.timeStart.getValue() + (duration * 1000);
          }
          return this.value;
        },
        setValue: function(value){
          var val, hour, min;
          if(typeof value === 'string'){
            value = { duration: value };
          }
          val = parseInt(value.duration);
          if(val == 'NaN') {
            AdminUI.log('Could not parse duration.');
          }
          if(this.fields.durationHour && this.fields.durationMin){
            val = val/1000; //milliseconds -> seconds
            hour  = Math.floor(val/3600);
            min   = Math.floor((val/60) % 60);
            this.fields.durationHour.val(hour);
            this.fields.durationMin.val(min);
          }
        },
        validate: function(){
          if(this.fields.durationHour && this.fields.durationMin && (this.fields.durationHour.val() !== '0' || this.fields.durationMin.val() !== '0')){
            return true;
          }
          return false;
        },
        toString: function(){
          var dur = this.getValue() / 1000;
          var hours = Math.floor(dur / 3600);
          var min   = Math.floor( ( dur /60 ) % 60 );
          return hours + ' hours, ' + min + ' minutes';
        }
      });

    Scheduler.components.device = new AdminForm.Component(['agent'],
      { label: 'label-agent', errorField: 'missing-agent', required: true, nodeKey: 'device' },
      { getValue: function(){
          if(this.fields.agent){
            this.value = this.fields.agent.val();
          }
          return this.value;
        },
        setValue: function(value){
          var opts, agentId, found;
          if(typeof value === 'string'){
            value = { agent: value };
          }
          opts = this.fields.agent.children();
          agentId = value.agent;
          if(opts.length > 0){
            found = false;
            for(var i = 0; i < opts.length; i++){
              if(opts[i].value == agentId){
                found = true;
                opts[i].selected = true;
                break;
              }
            }
            if(!found){ //Couldn't find the previsouly selected agent, add to list and notifiy user.
              this.fields.agent.append($('<option selected="selected">' + agentId + '</option>').val(agentId));
              $('#agent').change();
            }
            this.fields.agent.val(agentId);
          }
        },
        validate: function(){
          if(this.getValue()) {
            return true;
          }
          return false;
        }
      });
  }
}

UI.toICalDate = function(d){
  if(d.constructor != Date){
    d = new Date(0);
  }
  var month = UI.padstring(d.getUTCMonth() + 1, '0', 2);
  var hours = UI.padstring(d.getUTCHours(), '0', 2);
  var minutes = UI.padstring(d.getUTCMinutes(), '0', 2);
  var seconds = UI.padstring(d.getUTCSeconds(), '0', 2);
  return '' + d.getUTCFullYear() + month + d.getUTCDate() + 'T' + hours + minutes + seconds + 'Z';
}

UI.padstring = function(str, pad, padlen){
  if(typeof str != 'string'){ 
    str = str.toString();
  }
  while(str.length < padlen && pad.length > 0){
    str = pad + str;
  }
  return str;
}
