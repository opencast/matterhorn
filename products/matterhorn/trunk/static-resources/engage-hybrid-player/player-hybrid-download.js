/*global $, Videodisplay, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
    @namespace the global Opencast namespace
*/
var Opencast = Opencast || {};

Opencast.volume = 1.0;
Opencast.mouseOverBool = false;



/**
@namespace the global Opencast namespace global
*/
Opencast.global = (function () {

    var playing = "playing",
    pausing     = "pausing",
    unmute      = "Unmute",
    mute        = "Mute",
    infoBool    = false;

    /**
        @memberOf Opencast.global
        @description Do unmute the volume of the video.
    */
    function doUnmute() {
        if ($("#btn_volume").attr("value") === unmute) {  
            $("#btn_volume").attr({ 
                value: mute,
                alt: mute,
                title: mute
            });
            $("#btn_volume").attr("className", "oc-btn-volume-high");
        } 
    }
    
    /**
        @memberOf Opencast.global
        @description Mouse over effect, change the css style.
    */
    function mouseOver() {
        if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === playing) {
            $("#btn_play_pause").attr("className", "btn_pause_over");
            Opencast.mouseOverBool = true;
        }
         else if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === pausing) {
            $("#btn_play_pause").attr("className", "btn_play_over");
            Opencast.mouseOverBool = true;
        }
    }
    
    /**
        @memberOf Opencast.global
        @description Mouse out effect, change the css style.
    */
    function mouseOut() {
        if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === playing) {
            $("#btn_play_pause").attr("className", "btn_pause_out");
            Opencast.mouseOverBool = false;
        }
        else if (Opencast.ToVideodisplay.getCurrentPlayPauseState() === pausing) {
            $("#btn_play_pause").attr("className", "btn_play_out");
            Opencast.mouseOverBool = false;
        }
    }
    
 
    /**
        @memberOf Opencast.global
        @description Remove the alert div.
    */
    function removeOldAlert()
    {
        var oldAlert = document.getElementById("alert");
        if (oldAlert)
        {
            document.body.removeChild(oldAlert);
        }
    }

    /**
        @memberOf Opencast.global
        @description Remove the old alert div and create an new div with the aria role alert.
        @param String alertMessage
    */
    function addAlert(alertMessage)
    {
        removeOldAlert();
        var newAlert = document.createElement("div");
        newAlert.setAttribute("role", "alert");
        newAlert.setAttribute("id", "alert");
        newAlert.setAttribute("class", "fl-offScreen-hidden");
        var msg = document.createTextNode(alertMessage);
        newAlert.appendChild(msg);
        document.body.appendChild(newAlert);
    }

    
    /**
        @memberOf Opencast.global
        @description When the learner edit the current time.
    */
    function editTime()
    {
    
        var timeString = $("#editField").attr("value");
        timeString = timeString.replace(/[-\/]/g, ':'); 
        timeString = timeString.replace(/[^0-9: ]/g, ''); 
        timeString = timeString.replace(/ +/g, ' '); 
    
        var time = timeString.split(':');

        try
        {
            var seekHour = parseInt(time[0], 10);
            var seekMinutes = parseInt(time[1], 10);
            var seekSeconds = parseInt(time[2], 10);
      
        }
        catch (exception) 
        {
            addAlert('Wrong Time enter like this: HH:MM:SS');
        }
       
        if (seekHour > 99 || seekMinutes > 59 || seekSeconds > 59)
        {
            addAlert('Wrong Time enter like this: HH:MM:SS');
        } 
        else 
        {
            var seek = (seekHour * 60 * 60) + (seekMinutes * 60) + (seekSeconds);
            Videodisplay.seek(seek);
        }
    }
    
    /**
        @memberOf Opencast.global
        @description Toggle between Keyboard Shurtcuts visible or unvisible.
    */
    function toggleInfo() {
        if (infoBool === false)
        {
            $("#infoBlock").attr("className", "oc_infoDisplayBlock");
            addAlert( $("#infoBlock").text());
            infoBool = true;
        }
        else if (infoBool === true)
        {
            $("#infoBlock").attr("className", "oc_infoDisplayNone");
            infoBool = false;
        }
    }

    /**
        @memberOf Opencast.global
        @description When the learner press a key.
        @param Event evt
    */
    function keyListener(evt) {
        var charCode;
        if (evt && evt.which)
        {
            evt = evt;
            charCode = evt.which;
        }
        else
        {
            evt = event;
            charCode = evt.keyCode;
        }
        
        if (charCode === 13) // return
        {
            editTime(); 
        }
    }
    
    return {
        doUnmute : doUnmute,
        mouseOver : mouseOver,
        mouseOut : mouseOut,
        editTime : editTime,
        keyListener : keyListener,
        addAlert : addAlert,
        removeOldAlert : removeOldAlert,
        toggleInfo : toggleInfo
    };
}());