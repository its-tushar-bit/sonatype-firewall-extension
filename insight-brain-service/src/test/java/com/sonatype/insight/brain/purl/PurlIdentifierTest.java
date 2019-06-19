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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PurlIdentifierTest
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

    PurlIdentifier purlIdentifier1 = new PurlIdentifier(packageUrl1);
    PurlIdentifier purlIdentifier2 = new PurlIdentifier(packageUrl2);
    assertThat(purlIdentifier1.toComponentIdentifier()).isNotEqualTo(purlIdentifier2.toComponentIdentifier());
    assertThat(PurlIdentifier.fromComponentIdentifier(coordinates)).isEqualTo(purlIdentifier1);
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
    coordinates.put(PurlIdentifier.GENERIC_NAME, "name");
    coordinates.put(PurlIdentifier.GENERIC_NAMESPACE, "namespace");
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
    PurlIdentifier purlIdentifier =
        PurlIdentifier.fromComponentIdentifier(new ComponentIdentifier("generic", coordinates));
    String packageUrl = "pkg:generic/namespace/name@version";

    assertThat(purlIdentifier.getPackageUrl()).isEqualTo(packageUrl);
  }

  @Test
  public void testPurlIdentifier_Generic_ResolvePackageIdWithNamespace() throws Exception {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("packageId", "namespace/name");
    coordinates.put(ComponentIdentifier.VERSION, "version");
    PurlIdentifier purlIdentifier =
        PurlIdentifier.fromComponentIdentifier(new ComponentIdentifier("generic", coordinates));
    String packageUrl = "pkg:generic/namespace/name@version";

    assertThat(URLDecoder.decode(purlIdentifier.getPackageUrl(), StandardCharsets.UTF_8.name())).isEqualTo(packageUrl);
  }

  @Test
  public void testPurlIdentifier_InvalidComponentIdentifier() {
    final Map<String, String> coordinates = new HashMap<>();
    coordinates.put("blah", "blah");

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> PurlIdentifier.fromComponentIdentifier(new ComponentIdentifier("format", coordinates)));
  }

  @Test
  public void testPurlIdentifier_NullComponentIdentifier() {
    assertThat(PurlIdentifier.fromComponentIdentifier(null)).isNull();
  }

  @Test
  public void testPurlIdentifier_invalidPurlUrl() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new PurlIdentifier("invalid-purl-url"));
  }

  @Test
  public void testPurlIdentifier_ResolveNameAndNamespace_WithMultipleSlashes() {
    PurlIdentifier purlIdentifier = PurlIdentifier
        .fromComponentIdentifier(ComponentIdentifier.createRpmCoordinates("some/path/to/module", "version", null));
    String packageUrl = "pkg:rpm/some/path/to/module@version";
    assertThat(purlIdentifier.getPackageUrl()).isEqualTo(packageUrl);
  }

  @Test
  public void testPurlIdentifier_ResolveNameAndNamespace_WithoutLeadingOrTrailingSlashes() {
    PurlIdentifier purlIdentifier = PurlIdentifier
        .fromComponentIdentifier(ComponentIdentifier.createRpmCoordinates("///the/path////", "version", null));
    String packageUrl = "pkg:rpm/the/path@version";
    assertThat(purlIdentifier.getPackageUrl()).isEqualTo(packageUrl);
  }

  private void testCoordinateWithPurl(ComponentIdentifier identifier, String packageUrl) {
    PurlIdentifier purlUrlIdentifier = new PurlIdentifier(packageUrl);
    PurlIdentifier purlComponentIdentifier = PurlIdentifier.fromComponentIdentifier(identifier);

    assertThat(purlUrlIdentifier).isEqualTo(purlComponentIdentifier);
    ComponentIdentifier urlIdentifier = purlUrlIdentifier.toComponentIdentifier();
    assertThat(identifier).isEqualTo(urlIdentifier);
  }
}
