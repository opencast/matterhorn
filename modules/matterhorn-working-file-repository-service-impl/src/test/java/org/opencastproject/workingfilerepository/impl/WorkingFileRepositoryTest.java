package org.opencastproject.workingfilerepository.impl;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class WorkingFileRepositoryTest {
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryTest.class);
  private String mediaPackageID = "working-file-test-media-package-1";
  private String mediaPackageElementID = "working-file-test-element-1";
  private String collectionId = "collection-1";
  private String filename = "file.gif";
  private WorkingFileRepositoryImpl repo = new WorkingFileRepositoryImpl("target/working-file-repo-root", "http://localhost:8080");
  
  @Before
  public void setup() throws Exception {
    repo.activate(null);
    // Put an image file into the repository using the mediapackage / element storage
    InputStream in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
    repo.put(mediaPackageID, mediaPackageElementID, in);
    try {in.close();} catch (IOException e) {logger.error(e.getMessage());}

    // Put an image file into the repository into a collection
    in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
    repo.putInCollection(collectionId, filename, in);
    try {in.close();} catch (IOException e) {logger.error(e.getMessage());}
  }
  
  @After
  public void tearDown() throws Exception {
    FileUtils.forceDelete(new File("target/working-file-repo-root"));
  }
  
  @Test
  public void testPut() throws Exception {
    // Get the file back from the repository to check whether it's the same file that we put in.
    InputStream fromRepo = repo.get(mediaPackageID, mediaPackageElementID);
    byte[] bytesFromRepo = IOUtils.toByteArray(fromRepo);
    byte[] bytesFromClasspath = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("opencast_header.gif"));
    try {fromRepo.close();} catch (IOException e) {logger.error(e.getMessage());}
    
    Assert.assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
  }
  
  @Test
  public void testDelete() throws Exception {
    // Delete the file and ensure that we can no longer get() it
    repo.delete(mediaPackageID, mediaPackageElementID);
    Assert.assertTrue(repo.get(mediaPackageID, mediaPackageElementID) == null);
  }
  
  @Test
  public void testPutBadId() throws Exception {
    // Try adding a file with a bad ID
    String badId = "../etc";
    InputStream in = getClass().getClassLoader().getResourceAsStream("opencast_header.gif");
    try {
      repo.put(badId, mediaPackageElementID, in);
      Assert.fail();
    } catch (Exception e) {
    } finally {
      try {in.close();} catch (IOException e) {logger.error(e.getMessage());}
    }
  }

  @Test
  public void testGetBadId() throws Exception {
    String badId = "../etc";
    try {
      repo.get(badId, mediaPackageElementID);
      Assert.fail();
    } catch (Exception e) {}
  }


  @Test
  public void testPutIntoCollection() throws Exception {
    // Get the file back from the repository to check whether it's the same file that we put in.
    InputStream fromRepo = repo.getFromCollection(collectionId, filename);
    byte[] bytesFromRepo = IOUtils.toByteArray(fromRepo);
    byte[] bytesFromClasspath = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("opencast_header.gif"));
    try {fromRepo.close();} catch (IOException e) {logger.error(e.getMessage());}
    Assert.assertEquals(bytesFromClasspath.length, bytesFromRepo.length);
  }
  
  @Test
  public void testCollectionSize() throws Exception {
    Assert.assertEquals(1, repo.getCollectionSize(collectionId));
  }
  
  @Test
  public void testCopy() throws Exception {
    byte[] bytesFromCollection = IOUtils.toByteArray(repo.getFromCollection(collectionId, filename));
    repo.copyTo(collectionId, filename, "copied-mediapackage", "copied-element");
    byte[] bytesFromCopy = IOUtils.toByteArray(repo.get("copied-mediapackage", "copied-element"));
    Assert.assertTrue(Arrays.equals(bytesFromCollection, bytesFromCopy));
  }

  @Test
  public void testMove() throws Exception {
    byte[] bytesFromCollection = IOUtils.toByteArray(repo.getFromCollection(collectionId, filename));
    repo.moveTo(collectionId, filename, "moved-mediapackage", "moved-element");
    byte[] bytesFromMove = IOUtils.toByteArray(repo.get("moved-mediapackage", "moved-element"));
    Assert.assertTrue(Arrays.equals(bytesFromCollection, bytesFromMove));
  }
}
