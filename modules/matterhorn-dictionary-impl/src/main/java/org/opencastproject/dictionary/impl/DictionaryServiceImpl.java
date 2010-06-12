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
package org.opencastproject.dictionary.impl;

import org.opencastproject.dictionary.api.DictionaryService;

/**
 * The dictionary service can be used to clean a list of words with respect to a given dictionary.
 */
public class DictionaryServiceImpl implements DictionaryService {

  @Override
  public void addStopWord(String word, String language) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addWord(String word, String language) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addWord(String word, String language, Integer count) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void clear(String language) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String[] getLanguages(String word) {
    // TODO Auto-generated method stub
    return new String[]{"en", "sl"};
  }

  @Override
  public Integer getWordCount(String word, String language) {
    // TODO Auto-generated method stub
    return 42;
  }

  @Override
  public double getWordWeight(String word, String language) {
    // TODO Auto-generated method stub
    return 0.0042;
  }

  @Override
  public Boolean isStopWord(String word) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Boolean isStopWord(String word, String language) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Boolean isWord(String word) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public Boolean isWord(String word, String language) {
    // TODO Auto-generated method stub
    return true;
  }

}
