/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class FileCoordinateDisplayNameGeneratorTest
{
  ComponentIdentifier componentIdentifier = null;

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Maven() {
    // Cannot determine the component identifier for format, name and version: maven, p1, v1
    // Throws Error transforming to component identifier: The PackageURL specified is invalid.
    // Maven requires both a namespace and name.
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "maven", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Generic() {
    // Determines from component identifier the same as default with name and format
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "generic", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Nuget() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "nuget", "p1", "v1"))
            .isEqualTo("p1 v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_AName() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "a-name", "p1", "v1"))
            .isEqualTo("p1 v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Pypi() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "pypi", "p1", "v1"))
            .isEqualTo("p1 v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Rpm() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "rpm", "p1", "v1"))
            .isEqualTo("p1-v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Gem() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "gem", "p1", "v1"))
            .isEqualTo("p1 v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Golang() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "golang", "p1", "v1"))
            .isEqualTo("p1 v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Pecoff() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "pecoff", "p1", "v1"))
            .isEqualTo("p1 v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Swift() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "swift", "p1", "v1"))
            .isEqualTo("p1 v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Terraform() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "terraform", "p1", "v1"))
            .isEqualTo(" : p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Cocoapods() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "cocoapods", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Conan() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "conan", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Container() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "container", "p1", "v1"))
            .isEqualTo(" : p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Iac() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "iac", "p1", "v1"))
            .isEqualTo("p1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Cargo() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "cargo", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Cran() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "cran", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Conda() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "conda", "p1", "v1"))
            .isEqualTo("p1/v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Composer() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "composer", "p1", "v1"))
            .isEqualTo("/p1/v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Cpe() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "cpe", "p1", "v1"))
            .isEqualTo(" : p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_Swid() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "swid", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_HfRepo() {
    // Cannot determine the component identifier for format, name and version: hf-repo, p1, v1
    // Throws InvalidComponentIdentifierException: Coordinates contain the following incorrect entries for the given
    // format: [name]
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "hf-repo", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_HfModel() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "hf-model", "p1", "v1"))
            .isEqualTo("p1 : v1 : (Hugging Face Repo)");
  }

  @Test
  public void testCalculateDisplayNameFromFormatNameAndVersion_ForDefaultComponentIdentifierType() {
    // Determines the display name from component identifier created with the format, name and version
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(componentIdentifier, "npm", "p1", "v1"))
            .isEqualTo("p1 : v1");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Maven() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:maven/org.apache.commons/commons-lang3@3.12" +
            ".0?classifier=javadoc&extension=jar", null, null, null))
                .isEqualTo("org.apache.commons : commons-lang3 :  : javadoc : 3.12.0");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Generic() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:generic/example-component@1.0.0?arch=x86&os=linux", null,
            null, null))
                .isEqualTo("example-component : 1.0.0 : x86 : linux");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Nuget() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:nuget/Newtonsoft.Json@13.0.1?repository=https://api.nuget.org/v3/index.json", null, null, null))
                .isEqualTo("Newtonsoft.Json 13.0.1");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_AName() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:aname/example-component@1.2.3#/path/to/submodule", null,
            null, null))
                .isEqualTo("example-component : 1.2.3");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Pypi() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:pypi/requests@2.26.0?environment=python_version%3C3.10",
            null, null, null))
                .isEqualTo("requests 2.26.0");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Rpm() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:rpm/openssl@1.1.1g?repository=base", null,
            null,
            null))
                .isEqualTo("openssl-1.1.1g");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Gem() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:gem/rails@6.1.4?source=https://rubygems.org",
            null,
            null, null))
                .isEqualTo("rails 6.1.4");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Golang() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:golang/github.com/gin-gonic/gin@v1.7.4?arch=amd64",
            null, null, null))
                .isEqualTo("github.com/gin-gonic/gin v1.7.4");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Pecoff() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:pecoff/example-software@1.0.0", null, null, null))
                .isEqualTo("example-software 1.0.0");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Swift() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:swift/apple/swift@5.7.0?platform=macos", null,
            null,
            null))
                .isEqualTo("apple/swift 5.7.0");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Terraform() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:terraform/hashicorp/consul@1.14.0?repository=https://github.com/hashicorp/consul", null, null,
            null))
                .isEqualTo("hashicorp : consul : 1.14.0");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Cocoapods() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:cocoapods/Alamofire@5.4.3?platform=ios", null,
            null,
            null))
                .isEqualTo("Alamofire : 5.4.3");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Conan() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:conan/boost/1.75.0?compiler=gcc", null, null,
            null))
                .isEqualTo("1.75.0 :  : boost");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Container() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:docker/library/ubuntu@20.04?platform=linux",
            null, null,
            null))
                .isEqualTo("ubuntu : library : linux : 20.04");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Iac() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:iac/terraform/hashicorp/consul@1.14.0?arch=x86_64", null,
            null, null))
                .isEqualTo("consul");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Cargo() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:cargo/serde@1.0.130", null, null, null))
            .isEqualTo("serde : 1.0.130");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Cran() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:cran/ggplot2@3.3.5?platform=linux", null, null,
            null))
                .isEqualTo("ggplot2 : 3.3.5");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Conda() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:conda/anaconda/requests@2.25.1?channel=defaults", null,
            null, null))
                .isEqualTo("defaults/requests/2.25.1");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Composer() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:composer/monolog/monolog@2.3.5?arch=x86_64",
            null, null,
            null))
                .isEqualTo("monolog/monolog/2.3.5");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Cpe() {
    // Doesn't identify it as valid purl
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "cpe:2.3:a:microsoft:internet_explorer:11:*:*:*:*:*:*:*", null, "internet_explorer", "11"))
                .isEqualTo("internet_explorer : 11");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_Swid() {
    // Doesn't identify it as valid purl
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:swid:oracle/java@1.8.0_281", null, "java", "1.8.0_281"))
                .isEqualTo("java : 1.8.0_281");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_HfRepo() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:huggingface/models/bert-base-uncased@v1.0.0?platform=cpu", null, null, null))
                .isEqualTo("models/bert-base-uncased : v1.0.0 : (Hugging Face Repo)");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_HfModel() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:huggingface/models/bert-base-uncased@v2.1.0?platform=cpu", null, null, null))
                .isEqualTo("models/bert-base-uncased : v2.1.0 : (Hugging Face Repo)");
  }

  @Test
  public void testCalculateDisplayNameFromPackageUrl_ForDefaultComponentIdentifierType() {
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName("pkg:npm/lodash@4.17.21?platform=browser", null,
            null, null))
                .isEqualTo("lodash : 4.17.21");
  }

  @Test
  public void testCalculateDisplayNameMalformedPackageUrlAndNoFormat() {
    // < symbol must be encoded
    assertThat(
        FileCoordinateDisplayNameGenerator.generateDisplayName(
            "pkg:pypi/requests@2.26.0?environment=python_version<3.10",
            null, "requests", "2.26.0"))
                .isEqualTo("requests : 2.26.0");
  }
}
