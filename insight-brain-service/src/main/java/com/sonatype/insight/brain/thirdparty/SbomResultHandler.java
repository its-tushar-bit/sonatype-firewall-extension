/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.utils.Xpp3Util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.cyclonedx.BomGenerator;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;

public class SbomResultHandler
    implements ThirdPartyScanResultHandler
{
  @Override
  public String handleAndFilterContents(
      ThirdPartyScanContent content,
      ThirdPartyFile thirdPartyFile)
  {
    try {
      if (!StringUtils.isBlank(content.getContent())) {
        Bom sbom = new Bom();
        filterSbom(content.getContent(), sbom);

        if (sbom.getComponents() != null && sbom.getComponents().isEmpty()) {
          return content.getContent();
        }
        else {
          return generateFilteredSbom(sbom);
        }
      }
      return content.getContent();
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering sbom file", e);
    }
  }

  private void filterSbom(String content, Bom sbom) throws XmlPullParserException, IOException {
    Stack<String> elementNameStack = new Stack<>();
    XmlPullParser parser = new MXParser();
    parser.setInput(new StringReader(content));

    int eventType = parser.getEventType();
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if ("component".equals(elementName)) {
          Xpp3Dom component = Xpp3Util.loadElement("component", parser);
          Xpp3Dom packageUrl = component.getChild("purl");
          if (packageUrl != null && !StringUtils.isBlank(packageUrl.getValue())) {
            Xpp3Dom name = component.getChild("name");
            Xpp3Dom version = component.getChild("version");

            Component sbomComponent = new Component();
            sbomComponent.setType(Component.Type.valueOf(component.getAttribute("type").toUpperCase()));
            sbomComponent.setName(name.getValue());
            sbomComponent.setVersion(version.getValue());
            sbomComponent.setPurl(packageUrl.getValue());
            sbom.addComponent(sbomComponent);
          }
        }
        else {
          elementNameStack.push(elementName);
        }
      }
      else if (eventType == XmlPullParser.END_TAG) {
        String beginName = elementNameStack.pop();
        String endName = parser.getName();
        if (!beginName.equals(endName)) {
          throw new XmlPullParserException("End tag '" + endName + "' does not match start tag '" + beginName + "'.");
        }
      }
      eventType = parser.next();
    }
  }

  private String generateFilteredSbom(Bom sbom) throws ParserConfigurationException, TransformerException {
    BomGenerator generator = BomGeneratorFactory.create(Version.VERSION_11, sbom);
    generator.generate();
    return generator.toXmlString();
  }
}
