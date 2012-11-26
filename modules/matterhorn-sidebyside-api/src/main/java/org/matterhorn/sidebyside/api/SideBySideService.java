package org.matterhorn.sidebyside.api;

import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.NotFoundException;

import java.io.IOException;

/**
 * API for a service that, given a certain set of tracks from a Mediapackage, merges them into a single video 
 */
public interface SideBySideService {
  
  /**
   * Processes the input parameters to create the video composition.
   * @throws IOException 
   * @throws NotFoundException
   * @throws IllegalArgumentException 
   */
  Track process(Track presenterTrack, int presenterStream, int presenterX, int presenterY,
          Track presentationTrack, int presentationStream, int presentationX, int presentationY,
          int layoutWidth, int layoutHeight) throws NotFoundException, IOException, IllegalArgumentException;

  
  /**
   * Processes the input parameters to create the video composition.
   * This version takes the first video stream in each track
   * @throws IOException 
   * @throws NotFoundException 
   * @throws IllegalArgumentException
   */
  Track process(Track presenterTrack, int presenterX, int presenterY,
		  Track presentationTrack, int presentationX, int presentationY,
		  int layoutWidth, int layoutHeight) throws NotFoundException, IOException, IllegalArgumentException;
  
}
