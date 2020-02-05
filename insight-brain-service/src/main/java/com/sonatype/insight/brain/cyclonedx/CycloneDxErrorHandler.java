/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cyclonedx;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Named
@Singleton
public class CycloneDxErrorHandler
    implements ErrorHandler
{
  @Override
  public void warning(SAXParseException exception) throws SAXException {
    handleMessage("Warning", exception);
  }

  @Override
  public void error(SAXParseException exception) throws SAXException {
    handleMessage("Error", exception);
  }

  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    handleMessage("Fatal", exception);
  }

  private String handleMessage(String level, SAXParseException exception) throws SAXException {
    int lineNumber = exception.getLineNumber();
    int columnNumber = exception.getColumnNumber();
    throw new SAXException(
        level + " on line number: " + lineNumber + ", column number: " + columnNumber + " message: " +
            exception.getMessage());
  }
}
