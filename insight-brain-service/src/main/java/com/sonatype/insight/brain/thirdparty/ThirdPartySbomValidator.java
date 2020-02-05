/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.inject.Named;

import com.sonatype.insight.brain.utils.Xpp3Util;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ThirdPartySbomValidator
{
  private static final Logger log = LoggerFactory.getLogger(ThirdPartySbomValidator.class);

  public List<String> validateSbomContent(File scanFile) {
    List<String> errors = new ArrayList<>();
    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(scanFile))) {
      XmlPullParser parser = ThirdPartyUtils.getXmlParser(new XmlStreamReader(gis));
      parser.next();
      while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
        processContent(parser, errors);
      }
    }
    catch (IOException | XmlPullParserException e) {
      throw new IllegalArgumentException("Error validating SBOM content", e);
    }
    log.info("Completed processing third party content in file {}", scanFile.getName());
    return errors;
  }

  private void processContent(XmlPullParser parser, List<String> errors) {
    try {
      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        String elementName = parser.getName();
        if (eventType == XmlPullParser.START_TAG) {
          if ("content".equals(elementName)) {
            loadAndProcessContent(parser, errors);
          }
        }
        eventType = parser.next();
      }
    }
    catch (XmlPullParserException | IOException e) {
      throw new IllegalArgumentException("Error reading scan content from scan file", e);
    }
  }

  private void loadAndProcessContent(XmlPullParser parser, List<String> errors) {
    try {
      Xpp3Dom content = Xpp3Util.loadElement("content", parser);
      String value = content.getValue();
      XmlPullParser contentParser = ThirdPartyUtils.getXmlParser(new StringReader(value));

      int eventType = contentParser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          String elementName = contentParser.getName();
          if ("component".equals(elementName)) {
            Xpp3Dom component = Xpp3Util.loadElement("component", contentParser);
            Xpp3Dom vulnerabilities = component.getChild("vulnerabilities");
            Xpp3Dom licenses = component.getChild("licenses");
            String componentName = component.getChild("name").getValue();
            componentName = StringUtils.isBlank(componentName) ? "[Not Provided]" : componentName;

            if (vulnerabilities != null) {
              validateVulnerabilities(vulnerabilities, componentName, errors);
            }
            if (licenses != null) {
              validateLicenses(errors, licenses, componentName);
            }
          }
        }
        eventType = contentParser.next();
      }
    }
    catch (XmlPullParserException | IOException e) {
      throw new IllegalArgumentException("Error processing SBOM component", e);
    }
  }

  private void validateLicenses(final List<String> errors, final Xpp3Dom licenses, final String componentName) {
    Arrays.stream(licenses.getChildren())
        .filter(license -> isEmptyElement(license.getChild("id")))
        .forEach(l -> errors.add(formatErrorMessage(componentName, "An element <id> of a license is null or empty")));
  }

  private void validateVulnerabilities(
      final Xpp3Dom vulnerabilities,
      final String componentName,
      final List<String> errors)
  {
    for (Xpp3Dom vulnerability : vulnerabilities.getChildren()) {
      Xpp3Dom id = vulnerability.getChild("id");
      String ref = vulnerability.getAttribute("ref");

      if (isEmptyElement(id)) {
        errors.add(
            formatErrorMessage(componentName,
                String.format("An element <id> of vulnerability with ref %s is null or empty", ref)));
      }
      Xpp3Dom ratings = vulnerability.getChild("ratings");
      validateRatings(componentName, errors, ref, ratings);
    }
  }

  private void validateRatings(
      final String componentName,
      final List<String> errors,
      final String ref,
      final Xpp3Dom ratings)
  {
    if (ratings != null) {
      for (Xpp3Dom rating : ratings.getChildren()) {
        Xpp3Dom score = rating.getChild("score");
        if (score == null || isEmptyElement(score.getChild("base"))) {
          errors.add(formatErrorMessage(componentName,
              String.format("An element <base> of a vulnerability score with ref %s is null or empty", ref)));
        }
      }
    }
  }

  private boolean isEmptyElement(final Xpp3Dom element) {
    return element == null || StringUtils.isBlank(element.getValue());
  }

  private String formatErrorMessage(String componentName, String errorMessage) {
    return String.format("Error in component %s: %s", componentName, errorMessage);
  }
}
