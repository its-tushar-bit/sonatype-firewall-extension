/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.inject.Named;

import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.scan.model.ItemContentType;

import com.google.common.annotations.VisibleForTesting;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

@Named
public class ThirdPartyScanResultsProcessor
{
  private static final Logger log = LoggerFactory.getLogger(ThirdPartyScanResultsProcessor.class);

  private static final List<String> thirdPartyItemContentTypes = asList(ItemContentType.CLAIR_SCANNER.name());

  public void handle(final File scanFile) {
    try {
      log.info("Processing third party content");
      List<ThirdPartyScanContent> thirdPartyContents = getThirdPartyScanContents(scanFile);
      for (ThirdPartyScanContent thirdPartyContent : thirdPartyContents) {
        ThirdPartyScanResultHandler handler =
            ThirdPartyResultHandlerFactory.newHandler(thirdPartyContent.getItemContentType());
        handler.handle(thirdPartyContent);
      }
      log.info("Completed processing third party content in file {}", scanFile.getName());
    }
    catch (Exception e) {
      log.error("Error processing third party results", e);
    }
  }

  @VisibleForTesting
  List<ThirdPartyScanContent> getThirdPartyScanContents(final File scanFile) {
    List<ThirdPartyScanContent> scanContents = new ArrayList<>();
    try {
      GZIPInputStream gis = new GZIPInputStream(new FileInputStream(scanFile));
      XmlPullParser parser = new MXParser();
      parser.setInput(new XmlStreamReader(gis));

      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {

        String elementName = parser.getName();
        if ("item".equals(elementName)) {
          String contentType = parser.getAttributeValue(null, "contentType");
          if (contentType != null && thirdPartyItemContentTypes.contains(contentType)) {
            Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
            String path = itemElement.getAttribute("path");
            String lastModified = itemElement.getAttribute("lastModified");
            String sha1 = itemElement.getAttribute("sha1");
            Xpp3Dom contentElement = itemElement.getChild("content");
            if (contentElement != null) {
              scanContents
                  .add(new ThirdPartyScanContent(path, ItemContentType.valueOf(contentType), lastModified, sha1,
                      contentElement.getValue()));
            }
            else {
              log.error("scan file {} contained a third party scan item {} without any content", scanFile.getName(),
                  contentType);
            }
          }
        }
        eventType = parser.next();
      }
    }
    catch (IOException | XmlPullParserException e) {
      log.error("error reading third party scan content from scan file {}", scanFile.getName());
    }
    return scanContents;
  }
}
