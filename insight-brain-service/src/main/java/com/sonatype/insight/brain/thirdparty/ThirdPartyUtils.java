/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.Reader;

import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public final class ThirdPartyUtils
{
  public static XmlPullParser getXmlParser(Reader reader) throws XmlPullParserException {
    XmlPullParser parser = new MXParser();
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    parser.setInput(reader);
    return parser;
  }
}
