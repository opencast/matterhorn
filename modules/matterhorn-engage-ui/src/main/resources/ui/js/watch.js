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

      var MULTIPLAYER             = "Multiplayer",
          SINGLEPLAYER            = "Singleplayer",
          SINGLEPLAYERWITHSLIDES  = "SingleplayerWithSlides",
          AUDIOPLAYER             = "Audioplayer",
          ADVANCEDPLAYER          = "advancedPlayer",
          EMBEDPLAYER             = "embedPlayer",
          mediaUrlOne             = "",
          mediaUrlTwo             = "",
          mediaResolutionOne      = "",
          mediaResolutionTwo      = "";


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
          var timeDate = $('#oc-date').html();
          var sd = new Date();
          sd.setFullYear(parseInt(timeDate.substring(0,4)));
          sd.setMonth(parseInt(timeDate.substring(5,7))-1);
          sd.setDate(parseInt(timeDate.substring(8,10)));
          sd.setHours(parseInt(timeDate.substring(11,13)));
          sd.setMinutes(parseInt(timeDate.substring(14,16)));
          sd.setSeconds(parseInt(timeDate.substring(17,19)));

          $('#oc_segment-table').html($('#oc-segments').html());

          $('#oc-segments').html("");

          // set the media URLs
          mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-rtmp').html();
          mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-rtmp').html();

          mediaResolutionOne = $('#oc-resolution-presenter-delivery-x-flv-rtmp').html();
          mediaResolutionTwo = $('#oc-resolution-presentation-delivery-x-flv-rtmp').html();

          if(mediaUrlOne === null){
            mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-http').html();
            mediaResolutionOne = $('#oc-resolution-presenter-delivery-x-flv-http').html();
          }

          if(mediaUrlOne === null){
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-rtmp').html();
            mediaResolutionOne = $('#oc-resolution-presenter-source-x-flv-rtmp').html();
          }

          if(mediaUrlOne === null){
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-http').html();
            mediaResolutionOne = $('#oc-resolution-presenter-source-x-flv-http').html();
          }

          if(mediaUrlTwo === null){
            mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-http').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-delivery-x-flv-http').html();
          }

          if(mediaUrlTwo === null){
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-rtmp').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-source-x-flv-rtmp').html();
          }

          if(mediaUrlTwo === null){
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-http').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-source-x-flv-http').html();
          }

          if(mediaUrlOne === null) {
            mediaUrlOne = mediaUrlTwo;
            mediaUrlTwo = null;
            mediaResolutionOne = mediaResolutionTwo;
            mediaResolutionTwo = null;
          }

        
           

          
         
          
          mediaUrlOne = mediaUrlOne === null ? '' : mediaUrlOne;
          mediaUrlTwo = mediaUrlTwo === null ? '' : mediaUrlTwo;
          
          mediaResolutionOne = mediaResolutionOne === null ? '' : mediaResolutionOne;
          mediaResolutionTwo = mediaResolutionTwo === null ? '' : mediaResolutionTwo;
         
          
          
          

          var coverUrl = $('#oc-cover-engage').html();
          if(coverUrl === null)
          {	  
            coverUrl = $('#oc-cover-feed').html();
          }
          coverUrl = coverUrl === null ? '' : coverUrl;
          
          
         


          Opencast.Player.setMediaURL(coverUrl, mediaUrlOne, mediaUrlTwo);

          if (mediaUrlOne !== '' && mediaUrlTwo !== '')
          {
            Opencast.Player.setVideoSizeList(MULTIPLAYER);
            Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
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
              Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
            }
          }
    
          // Set the caption
          // oc-captions using caption file generated by Opencaps
          var captionsUrl = $('#oc-captions').html();
          captionsUrl = captionsUrl === null ? '' : captionsUrl;
          Opencast.Player.setCaptionsURL(captionsUrl);

          // init the volume scrubber
          Opencast.Scrubber.init();

          // bind handler 
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

          Opencast.Bookmarks.initialize();
          
          getClientShortcuts();
          
          $.ajax(
          {
            type: 'GET',
            contentType: 'text/xml',
            url:"../../feedback/rest/stats",
            data: "id=" + mediaPackageId,
            dataType: 'xml',

            success: function(xml) 
            {
            // set the dcDescription
            $('#oc_description').append("Presenter: "+  $('#oc-creator').html());
            $('#oc_description').append("<br/>Date: "+  sd.toLocaleString());
            $('#oc_description').append("<br/>Subject: "+  $('#dc-subject').html());
            $('#oc_description').append("<br/>Sponsoring Department: "+  $('#dc-contributor').html());
            $('#oc_description').append("<br/>Language: "+  $('#dc-language').html());
              $('#oc_description').append("<br/>Views: "+$(xml).find("views").text());
              $('#oc_description').append("<br/>" + $('#dc-description').html());
            },
            error: function(a, b, c) 
            {
              // Some error while trying to get the views
            }
          }); 
          
          // init
          Opencast.Initialize.init();
          
          // **************************************
          // Segments Text View
          $('.segments-time').each( function() {
            var seconds= $(this).html();
            $(this).html(Opencast.engage.formatSeconds(seconds));
          });
          
          $('#oc_slidetext-left').html($('#oc-segments-text').html());

          $('#oc-segments-text').html("");
          
          
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
    /**
     * @memberOf Opencast.Watch
     * @description Gets the OS-specific shortcuts of the client
     */
    function getClientShortcuts()
    {	
      $('#oc_client_shortcuts').append("Control + Alt + I = Toggle the keyboard shortcuts information between show or hide.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + P = Toggle the video between pause or play.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + S = Stop the video.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + M = Toggle between mute or unmute the video.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + U = Volume up<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + D = Volume down<br/>");
      $('#oc_client_shortcuts').append("Control + Alt 0 - 9 = Seek the time slider<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + C = Toggle between captions on or off.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + F = Forward the video.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + R = Rewind the video.<br/>");
      $('#oc_client_shortcuts').append("Control + Alt + T = the current time for the screen reader<br/>");

      switch($.client.os){
        case "Windows":
          $('#oc_client_shortcuts').append("Windows Control + = to zoom in the player<br/>"); 
          $('#oc_client_shortcuts').append("Windows Control - = to minimize in the player<br/>");
         break; 
        case "Mac":
          $('#oc_client_shortcuts').append("cmd + = to zoom in the player<br/>"); 
      	$('#oc_client_shortcuts').append("cmd - = to minimize the player<br/>");
      	break; 
         case "Linux":
      	break;
      } 	
    }

    return {
      onPlayerReady : onPlayerReady,
      hoverSegment : hoverSegment,
      seekSegment : seekSegment,
      getClientShortcuts : getClientShortcuts
    };
}());
