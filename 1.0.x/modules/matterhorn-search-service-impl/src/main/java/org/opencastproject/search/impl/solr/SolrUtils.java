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
package org.opencastproject.search.impl.solr;

/**
 * Utility class for the solr database.
 */
public class SolrUtils {

  /** The regular filter expression */
  private static final String queryCleanerRegex = "[^0-9a-zA-ZöäüßÖÄÜ/\" +-.,]";

  /**
   * Clean up the user query input string to avoid invalid input parameters.
   * 
   * @param q
   *          The input String.
   * @return The cleaned string.
   */
  static String clean(String q) {
    if (q == null)
      return null;
    return q.replaceAll(queryCleanerRegex, " ").trim();
  }

}
