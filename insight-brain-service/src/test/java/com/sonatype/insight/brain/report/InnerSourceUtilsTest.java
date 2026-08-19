/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.*;
import static org.assertj.core.api.Assertions.assertThat;

public class InnerSourceUtilsTest
{
  @Test
  public void testGetVersionlessPackageUrl() {
    assertGetVersionlessPackageUrl(createMavenCoordinates("company", "name", "1.0.1"), "pkg:maven/company/name");
    assertGetVersionlessPackageUrl(createNpmCoordinates("@angular", "2.0.1"), "pkg:npm/%40angular");
    assertGetVersionlessPackageUrl(createNugetCoordinates("simplejson", "0.38.0"), "pkg:nuget/simplejson");
    assertGetVersionlessPackageUrl(createAnameCoordinates("hawk", "win32", "0.3.0"), "pkg:a-name/hawk?qualifier=win32");
    assertGetVersionlessPackageUrl(createPypiCoordinates("PyYAML", "3.11", "win-amd64-py2.7", "exe"),
        "pkg:pypi/pyyaml?extension=exe&qualifier=win-amd64-py2.7");
    assertGetVersionlessPackageUrl(createGolangCoordinates("golang.org/x/text", "v0.3.0"),
        "pkg:golang/golang.org/x/text");
    assertGetVersionlessPackageUrl(createRpmCoordinates("glibc", "v0.3.0", "x86"), "pkg:rpm/glibc?arch=x86");
    assertGetVersionlessPackageUrl(createConanCoordinates("bison", "3.5.3", "org", "stable"),
        "pkg:conan/org/bison?channel=stable");
    assertGetVersionlessPackageUrl(createCargoCoordinates("humpty", "0.7.0", "pants"), "pkg:cargo/humpty?type=pants");
    assertGetVersionlessPackageUrl(createContainerCoordinates("docker", "psql", "14.4"),
        "pkg:generic/docker/psql?nexustype=container");
    assertGetVersionlessPackageUrl(createRubyGemsCoordinates("rails", "5.0.1", "x86"), "pkg:gem/rails?platform=x86");
    assertGetVersionlessPackageUrl(createTerraformCoordinates("tplan", "terra", "9.0"), "pkg:terraform/tplan/terra");
    assertGetVersionlessPackageUrl(createCocoapodsCoordinates("cave", "1.0.1"), "pkg:cocoapods/cave");
    assertGetVersionlessPackageUrl(createCondaCoordinates("pml", "1.0.1"), "pkg:conda/pml");
    assertGetVersionlessPackageUrl(createPecoffCoordinates("microsoft", "sysdll", "1.9.1"),
        "pkg:generic/sysdll?nexusnamespace=microsoft&nexustype=pecoff");
    assertGetVersionlessPackageUrl(createSwiftCoordinates("arc", "1.0.0"), "pkg:swift/arc");
    assertGetVersionlessPackageUrl(createIacCoordinates("bincrafters", "prima", "1.0.0"),
        "pkg:generic/bincrafters/prima?nexustype=iac");
  }

  @Test
  public void testGetVersionlessPackageUrl_Null() {
    assertThat(InnerSourceUtils.getVersionlessPackageUrl(null)).isNull();
  }

  private void assertGetVersionlessPackageUrl(final ComponentIdentifier id, final String s) {
    PackageUrlIdentifier versionlessPackageUrl = InnerSourceUtils.getVersionlessPackageUrl(id);
    assertThat(versionlessPackageUrl.getPackageUrl()).isEqualTo(s);
  }

  @Test
  public void testIsRemediationVersionApplicable_npmScenarios() {
    // Lower next version
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", "1.2.3"), "1.2.2")).isFalse();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", "1.2.3"), "1.1.3")).isFalse();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", "1.2.3"), "0.2.3")).isFalse();

    // Equal next version
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", "1.2.3"), "1.2.3")).isFalse();

    // Larger next version
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", "1.2.3"), "1.2.4")).isTrue();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", "1.2.3"), "1.3.0")).isTrue();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", "1.2.3"), "10.0.0")).isFalse();

    // Pre-release next version
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", "1.2.3"), "1.2.4-beta")).isFalse();
  }

  @Test
  public void testIsRemediationVersionApplicable_mavenScenarios() {
    // Lower next version
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.2.3"), "1.2.2")).isFalse();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.2.3"), "1.1.3")).isFalse();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "0.9.93")).isFalse();

    // Equal next version
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.2.3"), "1.2.3")).isFalse();

    // Larger next version
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.0.1")).isTrue();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.1.0")).isTrue();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "10.0.0")).isFalse();

    // Snapshot
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.1.0-SNAPSHOT")).isFalse();
  }

  @Test
  public void testIsRemediationVersionApplicable_mavenVersions_EdgeCases() {
    // Same version with release qualifier
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.0.0.RELEASE")).isFalse();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.0.0.Final")).isFalse();

    // Larger version with release qualifier
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.0.1.RELEASE")).isTrue();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.0.1.Final")).isTrue();

    // Larger version from snapshot
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0-SNAPSHOT"), "1.0.0")).isTrue();

    // Pre-release versions with Semver instead of SNAPSHOT
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.0.0-rc1")).isFalse();
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createMavenCoordinates("com.acme", "lib", "1.0.0"), "1.1.0-alpha")).isTrue();
  }

  @Test
  public void testIsRemediationVersionApplicable_invalidAndUnsupportedInputs() {
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(null, "1.0.0")).isFalse();

    // Component with null version
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", null), "1.0.1")).isFalse();

    // Next version is null
    assertThat(InnerSourceUtils.isValidAutomatedVersionUpdate(
        createNpmCoordinates("pkg", null), null)).isFalse();
  }
}
