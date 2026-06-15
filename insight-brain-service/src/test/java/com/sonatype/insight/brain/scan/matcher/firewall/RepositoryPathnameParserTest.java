/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.matcher.firewall;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryPathnameParserTest
{
  private final RepositoryPathnameParser parser = new RepositoryPathnameParser(new NpmPathnameParser());

  @Test
  public void test_Parse_Unknown() {
    assertThat(parser.parse("some/pathname/", "unknown")).isNull();
  }

  @Test
  public void test_Parse_Npm() {
    ComponentIdentifier result = parser.parse("packageId/-/packageId-1.0.0.tgz", "npm");
    assertThat(result).isEqualTo(ComponentIdentifier.createNpmCoordinates("packageId", "1.0.0"));
  }

  @Test
  public void test_Parse_Swift_archive() {
    ComponentIdentifier result = parser.parse("/github.com/apple/swift-nio/2.42.0.zip", "swift");
    assertThat(result).isEqualTo(ComponentIdentifier.createSwiftCoordinates("github.com/apple/swift-nio", "2.42.0"));
  }

  @Test
  public void test_Parse_Swift_archive_without_leading_slash() {
    ComponentIdentifier result = parser.parse("github.com/vapor/vapor/4.0.0.zip", "swift");
    assertThat(result).isEqualTo(ComponentIdentifier.createSwiftCoordinates("github.com/vapor/vapor", "4.0.0"));
  }

  @Test
  public void test_Parse_Swift_manifest() {
    ComponentIdentifier result = parser.parse("/github.com/apple/swift-nio/2.42.0/Package.swift", "swift");
    assertThat(result).isEqualTo(ComponentIdentifier.createSwiftCoordinates("github.com/apple/swift-nio", "2.42.0"));
  }

  @Test
  public void test_Parse_Swift_versionedManifest() {
    ComponentIdentifier result =
        parser.parse("/github.com/apple/swift-nio/2.42.0/Package@swift-5.9.swift", "swift");
    assertThat(result).isEqualTo(ComponentIdentifier.createSwiftCoordinates("github.com/apple/swift-nio", "2.42.0"));
  }

  @Test
  public void test_Parse_Swift_tooFewSegments() {
    assertThat(parser.parse("/apple/1.0.0.zip", "swift")).isNull();
  }
}
