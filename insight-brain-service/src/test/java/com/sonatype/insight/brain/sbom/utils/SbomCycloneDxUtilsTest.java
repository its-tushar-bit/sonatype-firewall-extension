/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import org.cyclonedx.BomParserFactory;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.Parser;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomCycloneDxUtilsTest
{
  @Test
  public void testGetApplicationNameAndVersionSafely_noMetadata() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-no-metadata.json");
    assertThat(SbomCycloneDxUtils.getApplicationNameSafely(bom)).isNull();
    assertThat(SbomCycloneDxUtils.getApplicationVersionSafely(bom)).isNull();
  }

  @Test
  public void testGetApplicationNameAndVersionSafely_Missing_AppName_AppVersion() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-no-appName-appVersion.xml");
    assertThat(SbomCycloneDxUtils.getApplicationNameSafely(bom)).isNull();
    assertThat(SbomCycloneDxUtils.getApplicationVersionSafely(bom)).isNull();
  }

  @Test
  public void testGetApplicationNameAndVersionSafely() throws Exception {
    Bom bom = getCycloneDxDocument("sbom-with-metadata.xml");
    assertThat(SbomCycloneDxUtils.getApplicationNameSafely(bom)).isEqualTo("MyAppName");
    assertThat(SbomCycloneDxUtils.getApplicationVersionSafely(bom)).isEqualTo("1.0.1");
  }

  private static Bom getCycloneDxDocument(final String fileName)
      throws IOException, ParseException, URISyntaxException
  {
    URL resource = SbomCycloneDxUtilsTest.class.getResource("/SbomCycloneDxUtilsTest/" + fileName);
    String content =
        new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(resource).toURI())), StandardCharsets.UTF_8);
    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(contentBytes);
    return parser.parse(contentBytes);
  }
}
