/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

import com.sonatype.insight.brain.scan.datastore.ScanEntity;

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

/**
 * Parses a hosted scan.xml.gz file to extract the single component's pathname, hash, and format.
 * <p>
 * A hosted scan file contains exactly one {@code
 *
<dir>
 * } element representing the artifact.
 * The {@code sha1} attribute on {@code
 *
<dir>
 * } is already truncated to 20 chars (matching
 * {@code repository_component.hash} column size). Format comes from {@code <repository format="..."/>}.
 */
public class ScanXmlParser
{
  private static final Logger log = LoggerFactory.getLogger(ScanXmlParser.class);

  private ScanXmlParser() {
  }

  /**
   * Extracts component info from a scan.xml.gz entity.
   *
   * @param scanEntity the scan entity to parse
   * @return the extracted component info, or {@code null} if parsing fails or no component found
   */
  public static ScanComponentInfo extractComponentInfo(final ScanEntity scanEntity) {
    try {
      InputStream raw = scanEntity.getInputStream();
      try {
        InputStream gzipped = new GZIPInputStream(raw);
        try (gzipped) {
          return parse(gzipped);
        }
      }
      catch (java.util.zip.ZipException e) {
        raw.close();
        try (InputStream plain = scanEntity.getInputStream()) {
          return parse(plain);
        }
      }
    }
    catch (IOException | XmlPullParserException e) {
      log.warn("Failed to parse scan.xml.gz for component info: {}", e.getMessage(), e);
      return null;
    }
  }

  private static ScanComponentInfo parse(
      final java.io.InputStream xmlStream) throws IOException, XmlPullParserException
  {
    try (XmlStreamReader reader = new XmlStreamReader(xmlStream)) {
      XmlPullParser parser = new MXParser();
      parser.setInput(reader);

      String format = null;
      String pathname = null;
      String hash = null;

      parser.next();
      while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
        if (parser.getEventType() == XmlPullParser.START_TAG) {
          String tagName = parser.getName();

          if ("repository".equals(tagName)) {
            format = parser.getAttributeValue(null, "format");
          }
          else if ("dir".equals(tagName)) {
            pathname = parser.getAttributeValue(null, "path");
            hash = parser.getAttributeValue(null, "sha1");
            if (pathname != null && hash != null) {
              break;
            }
          }
        }
        parser.next();
      }

      if (pathname == null || hash == null) {
        log.warn("Could not extract component info from scan file: pathname={}, hash={}", pathname, hash);
        return null;
      }

      return new ScanComponentInfo(pathname, hash, format);
    }
  }
}
