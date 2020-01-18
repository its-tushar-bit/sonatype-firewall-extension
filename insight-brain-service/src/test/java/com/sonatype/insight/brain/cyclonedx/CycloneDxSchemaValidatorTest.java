/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cyclonedx;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CycloneDxSchemaValidatorTest
{
  private CycloneDxSchemaValidator validator;

  @Before
  public void setup() {
    validator = new CycloneDxSchemaValidator();
  }

  @Test
  public void testValidate_validSbom() throws Exception {
    validator.validate(readSbom("/CycloneDxSchemaValidatorTest/valid_sbom.xml"));
  }

  @Test
  public void testValidate_invalidSbom()  {
    assertThatThrownBy(
        () -> validator.validate(readSbom("/CycloneDxSchemaValidatorTest/invalid_sbom.xml")))
        .isInstanceOf(SAXParseException.class)
        .hasMessageMatching("cvc-complex-type.4:.*['\"]ref['\"].*['\"]v:vulnerability['\"].*");
  }

  @Test
  public void testValidate_withValidVulnerabilityComponentNode() throws Exception {
    validator.validate(readSbom("/CycloneDxSchemaValidatorTest/valid_vulnerability_component_node.xml"));
  }

  @Test
  public void testValidate_withValidVulnerabilityBomNode() throws Exception {
    validator.validate(readSbom("/CycloneDxSchemaValidatorTest/valid_vulnerability_bom_node.xml"));
  }

  @Test
  public void testValidate_withInvalidVulnerability() {
    assertThatThrownBy(
        () -> validator.validate(readSbom("/CycloneDxSchemaValidatorTest/invalid_vulnerability.xml")))
        .isInstanceOf(SAXParseException.class)
        .hasMessageMatching("cvc-complex-type.4:.*['\"]ref['\"].*['\"]v:vulnerability['\"].*");
  }

  @Test
  public void testValidate_shouldNotAllowXmlReferencingExternalDTD() {
    assertThatThrownBy(
        () -> validator.validate(readSbom("/CycloneDxSchemaValidatorTest/xml_referencing_external_dtd.xml")))
        .isInstanceOf(SAXException.class)
        .hasMessageContaining("accessExternalDTD");
  }

  @Test
  public void testValidate_malformedXmlMissingClosingTag() {
    assertThatThrownBy(
        () -> validator.validate(readSbom("/CycloneDxSchemaValidatorTest/malformed_xml.xml")))
        .isInstanceOf(SAXException.class)
        .hasMessageContainingAll("component", ">", "/>");
  }

  private String readSbom(final String path) throws Exception {
    byte[] bytes = Files.readAllBytes(Paths.get(getClass().getResource(path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
