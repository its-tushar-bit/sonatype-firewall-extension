/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.matcher.firewall;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class NpmPathnameParserTest
    extends BrainInjectedTest
{
  @Inject
  private NpmPathnameParser npmPathnameParser;

  @Test
  public void testPathnameParser_NullPath() {
    assertThat(npmPathnameParser.parsePathname(null)).isNull();
  }

  @Test
  public void testPathnameParser_EmptyPath() {
    assertThat(npmPathnameParser.parsePathname("")).isNull();
  }

  @Test
  public void testPathnameParser_WhitespacePath() {
    assertThat(npmPathnameParser.parsePathname(" ")).isNull();
  }

  @Test
  public void testPathnameParser_UnscopedNpmCoordinates() {
    // Package id doesn't match
    assertThat(npmPathnameParser.parsePathname("packageId1/-/packageId2-version.tgz")).isNull();

    // Not the right extension
    assertThat(npmPathnameParser.parsePathname("packageId/-/packageId-version.json")).isNull();

    assertThat(npmPathnameParser.parsePathname("packageId/-/packageId-version.tgz"))
        .isEqualTo(ComponentIdentifier.createNpmCoordinates("packageId", "version"));

    // Dash in package id
    assertThat(npmPathnameParser.parsePathname("package-Id/-/package-Id-version.tgz"))
        .isEqualTo(ComponentIdentifier.createNpmCoordinates("package-Id", "version"));

    // Dash in version
    assertThat(npmPathnameParser.parsePathname("packageId/-/packageId-version-1.tgz"))
        .isEqualTo(ComponentIdentifier.createNpmCoordinates("packageId", "version-1"));
  }

  @Test
  public void testPathnameParser_ScopedNpmCoordinates() {
    // Package id doesn't match
    assertThat(npmPathnameParser.parsePathname("@scope/packageId1/-/packageId2-version.tgz")).isNull();

    // Not the right extension
    assertThat(npmPathnameParser.parsePathname("@scope/packageId/-/packageId-version.json")).isNull();

    assertThat(npmPathnameParser.parsePathname("@scope/packageId/-/packageId-version.tgz"))
        .isEqualTo(ComponentIdentifier.createNpmCoordinates("@scope/packageId", "version"));

    // Dash in package id
    assertThat(npmPathnameParser.parsePathname("@scope/package-Id/-/package-Id-version.tgz"))
        .isEqualTo(ComponentIdentifier.createNpmCoordinates("@scope/package-Id", "version"));

    // Dash in version
    assertThat(npmPathnameParser.parsePathname("@scope/packageId/-/packageId-version-1.tgz"))
        .isEqualTo(ComponentIdentifier.createNpmCoordinates("@scope/packageId", "version-1"));
  }
}
