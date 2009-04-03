/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.media.bundle;

import org.opencastproject.media.analysis.VideoStreamMetadata;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Takes {@link ch.ethz.replay.core.api.common.media.analysis.VideoStreamMetadata} in the Bundle context.
 *
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @author Christoph E. Driessen <ced@neopoly.de>
 * @version $Id$
 */
public interface VideoSettings extends Cloneable {

    /**
     * Returns the video metadata.
     */
    VideoStreamMetadata getMetadata();

    /**
     * Normally the system determines the technical metadata for a track. But there may
     * be some situations where it comes handy to provide the metadata from the outside, e.g.
     * when they are edited manually.
     */
    void setMetadata(VideoStreamMetadata metadata);

    /**
	 * Returns the xml representation of this settings object as found in the
	 * bundle manifest.
	 * 
	 * @param document the manifest dom
	 * @return the serialized settings object
	 */
	Node toManifest(Document document);

	Object clone() throws CloneNotSupportedException;
}