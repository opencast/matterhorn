/**
*  Copyright 2009 The Regents of the University of California
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
package org.opencast.engage.videodisplay.model
{
	import com.adobe.strobe.players.MediaPlayerWrapper;
	
	import mx.collections.ArrayCollection;
	import mx.controls.ProgressBar;
	
	import org.opencast.engage.videodisplay.control.util.TimeCode;
	import org.opencast.engage.videodisplay.vo.LanguageVO;
	
	[Bindable]
	public class VideodisplayModel
	{
		// Media Player Wrapper
		public var player : MediaPlayerWrapper;
	
	    // Current Duration
		public var currentDuration : Number;
		
		// Current Playhead
		public var currentPlayhead : Number;
		
		// Current Player State
		public var currentPlayerState : String;
		
		// Current Caption Set
		public var currentCaptionSet : Array;

		// The old Subtitle
		public var oldSubtitle : String = '';

		// The current Subtitle
		public var currentSubtitle : String = '';

		// Font Size of the Captions
		public var fontSizeCaptions : int = 16;

		// Height of the captions
		public var captionsHeight : int = 50;

		// An Array with the different captions
		public var captionSets : ArrayCollection = new ArrayCollection();

		// An Array width the language from the dfxp file
		public var languageComboBox : Array = new Array();

		// Close Caption Boolean
		public var ccBoolean : Boolean = false;

		// Array of LanguageVO
		public var languages : ArrayCollection = new ArrayCollection( [new LanguageVO('de' , "German") , new LanguageVO('en' , "English") , new LanguageVO('es' , "Spain")] );

		// Skip Backward Time
		public var skipBackwardTime:Number = 0;
		
		// Forward Time
		public var skipForwardTime:Number = 10;
		
		// Rewind Time
		public var rewindTime:Number = 10;
		
		// Skip Fast Forward Time
		public var fastForwardTime:Number = 10;
		
		// CC Button press
		public var ccButtonBool:Boolean = false;
		
		// Volume of the Player
		public var playerVolume:Number = 1.0;
		
		// Fullscreen Mode
		public var fullscreenMode:Boolean = false;
		
		// Time Code
		public var timeCode:TimeCode = new TimeCode();
		
		// Progress Percent
        public var progressPercent:int = 0;
        
        // Progress Bar
        public var progressBar:ProgressBar = new ProgressBar();
        
        [Embed("../../../../../../../resources/icons/volumemuteBig.png")]
        public var MuteIcon:Class;
        
        [Embed("../../../../../../../resources/icons/volumehighBig.png")]
        public var UnmuteIcon:Class;
        
        /** Constructor */		
		public function VideodisplayModel()
        {
        }
	}
}
