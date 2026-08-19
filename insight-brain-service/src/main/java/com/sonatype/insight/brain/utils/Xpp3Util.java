/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class Xpp3Util
{
  public static Xpp3Dom loadElement(String name, XmlPullParser parser) throws XmlPullParserException, IOException {
    Xpp3Dom element = new Xpp3Dom(name);

    Map<String, String> attributes = loadAttributes(parser);
    for (String attrName : attributes.keySet()) {
      element.setAttribute(attrName, attributes.get(attrName));
    }
    int eventType = parser.next();
    while (true) {
      if (eventType == XmlPullParser.START_TAG) {
        String childName = parser.getName();
        Xpp3Dom childElement = loadElement(childName, parser);
        element.addChild(childElement);
      }
      else if (eventType == XmlPullParser.END_TAG) {
        if (!name.equals(parser.getName())) {
          throw new XmlPullParserException("End tag '" + parser.getName() + "' does not match start tag '" + name
              + "'.");
        }
        break;
      }
      else if (eventType == XmlPullParser.TEXT && !parser.isWhitespace()) {
        element.setValue(parser.getText());
      }
      eventType = parser.next();
    }

    return element;
  }

  public static Map<String, String> loadAttributes(XmlPullParser parser) {
    Map<String, String> attributes = new LinkedHashMap<>();

    int attributesCount = parser.getAttributeCount();
    for (int i = 0; i < attributesCount; i++) {
      String name = parser.getAttributeName(i);
      String value = parser.getAttributeValue(i);

      attributes.put(name, value);
    }

    return attributes;
  }
}
