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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Content;
import org.opencastproject.feed.api.Content.Mode;
import org.opencastproject.feed.api.Feed.Type;
import org.opencastproject.feed.api.FeedEntry;

import junit.framework.Assert;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class FeedImplTest {

  @Test
  public void testFeedImpl() {
    try {
      Content title = new PlainTextContent("Test Feed");
      Content description = new HtmlContent("generated by <b>unit test</b>", Mode.Escaped);
      FeedImpl feed = new FeedImpl(Type.RSS, "http://localhost:8080/feeds/rss/2.0/test", title, description,
              "http://localhost:8080/feeds/rss/2.0/test");
      feed.addAuthor(new PersonImpl("Test User", "testuser@example.com", "http://www.example.com/testuser"));
      feed.addContributor(new PersonImpl("test university"));
      FeedEntry entry = new FeedEntryImpl(feed, "test entry", new LinkImpl(
              "http://localhost:8080/feeds/rss/2.0/test/testentry"),
              "http://localhost:8080/feeds/rss/2.0/test/testentry");
      entry.addEnclosure(new EnclosureImpl("http://www.example.com/video.mp4", "video/mp4", 12));
      feed.addEntry(entry);

      ITunesFeedExtension itunesmodule = new ITunesFeedExtension();
      itunesmodule.addKeyword("test");
      List<String> categories = new LinkedList<String>();
      categories.add("Higher Education");
      itunesmodule.setCategories(categories);
      feed.addModule(itunesmodule);

      DublinCoreExtension dcModule = new DublinCoreExtension();
      feed.addModule(dcModule);

      Assert.assertEquals(feed.getAuthors().get(0).getName(), "Test User");
      Assert.assertEquals(feed.getAuthors().get(0).getEmail(), "testuser@example.com");
      Assert.assertEquals(feed.getContributors().get(0).getName(), "test university");
      Assert.assertEquals(feed.getTitle().getValue(), "Test Feed");
      Assert.assertEquals(feed.getDescription().getType(), "text/html");

    } catch (Exception e) {
      Assert.fail();
    }
    // Todo test Services... but this cannot be done outside OSGI and the servlet Container, so this has to be done by
    // an integration test.
  }
  
}
