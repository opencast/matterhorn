/**
 *  Copyright 2009-2011 The Regents of the University of California
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
 
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Initialize
 */
Opencast.Initialize = (function () 
{
    
	var VOLUME = 'volume',
    VIDEOSIZE = 'videosize',
    divId = '';
    
	/**
        @memberOf Opencast.Initialize
        @description set the id of the div.
     */
    function setDivId(id)
    {
    	divId = id;
    }

    /**
        @memberOf Opencast.Initialize
        @description get the id of the div.
     */
    function getDivId()
    {
    	return divId;
    }
    
    /**
        @memberOf Opencast.Initialize
        @description Keylistener.
     */
    function keyboardListener() {
    
        $(document).keyup(function (event) {

            if (event.altKey === true && event.ctrlKey === true) 
            {
                if (event.which === 77 || event.which === 109) // press m or M
                {
                    Opencast.Player.doToggleMute();
                }
                if (event.which === 80 || event.which === 112 || event.which === 83 || event.which === 84 || event.which === 116 || event.which === 115 || event.which === 85 || event.which === 117  || event.which === 68 || event.which === 100 || event.which === 48 || event.which === 49 || event.which === 50 || event.which === 51 || event.which === 52 || event.which === 53 || event.which === 54  || event.which === 55 || event.which === 56 || event.which === 57 || event.which === 67 || event.which === 99 || event.which === 82 || event.which === 114 || event.which === 70 || event.which === 102 || event.which === 83 || event.which === 115 || event.which === 73 || event.which === 105)
                {
                    Videodisplay.passCharCode(event.which);
                }
                event.preventDefault();
            }
        });
    }
    
    // http://javascript-array.com/scripts/jquery_simple_drop_down_menu/
    var timeout         = 200;
    var closetimer		= 0;
    var ddmenuitem      = 0;

    /**
        @memberOf Opencast.Initialize
        @description close the drop dowan menue.
     */
    function dropdown_close()
    {
        if (ddmenuitem)
        {
            ddmenuitem.css('visibility', 'hidden');
        }
    }

    /**
        @memberOf Opencast.Initialize
        @description new timer.
     */
    function dropdown_timer()
    {
        closetimer = window.setTimeout(dropdown_close, timeout);
    }

    /**
        @memberOf Opencast.Initialize
        @description cancel the timer.
     */
    function dropdown_canceltimer()
    {
        if (closetimer)
        {
            window.clearTimeout(closetimer);
            closetimer = null;
        }
    }

    /**
        @memberOf Opencast.Initialize
        @description open the drop down menue.
     */
    function dropdown_open()
    {
        dropdown_canceltimer();
        dropdown_close();
       
	    if(getDivId() === VOLUME)
        {
            ddmenuitem = $('#oc_volume-menue').css('visibility', 'visible');

        }
	    else if(getDivId() === VIDEOSIZE)
	    {
            ddmenuitem = $('#oc_video-size-menue').css('visibility', 'visible');
        }
        else
        {
            ddmenuitem = $(this).find('ul').eq(0).css('visibility', 'visible');
        }
	    setDivId('');
    }
    
    /**
        @memberOf Opencast.Initialize
        @description open the drop down meneue video.
     */
    function dropdownVideo_open()
    {
    	setDivId(VIDEOSIZE);
        dropdown_open();
    }
    
    $(document).ready(function () 
    {
        keyboardListener();
        
        $('#oc_video-size-dropdown > li').bind('mouseover', dropdown_open);
        //$('#oc_video-size-dropdown > li').bind('click', dropdown_open);
        $('#oc_video-size-dropdown > li').bind('mouseout',  dropdown_timer);
        
        // Handler focus
        $('#oc_btn-dropdown').focus(function () 
        {	
        	setDivId(VIDEOSIZE);
        	dropdown_open();
        });
        
        // Handler blur
        $('#oc_btn-dropdown').blur(function () 
        {	
            dropdown_timer();
        });
        
        $('#oc_volume-dropdown > li').bind('mouseover', dropdown_open);
        //$('#oc_video-size-dropdown > li').bind('click', dropdown_open);
        $('#oc_volume-dropdown > li').bind('mouseout',  dropdown_timer);

        // Handler focus
        $('#oc_btn-volume').focus(function () 
        {	
        	setDivId(VOLUME);
        	dropdown_open();
        })
        
        $('#slider_volume_Thumb').focus(function () 
        {
        	setDivId(VOLUME);
        	dropdown_open();
        })
        
        // Handler blur
        $('#oc_btn-volume').blur(function () 
        {	
            dropdown_timer();
        })
        
        $('#slider_volume_Thumb').blur(function () 
        {
            dropdown_timer();
        })
    
        // init the aria slider for the volume
        Opencast.ariaSlider.init();
       
        // aria roles
        $("#editorContainer").attr("className", "oc_editTime");
        $("#editField").attr("className", "oc_editTime");
        
        $("#oc_btn-cc").attr('role', 'button');
        $("#oc_btn-cc").attr('aria-pressed', 'false'); 

        $("#oc_btn-volume").attr('role', 'button');
        $("#oc_btn-volume").attr('aria-pressed', 'false');

        $("#oc_btn-play-pause").attr('role', 'button');
        $("#oc_btn-play-pause").attr('aria-pressed', 'false');

        $("#oc_btn-skip-backward").attr('role', 'button');
        $("#oc_btn-skip-backward").attr('aria-labelledby', 'Skip Backward');

        $("#oc_btn-rewind").attr('role', 'button');
        $("#oc_btn-rewind").attr('aria-labelledby', 'Rewind: Control + Alt + R');

        $("#oc_btn-fast-forward").attr('role', 'button');
        $("#oc_btn-fast-forward").attr('aria-labelledby', 'Fast Forward: Control + Alt + F');

        $("#oc_btn-skip-forward").attr('role', 'button');
        $("#oc_btn-skip-forward").attr('aria-labelledby', 'Skip Forward');

        $("#oc_current-time").attr('role', 'timer');
        $("#oc_edit-time").attr('role', 'timer');
        
        $("#oc_btn-slides").attr('role', 'button');
        $("#oc_btn-slides").attr('aria-pressed', 'false'); 
        
        
        
        // Handler for .click()
        $('#oc_btn-skip-backward').click(function () 
        {
            Opencast.Player.doSkipBackward();
        });
        $('#oc_btn-play-pause').click(function () 
        {
            Opencast.Player.doTogglePlayPause();
        });
        $('#oc_btn-skip-forward').click(function () 
        {
            Opencast.Player.doSkipForward();
        });
        $('#oc_btn-volume').click(function () 
        {
            Opencast.Player.doToggleMute();
        });
        $('#oc_btn-cc').click(function () 
        {
            Opencast.Player.doToogleClosedCaptions();
        });
        $('#oc_current-time').click(function () 
        {
            Opencast.Player.showEditTime();
        });
        
        // Handler for .mouseover()
        $('#oc_btn-skip-backward').mouseover(function() 
        {
        	this.className='oc_btn-skip-backward-over';  
        });
        $('#oc_btn-rewind').mouseover(function()
        {
        	this.className='oc_btn-rewind-over';
        });
        $('#oc_btn-play-pause').mouseover(function() 
        {
        	Opencast.Player.PlayPauseMouseOver();
        });
        $('#oc_btn-fast-forward').mouseover(function() 
        {
        	this.className='oc_btn-fast-forward-over';
        });
        $('#oc_btn-skip-forward').mouseover(function() 
        {
        	this.className='oc_btn-skip-forward-over';
        });
        $('#oc_btn-cc').mouseover(function() 
        {
        	if(Opencast.Player.getCaptionsBool() === false)
        	{
        		this.className='oc_btn-cc-over';
        	}
        });
        
        // Handler for .mouseout()
        $('#oc_btn-skip-backward').mouseout(function() 
        {
            this.className='oc_btn-skip-backward';         
        });
        $('#oc_btn-rewind').mouseout(function()
        {
          	this.className='oc_btn-rewind';
        });
        $('#oc_btn-play-pause').mouseout(function() 
        {
          	Opencast.Player.PlayPauseMouseOut();
        });
        $('#oc_btn-fast-forward').mouseout(function() 
        {
           	this.className='oc_btn-fast-forward';
        });
        $('#oc_btn-skip-forward').mouseout(function() 
        {
           	this.className='oc_btn-skip-forward';
        });
        $('#oc_btn-cc').mouseout(function() 
        {
            if(Opencast.Player.getCaptionsBool() === false)
            {
                this.className='oc_btn-cc-off';
            }
        });
        
        // Handler for .mousedown()
        $('#oc_btn-skip-backward').mousedown(function () 
        {
            this.className = 'oc_btn-skip-backward-clicked';   
        });
        
        $('#oc_btn-rewind').mousedown(function ()
        {
            this.className = 'oc_btn-rewind-clicked';
            Opencast.Player.doRewind();
        });
         
        $('#oc_btn-play-pause').mousedown(function () 
        {
            Opencast.Player.PlayPauseMouseOut();
        });
        
        $('#oc_btn-fast-forward').mousedown(function () 
        {
            this.className = 'oc_btn-fast-forward-clicked';
            Opencast.Player.doFastForward();
        });
        
        $('#oc_btn-skip-forward').mousedown(function () 
        {
            this.className = 'oc_btn-skip-forward-clicked';
        });
                               
        // Handler for .mouseup()
        $('#oc_btn-skip-backward').mouseup(function () 
        {
            this.className = 'oc_btn-skip-backward-over';  
        });
        
        $('#oc_btn-rewind').mouseup(function ()
        {
            this.className = 'oc_btn-rewind-over';
            Opencast.Player.stopRewind();
        });
        
        $('#oc_btn-play-pause').mouseup(function () 
        {
            Opencast.Player.PlayPauseMouseOver();
        });
        
        $('#oc_btn-fast-forward').mouseup(function () 
        {
            this.className = 'oc_btn-fast-forward-over';
            Opencast.Player.stopFastForward();
        });
        
        $('#oc_btn-skip-forward').mouseup(function () 
        {
            this.className = 'oc_btn-skip-forward-over';
        });
        
        // Handler onBlur
        $('#oc_edit-time').blur(function() 
        {
            Opencast.Player.hideEditTime(); 	
        });
        
        // Handler keypress
        $('#oc_current-time').keypress(function (event) 
        {
            if (event.keyCode === 13) 
            {
                Opencast.Player.showEditTime();
            }
        });
        
        $('#oc_edit-time').keypress(function (event) 
        {
            if (event.keyCode === 13) 
            {
                Opencast.Player.editTime();
            }
        });
        
        // Handler keydown
        $('#oc_btn-rewind').keydown(function (event) 
        {
           
            if (event.keyCode === 13 || event.keyCode === 32) 
            {
                this.className = 'oc_btn-rewind-clicked';
                Opencast.Player.doRewind();
            }
            else if(event.keyCode === 9)
            {
                this.className = 'oc_btn-rewind-over';
                Opencast.Player.stopRewind();
            }
        });
        
        $('#oc_btn-fast-forward').keydown(function (event) 
        {
            if (event.keyCode === 13 || event.keyCode === 32) 
            {
                this.className = 'oc_btn-fast-forward-clicked';
                Opencast.Player.doFastForward();
            }
            else if(event.keyCode === 9)
            {
                this.className = 'oc_btn-fast-forward-over';
                Opencast.Player.stopFastForward();
            }
        });
        $('#oc_current-time').keydown(function (event) 
        {
            if (event.keyCode === 37) 
            {
                Opencast.Player.doRewind();
            }
            else if(event.keyCode === 39)
            {
                Opencast.Player.doFastForward();
            }
       });
        
        // Handler keyup
        $('#oc_btn-rewind').keyup(function (event) 
        {
            if (event.keyCode === 13 || event.keyCode === 32) 
            {
                this.className = 'oc_btn-rewind-over';
                Opencast.Player.stopRewind();
            }
        });
       
        $('#oc_btn-fast-forward').keyup(function (event) 
        {
            if (event.keyCode === 13 || event.keyCode === 32) 
            {
                this.className = 'oc_btn-fast-forward-over';
                Opencast.Player.stopFastForward();
            } 
        });
        $('#oc_current-time').keyup(function (event) 
        {
    	    if (event.keyCode === 37) 
    	    {
    	         Opencast.Player.stopRewind();
    	    }
    	    else if(event.keyCode === 39)
    	    {
    	         Opencast.Player.stopFastForward();
    	    }
        });
        
        // to calculate the embed flash height
        var iFrameHeight = document.documentElement.clientHeight;
        var otherDivHeight = 138;
        var flashHeight = iFrameHeight - otherDivHeight;
        $("#oc_flash-player").css('height',flashHeight + 'px'); 
        
       
        
        // to calculate the margin left of the video controls
        var marginleft    = 0;
            controlsWidth = 165,
            flashWidth = document.documentElement.clientWidth;
            
           
        marginleft = Math.round( (flashWidth * 0.4) - controlsWidth ) / 2;
        $('.oc_btn-play').css("margin-left", marginleft + 'px');
   
        // create watch.html link
        var embedUrl = window.location.href;
        var advancedUrl = embedUrl.replace(/embed.html/g, "watch.html");
        $("a[href='#']").attr('href', ''+advancedUrl+'');

    });
    
    return {
    	dropdownVideo_open : dropdownVideo_open,
        dropdown_timer : dropdown_timer
    };
}());

