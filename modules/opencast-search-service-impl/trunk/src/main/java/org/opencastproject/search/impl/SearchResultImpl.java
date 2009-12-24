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

package org.opencastproject.search.impl;

import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The search result represents a set of result items that has been compiled as a result for a search operation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="search-results", namespace="http://search.opencastproject.org/", propOrder={"query", "resultSet"})
@XmlRootElement(name="search-results", namespace="http://search.opencastproject.org/")
public class SearchResultImpl implements SearchResult {

  /** Logging facility */
  static Logger log_ = LoggerFactory.getLogger(SearchResultImpl.class);

  /** A list of search items. */
  @XmlElement(name="result")
  private List<SearchResultItemImpl> resultSet = null;

  /** The query that yielded the result set */
  @XmlElement(name="query")
  private String query = null;

  /** The pagination offset. */
  @XmlAttribute
  private long offset = 0;

  /** The pagination limit. Default is 10. */
  @XmlAttribute
  private long limit = 10;

  /** The search time in milliseconds */
  @XmlAttribute
  private long searchTime = 0;

  /**
   * A no-arg constructor needed by JAXB
   */
  public SearchResultImpl() {}

  /**
   * Creates a new and empty search result.
   * 
   * @param query
   *          the query
   */
  public SearchResultImpl(String query) {
    if (query == null)
      throw new IllegalArgumentException("Quey cannot be null");
    this.query = query;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.SearchResult#getItems()
   */
  public SearchResultItem[] getItems() {
    return resultSet.toArray(new SearchResultItem[resultSet.size()]);
  }

  /**
   * Adds an item to the result set.
   * 
   * @param item
   *          the item to add
   */
  public void addItem(SearchResultItem item) {
    if (item == null)
      throw new IllegalArgumentException("Parameter item cannot be null");
    if (resultSet == null)
      resultSet = new ArrayList<SearchResultItemImpl>();
    resultSet.add((SearchResultItemImpl)item);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.SearchResult#getQuery()
   */
  public String getQuery() {
    return query;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.SearchResult#size()
   */
  public long size() {
    return resultSet != null ? resultSet.size() : 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.SearchResult#getOffset()
   */
  public long getOffset() {
    return offset;
  }

  /**
   * Set the offset.
   * 
   * @param offset
   *          The offset.
   */
  public void setOffset(long offset) {
    this.offset = offset;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.SearchResult#getLimit()
   */
  public long getLimit() {
    return limit;
  }

  /**
   * Set the limit.
   * 
   * @param limit
   *          The limit.
   */
  public void setLimit(long limit) {
    this.limit = limit;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.SearchResult#getSearchTime()
   */
  public long getSearchTime() {
    return searchTime;
  }

  /**
   * Set the search time.
   * 
   * @param searchTime
   *          The time in ms.
   */
  public void setSearchTime(long searchTime) {
    this.searchTime = searchTime;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.SearchResult#getPage()
   */
  public long getPage() {
    if (limit != 0)
      return offset / limit;
    return 0;
  }

}
