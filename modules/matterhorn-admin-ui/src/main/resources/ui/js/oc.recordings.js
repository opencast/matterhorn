ocRecordings = new (function() {

  var WORKFLOW_LIST_URL = '../workflow/instances.json';          // URL of workflow instances list endpoint
  var WORKFLOW_INSTANCE_URL = '';                                // URL of workflow instance endpoint
  var WORKFLOW_STATISTICS_URL = '../workflow/statistics.json';   // URL of workflow instances statistics endpoint
  var SERIES_URL = '/series'

  var STATISTICS_DELAY = 3000;     // time interval for statistics update

  var SORT_FIELDS = {
    'Title' : 'TITLE',
    'Presenter' : 'CREATOR',
    'Series' : 'SERIES_TITLE',
    'Date' : 'DATE_CREATED'
  }

  var FILTER_FIELDS = [
  {
    q : 'Any fields',
    title : 'Title',
    seriestitle : 'Series',
    creator : 'Presenter'
  },
  {
    subject : 'Subject',
    language : 'Language',
    contributor : 'Contributor',
    license : 'License'
  }
  ]

  this.totalRecordings = 0;
  this.numSelectedRecordings = 0;
  this.changedBulkEditFields = {};

  // components
  this.searchbox = null;
  this.pager = null;

  this.data = null;     // currently displayed recording data
  this.statistics = null;

  var refreshing = false;      // indicates if JSONP requesting recording data is in progress
  this.refreshingStats = false; // indicates if JSONP requesting statistics data is in progress
  this.refreshInterval = null;
  this.statsInterval = null;
  
  this.bulkEditComponents = {};

  // object that holds the workflow and the operation object for the hold state UI currently displayed
  this.Hold = {
    workflow : null,
    operation : null,
    changedMediaPackage : null
  }

  /** Executed directly when script is loaded: parses url parameters and
   *  returns the configuration object.
   */
  this.Configuration = new (function() {

    // default configuartion
    this.state = 'all';
    this.pageSize = 10;
    this.page = 0;
    this.refresh = 5;
    this.doRefresh = true;
    this.sortField = 'Date';
    this.sortOrder = 'DESC';
    this.filterField = null;
    this.filterText = '';
    
    this.lastState = 'all'
    this.lastPageSize = 10;
    this.lastPage = 0;

    // parse url parameters
    try {
      var p = document.location.href.split('?', 2)[1] || false;
      if (p !== false) {
        p = p.split('&');
        for (i in p) {
          var param = p[i].split('=');
          if (this[param[0]] !== undefined) {
            this[param[0]] = unescape(param[1]);
          }
        }
      }
    } catch (e) {
      alert('Unable to parse url parameters:\n' + e.toString());
    }

    return this;
  })();

  /** Initiate new JSONP call to workflow instances list endpoint
   */
  function refresh() {
    if (!refreshing) {
      refreshing = true;
      var params = [];
      // 'state' to display
      var state = ocRecordings.Configuration.state;
      params.push('state=-stopped');
      if (state == 'upcoming') {
        params.push('state=paused');
        params.push('state=running');
        params.push('op=schedule');

        // filter out events that are in the past
        var now = ocUtils.toISODate(new Date());
        params.push('fromdate=' + now);
      }
      else if (state == 'capturing') {
        params.push('state=paused');
        params.push('op=capture');
        params.push('op=ingest');
      }
      else if (state == 'processing') {
        params.push('state=running');
        params.push('op=-schedule');
        params.push('op=-capture');
      }
      else if (state == 'finished') {
        params.push('state=succeeded');
        params.push('op=-schedule');
        params.push('op=-capture');
      }
      else if (state == 'hold') {
        params.push('state=paused');
        params.push('op=-schedule');
        params.push('op=-capture');
        params.push('op=-ingest');
      }
      else if (state == 'failed') {
        params.push('state=failed');
        params.push('state=failing');
      }
      else if (state === 'bulkedit' || state === 'bulkdelete') {
        ocRecordings.Configuration.pageSize = 100;
        ocRecordings.Configuration.page = 0;
        params.push('state=paused');
        params.push('state=running');
        params.push('op=schedule');
      }
      // sorting if specified
      if (ocRecordings.Configuration.sortField != null) {
        var sort = SORT_FIELDS[ocRecordings.Configuration.sortField];
        if (ocRecordings.Configuration.sortOrder == 'DESC') {
          sort += "_DESC";
        }
        params.push('sort=' + sort);
      }
      // filtering if specified
      if (ocRecordings.Configuration.filterText != '') {
        params.push(ocRecordings.Configuration.filterField + '=' + encodeURI(ocRecordings.Configuration.filterText));
      }
      // paging
      params.push('count=' + ocRecordings.Configuration.pageSize);
      params.push('startPage=' + ocRecordings.Configuration.page);
      params.push('jsonp=?');
      var url = WORKFLOW_LIST_URL + '?' + params.join('&');
      $.ajax(
      {
        url: url,
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success: function (data)
        {
          ocRecordings.render(data);
        }
      });
    }
  }

  function refreshStatistics() {
    if (!ocRecordings.refreshingStats) {
      ocRecordings.refreshingStats = true;
      $.ajax(
      {
        url: WORKFLOW_STATISTICS_URL,
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success: ocRecordings.updateStatistics
      });
    }
  }

  /** JSPON callback for statistics data requests. Translate numbers delivered
   *   by the statistics endpoint:
   *
   *  - upcoming : definition=scheduling, state=running + state=paused + state=instantiated
   *  - capturing: definition=capturing, state=running
   *  - processing: definition:all other than scheduling,capture, state=running
   *  - finished: definition:all other than scheduling,capture, state=succeeded
   *  - on hold: definition:all other than scheduling,capture, state=paused
   *  - failed: from summary -> failed  (assuming that scheduling goes into
   *      FAILED when recording was not started, capture goes into FAILED when
   *      the capture error occured etc.)
   *  - all: sum of the above
   */
  this.updateStatistics = function(data) {
    ocRecordings.refreshingStats = false;
    var stats = {
      all: 0,
      upcoming:0,
      capturing:0,
      processing:0,
      finished:0,
      hold:0,
      failed:0
    };

    if (data.statistics.definitions.definition !== undefined) {
      if ($.isArray(data.statistics.definitions.definition)) {
        $.each(data.statistics.definitions.definition, function(index, definition) {
          addStatistics(definition, stats)
        });
      } else {
        addStatistics(data.statistics.definitions.definition, stats);
      }
    }
    
    stats.all = stats.upcoming + stats.capturing + stats.processing + stats.finished + stats.failed + stats.hold;
    if (ocRecordings.statistics != null
      && ocRecordings.statistics[ocRecordings.Configuration.state] != stats[ocRecordings.Configuration.state]) {
      refresh();
    }
    ocRecordings.statistics = stats;
    displayStatistics();
  }

  /** Called by updateStatistics to add the numbers form one definition statistic
   *  to the statistics summary
   */
  function addStatistics(definition, stats) {
    if (definition.id == 'scheduling') {
      stats.failed += parseInt(definition.failed) + parseInt(definition.failing);
      definition.operations.operation = ocUtils.ensureArray(definition.operations.operation);
      $.each(definition.operations.operation, function(index, op) {
        if (op.id == 'schedule') {
          stats.upcoming = parseInt(op.instantiated) + parseInt(op.running) + parseInt(op.paused);
        } else if (op.id == 'capture' || op.id == 'ingest') {
          stats.capturing += parseInt(op.running) + parseInt(op.paused);
        }
      });
    } else {
      stats.processing += parseInt(definition.running);
      stats.finished += parseInt(definition.finished);
      stats.hold += parseInt(definition.paused);
      stats.failed += parseInt(definition.failed) + parseInt(definition.failing);
    }
  }

  function displayStatistics() {
    $.each(ocRecordings.statistics, function(key, value) {
      $('#stats-' + key).text(' (' + value + ')');
    });
  }
  
  this.startStatisticsUpdate = function() {
    refreshStatistics();
    if(ocRecordings.statsInterval == null) {
      ocRecordings.statsInterval = window.setInterval(refreshStatistics, STATISTICS_DELAY);
    }
  }
  
  this.stopStatisticsUpdate = function() {
    if(ocRecordings.statsInterval != null) {
      window.clearInterval(ocRecordings.statsInterval);
      ocRecordings.statsInterval = null;
    }
  }

  /** Construct an object representing a row in the recording table from a
   *  workflow instance object delivered by the workflow endpoint.
   */
  function Recording(wf) {
    this.id = wf.id;
    this.state = '';
    this.operation = false;
    this.creators='';
    this.series = '';
    this.seriesTitle = '';
    this.start='';
    this.end='';
    this.actions=[];
    this.holdAction=false;
    this.error = false;

    if (wf.mediapackage && wf.mediapackage.title) {
      this.title = wf.mediapackage.title;
    } else {
      this.title = 'NA';
    }

    // Series id and title
    this.series = wf.mediapackage.series;
    this.seriesTitle = wf.mediapackage.seriestitle;

    // Creator(s)
    if (wf.mediapackage.creators !== undefined) {
      this.creators = ocUtils.ensureArray(wf.mediapackage.creators.creator).join(', ');
    }

    // Start Time
    if (wf.mediapackage.start) {
      this.start = ocUtils.fromUTCDateString(wf.mediapackage.start);
    }

    // Status
    var op = ocRecordings.findLastOperation(wf, wf.state);
    if (wf.state == 'SUCCEEDED') {
      this.state = 'Finished';
    } else if (wf.state == 'FAILING' || wf.state == 'FAILED') {
      this.state = 'Failed';
      if (wf.errors === '') {
        if (op) {
          this.error = 'Failed in operation ' + op.description;
        } else {
          this.error = 'No error message available';
        }
      } else {
        this.error = ocUtils.ensureArray(wf.errors.error).join(', ');
      }
    } else if (wf.state == 'PAUSED') {
      if (op) {
        if (op.id == 'schedule') {
          this.state = 'Upcoming';
        } else if (op.id == 'capture')  {
          this.state = 'Capturing';
        } else if (op.id == 'ingest') {
          this.state = 'Captured';
          this.operation = 'Sending recording to processing';
        } else if (op.holdurl) {
          this.state = 'On Hold';
          this.operation = op.description;
          this.holdAction = {
            url : op.holdurl,
            title : op['hold-action-title']
          };
        }
      } else {
        ocUtils.log('Warning could not find current operation for worklfow ' + wf.id);
        this.state = 'Paused';
      }
    } else if (wf.state == 'RUNNING') {
      if (op) {
          this.state = 'Processing';
          this.operation = op.description;
      } else {
        op = ocRecordings.findFirstOperation(wf, "INSTANTIATED");    // MH-6426: it can happen that for running workflow there is no operation in state RUNNING
        if (op) {                                                    //     in this case we search for the next INSTANTIATED operation and display is as QUEUED
          this.operation = op.description;
        } else {
          ocUtils.log('Warning could not find current operation (neither RUNNING nor INSTANTIATED) for worklfow ' + wf.id);
        }
        this.state = 'Queued';
      }
    } else {
      this.state = wf.state;
    }

    // Actions
    this.actions = ['view'];
    if (this.state == 'Upcoming') {
      this.actions.push('edit');
      this.actions.push('delete');
    } else if (this.state == 'Finished') {
      this.actions.push('play');
      this.actions.push('delete');
    } else if (this.state == 'Failed') {
      this.actions.push('delete');
    }

    /*
    if (this.state == 'Upcoming') {
      this.actions = ['view', 'edit', 'delete'];
    } else if (this.state == 'Processing' || this.state == 'Queued') {
      this.actions = ['view'];
    } else {
      this.actions = ['view', 'delete'];
    }*/

    return this;
  }

  /** Prepare data delivered by workflow instances list endpoint for template
 *  rendering.
 */
  function makeRenderData(data) {
    var recordings = [];
    var wfs = ocUtils.ensureArray(data.workflows.workflow);
    $.each(wfs, function(index, wf) {
      recordings.push(new Recording(wf));
    });
    return {
      recordings : recordings
    };
  }

  /** JSONP callback for calls to the workflow instances list endpoint.
 */
  this.render = function(data) {
    var template = 'tableTemplate';
    var registerRecordingSelectHandler = false;
    if(ocRecordings.Configuration.state === 'bulkedit' || ocRecordings.Configuration.state === 'bulkdelete') {
      template = 'tableSelectTemplate'
      $('#controlsFoot').hide();
      registerRecordingSelectHandler = true;
    } else {
      $('#controlsFoot').show();
    }
    refreshing = false;
    ocRecordings.data = data;
    ocRecordings.totalRecordings = parseInt(data.workflows.totalCount);
    var result = TrimPath.processDOMTemplate(template, makeRenderData(data));
    $( '#tableContainer' ).empty().append(result);
    
    if(registerRecordingSelectHandler) {
      $('.selectRecording').click(function() {
        if(this.checked === true) {
          ocRecordings.numSelectedRecordings++;
        } else {
          ocRecordings.numSelectedRecordings--;
          var lastRecordingUnchecked = true;
          $.each($('.selectRecording'), function(i,v){
            if(v.checked) {
              lastRecordingUnchecked = false;
              return false;
            }
          });
          if(lastRecordingUnchecked) {
            $('#selectAllRecordings').attr('checked', false);
          }
        }
        ocRecordings.updateBulkActionApplyMessage();
      });
    }
    
    if(ocRecordings.Configuration.state === 'upcoming'){
      $('#bulkActionButton').show();
    } else {
      $('#bulkActionButton').hide();
    }
    
    // display number of matches if filtered
    if (ocRecordings.Configuration.filterText) {
      var countText;
      if (data.workflows.totalCount == '0') {
        countText = 'No Recordings matching Filter';
        $('#filterRecordingCount').css('color', 'red');
      } else {
        countText = data.workflows.totalCount;
        countText += parseInt(data.workflows.totalCount) > 1 ? ' Recordings' : ' Recording';
        countText += ' matching Filter';
        $('#filterRecordingCount').css('color', 'black');
      }
      $('#filterRecordingCount').text(countText).show();
    } else {
      $('#filterRecordingCount').hide();
    }

    var page = parseInt(ocRecordings.Configuration.page) + 1;
    var pageCount = Math.ceil(ocRecordings.totalRecordings / ocRecordings.Configuration.pageSize);
    pageCount = pageCount == 0 ? 1 : pageCount;
    $('#pageList').text( page + " of " + pageCount);
    if (page == 1) {
      $('.prevPage').each(function() {
        var text = $(this).text();
        var $elm = $('<span></span>').text(text).css('color', 'gray');
        $(this).replaceWith($elm);
      });
    }
    if (page == pageCount) {
      $('.nextPage').each(function() {
        var text = $(this).text();
        var $elm = $('<span></span>').text(text).css('color', 'gray');
        $(this).replaceWith($elm);
      })
    }

    // When table is ready, attach event handlers
    $('.sortable')
    .mouseenter( function() {
      $(this).addClass('ui-state-hover');
    })
    .mouseleave( function() {
      $(this).removeClass('ui-state-hover');
    })
    .click( function() {
      var sortDesc = $(this).find('.sort-icon').hasClass('ui-icon-circle-triangle-s');
      var sortField = ($(this).attr('id')).substr(4);
      $( '#ocRecordingsTable th .sort-icon' )
      .removeClass('ui-icon-circle-triangle-s')
      .removeClass('ui-icon-circle-triangle-n')
      .addClass('ui-icon-triangle-2-n-s');
      if (sortDesc) {
        ocRecordings.Configuration.sortField = sortField;
        ocRecordings.Configuration.sortOrder = 'ASC';
        ocRecordings.Configuration.page = 0;
        ocRecordings.reload();
      } else {
        ocRecordings.Configuration.sortField = sortField;
        ocRecordings.Configuration.sortOrder = 'DESC';
        ocRecordings.Configuration.page = 0;
        ocRecordings.reload();
      }
    });
    // if results are sorted, display icon indicating sort order in respective table header cell
    if (ocRecordings.Configuration.sortField != null) {
      var th = $('#sort' + ocRecordings.Configuration.sortField);
      $(th).find('.sort-icon').removeClass('ui-icon-triangle-2-n-s');
      if (ocRecordings.Configuration.sortOrder == 'ASC') {
        $(th).find('.sort-icon').addClass('ui-icon-circle-triangle-n');
      } else if (ocRecordings.Configuration.sortOrder == 'DESC') {
        $(th).find('.sort-icon').addClass('ui-icon-circle-triangle-s');
      }
    }
    // care for items in the table that can be unfolded
    //$('#recordingsTable .foldable').
    $('#recordingsTable .foldable').each( function() {
      $('<span></span>').addClass('fold-icon ui-icon ui-icon-triangle-1-e').css('float','left').prependTo($(this).find('.fold-header'));
      $(this).click( function() {
        $(this).find('.fold-icon')
        .toggleClass('ui-icon-triangle-1-e')
        .toggleClass('ui-icon-triangle-1-s');
        $(this).find('.fold-body').toggle('fast');
      });
    });
  }

  this.buildURLparams = function() {
    var pa = [];
    for (p in this.Configuration) {
      if (this.Configuration[p] != null) {
        pa.push(p + '=' + escape(this.Configuration[p]));
      }
    }
    return pa.join('&');
  }

  /** Make the page reload with the currently set configuration
  */
  this.reload = function() {
    var url = document.location.href.split('?', 2)[0];
    url += '?' + ocRecordings.buildURLparams();
    document.location.href = url;
  }
  
  /** Returns the workflow with the specified id from the currently loaded
 *  workflow data or false if workflow with given Id was not found.
 */
  this.getWorkflow = function(wfId) {
    var out = false;
    $.each(ocUtils.ensureArray(this.data.workflows.workflow), function(index, workflow) {
      if (workflow.id == wfId) {
        out = workflow;
      }
    });
    return out;
  }

  this.findFirstOperation = function(workflow, state) {
    var out = false;
    for (var i in ocUtils.ensureArray(workflow.operations.operation)) {
      if (workflow.operations.operation[i].state == state) {
        out = workflow.operations.operation[i];
        break;
      }
    }
    return out;
  }

  this.findLastOperation = function(workflow, state) {
    var out = false;
    $.each(ocUtils.ensureArray(workflow.operations.operation), function(index, operation) {
      if (operation.state == state) {
        out = operation;
      }
    });
    return out;
  }

  this.displayHoldUI = function(wfId) {
    var workflow = ocRecordings.getWorkflow(wfId);
    if (workflow) {
      var operation = ocRecordings.findFirstOperation(workflow, 'PAUSED');
      if (operation !== false && operation.holdurl !== undefined) {
        this.Hold.workflow = workflow;
        this.Hold.operation = operation;
        $('#holdWorkflowId').val(wfId);     // provide Id of hold actions workflow as value of html element (for backwards compatibility)
        $('#holdActionUI').attr('src', operation.holdurl);
        $('#stage').hide();
        $('#holdActionStage').show();
      } else {
        ocUtils.log('Warning: could not display hold action UI: hold operation not found (id=' + wfId + ')');
      }
    } else {
      ocUtils.log('Warning: could not display hold action UI: workflow not found (id=' + wfId + ')');
    }
  }

  this.adjustHoldActionPanelHeight = function() {
    var height = $('#holdActionUI').contents().find('html').height();
    $('#holdActionUI').height(height+10);
  }

  this.continueWorkflow = function(postData) {
    // data must include workflow id
    var data = {
      id : ocRecordings.Hold.workflow.id
    };

    // add properties for workflow resum if provided by hold operation
    if (postData !== undefined) {
      data.properties = "";
      $.each(postData, function(key, value) {
        if(key != 'id') {
          data.properties += key + '=' + value + "\n";
          ocUtils.log(key + '=' + value);
        }
      });
    }
    // add updated MP to data, if hold operation changed the MP
    if (ocRecordings.Hold.changedMediaPackage != null) {
      data['mediapackage'] = ocRecordings.Hold.changedMediaPackage;
      ocUtils.log(data['mediapackage']);
      ocRecordings.Hold.changedMediaPackage = null;
    }

    $.ajax({
      type       : 'POST',
      url        : '../workflow/replaceAndresume/',
      data       : data,
      error      : function(XHR,status,e){
        if (XHR.status == '204') {
          ocRecordings.reload();
        } else {
          alert('Could not resume Workflow: ' + status);
        }
      },
      success    : function(data) {
        ocRecordings.reload();
      }
    });
  }

  this.hideHoldActionUI = function() {
    ocRecordings.Hold = {
      workflow:null,
      operation:null,
      changedMediaPackage:null
    };
    $('#holdActionStage').hide();
    $('#stage').show();
  }

  this.disableRefresh = function() {
    if (ocRecordings.refreshInterval !== null) {
      window.clearInterval(ocRecordings.refreshInterval);
    }
  }

  this.updateRefreshInterval = function(enable, delay) {
    delay = delay < 5 ? 5 : delay;
    ocRecordings.Configuration.refresh = delay;
    ocUtils.log('Setting Refresh to ' + enable + " - " + delay + " sec");
    ocRecordings.Configuration.doRefresh = enable;
    ocRecordings.disableRefresh();
    if (enable) {
      ocRecordings.refreshInterval = window.setInterval(refresh, delay * 1000);
    }
  }

  /** $(document).ready()
 *
 */
  this.init = function() {

    // upload/schedule button
    $('#uploadButton').button({
      icons:{
        primary:'ui-icon-circle-plus'
      }
    })
    .click( function() {
      window.location.href = '../../admin/upload.html' + '?' + ocRecordings.buildURLparams();
    });
    $('#scheduleButton').button({
      icons:{
        primary:'ui-icon-circle-plus'
      }
    })
    .click( function() {
      window.location.href = '../../admin/scheduler.html';
    });

    // ocRecordings state selectors
    $( '#state-' +  ocRecordings.Configuration.state).attr('checked', true);
    $( '.state-filter-container' ).buttonset();
    $( '.state-filter-container input' ).click( function() {
      ocRecordings.Configuration.filterText = '';
      ocRecordings.Configuration.filterField = '';
      ocRecordings.Configuration.state = $(this).val();
      ocRecordings.Configuration.page = 0;
      ocRecordings.reload();
    })

    // search box
    this.searchbox = $( '#searchBox' ).searchbox({
      search : function(text, field) {
        if ($.trim(text) != '') {
          ocRecordings.Configuration.filterField = field;
          ocRecordings.Configuration.filterText = text;
          ocRecordings.Configuration.page = 0;
        }
        refresh();
      },
      clear : function() {
        ocRecordings.Configuration.filterField = '';
        ocRecordings.Configuration.filterText = '';
        ocRecordings.Configuration.page = 0;
        refresh();
      },
      searchText : ocRecordings.Configuration.filterText,
      options : FILTER_FIELDS,
      selectedOption : ocRecordings.Configuration.filterField,
      bgclass : 'oc-light-background'
    });

    // set refresh
    ocRecordings.updateRefreshInterval(ocRecordings.Configuration.doRefresh, ocRecordings.Configuration.refresh);

    // Refresh Controls
    // set values according to config
    if (ocRecordings.Configuration.doRefresh === 'true') {
      $('#refreshEnabled').attr('checked', 'checked');
      $('#refreshInterval').removeAttr('disabled');
      $('#refreshControlsContainer span').css('color', 'black');
    } else {
      $('#refreshEnabled').removeAttr('checked');
      $('#refreshInterval').attr('disabled', 'true');
      $('#refreshControlsContainer span').css('color', 'gray');
    }
    $('#refreshInterval').val(ocRecordings.Configuration.refresh);
    
    $('#refreshEnabled').change(function() {
      if ($(this).is(':checked')) {
        $('#refreshInterval').removeAttr('disabled');
        $('#refreshControlsContainer span').css('color', 'black');
      } else {
        $('#refreshInterval').attr('disabled', 'true');
        $('#refreshControlsContainer span').css('color', 'gray');
      }
      ocRecordings.updateRefreshInterval($(this).is(':checked'), $('#refreshInterval').val());
    });
    $('#refreshInterval').change(function() {
      ocRecordings.updateRefreshInterval($('#refreshEnabled').is(':checked'), $(this).val());
    });

    // Bulk Actions
    $('.oc-ui-collapsible-widget .ui-widget-header').click(
      function() {
        $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-e');
        $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-s');
        $(this).next().toggle();
        return false;
      });
    
    $('#bulkActionSelect').change(function(){
      ocRecordings.bulkActionHandler($(this).val());
    });
    
    $('.recordings-cancel-bulk-action').click(ocRecordings.cancelBulkAction);
    
    $('#applyBulkAction').click(ocRecordings.applyBulkAction);
    
    $('#seriesSelect').autocomplete({
      source: SERIES_URL + '/search',
      select: function(event, ui){
        $('#series').val(ui.item.id);
      },
      search: function(){
        $('#series').val('');
      }
    });

    // pager
    $('#pageSize').val(ocRecordings.Configuration.pageSize);
    
    $('#pageSize').change(function(){
      ocRecordings.Configuration.pageSize = $(this).val();
      ocRecordings.Configuration.page = 0;
      ocRecordings.reload();
    });
    
    $('#page').val(parseInt(ocRecordings.Configuration.page) + 1);
    
    $('#page').blur(function(){
      ocRecordings.gotoPage($(this).val() - 1);
    });
    
    $('#page').keypress(function(event) {
      if(event.keyCode == '13') {
        event.preventDefault();
        ocRecordings.gotoPage($(this).val() - 1);
      }
    });

    // set up statistics update
    ocRecordings.startStatisticsUpdate();
    
    if (ocRecordings.Configuration.state === 'bulkedit') {
      ocRecordings.bulkActionHandler('edit');
    } else if (ocRecordings.Configuration.state === 'bulkdelete') {
      ocRecordings.bulkActionHandler('delete');
    } else {
      refresh();    // load and render data for currently set configuration
    }
  };
  
  this.removeRecording = function(id, title) {
    if(confirm('Are you sure you wish to delete ' + title + '?')){
      $.ajax({
        url: '/scheduler/'+id,
        type: 'DELETE',
        error: function(XHR,status,e){
          alert('Could not remove Recording ' + title);
        },
        success: function(){
          ocRecordings.reload();
        }
      });
    }
  }
  //TEMPORARY (quick'n'dirty) PAGING
  this.nextPage = function() {
    numPages = Math.floor(this.totalRecordings / ocRecordings.Configuration.pageSize);
    if( ocRecordings.Configuration.page < numPages ) {
      ocRecordings.Configuration.page++;
    }
    ocRecordings.reload();
  }
  
  this.previousPage = function() {
    if(ocRecordings.Configuration.page > 0) {
      ocRecordings.Configuration.page--;
    }
    ocRecordings.reload();
  }
  
  this.lastPage = function() {
    ocRecordings.Configuration.page = Math.floor(this.totalRecordings / ocRecordings.Configuration.pageSize);
    ocRecordings.reload();
  }
  
  this.gotoPage = function(page) {
    if(page > (ocRecordings.totalRecordings / ocRecordings.Configuration.pageSize)) {
      ocRecordings.lastPage();
    } else {
      if( page < 0) {
        page = 0;
      }
      ocRecordings.Configuration.page = page;
      ocRecordings.reload();
    }
  }

  this.displayBulkAction = function(filter) {
    $('#bulkEditPanel').hide();
    $('#bulkDeletePanel').hide();
    $('#bulkActionApply').hide();
    $('#bulkActionPanel').show();
    ocRecordings.Configuration.lastState = ocRecordings.Configuration.state
    ocRecordings.Configuration.lastPageSize = ocRecordings.Configuration.pageSize;
    ocRecordings.Configuration.lastPage = ocRecordings.Configuration.page;
    ocRecordings.disableRefresh();
    ocRecordings.stopStatisticsUpdate();
    $('#bulkActionPanel :input[type=textarea], #bulkActionPanel :text').keyup(ocRecordings.bulkEditFieldHandler);
  }

  this.bulkEditFieldHandler = function(e) {
    if(e.target.value !== '') {
      ocRecordings.changedBulkEditFields[e.target.id] = e.target;
    } else {
      delete ocRecordings.changedBulkEditFields[e.target.id];
    }
    $('#bulkActionApplyMessage').text(bulkEditApplyMessage());
  }
  
  this.cancelBulkAction = function() {
    ocRecordings.resetBulkActionPanel();
    ocRecordings.Configuration.state = ocRecordings.Configuration.lastState;
    ocRecordings.Configuration.pageSize = ocRecordings.Configuration.lastPageSize;
    ocRecordings.Configuration.page = ocRecordings.Configuration.lastPage;
    refresh();
    ocRecordings.updateRefreshInterval(true, ocRecordings.Configuration.refresh);
    ocRecordings.startStatisticsUpdate();
  }

  this.resetBulkActionPanel = function() {
    $('#bulkActionSelect').val('select');
    $('#bulkActionSelect').change();
    $('#bulkActionPanel').hide();
    ocRecordings.bulkEditComponents = [];
    $('#bulkActionPanel :input[type=textarea], #bulkActionPanel :text').val('');
    ocRecordings.changedBulkEditFields = {};
    ocRecordings.numSelectedRecordings = 0;
  }

  this.bulkActionHandler = function(action) {
    $('#bulkActionPanel').show();
    if (action === 'select') {
      $('#bulkEditPanel').hide();
      $('#bulkDeletePanel').hide();
      $('#bulkActionApply').hide();
      $('#cancelBulkAction').show();
    } else {
      ocRecordings.numSelectedRecordings = 0;
      if(action === 'edit'){
        $('#bulkActionApplyMessage').text(bulkEditApplyMessage());
        $('#bulkEditPanel').show();
        $('#bulkDeletePanel').hide();
        $('#bulkActionApply').show();
        $('#cancelBulkAction').hide();
        ocRecordings.registerBulkEditComponents();
        ocRecordings.Configuration.state = 'bulkedit'
      } else if (action === 'delete') {
        $('#bulkActionApplyMessage').text(bulkDeleteApplyMessage());
        $('#bulkEditPanel').hide();
        $('#bulkDeletePanel').show();
        $('#bulkActionApply').show();
        $('#cancelBulkAction').hide();
        ocRecordings.Configuration.state = 'bulkdelete'
      }
      refresh();
    }
  }
  
  function bulkEditApplyMessage() {
    return "Changes will be made in " + ocUtils.sizeOf(ocRecordings.changedBulkEditFields) + 
      " field(s) for all " + ocRecordings.numSelectedRecordings + " selected recoding(s).";
  }
  
  function bulkDeleteApplyMessage() {
    return ocRecordings.numSelectedRecordings + " selected recording(s) will be deleted.";
  }
  
  this.updateBulkActionApplyMessage = function() {
    if(ocRecordings.Configuration.state === 'bulkedit'){
      $('#bulkActionApplyMessage').text(bulkEditApplyMessage());
    } else if (ocRecordings.Configuration.state === 'bulkdelete') {
      $('#bulkActionApplyMessage').text(bulkDeleteApplyMessage());
    }
  }

  this.selectAll = function(checked) {
    if(ocRecordings.Configuration.state != 'bulkedit' && ocRecordings.Configuration.state != 'bulkdelete'){
      return;
    }
    if(checked){
      $.each($('.selectRecording'), function(i,v){
        v.checked = true;
      });
      ocRecordings.numSelectedRecordings = ocRecordings.totalRecordings;
    } else {
      $.each($('.selectRecording'), function(i,v){
        v.checked = false;
      });
      ocRecordings.numSelectedRecordings = 0;
    }
    ocRecordings.updateBulkActionApplyMessage();
  }

  this.applyBulkAction = function() {
    var manager;
    var event;
    var progress = 0;
    var progressChunk = 0;
    var eventIdList = [];
    var failed = 0;
    $.each($('.selectRecording'), function(i,v){
      if(v.checked === true) {
        eventIdList.push(v.value);
      }
    });
    if(eventIdList.length > 0){
      if(ocRecordings.Configuration.state === 'bulkedit') {
        manager = new ocAdmin.Manager('event', '', ocRecordings.bulkEditComponents);
        event = manager.serialize();
        $.post('/scheduler/', 
        {
          event: event,
          idList: '[' + eventIdList.toString() + ']'
        },
        ocRecordings.bulkActionComplete);
      } else if(ocRecordings.Configuration.state === 'bulkdelete') {
        progressChunk = (100 / eventIdList.length)
        $('#deleteProgress').progressbar({
          value: 0,
          complete: function(){
            $('#deleteModal').dialog('destroy');
            ocRecordings.bulkActionComplete();
          }
        });
        $('#deleteModal').dialog({
          height: 140,
          modal: true
        });
        var toid = setInterval(function(){
          var id = eventIdList.pop();
          if(typeof id === 'undefined'){
            clearInterval(toid);
            ocUtils.log(progress);
            $('#deleteProgress').progressbar('value', ++progress);
            if(failed > 0) {
              $('#deleteError').show();
            }
            return;
          }
          $.ajax({
            url: '/scheduler/'+id,
            type: 'DELETE',
            complete: function(xhr, status) {
              if(xhr.status == 500) {
                failed++;
                $('#deleteErrorMessage').text('Failed to delete ' + failed + ' recordings.');
              } else {
                progress = progress + progressChunk;
                $('#deleteProgress').progressbar('value', progress);
              }
            }
          });
        }, 250);
      }
    }
  }

  this.bulkActionComplete = function() {
    ocRecordings.cancelBulkAction();
  }

  this.registerBulkEditComponents = function() {
    ocRecordings.bulkEditComponents.title = new ocAdmin.Component(['title'], {
      label: 'titleLabel'
    });
    ocRecordings.bulkEditComponents.creator = new ocAdmin.Component(['creator'], {
      label: 'creatorLabel'
    });
    ocRecordings.bulkEditComponents.contributor = new ocAdmin.Component(['contributor'], {
      label: 'contributorLabel'
    });
    ocRecordings.bulkEditComponents.seriesId = new ocAdmin.Component(['series', 'seriesSelect'],
    {
      label: 'seriesLabel',
      errorField: 'missingSeries',
      nodeKey: ['seriesId', 'series'],
      required: true
    },

    {
      getValue: function(){
        if(this.fields.series){
          this.value = this.fields.series.val();
        }
        return this.value;
      },
      setValue: function(value){
        this.fields.series.val(value.id);
        this.fields.seriesSelect.val(value.label)
      },
      asString: function(){
        if(this.fields.seriesSelect){
          return this.fields.seriesSelect.val();
        }
        return this.getValue() + '';
      },
      validate: function(){
        if(this.fields.seriesSelect.val() !== '' && this.fields.series.val() === ''){ //have text and no idea
          return this.createSeriesFromSearchText();
        }
        return true; //nothing, or we have an id.
      },
      toNode: function(parent){
        if(parent){
          doc = parent.ownerDocument;
        }else{
          doc = document;
        }
        if(this.getValue() != "" && this.asString() != ""){ //only add series if we have both id and name.
          seriesId = doc.createElement(this.nodeKey[0]);
          seriesId.appendChild(doc.createTextNode(this.getValue()));
          seriesName = doc.createElement(this.nodeKey[1]);
          seriesName.appendChild(doc.createTextNode(this.asString()));
          if(parent && parent.nodeType){
            parent.appendChild(seriesId);
            parent.appendChild(seriesName);
          }else{
            ocUtils.log('Unable to append node to document. ', parent, seriesId, seriesName);
          }
        }
      },
      createSeriesFromSearchText: function(){
        var series, seriesComponent, seriesId;
        var creationSucceeded = false;
        if(this.fields.seriesSelect !== ''){
          series = '<series><additionalMetadata><metadata><key>title</key><value>' + this.fields.seriesSelect.val() + '</value></metadata></additionalMetadata></series>';
          seriesComponent = this;
          $.ajax({
            async: false,
            type: 'PUT',
            url: SERIES_URL + '/',
            data: {
              series: series
            },
            dataType: 'json',
            success: function(data){
              creationSucceeded = true;
              seriesComponent.fields.series.val(data.series['id']);
            }
          });
        }
        return creationSucceeded;
      }
    });
    ocRecordings.bulkEditComponents.subject = new ocAdmin.Component(['subject'], {
      label: 'subjectLabel'
    });
    ocRecordings.bulkEditComponents.language = new ocAdmin.Component(['language'], {
      label: 'languageLabel'
    });
    ocRecordings.bulkEditComponents.description = new ocAdmin.Component(['description'], {
      label: 'descriptionLabel'
    });
  }
  
  this.closeDeleteDialog = function() {
    $('#deleteModal').dialog('close');
    refresh();
  }
  
  $(document).ready(this.init);

  this.makeActions = function(recording, actions) {
    var id = recording.id
    var links = [];
    for(i in actions){
      if(actions[i] === 'view') {
        links.push('<a href="viewinfo.html?id=' + id + '">View Info</a>');
      } else if(actions[i] === 'edit') {
        links.push('<a href="scheduler.html?eventId=' + id + '&edit=true">Edit</a>');
      } else if(actions[i] === 'play') {
        var workflow = ocRecordings.getWorkflow(id);
        if (workflow) {
          var mpId = workflow.mediapackage.id;
          links.push('<a href="../engage/ui/watch.html?id=' + mpId + '">Play</a>');
        }
      } else if(actions[i] === 'delete') {
        links.push('<a href="javascript:ocRecordings.removeRecording(\'' + id + '\',\'' + recording.title + '\')">Delete</a>');
      }
    }
    return links.join(' \n');
  }

  return this;
})();
