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

package org.opencastproject.search.api;

/**
 * A single result of searching.
 */
public interface SearchResult {

  /**
   * The search item list
   * 
   * @return Item list.
   */
  SearchResultItem[] getItems();

  /**
   * Get the user query.
   * 
   * @return The user query.
   */
  String getQuery();

  /**
   * Get the total number of items found, limited by the value returned by {@link #getLimit()}.
   * 
   * @return The number.
   */
  long size();
  
  /**
   * Returns the number of hits for this query, regardless of the limit that has been defined.
   * 
   * @return the total number of hits.
   */
  long getTotalSize();

  /**
   * Get the offset.
   * 
   * @return The offset.
   */
  long getOffset();

  /**
   * Get the limit.
   * 
   * @return The limit.
   */
  long getLimit();

  /**
   * Get the search time.
   * 
   * @return The time in ms.
   */
  long getSearchTime();

  /**
   * Get the page of the current result.
   * 
   * @return The page.
   */
  long getPage();

}
