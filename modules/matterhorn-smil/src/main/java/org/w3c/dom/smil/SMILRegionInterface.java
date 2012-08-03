/*
 * Copyright (c) 1999 World Wide Web Consortium,
 * (Massachusetts Institute of Technology, Institut National de Recherche
 *  en Informatique et en Automatique, Keio University).
 * All Rights Reserved. http://www.w3.org/Consortium/Legal/
 */

package org.w3c.dom.smil;

/**
 *  Declares rendering surface for an element. See the region attribute 
 * definition in SMIL 1.0. 
 */
public interface SMILRegionInterface {
  /**
   */
  public SMILRegionElement  getRegion();
  public void               setRegion(SMILRegionElement region);
}

