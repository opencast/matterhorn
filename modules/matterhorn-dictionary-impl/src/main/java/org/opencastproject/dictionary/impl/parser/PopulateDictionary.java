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
package org.opencastproject.dictionary.impl.parser;

import org.opencastproject.dictionary.impl.Word;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class PopulateDictionary {

  public static void main(String... args) throws Exception {
    String lang = getInput("Enter language", "en");
    File dir = null;
    while (true) {
      String wikiRepo = getInput("Enter path to expanded wikipedia archive", ".");
      dir = new File(wikiRepo);  
      if(dir.isDirectory()) {
        break;
      } else {
        System.out.println(wikiRepo + " is not a directory");
      }
    }
    File wikiArticles = new File(dir, lang + "wiki-latest-pages-articles.xml");
    populate(lang, wikiArticles, 10, 3);
  }

  protected static String getInput(String prompt, String defaultValue) {
    System.out.print(prompt);
    System.out.print("[" + defaultValue + "] >");
    Scanner scanner = new Scanner(System.in);
    String value = scanner.nextLine();
    return value == null || value.equals("") ? defaultValue : value;
  }

  public PopulateDictionary() {
    // populate("slwiki-latest-pages-articles.xml");
  }

  public static void populate(String lang, File in, int minWordCount, int minWordLength) {
    // Create the sax parser and start parsing
    WikipediaSAXHandler saxHandler = new WikipediaSAXHandler(lang, minWordCount, minWordLength);
    SAXParserFactory saxFactory = SAXParserFactory.newInstance();
    SAXParser parser;
    try {
      parser = saxFactory.newSAXParser();
      parser.parse(in, saxHandler);
    } catch (Exception e) {
      e.printStackTrace();
    }
  };

  static class WikipediaSAXHandler extends DefaultHandler {
    private static final int BUFFER_LENGTH = 10 * 1024 * 1024;
    private static final int PRINT_DOCUMENT = 10000;

    /** Timer for statistics */
    private long measureTime = 0;
    private long docParsed = 0;
    private long numAllW = 0;

    /** The element content */
    private StringBuffer content = new StringBuffer();

    /** Regex used to split words */
    private static final Pattern wordBreakPattern = Pattern.compile("[\\p{Punct}\\s]");

    /** Dictionary as HashSet */
    private Map<Long, Integer> wordCount = new HashMap<Long, Integer>();

    private BufferedWriter wordlist;

    // parameters
    private String lang;
    private int numW;

    public WikipediaSAXHandler(String lang, int minWordCount, int minWordLength) {
      super();
      this.lang = lang;
      numW = 0;

      try {
        wordlist = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lang + ".wordlist.csv"), "UTF8"),
                BUFFER_LENGTH);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      super.characters(ch, start, length);
      content.append(ch, start, length);
    }

    @Override
    public void startDocument() {
      measureTime = System.currentTimeMillis();
      System.out.println("Starting to parse the document");

    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes atts) {

    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      // if(docParsed>5000) return;
      // escape elements that are not the title or body of an article
      if (name != "title" && name != "text") {
        content = new StringBuffer();
        return;
      }

      // increase counter only on titles
      if (name == "title") {
        if (docParsed++ % PRINT_DOCUMENT == PRINT_DOCUMENT - 1) {
          System.out.println("Documents parsed: " + docParsed);
          System.out.println("Unique words: " + numW);
          SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
          format.setTimeZone(TimeZone.getTimeZone("GMT"));
          long runningTime = System.currentTimeMillis() - measureTime;
          System.out.println("Running time: " + format.format(new Date(runningTime)));
          System.out.println("-----------------------");
        }
      }

      // parse text of the element
      String str = content.toString().trim();
      content = new StringBuffer();

      fillDictionary(str, wordlist);

    }

    @Override
    public void endDocument() {
      System.out.println("-------------------------------------");
      SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
      format.setTimeZone(TimeZone.getTimeZone("GMT"));
      measureTime = System.currentTimeMillis() - measureTime;
      System.out.println("Parsing complete in: " + format.format(new Date(measureTime)));
      System.out.println("All documents parsed:" + docParsed);
      System.out.println("Number of unique words:" + wordCount.size());

      try {
        // csv
        wordlist.close();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lang + ".csv"), "UTF8"));
        bw.write("#numDoc:" + docParsed);
        bw.newLine();
        bw.write("#numUniqueW:" + wordCount.size());
        bw.newLine();
        bw.write("#numAllW:" + numAllW);
        bw.newLine();
//        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lang + ".wordlist.csv"),
//                "UTF8"), BUFFER_LENGTH);
        // CSVParser csvP = new CSVParser(br);
        // String w;
        // while ((w = csvP.nextValue()) != null) {
        // bw.write(w);
        // bw.write(',');
        // bw.write(getCount(w).toString());
        // bw.newLine();
        // }
        bw.close();

        // dict object
        FileOutputStream fos = new FileOutputStream(lang + ".dict");
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        for (Entry<Long, Integer> word : wordCount.entrySet()) {
          oos.writeLong(word.getKey());
          oos.writeInt(word.getValue());
        }
        oos.close();

        // stats
        BufferedWriter outStats = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lang + ".stats"),
                "UTF8"));
        outStats.write("Statistics for language: " + lang);
        outStats.newLine();
        outStats.write("-------------------------------------");
        outStats.newLine();
        outStats.write("Parsing complete in: " + format.format(new Date(measureTime)));
        outStats.newLine();
        outStats.write("All documents parsed:" + docParsed);
        outStats.newLine();
        outStats.write("Number of unique words:" + wordCount.size());
        outStats.newLine();
        outStats.write("Number of all words:" + numAllW);
        outStats.newLine();
        outStats.newLine();
        outStats.close();
        wordlist.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    /**
     * Returns the words that occur in a tag's content.
     * 
     * @return the tag content split into words
     */
    private String[] getWords(String str) {
      return wordBreakPattern.split(str);
    }

    /**
     * Fills the dictionary with words from the last encountered tag.
     * 
     * @return the tag content split into words
     */
    private void fillDictionary(String str, BufferedWriter bw) {
      str = removeTemplates(str, "[", "]");
      str = removeTemplates(str, "{{start box}}", "{{end box}}");
      str = removeTemplates(str, "{|", "|}");
      str = removeTemplates(str, "{", "}");
      // str = removeTemplates(str, "{{", "}}");
      str = removeTemplates(str, "<!--", "-->");
      str = removeTemplates(str, "<table", "</table");
      str = removeTemplates(str, "<TABLE", "</TABLE");
      str = removeTemplates(str, "<div", ">");
      str = removeTemplates(str, "<DIV", ">");
      str = removeTemplates(str, "<p", ">");
      str = removeTemplates(str, "<P", ">");
      str = removeTemplates(str, "<timeline", "</timeline");
      str = removeTemplates(str, "<blockquote", "</blockquote");
      str = removeTemplates(str, "<math", "</math");
      str = removeLine(str, "|");

      // Process the words
      for (String word : getWords(str)) {
        if (word.length() == 0)
          continue;
        numAllW++;
        String w = Word.fixCase(word);

        Long hash = StringUtil.hash(w);

        if (wordCount.containsKey(hash)) {
          wordCount.put(hash, wordCount.get(hash) + 1);
        } else {
          numW++;
          wordCount.put(hash, 1);
          try {
            bw.write(w);
            bw.write(",");
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    /**
     * Remove nested templates.
     * 
     * @return string without the templates
     */
    private String removeTemplates(String str, String startTag, String endTag) {
      String res = "";
      int count = 0;
      while (true) {
        int firstStartTag = str.indexOf(startTag);
        int firstEndTag = str.indexOf(endTag);

        // check for malformated text
        if (count > 0 && firstEndTag == -1)
          return res;

        // the begining of the string is a non template adding it to the
        // result
        if (count == 0) {
          // check if the end is reached
          if (firstStartTag == -1) {
            return res + str;
          }
          // add all of the non template text to result and cut the
          // string being parsed
          res += str.substring(0, firstStartTag);
          str = str.substring(firstStartTag + startTag.length());
          count++;
        } else {
          // we are inside a template
          if (firstStartTag != -1 && firstStartTag < firstEndTag) {
            // parse from the new nested template
            str = str.substring(firstStartTag + startTag.length());
            count++;
          } else {
            // we have escaped one template
            str = str.substring(firstEndTag + endTag.length());
            count--;
          }
        }

      }

    }

    /**
     * Remove lines starting with a prefix.
     * 
     * @return string without lines starting with a given prefix
     */
    private String removeLine(String str, String start) {
      StringBuilder sb = new StringBuilder();
      String[] lines = str.split(System.getProperty("line.separator"));
      for (String line : lines) {
        if (!line.startsWith(start))
          sb.append(line);
      }
      return sb.toString();
    }
  }
}
