/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.purl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PackageUrlIdentifierTest
{
  @Test
  public void testPurlIdentifier_Maven() {
    ComponentIdentifier coordinates = ComponentIdentifier.createMavenCoordinates("g",
        "a", "v");
    String packageUrl = "pkg:maven/g/a@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Maven_OptionalCoordinates() {
    ComponentIdentifier coordinates = ComponentIdentifier.createMavenCoordinates("g",
        "a", "v", "c", "e");
    String packageUrl = "pkg:maven/g/a@v?classifier=c&type=e";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Maven_Extension() {
    String packageUrl1 = "pkg:maven/g/a@v?classifier=c&type=e";
    String packageUrl2 = "pkg:maven/g/a@v?classifier=c&extension=e";
    ComponentIdentifier coordinates = ComponentIdentifier.createMavenCoordinates("g",
        "a", "v", "c", "e");

    PackageUrlIdentifier packageUrlIdentifier1 = new PackageUrlIdentifier(packageUrl1);
    PackageUrlIdentifier packageUrlIdentifier2 = new PackageUrlIdentifier(packageUrl2);
    assertThat(packageUrlIdentifier1.toComponentIdentifier())
        .isNotEqualTo(packageUrlIdentifier2.toComponentIdentifier());
    assertThat(PackageUrlIdentifier.fromComponentIdentifier(coordinates)).isEqualTo(packageUrlIdentifier1);
  }

  @Test
  public void testPurlIdentifier_Npm() {
    ComponentIdentifier coordinates = ComponentIdentifier.createNpmCoordinates("p", "v");
    String packageUrl = "pkg:npm/p@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Npm_WithPurlNameSpace() {
    ComponentIdentifier coordinates = ComponentIdentifier.createNpmCoordinates("namespace/p", "v");
    String packageUrl = "pkg:npm/namespace/p@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Nuget() {
    ComponentIdentifier coordinates = ComponentIdentifier.createNugetCoordinates("p", "v");
    String packageUrl = "pkg:nuget/p@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Aname() {
    ComponentIdentifier coordinates = ComponentIdentifier.createAnameCoordinates("n", null, "v");
    String packageUrl = "pkg:a-name/n@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Aname_WithQualifier() {
    ComponentIdentifier coordinates = ComponentIdentifier.createAnameCoordinates("n", "q", "v");
    String packageUrl = "pkg:a-name/n@v?qualifier=q";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Pypi() {
    ComponentIdentifier coordinates = ComponentIdentifier.createPypiCoordinates("n", "v", null, null);
    String packageUrl = "pkg:pypi/n@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Pypi_WithOptionalQualifiers() {
    ComponentIdentifier coordinates = ComponentIdentifier.createPypiCoordinates("n", "v", "q", "e");
    String packageUrl = "pkg:pypi/n@v?extension=e&qualifier=q";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Rpm() {
    ComponentIdentifier coordinates = ComponentIdentifier.createRpmCoordinates("n", "v", null);
    String packageUrl = "pkg:rpm/n@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Rpm_WithOptionalQualifiers() {
    ComponentIdentifier coordinates = ComponentIdentifier.createRpmCoordinates("n", "v", "a");
    String packageUrl = "pkg:rpm/n@v?arch=a";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Rubygems() {
    ComponentIdentifier coordinates = ComponentIdentifier.createRubyGemsCoordinates("n", "v", null);
    String packageUrl = "pkg:gem/n@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Rubygems_WithOptionalQualifiers() {
    ComponentIdentifier coordinates = ComponentIdentifier.createRubyGemsCoordinates("n", "v", "p");
    String packageUrl = "pkg:gem/n@v?platform=p";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Golang() {
    ComponentIdentifier coordinates = ComponentIdentifier.createGolangCoordinates("path/name", "v");
    String packageUrl = "pkg:golang/path/name@v";
    testCoordinateWithPurl(coordinates, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Generic() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(PackageUrlIdentifier.GENERIC_NAME, "name");
    coordinates.put(PackageUrlIdentifier.GENERIC_NAMESPACE, "namespace");
    coordinates.put(ComponentIdentifier.VERSION, "version");
    coordinates.put("foo", "bar");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("generic", coordinates);
    String packageUrl = "pkg:generic/namespace/name@version?foo=bar";
    testCoordinateWithPurl(componentIdentifier, packageUrl);
  }

  @Test
  public void testPurlIdentifier_Generic_ResolveArtifactIdAndGroupId() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("artifactId", "name");
    coordinates.put("groupId", "namespace");
    coordinates.put(ComponentIdentifier.VERSION, "version");
    PackageUrlIdentifier packageURLIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(new ComponentIdentifier("generic", coordinates));
    String packageUrl = "pkg:generic/namespace/name@version";

    assertThat(packageURLIdentifier.getPackageUrl()).isEqualTo(packageUrl);
  }

  @Test
  public void testPurlIdentifier_Generic_ResolvePackageIdWithNamespace() throws Exception {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("packageId", "namespace/name");
    coordinates.put(ComponentIdentifier.VERSION, "version");
    PackageUrlIdentifier packageURLIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(new ComponentIdentifier("generic", coordinates));
    String packageUrl = "pkg:generic/namespace/name@version";

    assertThat(URLDecoder.decode(packageURLIdentifier.getPackageUrl(), StandardCharsets.UTF_8.name()))
        .isEqualTo(packageUrl);
  }

  @Test
  public void testPurlIdentifier_InvalidComponentIdentifier() {
    final Map<String, String> coordinates = new HashMap<>();
    coordinates.put("blah", "blah");

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> PackageUrlIdentifier.fromComponentIdentifier(new ComponentIdentifier("format", coordinates)));
  }

  @Test
  public void testPurlIdentifier_NullComponentIdentifier() {
    assertThat(PackageUrlIdentifier.fromComponentIdentifier(null)).isNull();
  }

  @Test
  public void testPurlIdentifier_invalidPurlUrl() {
    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> new PackageUrlIdentifier("invalid-purl-url"));
  }

  @Test
  public void testPurlIdentifier_ResolveNameAndNamespace_WithMultipleSlashes() {
    PackageUrlIdentifier packageURLIdentifier = PackageUrlIdentifier
        .fromComponentIdentifier(ComponentIdentifier.createRpmCoordinates("some/path/to/module", "version", null));
    String packageUrl = "pkg:rpm/some/path/to/module@version";
    assertThat(packageURLIdentifier.getPackageUrl()).isEqualTo(packageUrl);
  }

  @Test
  public void testPurlIdentifier_ResolveNameAndNamespace_WithoutLeadingOrTrailingSlashes() {
    PackageUrlIdentifier packageURLIdentifier = PackageUrlIdentifier
        .fromComponentIdentifier(ComponentIdentifier.createRpmCoordinates("///the/path////", "version", null));
    String packageUrl = "pkg:rpm/the/path@version";
    assertThat(packageURLIdentifier.getPackageUrl()).isEqualTo(packageUrl);
  }

  @Test
  public void testEnsureComplete_Maven() {
    String packageUrl = "pkg:maven/g/a@v?type=t&classifier=c";
    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier(packageUrl);
    assertThatCode(packageURLIdentifier::ensureCompleteIdentifier).doesNotThrowAnyException();
  }

  @Test
  public void testEnsureComplete_Maven_MissingTypeForExtension() {
    String packageUrl = "pkg:maven/g/a@v?extension=e&classifier=c";
    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier(packageUrl);
    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> packageURLIdentifier.ensureCompleteIdentifier())
        .withMessage("The following coordinates are missing for given format: [type]");
  }

  @Test
  public void testEnsureComplete_Rpm() {
    String packageUrl = "pkg:rpm/n@v?arch=a&distro=d";
    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier(packageUrl);
    assertThatCode(packageURLIdentifier::ensureCompleteIdentifier).doesNotThrowAnyException();
  }

  @Test
  public void testEnsureComplete_Rpm_MissingArch() {
    String packageUrl = "pkg:rpm/n@v?architecture=a&distro=d";
    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier(packageUrl);
    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> packageURLIdentifier.ensureCompleteIdentifier())
        .withMessage("The following coordinates are missing for given format: [arch]");
  }

  @Test
  public void testEnsureComplete_PyPi_MissingExtension() {
    String packageUrl = "pkg:pypi/n@v";
    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier(packageUrl);
    assertThatExceptionOfType(InvalidPackageURLException.class)
        .isThrownBy(() -> packageURLIdentifier.ensureCompleteIdentifier())
        .withMessage("The following coordinates are missing for given format: [extension]");
  }

  @Test
  public void testToPackageUrl() {
    ComponentIdentifier coordinates = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "j");
    assertThat(PackageUrlIdentifier.toPackageUrl(coordinates)).isEqualTo("pkg:maven/g/a@v?classifier=c&type=j");
  }

  @Test
  public void testToPackageUrl_NullComponentIdentifier() {
    assertThat(PackageUrlIdentifier.toPackageUrl(null)).isNull();
  }

  @Test
  public void testToPackageUrl_InvalidCoordinates() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("blah", "blah");
    ComponentIdentifier invalidCoords = new ComponentIdentifier("format", coordinates);
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> {
          PackageUrlIdentifier.toPackageUrl(invalidCoords);
        })
        .withMessageContaining("The PackageURL name specified is invalid");
  }

  private void testCoordinateWithPurl(ComponentIdentifier identifier, String packageUrl) {
    PackageUrlIdentifier purlUrlIdentifier = new PackageUrlIdentifier(packageUrl);
    PackageUrlIdentifier purlComponentIdentifier = PackageUrlIdentifier.fromComponentIdentifier(identifier);

    assertThat(purlUrlIdentifier).isEqualTo(purlComponentIdentifier);
    ComponentIdentifier urlIdentifier = purlUrlIdentifier.toComponentIdentifier();
    assertThat(identifier).isEqualTo(urlIdentifier);
  }
}
