/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cyclonedx;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

@Named
public class CycloneDxSchemaValidator
{
  public void validate(final String sbom) throws SAXException {
    final Schema schema = getXmlSchema();
    final Validator validator = getValidator(schema);
    final Source xml = new StreamSource(new StringReader(sbom));
    try {
      validator.validate(xml);
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex.getMessage(), ex);
    }
  }

  private Schema getXmlSchema() {
    try {
      final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      schemaFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

      final Source[] schemaFiles = {
          new StreamSource(getClass().getClassLoader().getResourceAsStream(
              "com/sonatype/insight/brain/cyclonedx/spdx.xsd")),
          new StreamSource(getClass().getClassLoader().getResourceAsStream(
              "com/sonatype/insight/brain/cyclonedx/bom-1.1.xsd")),
          new StreamSource(
              getClass().getClassLoader().getResourceAsStream(
                  "com/sonatype/insight/brain/cyclonedx/vulnerability-1.0.xsd"))
      };
      return schemaFactory.newSchema(schemaFiles);
    }
    catch (SAXException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  private Validator getValidator(Schema schema) {
    try {
      final Validator validator = schema.newValidator();
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      return validator;
    }
    catch (SAXNotSupportedException | SAXNotRecognizedException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }
}
