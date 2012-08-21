/*
 * jQuery mhPlayer
 *
 * Copyright 2009-2011 The Regents of the University of California
 * Licensed under the Educational Community License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
(function($) {
  /*****************************************************************************
   * Synchronize two Matterhorn html5 players
   ****************************************************************************/
  var lastSynch = Date.now();

  function getDuration(id) {
    if (id) {
      return $(id).get(0).duration;
    } else {
      return -1;
    }
  }

  function getCurrentTime(id) {
    if (id) {
      return $(id).get(0).currentTime;
    } else {
      return -1;
    }
  }

  function seek(id, time) {
    if (id && time) {
      $(id).get(0).currentTime = time;
    }
  }

  function synch(id1, id2) {
    if (id1 && id2) {
      var ct1 = getCurrentTime(id1);
      var ct2 = getCurrentTime(id2);
      if ((ct1 != -1) && (ct2 != -1) && (ct2 != ct1)) {
        if (getDuration(id2) >= ct1) {
          seek(id2, ct1);
        }
      }
    }
  }

  var mhPlSyn = function(videoId1, videoId2, vid1Master) {
    if (vid1Master == false) {
      var tmp = videoId1;
      videoId1 = videoId2;
      videoId2 = tmp;
    }

    $(videoId1).get(0).onplay = function() {
      $(videoId2).get(0).play();
      $(videoId2).get(0).muted = true;
      synch(videoId1, videoId2);
    };

    $(videoId1).get(0).onpause = function() {
      $(videoId2).get(0).pause();
      synch(videoId1, videoId2);
    };

    $(videoId1).get(0).onended = function() {
      $(videoId2).get(0).pause();
      synch(videoId1, videoId2);
    };

    $(videoId1).get(0).ontimeupdate = function() {
      var now = Date.now();
      if ($(videoId1).get(0).paused || ((now - lastSynch) > 2000)) {
        synch(videoId1, videoId2);
        lastSynch = now;
      }
    };
  }

  $.mhPlayerSynch = function(videoId1, videoId2, vid1Master) {
    var notReadyYetInterval = 50;
    var vid1Init = false;
    var vid2Init = false;
    var vid1 = $(videoId1).get(0);
    if (vid1) {
      (function readyYet() {
        if (vid1.readyState) {
          vid1Init = true;
          if (vid2Init) {
            mhPlSyn(videoId1, videoId2, vid1Master);
          }
        } else {
          setTimeout(readyYet, notReadyYetInterval);
        }
      })();
    }
    var vid2 = $(videoId2).get(0);
    if (vid2) {
      (function readyYet() {
        if (vid2.readyState) {
          vid2Init = true;
          if (vid1Init) {
            mhPlSyn(videoId1, videoId2, vid1Master);
          }
        } else {
          setTimeout(readyYet, notReadyYetInterval);
        }
      })();
    }
  };

  /*****************************************************************************
   * Matterhorn html5 player
   ****************************************************************************/
  $.fn.mhPlayer = function(options) {
    /***************************************************************************
     * default options
     **************************************************************************/
    var defaults = {
      theme : 'default',
      preview : '',
      autoplay : 'false',
      subtitle : '',
      controls : 'true',
      fps: 25
    };
    // match default options and given options
    var options = $.extend(defaults, options);
    return $.each(this, function(i, val) {
      /*************************************************************************
       * strings
       ************************************************************************/
      var strPlay = 'Play';
      var strPause = 'Pause';
      var strVolumeOn = 'Switch Volume On';
      var strVolumeOff = 'Switch Volume Off';
      var strFullscreen = 'Fullscreen';
      
      var strPrevFrame = "Previous Frame";
      var strNextFrame = "Next Frame";

      /*************************************************************************
       * class names
       ************************************************************************/
      var mainClass = 'mhVideoPlayer';
      var classVideoControls = 'video-controls';
      var classVideoPlay = 'video-play';
      var classVideoSeek = 'video-seek';
      var classVideoTimer = 'video-timer';
      var classFullscreen = 'video-fullscreen';
      var classVolumeBox = 'volume-box';
      var classVolumeSlider = 'volume-slider';
      var classVolumeButton = 'volume-button';
      
      var classNextFrame = "video-next-frame";
      var classPreviousFrame = "video-prev-frame";

      /*************************************************************************
       * variables
       ************************************************************************/
      var mhVideo = $(val);
      var video_duration = 0;
      var currenttime = 0;
      var video_volume = 1;
      var onSeekTimerInterval = 150;
      var seeksliding;
      var autoplay = options.autoplay == 'true';
      var controls = options.controls == 'true';
      if (options.preview != '') {
        mhVideo.prop("poster", options.preview);
      }
      var video_wrap = $('<div></div>').addClass(mainClass).addClass(options.theme);
      var subtle = (options.subtitle != '') ? '<div class="srt" data-video="' + mhVideo.attr("id") + '" data-srt="'
          + options.subtitle + '"></div>' : '';
      var video_controls = '';
      if (controls) {
        video_controls = subtle + 
        '<div class="' + classVideoControls + ' ' + $(val).attr('id') + '">' + 
          '<a class="' + classVideoPlay + '" title="' + strPlay + '"></a>' +
          '<a class="' + classPreviousFrame + '" title="' + strPrevFrame + '"></a>' +
          '<a class="' + classNextFrame + '" title="' + strNextFrame + '"></a>' +
          '<div class="' + classVideoTimer + '"></div>' + 
          '<div class="' + classVolumeBox + '">' + 
            '<div class="' + classVolumeSlider + '"></div>' + 
            '<a class="' + classVolumeButton + '" title="' + strVolumeOn + '"></a>' + 
          '</div>' +
          '<div class="' + classVideoSeek + '"></div>' + 
          '<div class="' + classFullscreen + '"></div>' +
        '</div>';
      } else {
        video_controls = subtle;
      }
      mhVideo.wrap(video_wrap);
      mhVideo.after(video_controls);
      // set width of the captions
      if (subtle) {
        $(".srt").css("width", mhVideo.prop("width"));
      }
      var video_container = mhVideo.parent('.' + mainClass);
      var video_controls = $('.' + classVideoControls, video_container);
      var play_btn = $('.' + classVideoPlay, video_container);
      var fullscreen_btn = $('.' + classFullscreen, video_container);
      var video_seek = $('.' + classVideoSeek, video_container);
      var video_timer = $('.' + classVideoTimer, video_container);
      var volume = $('.' + classVolumeSlider, video_container);
      var volume_btn = $('.' + classVolumeButton, video_container);
      
      var next_btn = $('.' + classNextFrame, video_container);
      var prev_btn = $('.' + classPreviousFrame, video_container);
      
      video_controls.hide();

      /*************************************************************************
       * utitlity functions
       ************************************************************************/

      /**
       * formats given time in seconds to mm:ss
       * 
       * @param seconds
       *          time to format in seconds
       * @return given time in seconds formatted to mm:ss
       */
      var formatTime = function(seconds) {
        var m = "00";
        var s = "00";
        if (!isNaN(seconds) && (seconds >= 0)) {
          m = (Math.floor(seconds / 60) < 10) ? "0" + Math.floor(seconds / 60) : Math.floor(seconds / 60);
          s = (Math.floor(seconds - (m * 60)) < 10) ? "0" + Math.floor(seconds - (m * 60)) : Math.floor(seconds
              - (m * 60));
          ms = parseInt((seconds - parseInt(seconds)) * 100);
        }
        return m + ":" + s + "." + ms;
      };

      /**
       * updates the time on the video time field
       */
      var updateTime = function() {
        if (playerIsReady()) {
          currenttime = mhVideo.prop('currentTime');
          video_duration = mhVideo.prop('duration');
          video_timer.text(formatTime(currenttime) + "/" + formatTime(video_duration));
        }
      }

      /*************************************************************************
       * player functions
       ************************************************************************/

      /**
       * checks whether player is in ready state
       * 
       * @return a boolean value if player is in ready state
       */
      var playerIsReady = function() {
        return mhVideo.prop('readyState');
      }

      /**
       * raw play
       */
      var play = function() {
        mhVideo.get(0).play();
      }

      /**
       * raw pause
       */
      var pause = function() {
        mhVideo.get(0).pause();
      }

      /*************************************************************************
       * ui changes
       ************************************************************************/

      /**
       * sets the ui play button
       */
      var setUiPlayButton = function() {
        play_btn.button({
          text : false,
          icons : {
            primary : "ui-icon-play"
          }
        });
        $('.' + classVideoPlay, video_controls).attr("title", strPlay);
      }

      /**
       * sets the ui pause button
       */
      var setUiPauseButton = function() {
        play_btn.button({
          text : false,
          icons : {
            primary : "ui-icon-pause"
          }
        });
        $('.' + classVideoPlay, video_controls).attr("title", strPause);
      }

      /**
       * sets the ui fullscreen button
       */
      var setUiFullscreenButton = function() {
        fullscreen_btn.button({
          text : false,
          icons : {
            primary : "ui-icon-arrow-4-diag"
          }
        });
        $('.' + classFullscreen, video_controls).attr("title", strFullscreen);
      }

      /**
       * sets the ui volume on button
       */
      var setUiVolumeOnButton = function() {
        volume_btn.button({
          text : false,
          icons : {
            primary : "ui-icon-volume-on"
          }
        });
        $('.' + classVolumeButton, video_controls).attr("title", strVolumeOff);
      }

      /**
       * sets the ui volume off button
       */
      var setUiVolumeOffButton = function() {
        volume_btn.button({
          text : false,
          icons : {
            primary : "ui-icon-volume-off"
          }
        });
        $('.' + classVolumeButton, video_controls).attr("title", strVolumeOn);
      }
      
      var setUiNextFrameButton = function() {
        next_btn.button({
          text : false,
          icons : {
            primary : "ui-icon-arrowthickstop-1-e"
          } 
        });
      }
      
      var setUiPrevFrameButton = function() {
        prev_btn.button({
          text : false,
          icons : {
            primary : "ui-icon-arrowthickstop-1-w"
          } 
        });
      }

      /*************************************************************************
       * on-events
       ************************************************************************/

      /**
       * on play/pause
       */
      var onPlayPause = function() {
        if (mhVideo.prop('paused') == false) {
          pause();
        } else {
          play();
        }
      };

      /**
       * on seek update
       */
      var onSeekUpdate = function() {
        updateTime();
        if (!seeksliding) {
          video_seek.slider('value', currenttime)
        }
        ;
      };

      /**
       * on volume on/off
       */
      var onVolumeOnOff = function() {
        if (mhVideo.prop('muted')) {
          mhVideo.prop('muted', false);
          volume.slider('value', video_volume);
          setUiVolumeOnButton()
        } else {
          mhVideo.prop('muted', true);
          volume.slider('value', '0');
          setUiVolumeOffButton();
        }
        ;
      };
      
      function onNextFrame() {
        onSeekFrames(1);
      }
      
      function onPrevFrame() {
        onSeekFrames(-1);
      }
      
      function onSeekFrames(nr_of_frames) {
        var fps = options.fps;
        if (mhVideo.prop('paused') == false) {
          pause();
        }
        //var currentFrames = Math.round(video.currentTime * fps);
        var currentFrames = mhVideo.prop('currentTime') * fps;
        var newPos = (currentFrames + nr_of_frames) / fps;
        newPos = newPos + 0.00001; // FIXES A SAFARI SEEK ISSUE. myVdieo.currentTime = 0.04 would give SMPTE 00:00:00:00 wheras it should give 00:00:00:01
        
        mhVideo.prop('currentTime', newPos); // TELL THE PLAYER TO GO HERE
      } 

      /*************************************************************************
       * binds
       ************************************************************************/
      mhVideo.bind('play', function() {
        setUiPauseButton();
      });
      mhVideo.bind('pause', function() {
        setUiPlayButton();
      });
      mhVideo.bind('ended', function() {
        pause();
        setUiPlayButton();
      });
      fullscreen_btn.hide();
      mhVideo.bind('timeupdate', onSeekUpdate);

      /*************************************************************************
       * slider
       ************************************************************************/

      /**
       * initial seek for the seek slider
       */
      var initialSeek = function() {
        if (playerIsReady()) {
          updateTime();
          video_seek.slider({
            value : 0,
            step : 0.01,
            orientation : "horizontal",
            range : "min",
            max : video_duration,
            animate : true,
            slide : function(e, ui) {
              seeksliding = true;
              video_timer.text(formatTime(ui.value) + "/" + formatTime(video_duration));
            },
            stop : function(e, ui) {
              seeksliding = false;
              mhVideo.prop("currentTime", ui.value);
            }
          });
          if (controls) {
            video_controls.show();
          }
        } else {
          setTimeout(initialSeek, onSeekTimerInterval);
        }
      };

      volume.slider({
        value : 1,
        orientation : "vertical",
        range : "min",
        max : 1,
        step : 0.05,
        animate : true,
        slide : function(e, ui) {
          mhVideo.prop('muted', false);
          video_volume = ui.value;
          mhVideo.prop('volume', video_volume);
          setUiVolumeOnButton();
        }
      });

      /*************************************************************************
       * build ui
       ************************************************************************/
      initialSeek();
      setUiPlayButton();
      setUiFullscreenButton();
      setUiVolumeOnButton();
      setUiNextFrameButton();
      setUiPrevFrameButton();
      pause();
      // disable browser-specific controls
      mhVideo.removeAttr('controls');

      /*************************************************************************
       * clicks
       ************************************************************************/
      setUiPlayButton();
      play_btn.click(onPlayPause);
      mhVideo.click(onPlayPause);
      volume_btn.click(onVolumeOnOff);
      
      prev_btn.click(onPrevFrame);
      next_btn.click(onNextFrame);

      $.addSrt();

      /*************************************************************************
       * misc
       ************************************************************************/
      if (autoplay) {
        play();
      }
    });
  };
})(jQuery);
