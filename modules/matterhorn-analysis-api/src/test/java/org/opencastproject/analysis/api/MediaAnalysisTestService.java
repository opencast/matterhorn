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
package org.opencastproject.analysis.api;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.remote.api.Receipt;

import org.junit.Ignore;

/**
 * Test implementation for the abstract media analysis support.
 */
@Ignore
public class MediaAnalysisTestService extends MediaAnalysisServiceSupport {

  /**
   * {@inheritDoc}
   * @see org.opencastproject.analysis.api.MediaAnalysisService#getAnalysisType()
   */
  @Override
  public String getAnalysisType() {
    return "org.opencastproject.analysis.vsegmenter";
  }
  
  /**
   * Creates a new test implementation object.
   * 
   * @param resultingFlavor
   *          the resulting flavor
   * @param requiredFlavors
   *          the required flavors
   */
  public MediaAnalysisTestService(MediaPackageElementFlavor resultingFlavor, MediaPackageElementFlavor[] requiredFlavors) {
    super(resultingFlavor, requiredFlavors);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisServiceSupport#analyze(java.net.URL)
   */
  @Override
  public Receipt analyze(MediaPackageElement element, boolean block) throws MediaAnalysisException {
    ReceiptStub receipt = new ReceiptStub();
    try {
      receipt.element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder().newElement(Catalog.TYPE,
              MediaPackageElements.SEGMENTS_FLAVOR);
    } catch (Exception e) {
      throw new MediaAnalysisException(e.getMessage());
    }
    return receipt;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.analysis.api.MediaAnalysisService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    return null;
  }
  
  class ReceiptStub implements Receipt {
    MediaPackageElement element;
    Status status;
    public MediaPackageElement getElement() {
      return element;
    }
    public String getHost() {
      return null;
    }
    public String getId() {
      return null;
    }
    public Status getStatus() {
      return status;
    }
    public String getType() {
      return "analysis-test";
    }
    public void setElement(MediaPackageElement element) {
      this.element = element;
    }
    public void setHost(String host) {
    }
    public void setId(String id) {
    }
    public void setStatus(Status status) {
      this.status = status;
    }
    public void setType(String type) {
    }
    public String toXml() {
      return null;
    }
  }

}
