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

editor.splitData = {};
editor.splitData.splits = [];
editor.selectedSplit = null;
editor.player = null;
editor.smil = null;
editor.canvas = null;
editor.ready = false;

var timeout1 = null;
var timeout2 = null;
var timeout3 = null;
var timeout4 = null;
var currEvt = null;
var jumpBackTime = null;
var currSplitItem = null;
var lastTimeSplitItemClick = 0;
var endTime = 0;
var now = 100;
var isSeeking = false;
var timeoutUsed = false;
var currSplitItemClickedViaJQ = false;

/******************************************************************************/
// editor
/******************************************************************************/

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

/******************************************************************************/
// getter
/******************************************************************************/

function getCurrentTime() {
    var currentTime = editor.player.prop("currentTime");
    currentTime = isNaN(currentTime) ? 0 : currentTime;
    return currentTime;
}

function getDuration() {
    var duration = editor.player.prop("duration");
    duration = isNaN(duration) ? 0 : duration;
    return duration;
}

function getPlayerPaused() {
    var paused = editor.player.prop("paused");
    return paused;
}

/**
 * retrieves the current split item by time
 * 
 * @returns the current split item
 */
function getCurrentSplitItem() {
    var currentTime = getCurrentTime();
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
	var splitItem = editor.splitData.splits[i];
	if ((splitItem.clipBegin <= currentTime) && (currentTime < splitItem.clipEnd)) {
	    splitItem.id = i;
	    return splitItem;
	}
    }
    return currSplitItem;
}

function getTimefieldTimeBegin() {
    return $('#clipBegin').timefield('option', 'value');
}

function getTimefieldTimeEnd() {
    return $('#clipEnd').timefield('option', 'value');
}

/******************************************************************************/
// setter
/******************************************************************************/

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
 * set current time as the new inpoint of selected item
 */
function setCurrentTimeAsNewInpoint() {
    if (editor.selectedSplit != null) {
	setTimefieldTimeBegin(getCurrentTime());
    }
}

/**
 * set current time as the new outpoint of selected item
 */
function setCurrentTimeAsNewOutpoint() {
    if (editor.selectedSplit != null) {
	setTimefieldTimeEnd(getCurrentTime());
    }
}

function setCurrentTime(time) {
    time = isNaN(time) ? 0 : time;
    var duration = getDuration();
    time = (time > duration) ? duration : time;
    editor.player.prop("currentTime", time);
}

function setTimefieldTimeBegin(time) {
    $('#clipBegin').timefield('option', 'value', time);
}

function setTimefieldTimeEnd(time) {
    $('#clipEnd').timefield('option', 'value', time);
}

/******************************************************************************/
// helper
/******************************************************************************/

/**
 * formatting a time string to hh:MM:ss.mm
 * 
 * @param seconds
 *          the timeString in seconds
 * @returns the formated time string
 */
function formatTime(seconds) {
    if (typeof seconds == "string") {
	seconds = parseFloat(seconds);
    }
    
    var h = "00";
    var m = "00";
    var s = "00";
    if (!isNaN(seconds) && (seconds >= 0)) {
	var tmpH = Math.floor(seconds / 3600);
	var tmpM = Math.floor((seconds - (tmpH * 3600)) / 60);
	var tmpS = Math.floor(seconds - (tmpH * 3600) - (tmpM * 60));
	var tmpMS = seconds - tmpS;
	h = (tmpH < 10) ? "0" + tmpH : (Math.floor(seconds / 3600) + "");
	m = (tmpM < 10) ? "0" + tmpM : (tmpM + "");
	s = (tmpS < 10) ? "0" + tmpS : (tmpS + "");
	ms = tmpMS + "";
	var indexOfSDot = ms.indexOf(".");
	if(indexOfSDot != -1) {
	    ms = ms.substr(indexOfSDot + 1, ms.length);
	}
	ms = ms.substr(0, 4);
	while(ms.length < 4) {
	    ms += "0";
	}
    }
    return h + ":" + m + ":" + s + "." + ms;
}

/******************************************************************************/
// checks
/******************************************************************************/

function isInInterval(toCheck, lower, upper) {
    return (toCheck >= lower) && (toCheck <= upper);
}

function checkClipBegin() {
    var clipBegin = getTimefieldTimeBegin();
    if(isNaN(clipBegin) || (clipBegin < 0))
    {
	displayError("The inpoint is too low or the format is not correct. Correct format: hh:MM:ss.mmmm. Please check.", "Check inpoint");
	return false;
    }
    return true;
}

function checkClipEnd() {
    var clipEnd = getTimefieldTimeEnd();
    var duration = getDuration();
    if(isNaN(clipEnd) || (clipEnd > duration)) {
	displayError("The outpoint is too high or the format is not correct. Correct format: hh:MM:ss.mmmm. Please check.", "Check outpoint");
	return false;
    }
    return true;
}

function checkPrevAndNext(id) {
    var duration = getDuration();
    // new first item
    if (id == 0) {
        if (editor.splitData.splits.length > 1) {
            var next = editor.splitData.splits[id + 1];
            next.clipBegin = splitItem.clipEnd;
        }
        if (getTimefieldTimeBegin() != 0) {
            var newSplitItem = {
                description : "",
                clipBegin : 0,
                clipEnd : splitItem.clipBegin,
                enabled : true
            };
	    
            // add new item to front
            editor.splitData.splits.splice(0, 0, newSplitItem);
        }
	// new last item
    } else if (id == editor.splitData.splits.length - 1) {
	var duration = getDuration();
	if (getTimefieldTimeEnd() != duration) {
	    var newLastItem = {
		description : "",
		clipBegin : splitItem.clipEnd,
		clipEnd : duration,
		enabled : true
	    };

	    // add the new item to the end
	    editor.splitData.splits.push(newLastItem);
	}
	var prev = editor.splitData.splits[id - 1];
	prev.clipEnd = splitItem.clipBegin;
	// in the middle
    } else {
	var prev = editor.splitData.splits[id - 1];
	var next = editor.splitData.splits[id + 1];

	if(getTimefieldTimeBegin() <= prev.clipBegin) {
	    displayError("The inpoint is lower than the begin of the last segment. Please check.", "Check inpoint");
	    return false;
	}
	if(getTimefieldTimeEnd() >= next.clipEnd) {
	    displayError("The outpoint is bigger than the end of the next segment. Please check.", "Check outpoint");
	    return false;
	}

	prev.clipEnd = splitItem.clipBegin;
	next.clipBegin = splitItem.clipEnd;
    }
    return true;
}

/******************************************************************************/
// click
/******************************************************************************/

/**
 * click handler for saving data in editing box
 */
function okButtonClick() {
    if(checkClipBegin() && checkClipEnd()) {
	id = $('#splitUUID').val();
	if (id != "") {
	    id = parseInt(id);
	    if (getTimefieldTimeBegin() > getTimefieldTimeEnd()) {
		displayError("The inpoint is bigger than the outpoint. Please check.", "Check inpoint and outpoint");
		selectSegmentListElement(id);
		return;
	    }

	    var tmpBegin = splitItem.clipBegin;
	    var tmpEnd = splitItem.clipEnd;
	    var tmpDescription = splitItem.description;

	    splitItem = editor.splitData.splits[id];
	    splitItem.clipBegin = getTimefieldTimeBegin();
	    splitItem.clipEnd = getTimefieldTimeEnd();
	    splitItem.description = $('#splitDescription').val();
	    if(checkPrevAndNext(id)) {
		editor.updateSplitList(true);
		$('#videoPlayer').focus();
		selectSegmentListElement(id);
	    } else {
		splitItem = editor.splitData.splits[id];
		splitItem.clipBegin = tmpBegin;
		splitItem.clipEnd = tmpEnd;
		splitItem.description = tmpDescription;
		selectSegmentListElement(id);
	    }
	}
    } else {
	selectCurrentSplitItem();
    }
}

/**
 * click handler for canceling editing
 */
function cancelButtonClick() {
    $('#splitDescription').html('');
    $('#splitUUID').val('');
    $('#splitDescription').val("");
    setTimefieldTimeBegin(0);
    setTimefieldTimeEnd(0);
    $('#splitIndex').html('#');
    $('.splitItem').removeClass('splitItemSelected');
    $('.splitSegmentItem').removeClass('splitSegmentItemSelected');
    editor.selectedSplit = null;
    enableRightBox(false);
}

/**
 * click/shortcut handler for removing current split item
 */
function splitRemoverClick() {
    item = $(this);
    var id = item.prop('id');
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
 * click handler for selecting a split item in segment bar or list
 */
function splitItemClick() {
    if(!isSeeking || (isSeeking && ($(this).prop('id').indexOf('Div-') == -1))) {
	now = new Date();
    }

    if((now - lastTimeSplitItemClick) > 80) {
	lastTimeSplitItemClick = now;

	// if not disabled and not seeking
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
	    setTimefieldTimeBegin(splitItem.clipBegin);
	    setTimefieldTimeEnd(splitItem.clipEnd);
	    $('#splitIndex').html(parseInt(id) + 1);

	    currSplitItem = splitItem;
	    
	    if(!timeoutUsed) {
		if(!currSplitItemClickedViaJQ) {
		    setCurrentTime(splitItem.clipBegin);
		}
		$('.video-timer').html(formatTime(getCurrentTime()) + "/" + formatTime(getDuration()));
	    }

	    enableRightBox(true);
	}
    }
}

/**
 * click/shortcut handler for adding a split item at current time
 */
function splitButtonClick() {
    var currentTime = getCurrentTime();
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
	var splitItem = editor.splitData.splits[i];
	if ((splitItem.clipBegin < currentTime) && (currentTime < splitItem.clipEnd)) {
	    newEnd = 0;
	    if (editor.splitData.splits.length == (i + 1)) {
		newEnd = splitItem.clipEnd;
	    } else {
		newEnd = editor.splitData.splits[i + 1].clipBegin;
	    }
	    var newItem = {
		clipBegin : currentTime,
		clipEnd : newEnd,
		enabled : true,
		description : ""
	    }
	    splitItem.clipEnd = currentTime;
	    editor.splitData.splits.splice(i + 1, 0, newItem);
	    editor.updateSplitList();
	    selectSegmentListElement(i + 1);
	    return;
	}
    }
    selectCurrentSplitItem();
}

/******************************************************************************/
// select
/******************************************************************************/

/**
 * select the split at the current time
 */
function selectCurrentSplitItem() {
    var splitItem = getCurrentSplitItem();
    if(splitItem != null) {
	currSplitItemClickedViaJQ = true;
	$('#splitSegmentItem-' + splitItem.id).click();
	$('#descriptionCurrentTime').html(formatTime(getCurrentTime()));
    }
}

function selectSegmentListElement(number) {
    if($('#splitItemDiv-' + number))
    {
	$('#splitItemDiv-' + number).click();
    }
}

/******************************************************************************/
// visual
/******************************************************************************/

function displayError(errorMsg, errorTitle) {
    $('<div />').html(errorMsg).dialog({
	title : errorTitle,
	resizable : false,
	buttons : {
	    OK : function() {
		$(this).dialog("close");
	    }
	}
    });
}

/**
 * updates the currentTime div
 */
function updateCurrentTime() {
    $('#current_time').html(formatTime(getCurrentTime()));
}

/**
 * enable/disable the right editing box
 * 
 * @param enabled
 *          whether enabled or not
 */
function enableRightBox(enabled) {
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

/******************************************************************************/
// split
/******************************************************************************/

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

/******************************************************************************/
// events
/******************************************************************************/

function isSeeking() {
}

function hasSeeked() {
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
 * clearing events2
 */
function clearEvents2() {
    if(timeout3 != null) {
	window.clearTimeout(timeout3);
	timeout3 = null;
    }
    if(timeout4 != null) {
	window.clearTimeout(timeout4);
	timeout4 = null;
    }
    editor.player.on("play", {
	duration : 0,
	endTime : getDuration()
    }, onPlay);
    clearEvents();
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
 * the timeout function pausing the video again
 */
function onTimeout() {
    if(!timeoutUsed) {
	pauseVideo();
	var check = function() {
	    endTime = currEvt.data.endTime;
	    if (endTime > getCurrentTime()) {
		playVideo();
		timeout2 = window.setTimeout(check, 10);
		timeoutUsed = true;
	    } else {
		clearEvents();
		pauseVideo();
		if((timeout3 == null) && (timeout4 == null)) {
		    editor.player.on("play", {
			duration : 0,
			endTime : getDuration()
		    }, onPlay);
		}
		
		jumpBackTime = currEvt.data.jumpBackTime;
		jumpBackTime = ((jumpBackTime == null) || (jumpBackTime == undefined)) ? null : jumpBackTime;
		if(jumpBackTime != null) {
		    setCurrentTime(jumpBackTime);
		    jumpBackTime = null;
		}
	    }
	}
	check();
    }
}

/******************************************************************************/
// play/pause
/******************************************************************************/

/**
 * play the video
 */
function playVideo() {
    editor.player[0].play();
}

/**
 * pause the video
 */
function pauseVideo() {
    if (!getPlayerPaused()) {
	editor.player[0].pause();
    }
}

/**
 * plays the current split item from it's beginning
 */
function playCurrentSplitItem() {
    var splitItem = getCurrentSplitItem();
    if (splitItem != null) {
	pauseVideo();
	var duration = (splitItem.clipEnd - splitItem.clipBegin) * 1000;
	setCurrentTime(splitItem.clipBegin);

	clearEvents();
	editor.player.on("play", {
	    duration : duration,
	    endTime : splitItem.clipEnd
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
	var clipEnd = splitItem.clipEnd;
	setCurrentTime(clipEnd - 2);

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
    
    if (splitItem != null) {
	pauseVideo();

	var clipStartFrom = -1;
	var clipStartTo = -1;
	var segmentStart = splitItem.clipBegin;
	var segmentEnd = splitItem.clipEnd;
	var clipEndFrom = -1;
	var clipEndTo = -1;
	var hasPrevElem = true;
	var hasNextElem = true;

	if((splitItem.id - 1) >= 0) {
	    hasPrevElem = true;
	    var prevSplitItem = editor.splitData.splits[splitItem.id - 1];
	    while(!prevSplitItem.enabled) {
		if((prevSplitItem.id - 1) < 0) {
		    hasPrevElem = false;
		    break;
		} else {
		    prevSplitItem = editor.splitData.splits[prevSplitItem.id - 1];
		}
	    }
	    if(hasPrevElem) {
		clipStartTo = prevSplitItem.clipEnd;
		clipStartFrom = clipStartTo - 2;
	    }
	}
	if(hasPrevElem) {
	    clipStartFrom = (clipStartFrom < 0) ? 0 : clipStartFrom;
	}

	if((splitItem.id + 1) < editor.splitData.splits.length) {
	    hasNextElem = true;
	    var nextSplitItem = editor.splitData.splits[splitItem.id + 1];
	    while(!nextSplitItem.enabled) {
		if((nextSplitItem.id + 1) >= editor.splitData.splits.length) {
		    hasNextElem = false;
		    break;
		} else {
		    nextSplitItem = editor.splitData.splits[nextSplitItem.id + 1];
		}
	    }
	    if(hasNextElem) {
		clipEndFrom = nextSplitItem.clipBegin;
		clipEndTo = clipEndFrom + 2;
	    }
	}
	if(hasNextElem) {
	    var duration = getDuration();
	    clipEndTo = (clipEndTo > duration) ? duration : clipEndTo;
	}

	ocUtils.log("Play Times: " + clipStartFrom + " - " + clipStartTo + " | " + segmentStart + " - " + segmentEnd + " | " + clipEndFrom + " - " + clipEndTo);

	if(hasPrevElem && hasNextElem) {
	    setCurrentTime(clipStartFrom);
	    clearEvents();
	    editor.player.on("play", {
		duration : (clipStartTo - clipStartFrom) * 1000,
		endTime : clipStartTo
	    }, onPlay);
	    
	    playVideo();
	    
	    timeout3 = window.setTimeout(function(){
		pauseVideo();
		setCurrentTime(segmentStart);
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
		setCurrentTime(clipEndFrom);
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
	} else if(!hasPrevElem && hasNextElem) {
	    setCurrentTime(segmentStart);
	    clearEvents();
	    editor.player.on("play", {
		duration : (segmentEnd - segmentStart) * 1000,
		endTime : segmentEnd
	    }, onPlay);
	    
	    playVideo();
	    
	    timeout3 = window.setTimeout(function(){
		pauseVideo();
		setCurrentTime(clipEndFrom);
		clearEvents();
		currSplitItemClickedViaJQ = true;
		editor.player.on("play", {
		    duration : (clipEndTo - clipEndFrom) * 1000,
		    endTime : clipEndTo
		}, onPlay);
		playVideo();
		if(timeout3 != null) {
		    window.clearTimeout(timeout3);
		    timeout3 = null;
		}
	    }, ((segmentEnd - segmentStart) * 1000));
	} else if(hasPrevElem && !hasNextElem) {
	    setCurrentTime(clipStartFrom);
	    clearEvents();
	    editor.player.on("play", {
		duration : (clipStartTo - clipStartFrom) * 1000,
		endTime : clipStartTo
	    }, onPlay);
	    
	    playVideo();
	    
	    timeout3 = window.setTimeout(function(){
		pauseVideo();
		setCurrentTime(segmentStart);
		clearEvents();
		currSplitItemClickedViaJQ = true;
		editor.player.on("play", {
		    duration : (segmentEnd - segmentStart) * 1000,
		    endTime : segmentEnd
		}, onPlay);
		playVideo();
		if(timeout3 != null) {
		    window.clearTimeout(timeout3);
		    timeout3 = null;
		}
	    }, (clipStartTo - clipStartFrom) * 1000);
	} else if(!hasPrevElem && !hasNextElem) {
	    clearEvents();
	    editor.player.on("play", {
		duration : (segmentEnd - segmentStart) * 1000,
		endTime : segmentEnd
	    }, onPlay);
	    
	    playVideo();
	}
    }
}

/******************************************************************************/
// jumps
/******************************************************************************/

/**
 * jump to beginning of current split item
 */
function jumpToSegment() {
    id = $(this).prop('id');
    id = id.replace('splitItem-', '');
    id = id.replace('splitItemDiv-', '');
    id = id.replace('splitSegmentItem-', '');

    setCurrentTime(editor.splitData.splits[id].clipBegin);
}

/**
 * jump to next segment
 */
function nextSegment() {
    var playerPaused = getPlayerPaused();
    if(!playerPaused) {
	pauseVideo();
    }
    var currentTime = getCurrentTime();
    var new_id = -1;
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
	var splitItem = editor.splitData.splits[i];
	if((isInInterval(currentTime, splitItem.clipBegin - 0.1, splitItem.clipBegin + 0.1) || (currentTime >= splitItem.clipBegin)) &&
	   (!isInInterval(currentTime, splitItem.clipEnd - 0.1, splitItem.clipEnd + 0.1) && (currentTime < splitItem.clipEnd))) {
	    new_id = i + 1;
	    break;
	}
	
    }
    if(new_id > 0) {
	if (new_id <= editor.splitData.splits.length - 1) {
	    setCurrentTime(editor.splitData.splits[new_id].clipBegin);
	} else if (new_id <= editor.splitData.splits.length) {
	    setCurrentTime(getDuration());
	}
    }
}

/**
 * jump to previous segment
 */
function previousSegment() {
    var playerPaused = getPlayerPaused();
    if(!playerPaused) {
	pauseVideo();
    }
    var currentTime = getCurrentTime();
    var new_id = -1;
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
	var splitItem = editor.splitData.splits[i];
	if(((currentTime > splitItem.clipBegin) && (currentTime < splitItem.clipEnd)) || isInInterval(currentTime, splitItem.clipEnd - 0.1, splitItem.clipEnd + 0.1)) {
	    new_id = i;
	    break;
	} else if(isInInterval(currentTime, splitItem.clipBegin - 0.1, splitItem.clipBegin + 0.1)) {
	    new_id = i - 1;
	    break;
	} else if ((i == (editor.splitData.splits.length - 1)) && (currentTime >= splitItem.clipEnd)) {
	    new_id = editor.splitData.splits.length - 1;
	    break;
	}
    }
    if (new_id >= 0) {
	setCurrentTime(editor.splitData.splits[new_id].clipBegin);
    }
}

/******************************************************************************/
// other
/******************************************************************************/

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
	if (getPlayerPaused()) {
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
	    clipBegin : 0,
	    clipEnd : workflowInstance.mediapackage.duration / 1000,
	    enabled : true,
	    description : ""
	});
	editor.updateSplitList();
    });

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
	    var perc = getCurrentTime() / duration * 100;
	    $('#currentTimeDiv').css("left", perc + "%");
	});

	// create standard split point
	editor.splitData.splits.push({
	    clipBegin : 0,
	    clipEnd : workflowInstance.mediapackage.duration / 1000,
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
			    clipBegin : parseFloat(value.ref[0].clipBegin),
			    clipEnd : parseFloat(value.ref[0].clipEnd),
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
		      clipBegin : parseFloat(value.ref[0].clipBegin),
		      clipEnd : parseFloat(value.ref[0].clipEnd),
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
		/*
		  $('#waveformImage').addimagezoom();
		  $('#splitSegments').mouseover(function(evt) {
		  $('#waveformImage').trigger("mouseover");
		  }).mouseout(function(evt) {
		  $('#waveformImage').trigger("mouseout");
		  }).mousemove(function(evt) {
		  $('#waveformImage').trigger("mousemove");
		  });
		*/
	    }
	});

	// adjust size of the holdState UI
	var height = parseInt($('.holdStateUI').css('height').replace("px", ""));
	var heightVideo = parseInt($('#videoHolder').css('height').replace("px", ""));
	$('#videoHolder').css('width', '98%');
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
	    selectCurrentSplitItem();
	});

	$('#clipBegin input').blur(function(evt) {
	    // checkClipBegin();
	});
	$('#clipEnd input').blur(function(evt) {
	    // checkClipEnd();
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

    enableRightBox(false);
    initPlayButtons();
    addShortcuts();

    checkClipBegin();
    checkClipEnd();

    // 37 - left, 38 - up, 39 - right, 40 - down
    $(document).keydown(function(e){
	var keyCode = e.keyCode || e.which();
	if(keyCode == 32) {
	    clearEvents2();
	}
	if (!$('#clipBegin').is(":focus") && !$('#clipEnd').is(":focus") && ((keyCode == 37) || (keyCode == 38) || (keyCode == 39) || (keyCode == 40))) {
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
	    clearEvents2();
	}
	currSplitItemClickedViaJQ = false;
    });

    $(window).resize(function() {
	if(this.resizeTO) {
	    clearTimeout(this.resizeTO);
	}
	this.resizeTO = setTimeout(function() {
            $(this).trigger('resizeEnd');
	}, 500);
    });

    $(window).bind('resizeEnd', function() {
	// window hasn't changed size in 500ms
	ocUtils.log("Resize done. Updating split list view.");
	editor.updateSplitList();
	selectCurrentSplitItem();
    });

    window.setTimeout(function() {
	selectCurrentSplitItem();

	if(!$.browser.webkit && !$.browser.mozilla) {
	    playVideo();
	    pauseVideo();
	    setCurrentTime(0);
	}
    }, 100);
})
