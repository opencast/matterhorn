/*global $, Videodisplay, window, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
@namespace the global Opencast namespace engage
*/
Opencast.Watch = (function () 
{
    /**
     * @memberOf Opencast.Watch
     * @description Sets up the html page after the player has been initialized. The XSL files are loaded.
     */
    function onPlayerReady() 
    {
      var MULTIPLAYER        = "Multiplayer",
          SINGLEPLAYER       = "Singleplayer",
          SINGLEPLAYERWITHSLIDES = "SingleplayerWithSlides",
          AUDIOPLAYER        = "Audioplayer",
          ADVANCEDPLAYER         = "advancedPlayer",
          EMBEDPLAYER            = "embedPlayer";

      var mediaPackageId = Opencast.engage.getMediaPackageId();

      var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;

      $('#data').xslt(restEndpoint, "xsl/preview-hybrid-player.xsl", function () 
      {
        // set the media URLs
        var mediaUrlOne = Opencast.engage.getVideoUrl();
        var mediaUrlTwo = '';

        mediaUrlOne = mediaUrlOne === null ? '' : mediaUrlOne;

        Opencast.Player.setMediaURL(mediaUrlOne, mediaUrlTwo);

        if (mediaUrlOne !== '' && mediaUrlTwo !== '')
        {
          Opencast.Player.setVideoSizeList(SINGLEPLAYERWITHSLIDES);
          Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();
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

        // init the volume scrubber
        Opencast.Scrubber.init();

        $('#scrubber').bind('keydown', 'left', function(evt) 
        {
          var newPosition = Math.round((($("#draggable").position().left - 20 ) / $("#scubber-channel").width()) * Opencast.Player.getDuration());
          Videodisplay.seek(newPosition);
        });
        
        $('#scrubber').bind('keydown', 'right', function(evt)
        {
          var newPosition = Math.round((($("#draggable").position().left + 20 ) / $("#scubber-channel").width()) * Opencast.Player.getDuration());
          Videodisplay.seek(newPosition);            
        });
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