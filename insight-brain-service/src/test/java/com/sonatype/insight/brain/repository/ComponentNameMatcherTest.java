/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentNameMatcherTest
{
  private static final String REPOSITORY_ID = "repoId";

  private ProprietaryComponentNamePattern newNamePattern(String format, String namePattern) {
    ProprietaryComponentNamePattern pattern = new ProprietaryComponentNamePattern();
    pattern.setFormat(format);
    pattern.setNamePattern(namePattern);
    pattern.setRepositoryId(REPOSITORY_ID);
    return pattern;
  }

  private ProprietaryComponentNamePattern newNamespacePattern(String format, String namespacePattern) {
    ProprietaryComponentNamePattern pattern = new ProprietaryComponentNamePattern();
    pattern.setFormat(format);
    pattern.setNamespacePattern(namespacePattern);
    pattern.setRepositoryId(REPOSITORY_ID);
    return pattern;
  }

  private void assertMatch(ProprietaryComponentName conflict, String namePattern) {
    assertThat(conflict.getProprietaryNamePattern()).isEqualTo(namePattern);
    assertThat(conflict.getRepositoryId()).isEqualTo(REPOSITORY_ID);
  }

  @Test
  public void testFindMatch_NoCoordinates() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM, Collections.emptyList());
    assertThat(matcher.findMatch(null, null)).isNull();
  }

  @Test
  public void testFindMatch_Namespace_ExactMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Collections.singletonList(newNamespacePattern(ComponentIdentifier.FORMAT_NPM, "@sonatype")));
    assertMatch(matcher.findMatch("@sonatype", "cli"), "@sonatype/*");
    assertThat(matcher.findMatch("@sonatypeNOThere", "cli")).isNull();
  }

  @Test
  public void testFindMatch_Namespace_PrefixMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Collections.singletonList(newNamespacePattern(ComponentIdentifier.FORMAT_NPM, "@sonatype*")));
    assertMatch(matcher.findMatch("@sonatype", "cli"), "@sonatype*/*");
    assertMatch(matcher.findMatch("@sonatypeTOO", "cli"), "@sonatype*/*");
    assertThat(matcher.findMatch("@sonatyp", "cli")).isNull();
  }

  @Test
  public void testFindMatch_Namespace_PrefixMatch_ShortestReason() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_MAVEN,
        Arrays.asList(newNamespacePattern(ComponentIdentifier.FORMAT_MAVEN, "org.sonatype.sub.*"),
            newNamespacePattern(ComponentIdentifier.FORMAT_MAVEN, "org.sonatype.*"),
            newNamespacePattern(ComponentIdentifier.FORMAT_MAVEN, "org.sonatype.sub.id.*")));
    assertMatch(matcher.findMatch("org.sonatype.sub.id", "cli"), "org.sonatype.*/*");
  }

  @Test
  public void testFindMatch_Name_ExactMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Collections.singletonList(newNamePattern(ComponentIdentifier.FORMAT_NPM, "cli")));
    assertMatch(matcher.findMatch(null, "cli"), "cli");
    assertThat(matcher.findMatch("@scope", "cli")).isNull();
  }

  @Test
  public void testFindMatch_Name_PrefixMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Collections.singletonList(newNamePattern(ComponentIdentifier.FORMAT_NPM, "sonatype-*")));
    assertMatch(matcher.findMatch(null, "sonatype-cli"), "sonatype-*");
    assertThat(matcher.findMatch(null, "sonademo-cli")).isNull();
    assertThat(matcher.findMatch(null, "sonatype")).isNull();
    assertThat(matcher.findMatch("@scope", "sonatype-cli")).isNull();
  }

  @Test
  public void testFindMatch_Name_SuffixMatch() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Collections.singletonList(newNamePattern(ComponentIdentifier.FORMAT_NPM, "*-sonatype")));
    assertMatch(matcher.findMatch(null, "cli-sonatype"), "*-sonatype");
    assertThat(matcher.findMatch(null, "cli-demotype")).isNull();
    assertThat(matcher.findMatch(null, "sonatype")).isNull();
    assertThat(matcher.findMatch("@scope", "cli-sonatype")).isNull();
  }

  @Test
  public void testFindMatch_Maven_AutoPrefixMatchForGroupId() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_MAVEN,
        Collections.singletonList(newNamespacePattern(ComponentIdentifier.FORMAT_MAVEN, "org.sonatype")));
    assertMatch(matcher.findMatch("org.sonatype", "cli"), "org.sonatype/*");
    assertMatch(matcher.findMatch("org.sonatype.sub.id", "cli"), "org.sonatype.*/*");
    assertThat(matcher.findMatch("org.sonatypeNOThere", "cli")).isNull();
  }

  @Test
  public void testFindMatch_Pypi_CaseInsensitive() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_PYPI,
        Collections.singletonList(newNamePattern(ComponentIdentifier.FORMAT_PYPI, "Cli")));
    assertMatch(matcher.findMatch(null, "cli"), "cli");
    assertMatch(matcher.findMatch(null, "CLI"), "cli");
  }

  @Test
  public void testFindMatch_Nuget_CaseInsensitive() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NUGET,
        Collections.singletonList(newNamePattern(ComponentIdentifier.FORMAT_NUGET, "Cli")));
    assertMatch(matcher.findMatch(null, "cli"), "cli");
    assertMatch(matcher.findMatch(null, "CLI"), "cli");
  }

  @Test
  public void testFindMatch_Pypi_Normalization() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_PYPI,
        Collections.singletonList(newNamePattern(ComponentIdentifier.FORMAT_PYPI, "Sonatype-Cli")));
    assertMatch(matcher.findMatch(null, "sonatype-cli"), "sonatype-cli");
    assertMatch(matcher.findMatch(null, "sonatype-CLI"), "sonatype-cli");
    assertMatch(matcher.findMatch(null, "sonatype.CLI"), "sonatype-cli");
    assertMatch(matcher.findMatch(null, "sonatype_CLI"), "sonatype-cli");
    assertMatch(matcher.findMatch(null, "sonatype..--__CLI"), "sonatype-cli");
  }

  @Test
  public void testAdd() {
    ComponentNameMatcher matcher = new ComponentNameMatcher(ComponentIdentifier.FORMAT_NPM,
        Collections.singletonList(newNamePattern(ComponentIdentifier.FORMAT_NPM, "sonatype*")));

    ProprietaryComponentNamePattern pattern1 = newNamePattern(ComponentIdentifier.FORMAT_NPM, "sonatype*");
    ProprietaryComponentNamePattern pattern2 = newNamePattern(ComponentIdentifier.FORMAT_NPM, "sonatype");
    ProprietaryComponentNamePattern pattern3 = newNamespacePattern(ComponentIdentifier.FORMAT_NPM, "@sonatype");
    assertThat(matcher.add(Arrays.asList(pattern1, pattern2, pattern3))).containsExactlyInAnyOrder(pattern2, pattern3);
    assertThat(matcher.add(Arrays.asList(pattern1, pattern2, pattern3))).isEmpty();

    ProprietaryComponentNamePattern pattern4 =
        newNamespacePattern(pattern3.getFormat(), pattern3.getNamespacePattern());
    pattern4.setRepositoryId("repo-id");
    assertThat(matcher.add(Collections.singletonList(pattern4))).containsExactlyInAnyOrder(pattern4);
    assertThat(matcher.add(Collections.singletonList(pattern4))).isEmpty();
  }
}
