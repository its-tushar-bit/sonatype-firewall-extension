/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

import static com.sonatype.insight.brain.utils.RepositoryPathnameSerializer.toPathname;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @deprecated The tested class is deprecated
 */
@Deprecated
public class RepositoryPathnameSerializerTest
{
  @Test
  public void testToPathname_Maven() {
    assertThat(toPathname(ComponentIdentifier.createMavenCoordinates("com.sonatype", "test", "1.0", null, "jar")))
        .isEqualTo("com/sonatype/test/1.0/test-1.0.jar");
    assertThat(toPathname(ComponentIdentifier.createMavenCoordinates("com.sonatype", "test", "1.0", "", "jar")))
        .isEqualTo("com/sonatype/test/1.0/test-1.0.jar");
    assertThat(toPathname(ComponentIdentifier.createMavenCoordinates("com.sonatype", "test", "1.0", "uber", "jar")))
        .isEqualTo("com/sonatype/test/1.0/test-1.0-uber.jar");
  }

  @Test
  public void testToPathname_Nuget() {
    assertThat(toPathname(ComponentIdentifier.createNugetCoordinates("Sonatype.Test", "1.2.3")))
        .isEqualTo("Sonatype.Test/1.2.3/Sonatype.Test-1.2.3.nupkg");
  }

  @Test
  public void testToPathname_Npm() {
    assertThat(toPathname(ComponentIdentifier.createNpmCoordinates("package-Id", "version")))
        .isEqualTo("package-Id/-/package-Id-version.tgz");
    assertThat(toPathname(ComponentIdentifier.createNpmCoordinates("@scope/package-Id", "version")))
        .isEqualTo("@scope/package-Id/-/package-Id-version.tgz");
  }

  @Test
  public void testToPathname_Pypi() {
    assertThat(toPathname(ComponentIdentifier.createPypiCoordinates("name", "version", "qualifier", "extension")))
        .isEqualTo("ignored");
  }

  @Test
  public void testToPathname_RubyGems() {
    assertThat(toPathname(ComponentIdentifier.createRubyGemsCoordinates("name-with-hyphens", "1.2.3", null)))
        .isEqualTo("gems/name-with-hyphens-1.2.3.gem");
    assertThat(toPathname(ComponentIdentifier.createRubyGemsCoordinates("name-with-hyphens", "1.2.3", "")))
        .isEqualTo("gems/name-with-hyphens-1.2.3.gem");
    assertThat(toPathname(ComponentIdentifier.createRubyGemsCoordinates("name-with-hyphens", "1.2.3", " ")))
        .isEqualTo("gems/name-with-hyphens-1.2.3.gem");
    assertThat(toPathname(ComponentIdentifier.createRubyGemsCoordinates("name-with-hyphens", "1.2.3", "platform")))
        .isEqualTo("gems/name-with-hyphens-1.2.3-platform.gem");
  }

  @Test
  public void testToPathname_Golang() {
    assertThat(toPathname(ComponentIdentifier.createGolangCoordinates("github.com/sonatype/example", "v1.0.1")))
        .isEqualTo("github.com/sonatype/example/@v/v1.0.1.zip");
  }

  @Test
  public void testToPathname_Conan() {
    assertThat(toPathname(ComponentIdentifier.createConanCoordinates("project1", "version1", null, null)))
        .isEqualTo("conans/_/project1/version1/_/conan_package.tgz");
    assertThat(toPathname(ComponentIdentifier.createConanCoordinates("project1", "version1", "", "")))
        .isEqualTo("conans/_/project1/version1/_/conan_package.tgz");
    assertThat(toPathname(ComponentIdentifier.createConanCoordinates("project1", "version1", " ", " ")))
        .isEqualTo("conans/_/project1/version1/_/conan_package.tgz");
    assertThat(toPathname(ComponentIdentifier.createConanCoordinates("project1", "version1", "group1", "channel1")))
        .isEqualTo("conans/group1/project1/version1/channel1/conan_package.tgz");
  }

  @Test
  public void testToPathname_Conda() {
    assertThat(toPathname(
        ComponentIdentifier.createCondaCoordinates("name1", "version1", "path", "build", "arch", "conda")))
            .isEqualTo("path/arch/name1-version1-build.conda");
    assertThat(toPathname(
        ComponentIdentifier.createCondaCoordinates("name1", "version1", null, "build", "arch", "conda")))
            .isEqualTo("arch/name1-version1-build.conda");
  }

  @Test
  public void testToPathname_CocoaPods() {
    assertThat(toPathname(ComponentIdentifier.createCocoapodsCoordinates("project1", "version1")))
        .isEqualTo("pods/project1/version1");
  }

  @Test
  public void testToPathname_Composer() {
    assertThat(toPathname(ComponentIdentifier.createComposerCoordinates("vendor", "project", "version")))
        .isEqualTo("vendor/project/version/vendor-project-version.zip");
  }

  @Test
  public void testToPathname_Cran() {
    assertThat(toPathname(ComponentIdentifier.createCranCoordinates("name1", "version1", null)))
        .isEqualTo("bin/os/name1_version1.tgz");
    assertThat(toPathname(ComponentIdentifier.createCranCoordinates("name1", "version1", "type1")))
        .isEqualTo("bin/os/name1_version1.tgz");
  }

  @Test
  public void testToPathname_Bower() {
    assertThat(toPathname(new ComponentIdentifier("bower", createMap("name", "project1", "version", "version1"))))
        .isEqualTo("project1/version1/package.tar.gz");
  }

  @Test
  public void testToPathname_Alpine() {
    assertThat(toPathname(new ComponentIdentifier("alpine", createMap("name", "depName", "version", "1.2.3456-r0"))))
        .isEqualTo("path/depName-1.2.3456-r0.apk");
  }

  @Test
  public void testToPathname_Debian() {
    assertThat(toPathname(
        new ComponentIdentifier("deb", createMap("namespace", "ubuntu", "name", "vim", "version", "8.0.1453"))))
            .isEqualTo("path/vim_8.0.1453-ubuntu_amd64.deb");
  }

  @Test
  public void testToPathname_Cargo() {
    assertThat(toPathname(
        ComponentIdentifier.createCargoCoordinates("name1", "version1", null)))
            .isEqualTo("crates/name1/version1/download");
  }

  @Test
  public void testToPathname_PackageUrl() {
    assertThat(toPathname(PackageUrlIdentifier.toPackageUrl(
        ComponentIdentifier.createMavenCoordinates("com.sonatype", "test", "1.0", "uber", "jar"))))
            .isEqualTo("com/sonatype/test/1.0/test-1.0-uber.jar");
  }

  @Test
  public void testToPathname_HuggingfaceRepo() {
    assertThat(toPathname(
        ComponentIdentifier.createHuggingfaceRepoCoordinates("org/name", "version1")))
            .isEqualTo("org/name/resolve/version1");
  }

  @Test
  public void testToPathname_HuggingfaceModel() {
    assertThat(toPathname(
        ComponentIdentifier.createHuggingfaceModelCoordinates("org/name", "model", "version1", "fmt", "extension")))
            .isEqualTo("org/name/resolve/version1/model.extension");
  }

  private Map<String, String> createMap(String... keysAndValues) {
    Map<String, String> result = new HashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      result.put(keysAndValues[i], keysAndValues[i + 1]);
    }
    return result;
  }
}
