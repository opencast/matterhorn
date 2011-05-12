package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Content;
import org.opencastproject.feed.api.Content.Mode;
import org.opencastproject.feed.api.Feed.Type;
import org.opencastproject.feed.api.FeedEntry;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class FeedImplTest {

  @Before
  public void setUp() {

  }

  @After
  public void tearDown() {

  }

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
