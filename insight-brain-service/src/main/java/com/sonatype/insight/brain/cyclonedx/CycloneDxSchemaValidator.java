/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cyclonedx;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Named
public class CycloneDxSchemaValidator
{
  public List<SAXParseException> validate(final String sbom) {
    final Source xml = new StreamSource(new StringReader(sbom));
    final List<SAXParseException> schemaValidationErrors = new ArrayList<>();

    try {
      final Schema schema = getXmlSchema();
      final Validator validator = schema.newValidator();
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      validator.setErrorHandler(new ValidateSchemaErrorHandler(schemaValidationErrors));
      validator.validate(xml);
      return schemaValidationErrors;
    }
    catch (IOException | SAXException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Schema getXmlSchema() throws SAXException {
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

  private static class ValidateSchemaErrorHandler
      implements ErrorHandler
  {
    private final List<SAXParseException> exceptions;

    private ValidateSchemaErrorHandler(final List<SAXParseException> exceptions) {
      this.exceptions = exceptions;
    }

    @Override
    public void warning(SAXParseException ex) {} // don't fail validation due to warning exceptions

    @Override
    public void error(SAXParseException ex) {
      exceptions.add(ex);
    }

    @Override
    public void fatalError(SAXParseException ex) {
      exceptions.add(ex);
    }
  }
}
