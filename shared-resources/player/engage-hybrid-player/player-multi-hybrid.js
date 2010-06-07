/*global $, Player, window, Videodisplay, Scrubber*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
    @namespace the global Opencast namespace
*/
var Opencast = Opencast || {};

/**
    @namespace Opencast namespace Player
*/
Opencast.Player = (function () {

    /**
     * 
     * 
        global
     */
    var PLAYING            = "playing",
    PAUSING                = "pausing",
    PLAY                   = "Play: Control + Alt + P",
    PAUSE                  = "Pause: Control + Alt + P",
    CCON                   = "Closed Caption On: Control + Alt + C",
    CCOFF                  = "Closed Caption Off: Control + Alt + C",
    UNMUTE                 = "Unmute: Control + Alt + M",
    MUTE                   = "Mute: Control + Alt + M",
    SLIDERVOLUME           = "slider_volume_Thumb",
    SLIDES                 = "Slides",
    SLIDESHIDE             = "Hide Slides",
    NOTES                  = "Notes",
    NOTESHIDE              = "Hide Notes",
    SLIDETEXT              = "Slide Text",
    SLIDETEXTHIDE          = "Hide Slide Text",
    TRANSCRIPT             = "Transcript",
    TRANSCRIPTHIDE         = "Hide Transcript",
    SHORTCUTS              = "Shortcuts",
    SHORTCUTSHIDE          = "Hide Shortcuts",
    EMBED                  = "Embed",
    EMBEDHIDE              = "Hide Embed",
    BOOKMARKS              = "Bookmarks",
    BOOKMARKSHIDE          = "Hide Bookmarks",
    DESCRIPTION            = "Description",
    DESCRIPTIONHIDE        = "Hide Description",
    MULTIPLAYER            = "Multiplayer",
    SINGLEPLAYER           = "Singleplayer",
    SINGLEPLAYERWITHSLIDES = "SingleplayerWithSlides",
    AUDIOPLAYER            = "Audioplayer",
    ADVANCEDPLAYER         = "advancedPlayer",
    EMBEDPLAYER            = "embedPlayer",
    OPTIONMYCLASSNAME      = "oc_option-myBookmark",
    currentPlayPauseState  = PAUSING,
    showSections           = true,
    mouseOverBool          = false,
    captionsBool           = false,
    dragging               = false,
    duration               = 0,
    mediaWith              = 0;
    mediaHeight            = 0;
    browserWidth           = 0;
    FLASH_PLAYERTYPE       = "",
    FLASH_PLAYERSTATE      = "",
    FLASH_VIEWSTATE        = "",
    FLASH_MUTE             = "",
    intval                 = "",
    displayMode            = "",
    optionClassName        = "",
    seekState              = PAUSING;
   
   
     /**
     @memberOf Opencast.Player
     @description Returns communication values from the Flash Videodisplay
      */
    function flashVars() 
    {
        return {
            'playerType':  FLASH_PLAYERTYPE,
            'playerState': FLASH_PLAYERSTATE,
            'viewState':   FLASH_VIEWSTATE,
            'mute':        FLASH_MUTE
        };
    }
     
     
     /**
        @memberOf Opencast.Player
        @description Get the current play pause state.
     */
    function getCurrentPlayPauseState()
    {
        return currentPlayPauseState;
    }
    
    
    /**
        @memberOf Opencast.Player
        @description Set the current play pause state.
        @param String state
     */
    function setCurrentPlayPauseState(state)
    {
        currentPlayPauseState = state;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the seek state before.
     */
    function getSeekState()
    {
        return seekState;
    }

    /**
        @memberOf Opencast.Player
        @description Set the seek state.
        @param String state
     */
    function setSeekState(state)
    {
	    seekState = state;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the showSections.
     */
    function getShowSections()
    {
        return showSections;
    }

    /**
         @memberOf Opencast.Player
         @description Set the showSections.
         @param Boolean bool
     */
    function setShowSections(bool)
    {
        showSections = bool;
    } 
    
    /**
        @memberOf Opencast.Player
        @description Get the mouseOverBool.
     */
    function getMouseOverBool()
    {
        return mouseOverBool;
    }

    /**
        @memberOf Opencast.Player
        @description Set the mouseOverBool.
        @param Booelan bool
     */
    function setMouseOverBool(bool)
    {
        mouseOverBool = bool;
    }

    /**
        @memberOf Opencast.Player
        @description Get the captionsBool.
     */
    function getCaptionsBool()
    {
        return captionsBool;
    }

    /**
        @memberOf Opencast.Player
        @description Set the captionsBool.
        @param Booelan bool
     */
    function setCaptionsBool(bool)
    {
        captionsBool = bool;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the duration.
      */
    function getDuration()
    {
        return duration;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the dragging.
        @param Booelan bool
     */
    function getDragging()
    {
        return dragging;
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the dragging.
        @param Booelan bool
     */
    function setDragging(bool)
    {
        dragging = bool;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the displayMode.
     */
    function getDisplayMode()
    {
        return displayMode;
    }

    /**
        @memberOf Opencast.Player
        @description Set the displayMode.
        @param Sring mode
     */
    function setDisplayMode(mode)
    {
    	displayMode = mode;
    }  
    
    /**
        @memberOf Opencast.Player
        @description Set the optionClassName.
        @param Sring className
     */
    function setOptionClassName(className)
    {
    	optionClassName = className;
    } 
    
    /**
        @memberOf Opencast.Player
        @description Set the browserWidth.
        @param Number witdh
     */
    function setBrowserWidth(witdh)
    {
    	browserWidth = witdh;
    }
    
    /**
        @memberOf Opencast.Player
        @description Get browserWidth.
     */
    function getBrowserWidth()
    {
	    return browserWidth;
    }
        
    /**
        @memberOf Opencast.Player
        @description Mouse over effect, change the css style.
     */
    function PlayPauseMouseOver() 
    {
        if (getCurrentPlayPauseState() === PLAYING) 
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-pause-over");
            setMouseOverBool(true);
        }
        else if (getCurrentPlayPauseState() === PAUSING) 
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-play-over");
            setMouseOverBool(true);
        }
    }

    /**
        @memberOf Opencast.Player
        @description Mouse out effect, change the css style.
     */
    function PlayPauseMouseOut() 
    {
        if (getCurrentPlayPauseState() === PLAYING) 
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-pause");
            setMouseOverBool(false);
        }
        else if (getCurrentPlayPauseState() === PAUSING) 
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-play");
            setMouseOverBool(false);
        }
    }

    /**
        @memberOf Opencast.Player
        @description Show the slides
     */
    function showSlides()
    {
        $("#oc_slides").attr("className", "oc_DisplayBlock");
        $("#oc_btn-slides").attr({ 
            alt: SLIDESHIDE,
            title: SLIDESHIDE,
            value: SLIDESHIDE
        });
        $("#oc_btn-slides").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Hide the slides
     */
    function hideSlides()
    {
        $("#oc_slides").attr("className", "oc_DisplayNone");
        $("#oc_btn-slides").attr({ 
            alt: SLIDES,
            title: SLIDES,
            value: SLIDES
        });
        $("#oc_btn-slides").attr('aria-pressed', 'false');
    }

    /**
        @memberOf Opencast.Player
        @description Show the notes
     */
    function showNotes()
    {
        $("#oc_notes").attr("className", "oc_DisplayBlockTableLeft");
        $("#oc_btn-notes").attr({ 
            alt: NOTESHIDE,
            title: NOTESHIDE,
            value: NOTESHIDE
        });
        $("#oc_btn-notes").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Hide the notes
     */
    function hideNotes()
    {
        $("#oc_notes").attr("className", "oc_DisplayNone");
        $("#oc_btn-notes").attr({ 
            alt: NOTES,
            title: NOTES,
            value: NOTES
        });
        $("#oc_btn-notes").attr('aria-pressed', 'false');
    }

    /**
        @memberOf Opencast.Player
        @description Show the slide text
     */
    function showSlideText()
    {
        $("#oc_slidetext").attr("className", "oc_DisplayBlockTableRight");
        $("#oc_btn-slidetext").attr({ 
            alt: SLIDETEXTHIDE,
            title: SLIDETEXTHIDE,
            value: SLIDETEXTHIDE
        });
        $("#oc_btn-slidetext").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Hide the slide text
     */
    function hideSlideText()
    {
        $("#oc_slidetext").attr("className", "oc_DisplayNone");
        $("#oc_btn-slidetext").attr({ 
            alt: SLIDETEXT,
            title: SLIDETEXT,
            value: SLIDETEXT
        });
        $("#oc_btn-slidetext").attr('aria-pressed', 'false');
    }
    
    
    
    /**
        @memberOf Opencast.Player
        @description Show the transcript
     */
    function showTranscript()
    {
        $("#oc_transcript").attr("className", "oc_DisplayBlockTableRight");
        $("#oc_btn-transcript").attr({ 
            alt: TRANSCRIPTHIDE,
            title: TRANSCRIPTHIDE,
            value: TRANSCRIPTHIDE
        });
        $("#oc_btn-transcript").attr('aria-pressed', 'true');
    }

    /**
        @memberOf Opencast.Player
        @description Hide the transcript
     */
    function hideTranscript()
    {
        $("#oc_transcript").attr("className", "oc_DisplayNone");
        $("#oc_btn-transcript").attr({ 
            alt: TRANSCRIPT,
            title: TRANSCRIPT,
            value: TRANSCRIPT
        });
        $("#oc_btn-transcript").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Show the shortcuts
     */
    function showShortcuts()
    {
        $("#oc_shortcuts").attr("className", "oc_DisplayBlockTableLeft");
        $("#oc_btn-shortcuts").attr({ 
            alt: SHORTCUTSHIDE,
            title: SHORTCUTSHIDE,
            value: SHORTCUTSHIDE
        });
        $("#oc_btn-shortcuts").attr('aria-pressed', 'true');
        addAlert($("#oc_shortcuts").text());
    }
    
    /**
        @memberOf Opencast.Player
        @description Hide the shortcuts
     */
    function hideShortcuts()
    {
        $("#oc_shortcuts").attr("className", "oc_DisplayNone");
        $("#oc_btn-shortcuts").attr({ 
            alt: SHORTCUTS,
            title: SHORTCUTS,
            value: SHORTCUTS
        });
        $("#oc_btn-shortcuts").attr('aria-pressed', 'false');
    }

     /**
        @memberOf Opencast.Player
        @description Show the embed
     */
    function showEmbed()
    {
        $("#oc_embed").attr("className", "oc_DisplayBlock");
        $("#oc_btn-embed").attr({ 
            alt: EMBEDHIDE,
            title: EMBEDHIDE,
            value: EMBEDHIDE
        });
        $("#oc_btn-embed").attr('aria-pressed', 'true');
    }
    
    /**
       @memberOf Opencast.Player
       @description Hide the embed
     */
    function hideEmbed()
    {
        $("#oc_embed").attr("className", "oc_DisplayNone");
        $("#oc_btn-embed").attr({ 
            alt: EMBED,
            title: EMBED,
            value: EMBED
        });
        $("#oc_btn-embed").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Show the bookmarks
     */
    function showBookmarks()
    {
        $("#oc_bookmarks").attr("className", "oc_DisplayBlock");
        $("#oc_btn-bookmarks").attr({ 
            alt: BOOKMARKSHIDE,
            title: BOOKMARKSHIDE,
            value: BOOKMARKSHIDE
        });
        $("#oc_btn-bookmarks").attr('aria-pressed', 'true');
        $("#oc_bookmarksPoints").css('display', 'block');
    }

    /**
       @memberOf Opencast.Player
       @description Hide the bookmarks
     */
    function hideBookmarks()
    {
        $("#oc_bookmarks").attr("className", "oc_DisplayNone");
        $("#oc_btn-bookmarks").attr({ 
            alt: BOOKMARKS,
            title: BOOKMARKS,
            value: BOOKMARKS
        });
        $("#oc_btn-bookmarks").attr('aria-pressed', 'false');
        $("#oc_bookmarksPoints").css('display', 'none');
        
    }
    
    /**
        @memberOf Opencast.Player
        @description Show the description
     */
    function showDescription()
    {
        $("#oc_description").attr("className", "oc_DisplayBlockTableLeft");
        $("#oc_btn-description").attr({ 
            alt: DESCRIPTIONHIDE,
            title: DESCRIPTIONHIDE,
            value: DESCRIPTIONHIDE
        });
        $("#oc_btn-description").attr('aria-pressed', 'true');
    }

    /**
       @memberOf Opencast.Player
       @description Hide the description
     */
    function hideDescription()
    {
        $("#oc_description").attr("className", "oc_DisplayNone");
        $("#oc_btn-description").attr({ 
            alt: DESCRIPTION,
            title: DESCRIPTION,
            value: DESCRIPTION
        });
        $("#oc_btn-description").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle the slides
     */
    function doToggleSlides()
    {
        if ($("#oc_btn-slides").attr("title") === SLIDES)
        {
            showSlides();
            hideEmbed();
            hideBookmarks();
            setShowSections(true);
        }
        else
        {
            hideSlides();
            setShowSections(false);
        }
        Opencast.Initialize.doTest();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle the slides
     */
    function doToggleNotes()
    {
        if ($("#oc_btn-notes").attr("title") === NOTES)
        {
            showNotes(); 
            hideShortcuts();
            hideDescription();
            //setShowSections(true);
        }
        else
        {
            hideNotes();
            //setShowSections(false);
        }
        //Opencast.Initialize.doTest();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle the Slide Text
     */
    function doToggleSlideText()
    {
        if ($("#oc_btn-slidetext").attr("title") === SLIDETEXT)
        {
            showSlideText();
            hideTranscript();
            //setShowSections(true);
        }
        else
        {
            hideSlideText();
            //setShowSections(false);
        }
        //Opencast.Initialize.doTest();
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle the Transcript
     */
    function doToggleTranscript()
    {
        if ($("#oc_btn-transcript").attr("title") === TRANSCRIPT)
        {
            showTranscript();
            hideSlideText(); 
            //setShowSections(true);
        }
        else
        {
            hideTranscript();
            //setShowSections(false);
        }
        //Opencast.Initialize.doTest();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle the shortcuts
     */
    function doToggleShortcuts()
    {
        if ($("#oc_btn-shortcuts").attr("title") === SHORTCUTS)
        {
            showShortcuts();
            hideNotes(); 
            hideDescription();
            //setShowSections(true);
        }
        else
        {
            hideShortcuts();
            //setShowSections(false);
        }
        //Opencast.Initialize.doTest();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle the embed
     */
    function doToggleEmbed()
    {
        if ($("#oc_btn-embed").attr("title") === EMBED)
        {
            showEmbed();
            hideSlides();
            hideBookmarks();
            setShowSections(true);
        }
        else
        {
            hideEmbed();
            setShowSections(false);
        }
        Opencast.Initialize.doTest();
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle the bookmarks
     */
    function doToggleBookmarks()
    {
        if ($("#oc_btn-bookmarks").attr("title") === BOOKMARKS)
        {
            showBookmarks();
            hideEmbed();
            hideSlides();
            setShowSections(true);
        }
        else
        {
            hideBookmarks();
            setShowSections(false);
        }
        Opencast.Initialize.doTest();
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle the description
     */
    function doToggleDescription()
    {
        if ($("#oc_btn-description").attr("title") === DESCRIPTION)
        {
            showDescription();
            hideNotes(); 
            hideShortcuts();
            //setShowSections(true);
        }
        else
        {
            hideDescription();
            //setShowSections(false);
        }
        //Opencast.Initialize.doTest();
    }
    
    /**
        @memberOf Opencast.Player
        @description Remove the alert div.
     */
    function removeOldAlert()
    {
        var oldAlert = $("#alert").get(0);

        if (oldAlert)
        {
            document.body.removeChild(oldAlert);
        }
    }

    /**
        @memberOf Opencast.Player
        @description Remove the old alert div and create an new div with the aria role alert.
        @param String alertMessage
     */
    function addAlert(alertMessage)
    {
        removeOldAlert();
        var newAlert = document.createElement("div");
        newAlert.setAttribute("role", "alert");
        newAlert.setAttribute("id", "alert");
        newAlert.setAttribute("class", "oc_offScreen-hidden");
        var msg = document.createTextNode(alertMessage);
        newAlert.appendChild(msg);
        document.body.appendChild(newAlert);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the embed Player.
        @param String width, String height
     */
    function embedIFrame(width, height)
    {
        var iFrameText = '';
        var advancedUrl = window.location.href;
        var embedUrl = advancedUrl.replace(/watch.html/g, "embed.html");
        
        iFrameText = '<iframe src="' + embedUrl + '" style="border:0px #FFFFFF none;" name="Opencast Matterhorn - Media Player" scrolling="no" frameborder="1" marginheight="0px" marginwidth="0px" width="' + width + '" height="' + height + '"></iframe>';
        $('#oc_embed-textarea').val(iFrameText);
    }
    
    /**
        @memberOf Opencast.Player
        @description Add a bookmark
        @param String value, String name, String text
     */
    function addBookmark(value, name, text)
    {
    	var option = '<option value="'+value+'" class="oc_option-myBookmark" title="'+value+' '+name+': '+text+'">'+value+' '+name+': '+text+'</option>'
    	
    	$('#oc_bookmarkSelect').prepend( option );
    	if ($("#oc_myBookmarks-checkbox").attr('aria-checked') === 'false')
        {
            $("#oc_myBookmarks-checkbox").attr('aria-checked', 'true');
  	        $('#oc_myBookmarks-checkbox').attr('checked', 'true'); 
  	        $('option.oc_option-myBookmark').css('display', 'block' );
  	        $('.oc_option-myBookmark').css('visibility', 'visible' ); 
        }
    	
    	//
    	var posLeft = 0;
    	var windowWidth = getBrowserWidth();
    	var playhead = getPlayhead(value);
    	var duration = getDuration();
    	
    	var posLeft = ( playhead * 100 ) / duration;
    	
  
    	var btn = '<input class="oc_boomarkPoint" onClick="Opencast.Player.playBookmark(this.name)" style="left:'+posLeft+'%; width: 5px; height: 10px; margin-left: 5px; position: absolute; background-color: #90ee90 !important;" name="'+value+'" alt="'+value+' '+name+': '+text+'" title="'+value+' '+name+': '+text+'"></input>';
    	$('#oc_bookmarksPoints').append( btn );
    }
    
    /**
        @memberOf Opencast.Player
        @description remove a bookmark
     */
    function removeBookmark()
    {
    	$('#oc_bookmarkSelect option:selected').remove();
    	$('#oc_btn-removeBookmark').css('display', 'none' ); 
    }
    
    /**
        @memberOf Opencast.Player
        @description play a bookmark
        @param String playheadString
     */
    function playBookmark(playheadString)
    {
    	var newPlayhead = getPlayhead(playheadString);
    	Videodisplay.seek(newPlayhead);
    }
    
    /**
        @memberOf Opencast.Player
        @description get the playhead
        @param String playheadString
     */
    function getPlayhead(playheadString)
    {
        var playheadArray = playheadString.split(':');
    	
    	var playheadHour = parseInt(playheadArray[0], 10);
        var playheadMinutes = parseInt(playheadArray[1], 10);
        var playheadSeconds = parseInt(playheadArray[2], 10);
        
        return (playheadHour * 60 * 60) + (playheadMinutes * 60) + (playheadSeconds);
    }
    
    
    /**
     * 
     * 
     * 
        To Videodisplay
     */
    
    /**
        @memberOf Opencast.Player
        @description Set the media URL.
        @param String mediaURL
     */
    function setMediaURL(coverURL, mediaURLOne, mediaURLTwo)
    {
        if (mediaURLOne[0] === 'h' || mediaURLOne[0] === 'H' && mediaURLOne[2] === 't' || mediaURLOne[2] === 'T')
        {
            $("#oc-background-progress").attr('className', 'matterhorn-progress-bar-background');
        }
        Videodisplay.setMediaURL(coverURL, mediaURLOne, mediaURLTwo);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the captions URL.
        @param String captionsURL
     */
    function setCaptionsURL(captionsURL)
    {
        Videodisplay.setCaptionsURL(captionsURL);
    }  
    
    /**
        @memberOf Opencast.Player
        @description Do skip backward in the video.
     */
    function doSkipBackward() 
    {
        Videodisplay.skipBackward();
    }
    
    /**
        @memberOf Opencast.Player
        @description Do rewind in the video.
     */
    function doRewind()  
    {
        if (intval === "")
        {
            setSeekState(getCurrentPlayPauseState());
            Videodisplay.rewind();
            intval = window.setInterval("Videodisplay.rewind()", 1000);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Stop the rewind in the video.
     */
    function stopRewind()
    {
        if (intval !== "")
        {
            window.clearInterval(intval);
            intval = "";
        }
        if (getSeekState() === PLAYING)
        {
            doPlay();
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Do play the video.
     */
    function doPlay() 
    {
        FLASH_PLAYERSTATE = Videodisplay.play();
    }

    /**
        @memberOf Opencast.Player
        @description Do pause the video.
     */
    function doPause() 
    {
        FLASH_PLAYERSTATE = Videodisplay.pause();
    }

    /**
        @memberOf Opencast.Player
        @description Do fast forward the video.
     */
    function doFastForward() 
    {
        if (intval === "")
        {
            setSeekState(getCurrentPlayPauseState());
            Videodisplay.fastForward();
            intval = window.setInterval("Videodisplay.fastForward()", 1000);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Stop fast forward the video.
     */
    function stopFastForward() 
    {
        if (intval !== "")
        {
            window.clearInterval(intval);
            intval = "";
        }
        if (getSeekState() === PLAYING)
        {
            doPlay();
        }
    }  
    
    /**
        @memberOf Opencast.Player
        @description Do skip forward in the vido.
     */
    function doSkipForward()  
    {
        Videodisplay.skipForward();
    }

    /**
        @memberOf Opencast.Player
        @description Toggle between play and pause the video.
     */
    function doTogglePlayPause()
    {
        // Checking if btn_play_pause is "play"
        if (getCurrentPlayPauseState() === PAUSING)
        {
            setCurrentPlayPauseState(PLAYING);
            doPlay();
        } 
        else 
        {
            setCurrentPlayPauseState(PAUSING);
            doPause();
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle between mute and unmute
     */
    function doToggleMute() 
    {
        FLASH_MUTE = Videodisplay.mute();
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume icon
     */
    function lowSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-low');
        $("#oc_btn-volume").attr({ 
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume icon
     */
    function noneSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-none');
        $("#oc_btn-volume").attr({ 
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume icon
     */
    function highSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-high');
        $("#oc_btn-volume").attr({ 
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume icon
     */
    function muteSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-mute');
        $("#oc_btn-volume").attr({ 
            alt: MUTE,
            title: MUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'true');
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the cc icon
     */
    function setCCIconOn()
    {
        $("#oc_btn-cc").attr({ 
            alt: CCON,
            title: CCON
        });
        $("#oc_btn-cc").attr("className", "oc_btn-cc-on");
        $("#oc_btn-cc").attr('aria-pressed', 'true');
        setCaptionsBool(true);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the cc icon
     */
    function setCCIconOff()
    {
        $("#oc_btn-cc").attr({ 
            alt: CCOFF,
            title: CCOFF
        });
        $("#oc_btn-cc").attr("className", "oc_btn-cc-off");
        $("#oc_btn-cc").attr('aria-pressed', 'false');
        setCaptionsBool(false);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume slider
        @param Number newVolume
     */
    function setPlayerVolume(newPlayerVolume) 
    {
        Videodisplay.setVolumePlayer(newPlayerVolume);
    }
    
    /**
        @memberOf Opencast.Player
        @description Toggle between closed captions on or off.
        @param Boolean cc
     */
    function doToogleClosedCaptions() 
    {
        Videodisplay.closedCaptions();
    }
    
    /**
        @memberOf Opencast.Player
        @description Show the single video display
     */
    function videoSizeControlSingleDisplay()
    {
        $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay");
    }
    
    /**
        @memberOf Opencast.Player
        @description Show the audio display
     */
    function videoSizeControlAudioDisplay()
    {
    	if (getDisplayMode() === MULTIPLAYER)
        {
    		Videodisplay.videoSizeControl(100, 0);
        }
    	$("#oc_btn-dropdown").attr("className", "oc_btn-audioDisplay");
    }
    
    /**
        @memberOf Opencast.Player
        @description Show only the presenter video display
     */
    function videoSizeControlMultiOnlyLeftDisplay()
    {
        Videodisplay.videoSizeControl(100, 0);
        $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay");
    }
    
    /**
        @memberOf Opencast.Player
        @description Show only the presentation video display
     */
    function videoSizeControlMultiOnlyRightDisplay()
    {
        Videodisplay.videoSizeControl(0, 100);
        $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay");
   }

    /**
        @memberOf Opencast.Player
        @description Resize the presentation video
     */
    function videoSizeControlMultiBigRightDisplay()
    {
        Videodisplay.videoSizeControl(25, 75);
        $("#oc_btn-dropdown").attr("className", "oc_btn-bigRightDisplay");
    }
    
    /**
        @memberOf Opencast.Player
        @description Resize the presenter video
     */
    function videoSizeControlMultiBigLeftDisplay()
    {
        Videodisplay.videoSizeControl(75, 25);
        $("#oc_btn-dropdown").attr("className", "oc_btn-bigLeftDisplay");
    }

    /**
        @memberOf Opencast.Player
        @description Show presenter and presentation video
     */
    function videoSizeControlMultiDisplay()
    {
        Videodisplay.videoSizeControl(100, 100);
        $("#oc_btn-dropdown").attr("className", "oc_btn-centerDisplay");
    }
    
    /**
        @memberOf Opencast.Player
        @description Get the View State.
     */
    function getViewState()
    {
        FLASH_VIEWSTATE = Videodisplay.getViewState();  
    }

   /**
        @memberOf Opencast.Player
        @description Set the play/pause state and change the css style of the play/pause button.
        @param String state 
     */
    function setPlayPauseState(state) 
    {
        if (state === PLAYING) 
        {
            $("#oc_btn-play-pause").attr({ 
                alt: PLAY,
                title: PLAY
            });

            $("#oc_btn-play-pause").attr('className', 'oc_btn-play');
            $("#oc_btn-play-pause").attr('aria-pressed', 'false');

            if (getMouseOverBool() === true)
            {
                $("#oc_btn-play-pause").attr("className", "oc_btn-play-over");
            }
            setCurrentPlayPauseState(PAUSING);
        }
        else 
        {
            $("#oc_btn-play-pause").attr({ 
                alt: PAUSE,
                title: PAUSE
            });

            $("#oc_btn-play-pause").attr("className", "oc_btn-pause");
            $("#oc_btn-play-pause").attr('aria-pressed', 'true');

            if (getMouseOverBool() === true)
            {
                $("#oc_btn-play-pause").attr("className", "oc_btn-pause-over");
            }

            setCurrentPlayPauseState(PLAYING);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the current time of the video.
        @param String text 
     */
    function setCurrentTime(text) 
    {
        $("#oc_current-time").attr("value", text);
        $("#oc_edit-time").attr("value", text);
        $("#slider_seek_Rail").attr("title", "Time " + text);
    }

    /**
        @memberOf Opencast.Player
        @description Set the total time of the video.
        @param String text
     */
    function setTotalTime(text) 
    {
        $("#oc_duration").text(text);
    }
    
    /**
        @memberOf Opencast.Player
        @description Show the edit time input and hide the current time span.
     */
    function showEditTime()
    {
        $("#oc_current-time").attr("className", "oc_current-time-hide");
        $("#oc_edit-time").attr("className", "oc_edit-time");
        Videodisplay.pause();
        $("#oc_edit-time").focus();
    }
    
    /**
        @memberOf Opencast.Player
        @description Show the current time span and hide the edit time input.
     */
    function hideEditTime()
    {
        $("#oc_current-time").attr("className", "oc_current-time");
        $("#oc_edit-time").attr("className", "oc_edit-time-hide");
        Videodisplay.play();
    }
    
    /**
        @memberOf Opencast.Player
        @description Check the new time and seek the video to the new time.
     */
    function editTime()
    {
        var playheadString = $("#oc_edit-time").attr("value");
        var durationString = $("#oc_duration").text();
        playheadString = playheadString.replace(/[-\/]/g, ':'); 
        playheadString = playheadString.replace(/[^0-9:]/g, ''); 
        playheadString = playheadString.replace(/ +/g, ' '); 
        var playheadArray = playheadString.split(':');
        var durationArray = durationString.split(':');

        try
        {
            var playheadHour = parseInt(playheadArray[0], 10);
            var playheadMinutes = parseInt(playheadArray[1], 10);
            var playheadSeconds = parseInt(playheadArray[2], 10);
            
            var durationHour = parseInt(durationArray[0], 10);
            var durationMinutes = parseInt(durationArray[1], 10);
            var durationSeconds = parseInt(durationArray[2], 10);
            if (playheadHour > 99 || playheadMinutes > 59 || playheadSeconds > 59)
            {
                addAlert('Wrong time enter like this: HH:MM:SS');
                $("#oc_edit-time").attr("className", "oc_edit-time-error");
            } 
            else 
            {
                var newPlayhead = (playheadHour * 60 * 60) + (playheadMinutes * 60) + (playheadSeconds);
                var durationSeconds = (durationHour * 60 * 60) + (durationMinutes * 60) + (durationSeconds);
                
                if( isNaN(newPlayhead) || newPlayhead > durationSeconds )
                {
                    addAlert('Wrong time enter like this: HH:MM:SS');
                    $("#oc_edit-time").attr("className", "oc_edit-time-error");
                }
                else
                {
                    Videodisplay.seek(newPlayhead);
                    Videodisplay.play();
                    hideEditTime();
                }
            }
        }
        catch (exception) 
        {
            addAlert('Wrong time enter like this: HH:MM:SS');
            $("#oc_edit-time").attr("className", "oc_edit-time-error");
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the slider max time and set the duration.
        @param Number time 
     */
    function setDuration(time) 
    {
        duration = time;
    }
    
    /**
    @memberOf Opencast.Player
    @description Set the mediaPackageId
    @param String mediaPackageId 
     */
    function setMediaPackageId(id) 
    {
      mediaPackageId = id;
    }

    /**
    @memberOf Opencast.Player
    @description Set the mediaPackageId
    @param String mediaPackageId 
     */
    function setSessionId(id) 
    {
      sessionId = id;
    }

    var mediaPackageId;
    var sessionId;

    var inPosition = 0;
    var outPosition = 0;
    var INTERVAL_LENGTH = 5;
    
    /**
        @memberOf Opencast.Player
        @description Set the new position of the seek slider.
        @param Number newPosition
     */
    function setPlayhead(newPosition) 
    {
        var fullPosition = Math.round(newPosition);

        

        if(inPosition <= fullPosition && fullPosition <= inPosition + INTERVAL_LENGTH){
          outPosition = fullPosition;
          if(inPosition + INTERVAL_LENGTH === outPosition){
            addFootprint();
            inPosition = outPosition;
          }
        } else {
          addFootprint();
          inPosition = fullPosition;
          outPosition = fullPosition;
        }

        if (getDragging() === false)
        {
            var newPos = Math.round((newPosition / getDuration()) *  $("#scubber-channel").width());
            $("#draggable").css("left", newPos);
            $("#scrubber").css("left", newPos);
            $("#play-progress").css("width", newPos);
        }
    }
    
    function addFootprint(){
      $.ajax({
        type: 'GET',
        contentType: 'text/xml',
        url:"../../feedback/rest/add",
        data: "id=" + mediaPackageId + "&session=" + sessionId + "&in=" + inPosition + "&out=" + outPosition + "&key=FOOTPRINT",
        dataType: 'xml',
        success: function(xml) {
          // Do nothing, the FOOTPRINT has been saved
        },
        error: function(a, b, c) {
          // Some error while adding the FOOTPRINT
        }
        
     }); 
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the with of the progress bar.
        @param Number value
     */
    function setProgress(value) 
    {
        $('.load-progress').css("width", (Math.min(value,100) + "%"));
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the volume slider
        @param Number newVolume
     */
    function setVolumeSlider(newVolume) 
    {
        Opencast.ariaSlider.changeValueFromVideodisplay(Opencast.ariaSlider.getElementId(SLIDERVOLUME), newVolume);
    }
    
    /**
        @memberOf Opencast.Player
        @description Set the video size list
        @param String displayMode
     */
    function setVideoSizeList(displayMode) 
    {
        if (displayMode === MULTIPLAYER)
        {
            var content = '<li><div id="oc_video-size-dropdown-div">';
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay" type="submit" name="Only Talking Head" alt="Only Talking Head" title="Only Talking Head" value="" onclick="Opencast.Player.videoSizeControlMultiOnlyLeftDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '<input id="oc_btn-bigLeftDisplay" class="oc_btn-bigLeftDisplay" type="submit" name="Big Talking Head" alt="Big Talking Head " title="Big Talking Head" value="" onclick="Opencast.Player.videoSizeControlMultiBigLeftDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '<input id="oc_btn-centerDisplay" class="oc_btn-centerDisplay" type="submit" name="Center Videodisplays" alt="Center Videodisplays" title="Center Videodisplays" value="" onclick="Opencast.Player.videoSizeControlMultiDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '<input id="oc_btn-bigRightDisplay" class="oc_btn-bigRightDisplay" type="submit" name="Big Content View" alt="Big Content View" title="Big Content View" value="" onclick="Opencast.Player.videoSizeControlMultiBigRightDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay" type="submit" name="Only Content View" alt="Only Content View" title="Only Content View" value="" onclick="Opencast.Player.videoSizeControlMultiOnlyRightDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '<input id="oc_btn-audioDisplay" class="oc_btn-audioDisplay" type="submit" name="Audio" alt="Audio" title="Audio" value="" onclick="Opencast.Player.videoSizeControlAudioDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '</div> </li>';
            $('#oc_video-size-menue').prepend(content);
            $("#oc_btn-dropdown").attr("className", "oc_btn-centerDisplay");
            $('#oc_video-size-dropdown-div').css("width", '290px');
            $('#oc_video-size-dropdown-div').css("margin-left", '-223px');
            setDisplayMode(displayMode);
        }
        else if (displayMode === SINGLEPLAYER)
        {
        	var content = '<li><div id="oc_video-size-dropdown-div">';
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay" type="submit" name="Only Talking Head" alt="Only Talking Head" title="Only Talking Head" value="" onclick="Opencast.Player.videoSizeControlSingleDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '<input id="oc_btn-audioDisplay" class="oc_btn-audioDisplay" type="submit" name="Audio" alt="Audio" title="Audio" value="" onclick="Opencast.Player.videoSizeControlAudioDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '</div> </li>';
            $('#oc_video-size-menue').prepend(content);
            $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay");
            $('#oc_video-size-dropdown-div').css("width", '90px');
            $('#oc_video-size-dropdown-div').css("margin-left", '-22px');
            setDisplayMode(displayMode);
        }
        else if (displayMode === SINGLEPLAYERWITHSLIDES)
        {
            var content = '<li><div id="oc_video-size-dropdown-div">';
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay" type="submit" name="Only Talking Head" alt="Only Talking Head" title="Only Talking Head" value="" onclick="Opencast.Player.videoSizeControlMultiOnlyLeftDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay" type="submit" name="Only Content View" alt="Only Content View" title="Only Content View" value="" onclick="Opencast.Player.videoSizeControlMultiOnlyRightDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '<input id="oc_btn-audioDisplay" class="oc_btn-audioDisplay" type="submit" name="Audio" alt="Audio" title="Audio" value="" onclick="Opencast.Player.videoSizeControlAudioDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            content = content + '</div> </li>';
            $('#oc_video-size-menue').prepend(content);
            $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay");
            $('#oc_video-size-dropdown-div').css("width", '135px');
            $('#oc_video-size-dropdown-div').css("margin-left", '-67px');
            setDisplayMode(displayMode);
        }
        else if (displayMode === AUDIOPLAYER)
        {
            $("#oc_btn-dropdown").attr("className", "oc_btn-audioDisplay");
            setDisplayMode(displayMode);
        }
    }
    
    /**
        @memberOf Opencast.Player
        @description addAlert in html code.
        @param String alertMessage 
     */
    function currentTime(alertMessage) 
    {
        addAlert(alertMessage);
    }
    return {
        PlayPauseMouseOver : PlayPauseMouseOver,
        PlayPauseMouseOut : PlayPauseMouseOut,
        getShowSections : getShowSections,
        getDuration : getDuration,
        setDragging : setDragging,
        getCaptionsBool : getCaptionsBool,
        doToggleSlides : doToggleSlides,
        doToggleNotes : doToggleNotes,
        doToggleSlideText : doToggleSlideText,
        doToggleTranscript : doToggleTranscript,
        doToggleShortcuts : doToggleShortcuts,
        doToggleEmbed : doToggleEmbed,
        doToggleBookmarks : doToggleBookmarks,
        doToggleDescription : doToggleDescription,
        removeOldAlert : removeOldAlert,
        addAlert : addAlert,
        embedIFrame : embedIFrame,
        setMediaURL : setMediaURL,
        setCaptionsURL : setCaptionsURL,
        setBrowserWidth : setBrowserWidth,
        lowSound : lowSound,
        noneSound : noneSound,
        highSound : highSound,
        muteSound : muteSound,
        setCCIconOn : setCCIconOn,
        setCCIconOff : setCCIconOff,
        doSkipBackward : doSkipBackward,
        doRewind : doRewind,
        stopRewind : stopRewind,
        doPlay : doPlay,
        doPause : doPause,
        doFastForward : doFastForward,
        stopFastForward : stopFastForward,
        doSkipForward : doSkipForward,
        doTogglePlayPause : doTogglePlayPause,
        doToggleMute : doToggleMute,
        setPlayerVolume : setPlayerVolume,
        doToogleClosedCaptions : doToogleClosedCaptions,
        videoSizeControlSingleDisplay : videoSizeControlSingleDisplay,
        videoSizeControlAudioDisplay : videoSizeControlAudioDisplay,
        videoSizeControlMultiOnlyLeftDisplay : videoSizeControlMultiOnlyLeftDisplay,
        videoSizeControlMultiOnlyRightDisplay : videoSizeControlMultiOnlyRightDisplay,
        videoSizeControlMultiBigRightDisplay : videoSizeControlMultiBigRightDisplay,
        videoSizeControlMultiBigLeftDisplay : videoSizeControlMultiBigLeftDisplay,
        videoSizeControlMultiDisplay : videoSizeControlMultiDisplay,
        getViewState : getViewState,
        setPlayPauseState : setPlayPauseState,
        setCurrentTime : setCurrentTime,
        setTotalTime : setTotalTime,
        setMediaPackageId : setMediaPackageId,
        setSessionId : setSessionId,
        showEditTime : showEditTime,
        hideEditTime : hideEditTime,
        editTime : editTime,
        addBookmark : addBookmark,
        removeBookmark : removeBookmark,
        setOptionClassName : setOptionClassName,
        playBookmark : playBookmark,
        setDuration : setDuration,
        setPlayhead : setPlayhead,
        setProgress : setProgress,
        setVolumeSlider : setVolumeSlider,
        setVideoSizeList : setVideoSizeList,
        currentTime : currentTime,
        flashVars: flashVars
    };
}());
