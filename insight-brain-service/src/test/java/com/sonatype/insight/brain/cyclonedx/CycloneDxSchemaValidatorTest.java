/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cyclonedx;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXParseException;

import static org.assertj.core.api.Assertions.assertThat;
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
    List<SAXParseException> errors = validator.validate(readSbom("/CycloneDxSchemaValidatorTest/valid_sbom.xml"));
    assertThat(errors.isEmpty());
  }

  @Test
  public void testValidate_invalidSbom() throws Exception {
    List<SAXParseException> errors = validator.validate(readSbom("/CycloneDxSchemaValidatorTest/invalid_sbom.xml"));
    assertThat(errors).isNotEmpty();
    assertErrorMessages(errors, "cvc-complex-type.4: Attribute 'ref' must appear on element 'v:vulnerability'.",
        "cvc-complex-type.4: Attribute 'name' must appear on element 'v:source'.");
  }

  @Test
  public void testValidate_withValidVulnerabilityComponentNode() throws Exception {
    List<SAXParseException> errors =
        validator.validate(readSbom("/CycloneDxSchemaValidatorTest/valid_vulnerability_component_node.xml"));
    assertThat(errors).isEmpty();
  }

  @Test
  public void testValidate_withValidVulnerabilityBomNode() throws Exception {
    List<SAXParseException> errors =
        validator.validate(readSbom("/CycloneDxSchemaValidatorTest/valid_vulnerability_bom_node.xml"));
    assertThat(errors).isEmpty();
  }

  @Test
  public void testValidate_withInvalidVulnerability() throws Exception {
    List<SAXParseException> errors =
        validator.validate(readSbom("/CycloneDxSchemaValidatorTest/invalid_vulnerability.xml"));
    assertThat(errors).isNotEmpty();
    assertErrorMessages(errors, "cvc-complex-type.4: Attribute 'ref' must appear on element 'v:vulnerability'.");
  }

  @Test
  public void testValidate_shouldNotAllowXmlReferencingExternalDTD() {
    assertThatThrownBy(
        () -> validator.validate(readSbom("/CycloneDxSchemaValidatorTest/xml_referencing_external_dtd.xml")))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("SAXParseException")
        .hasMessageContaining("access is not allowed due to restriction set by the accessExternalDTD property");
  }

  private String readSbom(final String path) throws Exception {
    byte[] bytes = Files.readAllBytes(Paths.get(getClass().getResource(path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private void assertErrorMessages(final List<SAXParseException> exceptions, final String... expectedErrorMessages) {
    final List<String> actualErrorMessages =
        exceptions.stream().map(SAXParseException::getMessage).collect(Collectors.toList());

    assertThat(actualErrorMessages).containsExactlyInAnyOrder(expectedErrorMessages);
  }
}
