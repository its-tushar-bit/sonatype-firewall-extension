/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class McpPurlCompleterTest
{
  @Test
  public void complete_bareMavenPurl_addsTypeJar() {
    String result = McpPurlCompleter.complete("pkg:maven/org.springframework.boot/spring-boot@3.1.3");
    assertThat(result).isEqualTo("pkg:maven/org.springframework.boot/spring-boot@3.1.3?type=jar");
  }

  @Test
  public void complete_mavenWithExplicitTypeJar_isUnchanged() {
    String purl = "pkg:maven/org.springframework.boot/spring-boot@3.1.3?type=jar";
    assertThat(McpPurlCompleter.complete(purl)).isEqualTo(purl);
  }

  @Test
  public void complete_mavenWithExplicitTypePom_isUnchanged() {
    String purl = "pkg:maven/org.springframework.boot/spring-boot@3.1.3?type=pom";
    assertThat(McpPurlCompleter.complete(purl)).isEqualTo(purl);
  }

  @Test
  public void complete_barePypiPurl_addsExtensionTarGz() {
    String result = McpPurlCompleter.complete("pkg:pypi/requests@2.28.0");
    assertThat(result).contains("extension=tar.gz");
  }

  @Test
  public void complete_pypiWithExtension_isUnchanged() {
    String purl = "pkg:pypi/requests@2.28.0?extension=whl";
    assertThat(McpPurlCompleter.complete(purl)).isEqualTo(purl);
  }

  @Test
  public void complete_bareRubyGemsPurl_addsPlatformRuby() {
    String result = McpPurlCompleter.complete("pkg:gem/rails@7.0.0");
    assertThat(result).contains("platform=ruby");
  }

  @Test
  public void complete_rubyGemsWithExplicitPlatform_isUnchanged() {
    String purl = "pkg:gem/rails@7.0.0?platform=java";
    assertThat(McpPurlCompleter.complete(purl)).isEqualTo(purl);
  }

  @Test
  public void complete_mavenWithBlankTypeQualifier_appliesDefault() {
    String result = McpPurlCompleter.complete("pkg:maven/org.foo/bar@1.0?type=");
    assertThat(result).contains("type=jar");
  }

  @Test
  public void complete_npmPurl_isUnchanged() {
    String purl = "pkg:npm/react@18.2.0";
    assertThat(McpPurlCompleter.complete(purl)).isEqualTo(purl);
  }

  @Test
  public void complete_nullInput_returnsNull() {
    assertThat(McpPurlCompleter.complete(null)).isNull();
  }

  @Test
  public void complete_blankInput_isUnchanged() {
    assertThat(McpPurlCompleter.complete("")).isEqualTo("");
    assertThat(McpPurlCompleter.complete("   ")).isEqualTo("   ");
  }

  @Test
  public void complete_malformedPurl_returnsOriginal() {
    String garbage = "not-a-purl";
    assertThat(McpPurlCompleter.complete(garbage)).isEqualTo(garbage);
  }
}
