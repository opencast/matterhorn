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
package org.opencastproject.util;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Provides static methods for compressing and extracting zip files using zip64 extensions when necessary.
 */
public class ZipUtil {
  
  /**
   * Compresses source files into a zip archive.  Does not support recursive addition of directories.
   * 
   * @param sourceFiles The files to include in the root of the archive
   * @param destination The path to put the resulting zip archive file.
   * @return the resulting zip archive file
   */
  public static java.io.File zip(java.io.File[] sourceFiles, String destination) {
    return zip(sourceFiles, destination, false, ZipEntry.DEFLATED);
  }

  public static java.io.File zip(java.io.File[] sourceFiles, File destination) {
    return zip(sourceFiles, destination, false, ZipEntry.DEFLATED);
  }

  public static java.io.File zip(java.io.File[] sourceFiles, String destination, int compressionMethod) {
    return zip(sourceFiles, destination, false, compressionMethod);
  }

  public static java.io.File zip(java.io.File[] sourceFiles, File destination, int compressionMethod) {
    return zip(sourceFiles, destination, false, compressionMethod);
  }

  /**
   * Compresses a files into a zip archive. May add files recursively.
   *
   * @param sourceFiles The files to include in the root of the archive
   * @param destination The path to put the resulting zip archive file.
   * @param recursively Set true to recursively add directories.
   * @return the resulting zip archive file
   */
  public static File zip(File[] sourceFiles, String destination, boolean recursively) {
    return zip(sourceFiles, new File(destination), recursively, ZipEntry.DEFLATED);
  }

  public static File zip(File[] sourceFiles, String destination, boolean recursively, int compressionMethod) {
    return zip(sourceFiles, new File(destination), recursively, compressionMethod);
  }

  public static File zip(File[] sourceFiles, File destination, boolean recursively) {
    return zip(sourceFiles, destination, recursively, ZipEntry.DEFLATED);
  }
  
  public static File zip(File[] sourceFiles, File destination, boolean recursively, int compressionMethod) {
    if (sourceFiles == null || sourceFiles.length <= 0) {
      throw new IllegalArgumentException("sourceFiles must include at least 1 file");
    }
    if (destination == null) {
      throw new IllegalArgumentException("destination must be set");
    }
    ZipOutputStream out = new ZipOutputStream(outputStream(destination));
    out.setMethod(compressionMethod);
    try {
      _zip(sourceFiles, out, -1, recursively);
      return destination;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  private static void _zip(File[] files, ZipOutputStream out, int basePath, boolean recursively) {
    for (File file : files) {
      if (file.isDirectory()) {
        if (recursively)
          _zip(file.listFiles(), out, curBasePath(file, basePath), recursively);
      } else {
        InputStream in = inputStream(file);
        try {
          out.putNextEntry(new ZipEntry(entryName(file, curBasePath(file, basePath))));
          IOUtils.copy(in, out);
        } catch (IOException e) {
          throw new RuntimeException(e);
        } finally {
          IOUtils.closeQuietly(in);
        }
      }
    }
  }

  private static int curBasePath(File f, int basePath) {
    return basePath >= 0 ? basePath : f.getParentFile().getAbsolutePath().length();
  }

  private static String entryName(File f, int basePath) {
    return rel(f.getAbsolutePath().substring(basePath));
  }

  private static InputStream inputStream(File file) {
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static OutputStream outputStream(File file) {
    try {
      return new BufferedOutputStream(new FileOutputStream(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** Chop of a leading separator. */
  private static String rel(String path) {
    return path.startsWith(File.separator) ? path.substring(1) : path;
  }

  /**
   * Extracts a zip file to a directory.
   * 
   * @param zipFile The source zip archive
   * @param destination the destination to extract the zip archive.  If this destination directory does not exist, it
   * will be created.
   */
  public static void unzip(java.io.File zipFile, java.io.File destination) {
    if (zipFile == null) {
      throw new IllegalArgumentException("zipFile must be set");
    }
    if (destination == null) {
      throw new IllegalArgumentException("destination must be set");
    }
    if (destination.exists() && destination.isFile()) {
      throw new IllegalArgumentException("destination file must be a directory");
    }
    if (!destination.exists())
      if (!destination.mkdirs())
        throw new RuntimeException("cannot create " + destination);

    ZipInputStream in = new ZipInputStream(inputStream(zipFile));
    try {
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        File file = new File(destination, entry.getName());
        if (entry.isDirectory()) {
          file.mkdirs();
          continue;
        }
        file.getParentFile().mkdirs();
        OutputStream out = outputStream(file);
        try {
          IOUtils.copy(in, out);
        } finally {
          IOUtils.closeQuietly(out);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }
}
