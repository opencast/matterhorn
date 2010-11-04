package org.opencast.engage.videodisplay.util
{
	import mx.core.Application;
	
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.osmf.containers.MediaContainer;
	import org.osmf.elements.LightweightVideoElement;
	import org.osmf.elements.ParallelElement;
	import org.osmf.layout.HorizontalAlign;
	import org.osmf.layout.LayoutMetadata;
	import org.osmf.layout.LayoutMode;
	import org.osmf.layout.ScaleMode;
	import org.osmf.layout.VerticalAlign;
	import org.osmf.media.MediaPlayer;
	import org.osmf.media.URLResource;
	import org.osmf.traits.AudioTrait;
	


	public class ParallelMedia
	{
		public var player:MediaPlayer;
		public var mediaContainer:MediaContainer;

		[Bindable]
       public var model:VideodisplayModel = VideodisplayModel.getInstance();


		public static const HUTTO1:String="rtmp://freecom.serv.uni-osnabrueck.de/oflaDemo/matterhorn/0a78520a-3c23-4379-bcc7-eb7c0a803070/track-6/dozent.flv";
		public static const HUTTO2:String="rtmp://freecom.serv.uni-osnabrueck.de/oflaDemo/matterhorn/0a78520a-3c23-4379-bcc7-eb7c0a803070/track-7/vga.flv";
		public static const PROGRESS:String="http://mediapm.edgesuite.net/strobe/content/test/AFaerysTale_sylviaApostol_640_500_short.flv";
		public static const lighty:String="http://131.173.22.24/static/dab950e1-c64b-4907-b9e3-c61bf8ec6110/track-10/vga.mp4";
		public var audiotrait:AudioTrait;
	
		
		public function ParallelMedia()
		{
			
			mediaContainer = new MediaContainer();
			var layoutData:LayoutMetadata=new LayoutMetadata();
			layoutData.percentWidth = 120;
			layoutData.percentHeight = 120;
			layoutData.scaleMode = ScaleMode.LETTERBOX;
			
			var videoElement:LightweightVideoElement=new LightweightVideoElement();
			videoElement.resource=new URLResource(HUTTO1);
			videoElement.smoothing=true;
			videoElement.defaultDuration=1000;
			//var mediaElementVideoOne:MediaElement=videoElement;

			var videoElement2:LightweightVideoElement=new LightweightVideoElement();
			videoElement2.resource=new URLResource(lighty);
			videoElement2.smoothing=true;
			
			videoElement2.defaultDuration=1000;
			//var mediaElementVideoTwo:MediaElement=videoElement2;

			var oProxyElementTwo:OProxyElement=new OProxyElement(videoElement2);

			videoElement.metadata.addValue(LayoutMetadata.LAYOUT_NAMESPACE, layoutData);
			videoElement2.metadata.addValue(LayoutMetadata.LAYOUT_NAMESPACE, layoutData);

			var parallelElement:ParallelElement=new ParallelElement();
		
			parallelElement.addChildAt(videoElement, 0);
			parallelElement.addChildAt(oProxyElementTwo, 1);
			

			layoutData=new LayoutMetadata();
			layoutData.layoutMode=LayoutMode.HORIZONTAL;
			
			//layoutData.horizontalAlign=HorizontalAlign.LEFT;
			//layoutData.verticalAlign=VerticalAlign.TOP;
			layoutData.horizontalAlign = HorizontalAlign.CENTER;
			layoutData.verticalAlign = VerticalAlign.MIDDLE;
			layoutData.top = -10;
			layoutData.left = -10;
			layoutData.width= Application.application.width;
			layoutData.height= Application.application.height;
			
			parallelElement.metadata.addValue(LayoutMetadata.LAYOUT_NAMESPACE, layoutData);

			//player = new MediaPlayer( parallelElement );
			player = new MediaPlayer(parallelElement);
			model.player = player;
			mediaContainer.addMediaElement(parallelElement);
			model.parallelMediaContainer = mediaContainer;
			
		}

	}
}