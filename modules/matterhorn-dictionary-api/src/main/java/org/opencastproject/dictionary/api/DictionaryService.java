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

package org.opencastproject.dictionary.api;

/**
 * Api for dictionary service, aimed at correcting textual information in catalogs.
 */
public interface DictionaryService {

  public static enum DICT_TOKEN {
    NONE, WORD, STOPWORD
  }

  /**
   * Takes a given text represented as array of words and returns weather each word exist in the specified language
   * 
   * @param text
   *          text represented as array of its words
   * @param language
   *          language for which the word will be added
   * @return for each word a dictionary token is returned in array
   */
  public DICT_TOKEN[] cleanText(String[] text, String language);

  /**
   * Returns an array of all the languages installed
   * 
   * @return all the language codes of installed dictionaries
   */
  public String[] getLanguages();
  
  /**
   * Returns an array of all the language codes that contain a specified word
   * 
   * @param word
   *          word that will be checked in which languages it exists
   * @return all the language codes that contain the specified word
   */
  public String[] getLanguages(String word);
  
  /**
   * For a given text returns the most similar language of the dictionaries installed
   * @param text Text being analyzed - represented as array of words
   * @return language code of the detected language
   */
  public String detectLanguage(String[] text);

  /**
   * Adds a specified word to the dictionary for the specified language
   * 
   * @param word
   *          word that will be added to the dictionary
   * @param language
   *          language for which the word will be added
   */
  public void addWord(String word, String language);

  /**
   * Adds a specified word to the dictionary for the specified language
   * 
   * @param word
   *          word that will be added to the dictionary
   * @param language
   *          language for which the word will be added
   * @param count
   *          occurrence of the word being added
   * @param weight
   *          percentage of occurrences of the word being added
   */
  public void addWord(String word, String language, Integer count, Double weight);

  /**
   * Adds a specified word to the dictionary for the specified language
   * 
   * @param word
   *          word that will be added to the dictionary
   * @param language
   *          language for which the word will be added
   * @param count
   *          occurrence of the word being added
   */
  public void addWord(String word, String language, Integer count);

  /**
   * Adds a stop word. Stop words are words that occur frequently or are not wanted for some of the processes.
   * 
   * @param word
   *          stop word being added
   * @param language
   *          language in which the stop word is being added
   */
  public void markStopWord(String word, String language);
  
  /**
   * Automatically parses dictionary and marks all stop words found
   * 
   * @param threshold
   *          all words that appear more frequently than the specified threshold will be marked as stopwords
   *          frequency is percentage of the word occurrence in all the text
   * @param language
   *          language in which the stop word is being added
   */
  public void parseStopWords(Double threshold, String language);

  /**
   * Gets a normalized value of word frequency for a given language
   * 
   * @param word
   *          word being tested
   * @param language
   *          language in which the word is being tested
   * @return percentage of occurrence of the specified word in a language
   */
  public double getWordWeight(String word, String language);

  /**
   * Gets a number of times the word appeared in the specified language
   * 
   * @param word
   *          word being tested
   * @param language
   *          language in which the word is being tested
   * @return count of the occurrences of the word in specified language
   */
  public Long getWordCount(String word, String language);

  /**
   * Tests weather the word exists in any of the languages
   * 
   * @param word
   *          word being tested
   * @return true if word is found in any of the languages, false otherwise
   */
  public Boolean isWord(String word);

  /**
   * Tests weather the word exists in the specified language
   * 
   * @param word
   *          word being tested
   * @param language
   *          a language code for the language in which the word is being tested
   * @return true if word is found in the specified language, false otherwise
   */
  public Boolean isWord(String word, String language);

  /**
   * Tests weather the word is a stop word in any of the languages
   * 
   * @param word
   *          word being tested
   * @return true if word is found to be a stop word in any of the languages, false otherwise
   */
  public Boolean isStopWord(String word);

  /**
   * Tests weather the word is a stop word in the specified language
   * 
   * @param word
   *          word being tested
   * @param language
   *          a language code for the language in which the word is being tested
   * @return true if word is found to be a stop word in the specified language, false otherwise
   */
  public Boolean isStopWord(String word, String language);

  /**
   * Removes a dictionary of the specified language
   * 
   * @param language
   *          language code for the dictionary to be removed
   */
  public void clear(String language);
}
