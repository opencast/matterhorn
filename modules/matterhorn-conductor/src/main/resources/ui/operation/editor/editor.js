/**
 * Copyright 2009-2011 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

var editor = editor || {};

var SMIL_RESTSERVICE = "/smil/";
var SMIL_FLAVOR = "smil/smil";
var WAVEFORM_FLAVOR = "image/waveform";

var ME_JSON = "/info/me.json";

var PREVIOUS_FRAME = "trim.previous_frame";
var NEXT_FRAME = "trim.next_frame";
var SPLIT_AT_CURRENT_TIME = "trim.split_at_current_time";
var PLAY_CURRENT_SEGMENT = "trim.play_current_segment";
var PLAY_CURRENT_PRE_POST = "trim.play_current_pre_post";
var SET_CURRENT_TIME_AS_INPOINT = "trim.set_current_time_as_inpoint";
var SET_CURRENT_TIME_AS_OUTPOINT = "trim.set_current_time_as_outpoint";
var PLAY_PAUSE = "trim.play_pause";
var SELECT_ITEM_AT_CURRENT_TIME = "trim.select_item_at_current_time";
var DELETE_SELECTED_ITEM = "trim.delete_selected_segment";
var NEXT_MARKER = "trim.next_marker";
var PREVIOUS_MARKER = "trim.previous_marker";
var PLAY_ENDING_OF_CURRENT_SEGMENT = "trim.play_ending_of_current_segment";

var default_config = {};
default_config[PREVIOUS_FRAME] = "left";
default_config[NEXT_FRAME] = "right";
default_config[SPLIT_AT_CURRENT_TIME] = "v";
default_config[PLAY_CURRENT_SEGMENT] = "c";
default_config[PLAY_CURRENT_PRE_POST] = "Shift+c";
default_config[SET_CURRENT_TIME_AS_INPOINT] = "i";
default_config[SET_CURRENT_TIME_AS_OUTPOINT] = "o";
default_config[PLAY_PAUSE] = "space";
default_config[SELECT_ITEM_AT_CURRENT_TIME] = "y";
default_config[DELETE_SELECTED_ITEM] = "Delete";
default_config[PREVIOUS_MARKER] = "Up";
default_config[NEXT_MARKER] = "Down";
default_config[PLAY_ENDING_OF_CURRENT_SEGMENT] = "n";

var timeout_playEndingOfCurrSegment = null;

var currSplitItem = null;
var timeout1 = null;
var timeout2 = null;
var timeout3 = null;
var timeout4 = null;
var endTime = 0;
var jumpBackTime = null;
var currEvt = null;
var timeoutUsed = false;
var now = 100;
var lastTimeSplitItemClick = 0;
var isSeeking = false;
var currSplitItemClickedViaJQ = false;

editor.splitData = {};
editor.splitData.splits = [];
editor.selectedSplit = null;
editor.player = null;
editor.smil = null;
editor.canvas = null;
editor.ready = false;

/**
 * update split list in UI
 */
editor.updateSplitList = function(dontClickCancel) {
    if(!dontClickCancel) {
	cancelButtonClick();
    }
    $('#leftBox').html($('#splitElements').jqote(editor.splitData));
    $('#splitSegments').html($('#splitSegmentTemplate').jqote(editor.splitData));
    $('.splitItemDiv').click(splitItemClick);
    $('.splitSegmentItem').click(splitItemClick);
    // $('.splitRemover').click(splitRemoverClick);

    $('.splitRemoverLink').button({
	text : false,
	icons : {
	    primary : "ui-icon-trash"
	}
    });

    $('.splitAdderLink').button({
	text : false,
	icons : {
	    primary : "ui-icon-arrowreturnthick-1-w"
	}
    });

    $('.splitRemoverLink').click(splitRemoverClick);
    $('.splitAdderLink').click(splitRemoverClick);

    $('.splitSegmentItem').dblclick(jumpToSegment);
    $('.splitItemDiv').dblclick(jumpToSegment);

    $('.splitSegmentItem').hover(splitHoverIn, splitHoverOut);
    $('.splitItemDiv').hover(splitHoverIn, splitHoverOut);
}

/**
 * create a new smil for the current workflow should already be handled by
 * workflowoperationhandler
 */
editor.createSMIL = function() {
    $.ajax({
	url : SMIL_RESTSERVICE + "new",
	data : {
	    workflowId : workflowInstance.id
	}
    });
}

/**
 * clear the content of the smil
 */
editor.clearSMIL = function() {
    $.ajax({
	url : SMIL_RESTSERVICE + "clear/" + workflowInstance.id,
	async : false
    });
}

/**
 * save the content to smil file
 */
editor.saveSplitList = function() {
    editor.clearSMIL();
    $.each(editor.splitData.splits, function(key, value) {
	if (value.enabled) {
	    parallelId = editor.addParallel();
	    $.each(workflowInstance.mediapackage.media.track, function(key, track) {
		if (track.type.indexOf("work") != -1) {
		    value.src = track.url;
		    value.mhElement = track.id;
		    editor.addMediaElement(parallelId, value);
		}
	    });
	}
    });
}

/**
 * add a media element to the smil
 * 
 * @param parallelId
 *          the id of the parallelElement this MediaElement should be added to
 * @param data
 *          the data of this MediaElement
 */
editor.addMediaElement = function(parallelId, data) {
    $.ajax({
	url : SMIL_RESTSERVICE + "addMediaElement/" + workflowInstance.id + "/" + parallelId,
	data : data,
	async : false,
	success : function(data) {
	}
    })
}

/**
 * add a ParallelElement to the SMIL
 */
editor.addParallel = function() {
    var parallelId = "";
    $.ajax({
	url : SMIL_RESTSERVICE + "addParallel/" + workflowInstance.id,
	success : function(data) {
	    parallelId = data;
	},
	async : false
    });
    return parallelId;
}

/**
 * download the SMIL if there is already one e.g. from silence detection
 */
editor.downloadSMIL = function() {
    var smil = null;
    $.ajax({
	url : SMIL_RESTSERVICE + "get/" + workflowInstance.id,
	data : {
	    format : "json"
	},
	dataType : "json",
	async : false,
	success : function(data) {
	    smil = data;
	}
    });
    return smil;
}

/**
 * add all shortcuts
 */
function addShortcuts() {
    $.ajax({
	url : ME_JSON,
	dataType : "json",
	async : false,
	success : function(data) {
	    $.each(data.org.properties, function(key, value) {
		default_config[key] = value;
		$('#' + key.replace(".", "_")).html(value);
	    });
	}
    });
    
    $.each(default_config, function(key, value) {
	$('#' + key.replace(".", "_")).html(value);
    });

    // add shortcuts for easier editing
    shortcut.add(default_config[SPLIT_AT_CURRENT_TIME], splitButtonClick, {
	disable_in_input : true,
    });
    shortcut.add(default_config[PREVIOUS_FRAME], function() {
	pauseVideo();
	$('.video-prev-frame').click();
    }, {
	disable_in_input : true,
    });
    shortcut.add(default_config[NEXT_FRAME], function() {
	pauseVideo();
	$('.video-next-frame').click();
    }, {
	disable_in_input : true,
    });
    shortcut.add(default_config[PLAY_PAUSE], function() {
	if (editor.player.prop("paused")) {
	    playVideo();
	} else {
	    pauseVideo();
	}
    }, {
	disable_in_input : true,
    });
    shortcut.add(default_config[PLAY_CURRENT_SEGMENT], playCurrentSplitItem, {
	disable_in_input : true,
    });
    shortcut.add(default_config[DELETE_SELECTED_ITEM], splitRemoverClick, {
	disable_in_input : true,
    });
    shortcut.add(default_config[SELECT_ITEM_AT_CURRENT_TIME], selectCurrentSplitItem, {
	disable_in_input : true,
    });
    shortcut.add(default_config[SET_CURRENT_TIME_AS_INPOINT], setCurrentTimeAsNewInpoint, {
	disable_in_input : true,
    });
    shortcut.add(default_config[SET_CURRENT_TIME_AS_OUTPOINT], setCurrentTimeAsNewOutpoint, {
	disable_in_input : true,
    });
    shortcut.add(default_config[NEXT_MARKER], nextSegment, {
	disable_in_input : true,
    });
    shortcut.add(default_config[PREVIOUS_MARKER], previousSegment, {
	disable_in_input : true,
    });
    shortcut.add(default_config[PLAY_ENDING_OF_CURRENT_SEGMENT], playEnding, {
	disable_in_input : true,
    });
    shortcut.add(default_config[PLAY_CURRENT_PRE_POST], playWithoutDeleted, {
	disable_in_input : true
    });
}

/**
 * function executed when play event was thrown
 * 
 * @param evt
 *          the event
 */
function onPlay(evt) {
    if(timeout1 == null) {
	currEvt = evt;
	timeout1 = window.setTimeout(onTimeout, evt.data.duration);
    }
}

/**
 * clearing events
 */
function clearEvents() {
    if(timeout1 != null) {
	window.clearTimeout(timeout1);
	timeout1 = null;
    }
    if(timeout2 != null) {
	window.clearTimeout(timeout2);
	timeout2 = null;
    }
    timeoutUsed = false;
}

/**
 * the timeout function pausing the video again
 */
function onTimeout() {
    if(!timeoutUsed) {
	pauseVideo();
	var check = function() {
	    endTime = currEvt.data.endTime;
	    if (endTime > editor.player.prop("currentTime")) {
		playVideo();
		timeout2 = window.setTimeout(check, 10);
		timeoutUsed = true;
	    } else {
		pauseVideo();
		clearEvents();
		
		jumpBackTime = currEvt.data.jumpBackTime;
		jumpBackTime = ((jumpBackTime == null) || (jumpBackTime == undefined)) ? null : jumpBackTime;
		if(jumpBackTime != null) {
		    editor.player.prop("currentTime", jumpBackTime);
		    jumpBackTime = null;
		}
	    }
	}
	check();
    }
}

/**
 * plays the current split item from it's beginning
 */
function playCurrentSplitItem() {
    var splitItem = getCurrentSplitItem();
    var currentTime = editor.player.prop("currentTime");
    if (splitItem != null) {
	pauseVideo();
	var clipBegin = parseFloat(splitItem.clipBegin.replace("s", ""));
	var clipEnd = parseFloat(splitItem.clipEnd.replace("s", ""));
	var duration = (clipEnd - clipBegin) * 1000;
	editor.player.prop("currentTime", clipBegin);

	clearEvents();
	editor.player.on("play", {
	    duration : duration,
	    endTime : clipEnd
	}, onPlay);
	playVideo();
    }
}

/**
 * play last 2 seconds of the current segment
 */
function playEnding() {
    var splitItem = getCurrentSplitItem();
    if (splitItem != null) {
	pauseVideo();
	var clipEnd = parseFloat(splitItem.clipEnd.replace("s", ""));
	editor.player.prop("currentTime", clipEnd - 2);

	clearEvents();
	editor.player.on("play", {
	    duration : 2000,
	    endTime : clipEnd
	}, onPlay);
	playVideo();
    }
}

/**
 * play current segment -2s exclude the removed segments
 */
function playWithoutDeleted() {
    var splitItem = getCurrentSplitItem();
    var currentTime = editor.player.prop("currentTime");
    
    if (splitItem != null) {
	pauseVideo();

	var clipStartFrom = -1;
	var clipStartTo = -1;
	var segmentStart = parseFloat(splitItem.clipBegin.replace("s", ""));
	var segmentEnd = parseFloat(splitItem.clipEnd.replace("s", ""));
	var clipEndFrom = -1;
	var clipEndTo = -1;

	if((splitItem.id - 1) >= 0) {
	    var prevSplitItem = editor.splitData.splits[splitItem.id - 1];
	    while(!prevSplitItem.enabled) {
		if((prevSplitItem.id - 1) < 0) {
		    break;
		}
		prevSplitItem = editor.splitData.splits[prevSplitItem.id - 1];
	    }
	    clipStartTo = parseFloat(prevSplitItem.clipEnd.replace("s", ""));
	    clipStartFrom = clipStartTo - 2;
	}
	if(clipStartTo == -1) {
	    clipStartTo = segmentStart;
	    clipStartFrom = clipStartTo - 2;
	}
	clipStartFrom = (clipStartFrom < 0) ? 0 : clipStartFrom;

	if((splitItem.id + 1) < editor.splitData.splits.length) {
	    var nextSplitItem = editor.splitData.splits[splitItem.id + 1];
	    while(!nextSplitItem.enabled) {
		if((nextSplitItem.id + 1) >= editor.splitData.splits.length) {
		    break;
		}
		nextSplitItem = editor.splitData.splits[nextSplitItem.id + 1];
	    }
	    clipEndFrom = parseFloat(nextSplitItem.clipBegin.replace("s", ""));
	    clipEndTo = clipEndFrom + 2;
	}
	if(clipEndFrom == -1) {
	    clipEndFrom = segmentEnd;
	    clipEndTo = clipEndFrom + 2;
	}
	var duration = editor.player.prop("duration");
	clipEndTo = (clipEndTo > duration) ? duration : clipEndTo;

	ocUtils.log("Play Times: " + clipStartFrom + " - " + clipStartTo + " | " + segmentStart + " - " + segmentEnd + " | " + clipEndFrom + " - " + clipEndTo);
	
	editor.player.prop("currentTime", clipStartFrom);

	clearEvents();
	editor.player.on("play", {
	    duration : (clipStartTo - clipStartFrom) * 1000,
	    endTime : clipStartTo
	}, onPlay);
	
	playVideo();
	
	timeout3 = window.setTimeout(function(){
	    pauseVideo();
	    editor.player.prop("currentTime", segmentStart);
	    clearEvents();
	    currSplitItemClickedViaJQ = true;
	    editor.player.on("play", {
		duration : (segmentEnd - segmentStart) * 1000,
		endTime : segmentEnd
	    }, onPlay);
	    playVideo();
	}, (clipStartTo - clipStartFrom) * 1000);
	
	timeout4 = window.setTimeout(function(){
	    pauseVideo();
	    if(timeout3 != null) {
		window.clearTimeout(timeout3);
		timeout3 = null;
	    }
	    editor.player.prop("currentTime", clipEndFrom);
	    clearEvents();
	    currSplitItemClickedViaJQ = true;
	    editor.player.on("play", {
		duration : (clipEndTo - clipEndFrom) * 1000,
		endTime : clipEndTo
	    }, onPlay);
	    playVideo();
	    if(timeout4 != null) {
		window.clearTimeout(timeout4);
		timeout4 = null;
	    }
	}, ((clipStartTo - clipStartFrom) * 1000) + ((segmentEnd - segmentStart) * 1000));
    }
}

/**
 * jump to beginning of current split item
 */
function jumpToSegment() {
    id = $(this).prop('id');
    id = id.replace('splitItem-', '');
    id = id.replace('splitItemDiv-', '');
    id = id.replace('splitSegmentItem-', '');

    editor.player.prop("currentTime", editor.splitData.splits[id].clipBegin.replace("s", ""));
}

function isInInterval(toCheck, lower, upper) {
    return (toCheck >= lower) && (toCheck <= upper);
}

/**
 * jump to next segment
 */
function nextSegment() {
    var playerPaused = editor.player.prop("paused");
    if(!playerPaused) {
	pauseVideo();
    }
    var currentTime = editor.player.prop("currentTime");
    var new_id = -1;
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
	var splitItem = editor.splitData.splits[i];
	var clipBegin = parseFloat(splitItem.clipBegin.replace("s", ""));
	var clipEnd = parseFloat(splitItem.clipEnd.replace("s", ""));
	if((isInInterval(currentTime, clipBegin - 0.1, clipBegin + 0.1) || (currentTime >= clipBegin)) &&
	   (!isInInterval(currentTime, clipEnd - 0.1, clipEnd + 0.1) && (currentTime < clipEnd))) {
	    new_id = i + 1;
	    break;
	}
	
    }
    if(new_id > 0) {
	if (new_id <= editor.splitData.splits.length - 1) {
	    editor.player.prop("currentTime", editor.splitData.splits[new_id].clipBegin.replace("s", ""));
	} else if (new_id <= editor.splitData.splits.length) {
	    editor.player.prop("currentTime", editor.player.prop("duration"));
	}
    }
}

/**
 * jump to previous segment
 */
function previousSegment() {
    var playerPaused = editor.player.prop("paused");
    if(!playerPaused) {
	pauseVideo();
    }
    var currentTime = editor.player.prop("currentTime");
    var new_id = -1;
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
	var splitItem = editor.splitData.splits[i];
	var clipBegin = parseFloat(splitItem.clipBegin.replace("s", ""));
	var clipEnd = parseFloat(splitItem.clipEnd.replace("s", ""));
	if(((currentTime > clipBegin) && (currentTime < clipEnd)) || isInInterval(currentTime, clipEnd - 0.1, clipEnd + 0.1)) {
	    new_id = i;
	    break;
	} else if(isInInterval(currentTime, clipBegin - 0.1, clipBegin + 0.1)) {
	    new_id = i - 1;
	    break;
	} else if ((i == (editor.splitData.splits.length - 1)) && (currentTime >= clipEnd)) {
	    new_id = editor.splitData.splits.length - 1;
	    break;
	}
    }
    if (new_id >= 0) {
	editor.player.prop("currentTime", editor.splitData.splits[new_id].clipBegin.replace("s", ""));
    }
}

/**
 * handler for hover in events on split segements and -list
 * 
 * @param evt
 *          the corresponding event
 */
function splitHoverIn(evt) {
    id = $(this).prop('id');
    id = id.replace('splitItem-', '');
    id = id.replace('splitItemDiv-', '');
    id = id.replace('splitSegmentItem-', '');

    $('#splitItem-' + id).addClass('hover');
    if (!$('#splitSegmentItem-' + id).hasClass("splitSegmentItemSelected")) {
	$('#splitSegmentItem-' + id).addClass('hover hoverOpacity');
    }
}

/**
 * handler for hover out events on split segements and -list
 * 
 * @param evt
 *          the corresponding event
 */
function splitHoverOut(evt) {
    id = $(this).prop('id');
    id = id.replace('splitItem-', '');
    id = id.replace('splitItemDiv-', '');
    id = id.replace('splitSegmentItem-', '');

    $('#splitItem-' + id).removeClass('hover');
    $('#splitSegmentItem-' + id).removeClass('hover hoverOpacity');
}

/**
 * init the playbuttons in the editing box
 */
function initPlayButtons() {
    $('#clipBeginSet, #clipEndSet').button({
	text : false,
	icons : {
	    primary : "ui-icon-arrowthickstop-1-s"
	}
    });

    $('#clipBeginSet').click(setCurrentTimeAsNewInpoint);
    $('#clipEndSet').click(setCurrentTimeAsNewOutpoint);

    $('#shortcuts').button();

    $('#shortcuts').click(function() {
	$('#shortcutsDialog').dialog({
	    title : "shortcuts for video editing",
	    resizable : false,
	    buttons : {
		Close : function() {
		    $(this).dialog("close");
		}
	    }
	})
    });

    $('#clearList').button();

    $('#clearList').click(function() {
	editor.splitData.splits = [];
	// create standard split point
	editor.splitData.splits.push({
	    clipBegin : '0s',
	    clipEnd : workflowInstance.mediapackage.duration / 1000 + "s",
	    enabled : true,
	    description : ""
	});
	editor.updateSplitList();
    });

}

function checkClipBegin()
{
    if($('#clipBegin').timefield('option', 'value').replace) {
	var clipBegin = parseFloat($('#clipBegin').timefield('option', 'value').replace("s", ""));
	if(isNaN(clipBegin))
	{
	    $('#clipBegin').timefield('option', 'value', '0s');
	}
	else
	{
	    $('#clipBegin').timefield('option', 'value', clipBegin + 's');
	}
    }
}

function checkClipEnd()
{
    if($('#clipEnd').timefield('option', 'value').replace) {
	var duration = editor.player.prop("duration");
	var clipEnd = parseFloat($('#clipEnd').timefield('option', 'value').replace("s", ""));
	if(clipEnd > duration) {
	    clipEnd = duration;
	}
	if(isNaN(clipEnd))
	{
	    $('#clipEnd').timefield('option', 'value', '0s');
	}
	else
	{
	    $('#clipEnd').timefield('option', 'value', clipEnd + 's');
	}
	var currSplitItem_ = getCurrentSplitItem();
	if(currSplitItem_) {
	    getCurrentSplitItem().clipEnd = clipEnd + 's';
	    editor.updateSplitList(true);
	}
    }
}

/**
 * click handler for saving data in editing box
 */
function okButtonClick() {
    checkClipBegin();
    checkClipEnd();
    id = $('#splitUUID').val();
    if (id != "") {
	id = parseInt(id);
	if (parseFloat($('#clipBegin').timefield('option', 'value').replace("s", "")) > parseFloat($('#clipEnd').timefield(
	    'option', 'value').replace("s", ""))) {
	    $('<div />').html("The inpoint is bigger than the outpoint. Please check.").dialog({
		title : "Check in and outpoint",
		resizable : false,
		buttons : {
		    OK : function() {
			$(this).dialog("close");
		    }
		}
	    });
	    return;
	}

	splitItem = editor.splitData.splits[id];
	splitItem.description = $('#splitDescription').val();
	splitItem.clipBegin = $('#clipBegin').timefield('option', 'value');
	splitItem.clipEnd = $('#clipEnd').timefield('option', 'value');

	checkPrevAndNext(id);

	editor.updateSplitList(true);
	selectSegmentListElement(id);
    }
}

function checkPrevAndNext(id) {
    // it's the first check whether we need a new first item
    if (id == 0) {
	if (editor.splitData.splits.length > 1) {
	    var next = editor.splitData.splits[id + 1];
	    next.clipBegin = splitItem.clipEnd;
	}
	if ($('#clipBegin').timefield('option', 'seconds') != 0) {
	    var newSplitItem = {
		description : "",
		clipBegin : "0s",
		enabled : true,
		clipEnd : splitItem.clipBegin
	    };

	    // add new item to front
	    editor.splitData.splits.splice(0, 0, newSplitItem);
	}
    } else if (id == editor.splitData.splits.length - 1) {
	if ($('#clipEnd').timefield('option', 'seconds') != editor.player.prop("duration")) {
	    var newLastItem = {
		description : "",
		enabled : true,
		clipBegin : splitItem.clipEnd,
		clipEnd : editor.player.prop("duration") + "s"
	    };

	    // add the new item to the end
	    editor.splitData.splits.push(newLastItem);
	}
	var prev = editor.splitData.splits[id - 1];
	prev.clipEnd = splitItem.clipBegin
    } else {
	var next = editor.splitData.splits[id + 1];
	var prev = editor.splitData.splits[id - 1];

	prev.clipEnd = splitItem.clipBegin;
	next.clipBegin = splitItem.clipEnd;
    }
}

/**
 * click handler for canceling editing
 */
function cancelButtonClick() {
    $('#splitDescription').html('');
    $('#splitUUID').val('');
    $('#splitDescription').val("");
    $('#clipBegin').timefield('option', 'value', '0s');
    $('#clipEnd').timefield('option', 'value', '0s');
    $('#splitIndex').html('#');
    $('.splitItem').removeClass('splitItemSelected');
    $('.splitSegmentItem').removeClass('splitSegmentItemSelected');
    editor.selectedSplit = null;
    enabledRightBox(false);
}

/**
 * enable/disable the right editing box
 * 
 * @param enabled
 *          whether enabled or not
 */
function enabledRightBox(enabled) {
    if (enabled) {
	$('#rightBox :input').removeProp('disabled');
	$('.frameButton').button("enable");
	$('#descriptionCurrentTimeDiv').show();
    } else {
	$('#rightBox :input').prop('disabled', 'disabled');
	$('.frameButton').button("disable");
	$('#descriptionCurrentTimeDiv').hide();
    }
}

/**
 * select the split at the current time
 */
function selectCurrentSplitItem() {
    var splitItem = getCurrentSplitItem();
    if(splitItem != null) {
	currSplitItemClickedViaJQ = true;
	$('#splitSegmentItem-' + splitItem.id).click();
    }
}

/**
 * set current time as the new inpoint of selected item
 */
function setCurrentTimeAsNewInpoint() {
    if (editor.selectedSplit != null) {
	$('#clipBegin').timefield('option', 'value', editor.player.prop("currentTime") + "s");
    }
}

/**
 * set current time as the new outpoint of selected item
 */
function setCurrentTimeAsNewOutpoint() {
    if (editor.selectedSplit != null) {
	$('#clipEnd').timefield('option', 'value', editor.player.prop("currentTime") + "s");
    }
}

/**
 * retrieves the current split item by time
 * 
 * @returns the current split item
 */
function getCurrentSplitItem() {
    var currentTime = editor.player.prop("currentTime");
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
	var splitItem = editor.splitData.splits[i];
	var clipBegin = parseFloat(splitItem.clipBegin.replace("s", ""));
	var clipEnd = parseFloat(splitItem.clipEnd.replace("s", ""));
	if (clipBegin <= currentTime && currentTime < clipEnd) {
	    splitItem.id = i;
	    return splitItem;
	}
    }
    // return null;
    return currSplitItem;
}

/**
 * click/shortcut handler for adding a split item at current time
 */
function splitButtonClick() {
    var currentTime = editor.player.prop('currentTime');
    for ( var i = 0; i < editor.splitData.splits.length; i++) {
	var splitItem = editor.splitData.splits[i];
	var clipBegin = parseFloat(splitItem.clipBegin.replace("s", ""));
	var clipEnd = parseFloat(splitItem.clipEnd.replace("s", ""));
	if (clipBegin < currentTime && currentTime < clipEnd) {
	    newEnd = "0s";
	    if (editor.splitData.splits.length == i + 1) {
		newEnd = splitItem.clipEnd;
	    } else {
		newEnd = editor.splitData.splits[i + 1].clipBegin;
	    }
	    var newItem = {
		clipBegin : currentTime + 's',
		clipEnd : newEnd,
		enabled : true,
		description : ""
	    }
	    splitItem.clipEnd = currentTime + 's';
	    editor.splitData.splits.splice(i + 1, 0, newItem);
	    editor.updateSplitList();
	    selectSegmentListElement(i + 1);
	    return;
	}
    }
    selectCurrentSplitItem();
}

function selectSegmentListElement(number)
{
    if($('#splitItemDiv-' + number))
    {
	$('#splitItemDiv-' + number).click();
    }
}

/**
 * when player is ready set all neccassary stuff
 */
function playerReady() {
    if (!editor.ready) {
	editor.ready = true;
	// create additional data output
	$('#videoHolder').append('<div id="segmentsWaveform"></div>');
	$('#segmentsWaveform').append('<div id="splitSegments"></div>')
	$('#segmentsWaveform').append('<div id="imageDiv"><img id="waveformImage" alt="waveform"/></div>');
	$('#segmentsWaveform').append('<div id="currentTimeDiv"></div>');

	// paint a green line for current time
	editor.player.bind("timeupdate", function() {
	    var duration = workflowInstance.mediapackage.duration / 1000;
	    var perc = editor.player.prop("currentTime") / duration * 100;
	    $('#currentTimeDiv').css("left", perc + "%");
	});

	// create standard split point
	editor.splitData.splits.push({
	    clipBegin : '0s',
	    clipEnd : workflowInstance.mediapackage.duration / 1000 + "s",
	    enabled : true,
	    description : ""
	});

	// load smil if there is already one
	smil = null;
	$.each(workflowInstance.mediapackage.metadata.catalog, function(key, value) {
	    if (value.type == SMIL_FLAVOR) {
		// download smil
		smil = editor.downloadSMIL();
		// check whether SMIL has already cutting points
		if (smil.smil.body.seq.par) {

		    editor.splitData.splits = [];
		    smil.smil.body.seq.par = ocUtils.ensureArray(smil.smil.body.seq.par);
		    $.each(smil.smil.body.seq.par, function(key, value) {
			value.ref = ocUtils.ensureArray(value.ref);
			editor.splitData.splits.push({
			    clipBegin : value.ref[0].clipBegin,
			    clipEnd : value.ref[0].clipEnd,
			    enabled : true,
			    description : value.ref[0].description ? value.ref[0].description : "",
			});
		    });	
		    
		    window.setTimeout(function() { $('#splitSegmentItem-0').click(); }, 200);	    
		    /*
		      $('<div/>').html(
		      "Found existing SMIL from silence detection. Do you want to transfer the data into the list?").dialog({
		      buttons : {
		      Yes : function() {
		      editor.splitData.splits = [];
		      smil.smil.body.seq.par = ocUtils.ensureArray(smil.smil.body.seq.par);
		      $.each(smil.smil.body.seq.par, function(key, value) {
		      value.ref = ocUtils.ensureArray(value.ref);
		      editor.splitData.splits.push({
		      clipBegin : value.ref[0].clipBegin,
		      clipEnd : value.ref[0].clipEnd,
		      enabled : true,
		      description : value.ref[0].description ? value.ref[0].description : "",
		      });
		      });
		      $(this).dialog('close');
		      editor.updateSplitList();
		      selectSegmentListElement(0);
		      },
		      No : function() {
		      $(this).dialog('close');
		      }
		      },
		      title : "Apply existent SMIL"
		      });
		    */
		}
	    }
	});

	// create smil if it doesn't exist
	if (smil == null) {
	    editor.createSMIL();
	}
	// update split list and enable the editor
	editor.updateSplitList();
	$('#editor').removeClass('disabled');

	// load waveform image
	$.each(ocUtils.ensureArray(workflowInstance.mediapackage.attachments.attachment), function(key, value) {
	    if (value.type == WAVEFORM_FLAVOR) {
		$('#waveformImage').prop("src", value.url);
		$('#waveformImage').load(function() {
		    $('#segmentsWaveform').height($('#waveformImage').height());
		});
		$(window).resize(function(evt) {
		    $('#segmentsWaveform').height($('#waveformImage').height());
		    $('.holdStateUI').height($('#segmentsWaveform').height() + $('#videoPlayer').height() + 70);
		});
		// $('#waveformImage').addimagezoom();
		// $('#splitSegments').mouseover(function(evt) {
		// $('#waveformImage').trigger("mouseover");
		// }).mouseout(function(evt) {
		// $('#waveformImage').trigger("mouseout");
		// }).mousemove(function(evt) {
		// $('#waveformImage').trigger("mousemove");
		// })
	    }
	});

	// adjust size of the holdState UI
	var height = parseInt($('.holdStateUI').css('height').replace("px", ""));
	var heightVideo = parseInt($('#videoHolder').css('height').replace("px", ""));
	$('.holdStateUI').css('height', (height + heightVideo) + "px");
	parent.ocRecordings.adjustHoldActionPanelHeight();

	// grab the focus in the Iframe so one can use the keyboard shortcuts
	$(window).focus();

	// add click handler for btns in control bar
	$('.video-previous-marker').click(previousSegment);
	$('.video-next-marker').click(nextSegment);
	$('.video-play-pre-post').click(playWithoutDeleted);

	// add timelistener for current time in description div
	editor.player.on("timeupdate", function() {
	    $('#descriptionCurrentTime').html(formatTime(editor.player.prop("currentTime")));
	    selectCurrentSplitItem();
	});

	$('#clipBegin input').blur(function(evt) {
	    checkClipBegin();
	});
	$('#clipEnd input').blur(function(evt) {
	    checkClipEnd();
	});

	// add evtl handler for enter in editing fields
	$('#clipBegin input').keyup(function(evt) {
	    if (evt.keyCode == 13) {
		okButtonClick();
	    }
	});
	$('#clipEnd input').keyup(function(evt) {
	    if (evt.keyCode == 13) {
		okButtonClick();
	    }
	});
    }

    selectCurrentSplitItem();
}

/**
 * play the video
 */
function playVideo() {
    // if (!editor.player.prop("playing")) {
    editor.player[0].play();
    // }
}

/**
 * pause the video
 */
function pauseVideo() {
    if (!editor.player.prop("paused")) {
	editor.player[0].pause();
    }
}

/**
 * updates the currentTime div
 */
function updateCurrentTime() {
    $('#current_time').html(formatTime(editor.player.prop("currentTime")));
}

/**
 * formating a time String to hh:MM:ss.mm
 * 
 * @param time
 *          the timeString
 * @returns the formated time string
 */
function formatTime(time) {
    if (typeof time == "string") {
	time = time.replace("s", "");
	time = parseFloat(time);
    }
    seconds = parseInt(time);
    millis = parseInt((time - seconds) * 100);
    formatted = ocUtils.formatSeconds(time) + "." + millis;
    return formatted;
}

/**
 * click/shortcut handler for removing current split item
 */
function splitRemoverClick() {
    item = $(this);
    id = item.prop('id');
    if (id != undefined) {
	id = id.replace("splitItem-", "");
	id = id.replace("splitRemover-", "");
	id = id.replace("splitAdder-", "");
    } else {
	id = "";
    }
    if (id == "" || id == "deleteButton") {
	id = $('#splitUUID').val();
    }
    if (editor.splitData.splits[id].enabled) {
	$('#deleteDialog').dialog({
	    buttons : {
		"Yes" : function() {
		    $('#splitItemDiv-' + id).addClass('disabled');
		    $('#splitRemover-' + id).hide();
		    $('#splitAdder-' + id).show();
		    $('.splitItem').removeClass('splitItemSelected');
		    $(this).dialog('close');
		    setEnabled(id, false);
		},
		"No" : function() {
		    $(this).dialog('close')
		}
	    },
	    title : "Remove Item?"
	});
    } else {
	$('#splitItemDiv-' + id).removeClass('disabled');
	$('#splitRemover-' + id).show();
	$('#splitAdder-' + id).hide();
	setEnabled(id, true);
    }
    cancelButtonClick();
}

/**
 * enable/disable a split item
 * 
 * @param uuid
 *          the id of the splitItem
 * @param enabled
 *          whether enabled or not
 */
function setEnabled(uuid, enabled) {
    editor.splitData.splits[uuid].enabled = enabled;
    editor.updateSplitList();
}

/**
 * click handler for selecting a split item in segment bar or list
 */
function splitItemClick() {
    if(!isSeeking || (isSeeking && ($(this).prop('id').indexOf('Div-') == -1))) {
	now = new Date();
    }

    if((now - lastTimeSplitItemClick) > 80) {
	lastTimeSplitItemClick = now;

	// if it's not already disabled
	if (!$(this).hasClass('disabled') && ((isSeeking && ($(this).prop('id').indexOf('Div-') == -1)) || !isSeeking)) {

	    // remove all selected classes
	    $('.splitSegmentItem').removeClass('splitSegmentItemSelected');
	    $('.splitItem').removeClass('splitItemSelected');

	    // get the id of the split item
	    id = $(this).prop('id');
	    id = id.replace('splitItem-', '');
	    id = id.replace('splitItemDiv-', '');
	    id = id.replace('splitSegmentItem-', '');

	    // add the selected class to the corresponding items
	    $('#splitItem-' + id).addClass('splitItemSelected');
	    $('#splitSegmentItem-' + id).addClass('splitSegmentItemSelected');

	    $('#splitSegmentItem-' + id).removeClass('hover hoverOpacity');

	    // load data into right box
	    splitItem = editor.splitData.splits[id];
	    editor.selectedSplit = splitItem;
	    editor.selectedSplit.id = parseInt(id);
	    $('#splitDescription').val(splitItem.description);
	    $('#splitUUID').val(id);
	    $('#clipBegin').timefield('option', 'value', splitItem.clipBegin);
	    $('#clipEnd').timefield('option', 'value', splitItem.clipEnd);
	    $('#splitIndex').html(parseInt(id) + 1);

	    currSplitItem = splitItem;
	    
	    var clipBegin = parseFloat(splitItem.clipBegin.replace("s", ""));
	    if(!timeoutUsed) {
		if(!currSplitItemClickedViaJQ) {
		    editor.player.prop("currentTime", clipBegin);
		}
	    }

	    enabledRightBox(true);
	}
    }
}

function isSeeking() {
}

function hasSeeked() {
}

$(document).ready(function() {
    editor.player = $('#videoPlayer');
    editor.player.on("canplay", playerReady);
    editor.player.on("seeking", isSeeking);
    editor.player.on("seeked", hasSeeked);
    $('.clipItem').timefield();
    $('.video-split-button').click(splitButtonClick);
    $('#okButton').click(okButtonClick);
    $('#cancelButton').click(cancelButtonClick);
    $('#deleteButton').click(splitRemoverClick);

    enabledRightBox(false);
    initPlayButtons();
    addShortcuts();

    checkClipBegin();
    checkClipEnd();

    // 37 - left, 38 - up, 39 - right, 40 - down
    $(document).keydown(function(e){
	var keyCode = e.keyCode || e.which();
	if ((keyCode == 37) || (keyCode == 38) || (keyCode == 39) || (keyCode == 40)) {
	    isSeeking=true;
	    return false;
	}
    }).keyup(function(e){
	var keyCode = e.keyCode || e.which();
	if ((keyCode == 37) || (keyCode == 38) || (keyCode == 39) || (keyCode == 40)) {
	    isSeeking=false;
	    lastTimeSplitItemClick = new Date();
	    return false;
	}
    });

    $(document).click(function() {
	if(!currSplitItemClickedViaJQ) {
	    clearEvents();
	    if(timeout3 != null) {
		window.clearTimeout(timeout3);
		timeout3 = null;
	    }
	    if(timeout4 != null) {
		window.clearTimeout(timeout4);
		timeout4 = null;
	    }
	}
	currSplitItemClickedViaJQ = false;
    });
})