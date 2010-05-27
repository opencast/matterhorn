/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
@namespace the global Opencast namespace watch
*/
Opencast.Watch = (function () 
{
    /**
     * @memberOf Opencast.Watch
     * @description Sets up the html page after the player has been initialized. The XSL files are loaded.
     */
    function onPlayerReady() 
    {
	    var MULTIPLAYER			   = "Multiplayer",
	        SINGLEPLAYER		   = "Singleplayer",
	        SINGLEPLAYERWITHSLIDES = "SingleplayerWithSlides",
	        AUDIOPLAYER			   = "Audioplayer",
	        ADVANCEDPLAYER         = "advancedPlayer",
	        EMBEDPLAYER            = "embedPlayer",
	        mediaOneHeight         = 0,
	        mediaOneWidth          = 0,
	        mediaOneFormat         = 0,
	        mediaTwoHeight         = 0,
	        mediaTwoWidth          = 0,
	        mediaTwoFormat         = 0;
	  
        var mediaPackageId = Opencast.engage.getMediaPackageId();

        var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;

        Opencast.Player.setSessionId(Opencast.engage.getCookie("JSESSIONID"));
        Opencast.Player.setMediaPackageId(mediaPackageId);

        $('#data').xslt(restEndpoint, "xsl/player-hybrid-download.xsl", function () 
        {
          // some code to run after the mapping
          // set the title of the page
          document.title = "Opencast Matterhorn - Media Player - " + $('#oc-title').html();

          // set the title on the top of the player
          $('#oc_title').html($('#oc-title').html());

          // set date
          if (!($('#oc-creator').html() === ""))
            $('#oc_title_from').html(" by " + $('#oc-creator').html());

          if ($('#oc-date').html() === "")
            $('#oc_title_from').html(" by " + $('#oc-creator').html());
          else 
            $('#oc_title_from').html(" by " + $('#oc-creator').html() + " (" + $('#oc-date').html() + ")");

          // set the abstract
          $('#oc_description').html($('#oc-abstract').html());
          
          $('#oc_segment-table').html($('#oc-segments').html());
          
          $('#oc-segments').html("");

          // set the media URLs
          var mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-rtmp').html();
          var mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-rtmp').html();

          if(mediaUrlOne === null)
            mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-http').html();

          if(mediaUrlOne === null)
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-rtmp').html();

          if(mediaUrlOne === null)
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-http').html();

          if(mediaUrlTwo === null)
            mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-http').html();
           
          if(mediaUrlTwo === null)
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-rtmp').html();

          if(mediaUrlTwo === null)
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-http').html();

          if(mediaUrlOne === null) {
            mediaUrlOne = mediaUrlTwo;
            mediaUrlTwo = null;
          }

          mediaUrlOne = mediaUrlOne === null ? '' : mediaUrlOne;
          mediaUrlTwo = mediaUrlTwo === null ? '' : mediaUrlTwo;

          Opencast.Player.setMediaURL(mediaUrlOne, mediaUrlTwo);

          if (mediaUrlOne !== '' && mediaUrlTwo !== '')
          {
            Opencast.Player.setVideoSizeList(MULTIPLAYER);

            //
            mediaOneWidth = 720;
            mediaOneHeight = 480;
            mediaTwoWidth = 1024;
            mediaTwoHeight = 768;

            //
            mediaOneFormat = mediaOneWidth / mediaOneHeight;
            mediaTwoFormat = mediaTwoWidth / mediaTwoHeight;
          }
          else if (mediaUrlOne !== '' && mediaUrlTwo === '')
          {
            var pos = mediaUrlOne.lastIndexOf(".");
            var fileType = mediaUrlOne.substring(pos + 1);
            //
            if (fileType === 'mp3')
            {
              Opencast.Player.setVideoSizeList(AUDIOPLAYER);
            }
            else
            {
              Opencast.Player.setVideoSizeList(SINGLEPLAYER);
            }
          }
    
          // Set the caption
          Opencast.Player.setCaptionsURL('engage-hybrid-player/dfxp/matterhorn.dfxp.xml');

          // init the volume scrubber
          Opencast.Scrubber.init();

          // init
          Opencast.Initialize.init();
                     
          $('#scrubber').bind('keydown', 'left', function(evt) 
          {
            Opencast.Player.doRewind();
          });
          
          $('#scrubber').bind('keyup', 'left', function(evt) 
          {
            Opencast.Player.stopRewind();
          });
          
          $('#scrubber').bind('keydown', 'right', function(evt)
          {
            Opencast.Player.doFastForward();
          });
          
          $('#scrubber').bind('keyup', 'right', function(evt)
          {
            Opencast.Player.stopFastForward();
          });
          Opencast.segments.initialize();
       });
    }
  
    /**
     * @memberOf Opencast.Watch
     * @description Toggles the class segment-holder-over
     * @param String segmentId the id of the segment
     */
    function hoverSegment(segmentId)
    {
      $("#" + segmentId).toggleClass("segment-holder");
      $("#" + segmentId).toggleClass("segment-holder-over");
    }
  
    /**
     * @memberOf Opencast.Watch
     * @description Seeks the video to the passed position. Is called when the user clicks on a segment
     * @param int seconds the position in the video
     */
    function seekSegment(seconds)
    {
      // Opencast.Player.setPlayhead(seconds);
      var eventSeek = Videodisplay.seek(seconds);
    }
    return {
      onPlayerReady : onPlayerReady,
      hoverSegment : hoverSegment,
      seekSegment : seekSegment
    };
}());