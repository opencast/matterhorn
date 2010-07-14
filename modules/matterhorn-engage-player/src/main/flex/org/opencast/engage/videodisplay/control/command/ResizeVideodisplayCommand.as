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
package org.opencast.engage.videodisplay.control.command
{
    import mx.controls.Alert;
    import mx.core.Application;
    
    import org.opencast.engage.videodisplay.control.event.ResizeVideodisplayEvent;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.swizframework.Swiz;

    public class ResizeVideodisplayCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

        /** Constructor */
        public function ResizeVideodisplayCommand()
        {
            Swiz.autowire( this );
        }

        /** execute
         *
         * When the learner resize the Videodisplay in the browser
         *
         * @eventType event:ResizeVideodisplayEvent
         * */
        public function execute( event:ResizeVideodisplayEvent ):void
        {
             if( model.mediaContainer != null)
             {
             	 model.mediaContainer.height = Application.application.height;
                 model.mediaContainer.width = Application.application.width;
             }
           
            /**
             * Application max width: 1194px, max Font Size ?, 1194/33 = 36px ( 36 > 20 ) = 20px
             * Application min widht: 231px, min Font Size ?, 231/33 = 7px
             *
             * */
            var divisor:int = 50;
            var fontSize:int = Application.application.width / divisor;
            
            if( fontSize > 16 )
            {
                model.fontSizeCaptions = 16;
                model.endIndexSubtitle = 100;
            }
            else if( fontSize < 13 && fontSize >= 9 )
            {
            	model.endIndexSubtitle = 80;
            	model.fontSizeCaptions = fontSize;
            }
            else if( fontSize < 9 && fontSize > 7 )
            {
                model.endIndexSubtitle = 70;
                model.fontSizeCaptions = 9;
            }
            else if( fontSize < 8 )
            {
                model.endIndexSubtitle = 50;
                model.fontSizeCaptions = 9;
            }
            else
            {
                model.fontSizeCaptions = fontSize;
            }
            
        }
    }
}