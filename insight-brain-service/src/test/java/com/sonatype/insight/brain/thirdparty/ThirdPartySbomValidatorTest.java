/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.test.InjectedTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ThirdPartySbomValidatorTest
    extends InjectedTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Inject
  private ThirdPartySbomValidator thirdPartySbomValidator;

  @Inject
  private Scanner scanner;

  @Test
  public void testValidateBom_valid_bom() throws Exception {
    List<String> errors = thirdPartySbomValidator
        .validateSbomContent(getScanFile("valid_bom.xml"));
    assertThat(errors).isEmpty();
  }

  @Test
  public void testValidateBom_invalid_bom() throws Exception {
    List<String> errors = thirdPartySbomValidator
        .validateSbomContent(getScanFile("invalid_bom.xml"));
    assertThat(errors).hasSize(4).containsExactly(
        "Error in component jackson-databind: An element <id> of vulnerability with ref " +
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9 is null or empty",
        "Error in component jackson-databind: An element <base> of a vulnerability score with ref " +
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9 is null or empty",
        "Error in component jackson-databind: An element <base> of a vulnerability score with ref " +
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9 is null or empty",
        "Error in component jackson-databind: An element <id> of a license is null or empty");
  }

  @Test
  public void testValidateBom_invalid_bom_not_component_name() throws Exception {
    List<String> errors = thirdPartySbomValidator
        .validateSbomContent(getScanFile("invalid_bom_not_component_name.xml"));
    assertThat(errors).isNotEmpty().hasSize(3)
        .containsExactly(
            "Error in component [Not Provided]: An element <id> of vulnerability with ref " +
                "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9 is null or empty",
            "Error in component [Not Provided]: An element <base> of a vulnerability score with ref " +
                "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9 is null or empty",
            "Error in component [Not Provided]: An element <id> of a license is null or empty");
  }

  @Test
  public void testValidateBom_invalid_file() throws Exception {
    assertThatThrownBy(() -> thirdPartySbomValidator.validateSbomContent(getScanFile("invalid_bom_file.xml")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching(
            "Error processing SBOM component *");
  }

  private File getScanFile(String fileName) throws Exception {
    URL resource = getClass().getResource("/ThirdPartySbomValidatorTest/" + fileName);
    String sbom = new String(Files.readAllBytes(Paths.get(resource.toURI())));

    ScanResult scanResult =
        scanner.scanContent(sbom, new File(tempDir.getRoot(), "sbom"), ItemContentType.SBOM, "ABCD", null);
    return scanResult.getScanFile();
  }
}
