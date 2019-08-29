/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.inject.Named;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
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

  private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();

  public void handle(final File scanFile) {
    log.info("Processing third party content");
    try {
      File filteredFile = File.createTempFile("temp-", ".xml");
      try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(scanFile));
          OutputStream out = new FileOutputStream(filteredFile)) {

        XmlPullParser parser = new MXParser();
        parser.setInput(new XmlStreamReader(gis));

        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLEventWriter writer = outputFactory.createXMLEventWriter(out);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
          processEvent(parser, writer, eventType, scanFile);
          eventType = parser.next();
        }
        writer.flush();
        writer.close();
        compressScanFile(filteredFile, scanFile);
        log.info("Completed processing third party content in file {}", scanFile.getName());
      }
      finally {
        filteredFile.delete();
      }
    }
    catch (Exception e) {
      log.error("Error reading third party scan content from scan file", e);
    }
  }

  private void processEvent(XmlPullParser parser, XMLEventWriter writer, int eventType, File scanFile) {
    try {
      addElement(parser, writer);
      String elementName = parser.getName();
      if ("item".equals(elementName)) {
        processItemElement(parser, writer, scanFile, elementName);
      }
      else if (eventType == XmlPullParser.TEXT) {
        writer.add(EVENT_FACTORY.createCharacters(parser.getText()));
      }
    }
    catch (Exception e) {
      log.error("Error parsing third party scan file", e);
    }
  }

  private void processItemElement(
      XmlPullParser parser,
      XMLEventWriter writer,
      File scanFile,
      String elementName) throws XmlPullParserException, IOException, XMLStreamException
  {
    String contentType = parser.getAttributeValue(null, "contentType");
    if (contentType != null && thirdPartyItemContentTypes.contains(contentType)) {
      Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
      Xpp3Dom contentElement = itemElement.getChild("content");
      if (contentElement != null) {
        String filteredContent = handleContent(itemElement, contentElement.getValue(), contentType);
        writeFilteredInformation(writer, filteredContent);
      }
      else {
        log.error("scan file {} contained a third party scan item {} without any content", scanFile.getName(),
            contentType);
      }
    }
    else {
      parser.next();
    }
    writer.add(EVENT_FACTORY.createEndElement(new QName(elementName), null));
  }

  private String handleContent(Xpp3Dom itemElement, String contentElement, String contentType) {
    String path = itemElement.getAttribute("path");
    String lastModified = itemElement.getAttribute("lastModified");
    String sha1 = itemElement.getAttribute("sha1");

    ItemContentType contentItemType = ItemContentType.valueOf(contentType);
    ThirdPartyScanResultHandler handler = createHandler(contentItemType);
    return handler
        .handleAndFilterContents(new ThirdPartyScanContent(path, contentItemType, lastModified, sha1, contentElement));
  }

  private void compressScanFile(File filteredFile, File scanFile) throws FileNotFoundException, IOException {
    try (GzipCompressorOutputStream outStream = new GzipCompressorOutputStream(new FileOutputStream(scanFile))) {
      IOUtils.copy(new FileInputStream(filteredFile), outStream);
    }
  }

  private void addElement(
      XmlPullParser parser,
      XMLEventWriter writer) throws XmlPullParserException, XMLStreamException
  {
    int eventType = parser.getEventType();
    if (eventType == XmlPullParser.START_TAG) {
      writer.add(EVENT_FACTORY.createStartElement(new QName(parser.getName()), null, null));
      addElementAttributes(parser, writer);
    }
    else if (eventType == XmlPullParser.END_TAG) {
      writer.add(EVENT_FACTORY.createEndElement(new QName(parser.getName()), null));
    }
  }

  private void addElementAttributes(XmlPullParser parser, XMLEventWriter writer) throws XMLStreamException {
    Map<String, String> attributes = Xpp3Util.loadAttributes(parser);
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      writer.add(EVENT_FACTORY.createAttribute(attribute.getKey(), attribute.getValue()));
    }
  }

  private void writeFilteredInformation(
      XMLEventWriter writer,
      String filteredInformation) throws XMLStreamException
  {
    QName name = new QName("content");
    writer.add(EVENT_FACTORY.createStartElement(name, null, null));
    XMLEvent contentEvent;
    contentEvent = EVENT_FACTORY.createCData(filteredInformation);
    writer.add(contentEvent);
    writer.add(EVENT_FACTORY.createEndElement(name, null));
  }

  ThirdPartyScanResultHandler createHandler(ItemContentType contentItemType) {
    return ThirdPartyResultHandlerFactory.newHandler(contentItemType);
  }
}
