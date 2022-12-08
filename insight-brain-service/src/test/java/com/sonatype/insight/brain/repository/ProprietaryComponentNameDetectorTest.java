/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProprietaryComponentNameDetectorTest
    extends AbstractComponentTest
{
  @Inject
  private ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  @Inject
  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private String repoManId;

  private String repoId = "hosted-repo-with-proprietary-components";

  private Map<String, ComponentNameMatcher> matchersByFormat;

  @Before
  public void init() {
    repoManId = tempEntity.newRepositoryManager().getInstanceId();
    matchersByFormat = new HashMap<>();
  }

  private void assertMatch(ProprietaryComponentName conflict, String namePattern) {
    assertThat(conflict.getProprietaryNamePattern()).isEqualTo(namePattern);
    assertThat(conflict.getRepositoryManagerInstanceId()).isEqualTo(repoManId);
    assertThat(conflict.getRepositoryPublicId()).isEqualTo(repoId);
  }

  @Test
  public void testFindProprietaryComponentName() {
    proprietaryComponentNamePatternDAO.insert(new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM)
        .withNamePattern("sonatype*").withRepository(repoManId, repoId));
    proprietaryComponentNamePatternDAO.insert(new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM)
        .withNamespacePattern("@sonatype").withRepository(repoManId, repoId));

    assertMatch(proprietaryComponentNameDetector
        .findProprietaryComponentName(matchersByFormat,
            ComponentIdentifier.createNpmCoordinates("sonatype-cli", "999")), "sonatype*");
    assertMatch(proprietaryComponentNameDetector
        .findProprietaryComponentName(matchersByFormat,
            ComponentIdentifier.createNpmCoordinates("@sonatype/cli", "999")), "@sonatype/*");
    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(matchersByFormat,
            ComponentIdentifier.createNpmCoordinates("NOTsonatype-cli", "99"))).isNull();
    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(matchersByFormat,
            ComponentIdentifier.createNpmCoordinates("@NOTsonatype/cli", "99"))).isNull();
  }

  @Test
  public void testFindProprietaryComponentName_Namespacing_Maven() {
    proprietaryComponentNamePatternDAO.insert(new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_MAVEN)
        .withNamespacePattern("org.sonatype").withRepository(repoManId, repoId));
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(matchersByFormat,
        ComponentIdentifier.createMavenCoordinates("org.sonatype", "test", "1.0", "", "jar")), "org.sonatype/*");
  }

  @Test
  public void testFindProprietaryComponentName_Namespacing_Npm() {
    proprietaryComponentNamePatternDAO.insert(new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM)
        .withNamespacePattern("@sonatype").withRepository(repoManId, repoId));
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(matchersByFormat,
        ComponentIdentifier.createNpmCoordinates("@sonatype/test", "1.0")), "@sonatype/*");
  }

  @Test
  public void testFindProprietaryComponentName_Namespacing_Nuget() {
    proprietaryComponentNamePatternDAO.insert(new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NUGET)
        .withNamePattern("Sonatype.*").withRepository(repoManId, repoId));
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(matchersByFormat,
        ComponentIdentifier.createNugetCoordinates("Sonatype.Type", "1.0")), "sonatype.*");
  }

  @Test
  public void testFindProprietaryComponentName_NullIdentifier() {
    assertThat(proprietaryComponentNameDetector.findProprietaryComponentName(matchersByFormat, null)).isNull();
  }

  @Test
  public void testAddPatterns() {
    ProprietaryComponentNamePattern pattern1 = new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM)
        .withNamePattern("sonatype*").withRepository(repoManId, repoId);
    ProprietaryComponentNamePattern pattern2 = new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM)
        .withNamespacePattern("@sonatype").withRepository(repoManId, repoId);
    assertThat(
        proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(pattern1, pattern2)))
        .isEqualTo(2);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern1.getId(), pattern2.getId());

    assertMatch(proprietaryComponentNameDetector
        .findProprietaryComponentName(matchersByFormat,
            ComponentIdentifier.createNpmCoordinates("sonatype-cli", "999")), "sonatype*");
    assertMatch(proprietaryComponentNameDetector
        .findProprietaryComponentName(matchersByFormat,
            ComponentIdentifier.createNpmCoordinates("@sonatype/cli", "999")), "@sonatype/*");
    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(matchersByFormat,
            ComponentIdentifier.createNpmCoordinates("NOTsonatype-cli", "99"))).isNull();
    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(matchersByFormat,
            ComponentIdentifier.createNpmCoordinates("@NOTsonatype/cli", "99"))).isNull();

    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM,
        Collections.singletonList(
            new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM).withNamePattern("sonatype*")
                .withRepository(repoManId, repoId)))).isEqualTo(0);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern1.getId(), pattern2.getId());
  }

  @Test
  public void testRemovePatterns_ForSpecificRepo() {
    ProprietaryComponentNamePattern pattern = new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM)
        .withNamePattern("sonatype*").withRepository(repoManId, repoId);
    proprietaryComponentNamePatternDAO.insert(pattern);

    proprietaryComponentNameDetector.removePatterns(repoManId, repoId);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM)).isEmpty();
  }

  @Test
  public void testRemovePatterns_ForEntireRepoManager() {
    ProprietaryComponentNamePattern pattern1 = new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM)
        .withNamePattern("sonatype*").withRepository(repoManId, repoId);
    proprietaryComponentNamePatternDAO.insert(pattern1);
    ProprietaryComponentNamePattern pattern2 = new ProprietaryComponentNamePattern(ComponentIdentifier.FORMAT_NPM)
        .withNamespacePattern("@sonatype").withRepository(repoManId, repoId + "-other");
    proprietaryComponentNamePatternDAO.insert(pattern2);

    proprietaryComponentNameDetector.removePatterns(repoManId, "*");

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM)).isEmpty();
  }
}
