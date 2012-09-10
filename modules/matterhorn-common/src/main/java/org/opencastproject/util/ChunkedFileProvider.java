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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ChunkedFileProvider implements MessageBodyWriter<ChunkedFile> {

  public ChunkedFileProvider() {
    logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!:::::::::::::::::::::______________________________________");
  }

  private Logger logger = LoggerFactory.getLogger(ChunkedFileProvider.class);

  @Override
  public long getSize(ChunkedFile file, Class<?> type, Type genericType, Annotation[] annotations,
                      MediaType mediaType) {
    long size = 0;
    size = file.getContentLength();
    logger.debug("ChunkedSize: {}", size);
    return size;
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                             MediaType mediaType) {
    return ChunkedFile.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(ChunkedFile file, Class<?> type, Type genericType, Annotation[] annotations,
                      MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                      OutputStream entityStream) throws IOException, WebApplicationException {
    InputStream in = new BufferedInputStream(new FileInputStream(""));
    in.skip(file.getOffset());
    IOUtils.copy(in, entityStream);

  }
}
