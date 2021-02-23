/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Arrays;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentNameMatcherTest
{
  private ProprietaryComponentNamePattern newNamePattern(String format, String namePattern) {
    ProprietaryComponentNamePattern pattern = new ProprietaryComponentNamePattern();
    pattern.setFormat(format);
    pattern.setNamePattern(namePattern);
    return pattern;
  }

  private ProprietaryComponentNamePattern newNamespacePattern(String format, String namespacePattern) {
    ProprietaryComponentNamePattern pattern = new ProprietaryComponentNamePattern();
    pattern.setFormat(format);
    pattern.setNamespacePattern(namespacePattern);
    return pattern;
  }

  @Test
  public void testFindMatch_NoCoordinates() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM, Arrays.asList());
    assertThat(matcher.findMatch(null, null)).isNull();
  }

  @Test
  public void testFindMatch_Namespace_ExactMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(newNamespacePattern(ComponentIdentifier.FORMAT_NPM, "@sonatype")));
    assertThat(matcher.findMatch("@sonatype", "cli")).isEqualTo("@sonatype/*");
    assertThat(matcher.findMatch("@sonatypeNOThere", "cli")).isNull();
  }

  @Test
  public void testFindMatch_Namespace_PrefixMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(newNamespacePattern(ComponentIdentifier.FORMAT_NPM, "@sonatype*")));
    assertThat(matcher.findMatch("@sonatype", "cli")).isEqualTo("@sonatype*/*");
    assertThat(matcher.findMatch("@sonatypeTOO", "cli")).isEqualTo("@sonatype*/*");
    assertThat(matcher.findMatch("@sonatyp", "cli")).isNull();
  }

  @Test
  public void testFindMatch_Namespace_PrefixMatch_ShortestReason() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_MAVEN,
        Arrays.asList(newNamespacePattern(ComponentIdentifier.FORMAT_MAVEN, "org.sonatype.sub.*"),
            newNamespacePattern(ComponentIdentifier.FORMAT_MAVEN, "org.sonatype.*"),
            newNamespacePattern(ComponentIdentifier.FORMAT_MAVEN, "org.sonatype.sub.id.*")));
    assertThat(matcher.findMatch("org.sonatype.sub.id", "cli")).isEqualTo("org.sonatype.*/*");
  }

  @Test
  public void testFindMatch_Name_ExactMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(newNamePattern(ComponentIdentifier.FORMAT_NPM, "cli")));
    assertThat(matcher.findMatch(null, "cli")).isEqualTo("cli");
    assertThat(matcher.findMatch("@scope", "cli")).isNull();
  }

  @Test
  public void testFindMatch_Name_PrefixMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(newNamePattern(ComponentIdentifier.FORMAT_NPM, "sonatype-*")));
    assertThat(matcher.findMatch(null, "sonatype-cli")).isEqualTo("sonatype-*");
    assertThat(matcher.findMatch(null, "sonademo-cli")).isNull();
    assertThat(matcher.findMatch(null, "sonatype")).isNull();
    assertThat(matcher.findMatch("@scope", "sonatype-cli")).isNull();
  }

  @Test
  public void testFindMatch_Name_SuffixMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(newNamePattern(ComponentIdentifier.FORMAT_NPM, "*-sonatype")));
    assertThat(matcher.findMatch(null, "cli-sonatype")).isEqualTo("*-sonatype");
    assertThat(matcher.findMatch(null, "cli-demotype")).isNull();
    assertThat(matcher.findMatch(null, "sonatype")).isNull();
    assertThat(matcher.findMatch("@scope", "cli-sonatype")).isNull();
  }

  @Test
  public void testFindMatch_Maven_AutoPrefixMatchForGroupId() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_MAVEN,
        Arrays.asList(newNamespacePattern(ComponentIdentifier.FORMAT_MAVEN, "org.sonatype")));
    assertThat(matcher.findMatch("org.sonatype", "cli")).isEqualTo("org.sonatype/*");
    assertThat(matcher.findMatch("org.sonatype.sub.id", "cli")).isEqualTo("org.sonatype.*/*");
    assertThat(matcher.findMatch("org.sonatypeNOThere", "cli")).isNull();
  }

  @Test
  public void testFindMatch_Pypi_CaseInsensitive() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_PYPI,
        Arrays.asList(newNamePattern(ComponentIdentifier.FORMAT_PYPI, "Cli")));
    assertThat(matcher.findMatch(null, "cli")).isEqualTo("cli");
    assertThat(matcher.findMatch(null, "CLI")).isEqualTo("cli");
  }

  @Test
  public void testFindMatch_Nuget_CaseInsensitive() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NUGET,
        Arrays.asList(newNamePattern(ComponentIdentifier.FORMAT_NUGET, "Cli")));
    assertThat(matcher.findMatch(null, "cli")).isEqualTo("cli");
    assertThat(matcher.findMatch(null, "CLI")).isEqualTo("cli");
  }

  @Test
  public void testFindMatch_Pypi_Normalization() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_PYPI,
        Arrays.asList(newNamePattern(ComponentIdentifier.FORMAT_PYPI, "Sonatype-Cli")));
    assertThat(matcher.findMatch(null, "sonatype-cli")).isEqualTo("sonatype-cli");
    assertThat(matcher.findMatch(null, "sonatype-CLI")).isEqualTo("sonatype-cli");
    assertThat(matcher.findMatch(null, "sonatype.CLI")).isEqualTo("sonatype-cli");
    assertThat(matcher.findMatch(null, "sonatype_CLI")).isEqualTo("sonatype-cli");
    assertThat(matcher.findMatch(null, "sonatype..--__CLI")).isEqualTo("sonatype-cli");
  }

  @Test
  public void testAdd() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(newNamePattern(ComponentIdentifier.FORMAT_NPM, "sonatype*")));

    ProprietaryComponentNamePattern pattern1 = newNamePattern(ComponentIdentifier.FORMAT_NPM, "sonatype*");
    ProprietaryComponentNamePattern pattern2 = newNamePattern(ComponentIdentifier.FORMAT_NPM, "sonatype");
    ProprietaryComponentNamePattern pattern3 = newNamespacePattern(ComponentIdentifier.FORMAT_NPM, "@sonatype");
    assertThat(matcher.add(Arrays.asList(pattern1, pattern2, pattern3))).containsExactlyInAnyOrder(pattern2, pattern3);
    assertThat(matcher.add(Arrays.asList(pattern1, pattern2, pattern3))).isEmpty();

    ProprietaryComponentNamePattern pattern4 = newNamespacePattern(pattern3.getFormat(), pattern3.getNamespacePattern())
        .withRepository("repo-man-id", "repo-id");
    assertThat(matcher.add(Arrays.asList(pattern4))).containsExactlyInAnyOrder(pattern4);
    assertThat(matcher.add(Arrays.asList(pattern4))).isEmpty();
  }
}
