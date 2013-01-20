/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.episode.api;

/**
 * A HttpMediaPackageElementProvider is an instance that is able to deliver archived media package elements
 * via HTTP and therefore provides a means to rewrite element URIs to point to itself.
 *
 * @see EpisodeService#applyWorkflow(ConfiguredWorkflow, UriRewriter, java.util.List)
 * @see EpisodeService#applyWorkflow(ConfiguredWorkflow, UriRewriter, EpisodeQuery)
 */
public interface HttpMediaPackageElementProvider {
  /**
   * Return a URI rewriter that rewrites media package element URIs so that they point to
   * the HttpMediaPackageElementProvider.
   */
  UriRewriter getUriRewriter();
}
