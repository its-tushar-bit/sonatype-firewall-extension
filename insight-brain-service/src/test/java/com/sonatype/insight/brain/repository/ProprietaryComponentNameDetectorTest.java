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
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
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

  private RepositoryManager repoManager;

  private Repository repo;

  private Map<String, ComponentNameMatcher> matchersByFormat;

  @Before
  public void init() {
    repoManager = tempEntity.newRepositoryManager();
    
    matchersByFormat = new HashMap<>();
  }

  private void assertMatch(ProprietaryComponentName conflict, String namePattern) {
    assertThat(conflict.getProprietaryNamePattern()).isEqualTo(namePattern);
    assertThat(conflict.getRepositoryId()).isEqualTo(repo.getId());
  }

  @Test
  public void testFindProprietaryComponentName() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    tempEntity.newProprietaryComponentNamePattern(repo, null, "sonatype*");
    tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);

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
    repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    tempEntity.newProprietaryComponentNamePattern(repo, "org.sonatype", null);
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(matchersByFormat,
        ComponentIdentifier.createMavenCoordinates("org.sonatype", "test", "1.0", "", "jar")), "org.sonatype/*");
  }

  @Test
  public void testFindProprietaryComponentName_Namespacing_Npm() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(matchersByFormat,
        ComponentIdentifier.createNpmCoordinates("@sonatype/test", "1.0")), "@sonatype/*");
  }

  @Test
  public void testFindProprietaryComponentName_Namespacing_Nuget() {
    repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NUGET);
    tempEntity.newProprietaryComponentNamePattern(repo, null, "Sonatype.*");
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(matchersByFormat,
        ComponentIdentifier.createNugetCoordinates("Sonatype.Type", "1.0")), "sonatype.*");
  }

  @Test
  public void testFindProprietaryComponentName_NullIdentifier() {
    assertThat(proprietaryComponentNameDetector.findProprietaryComponentName(matchersByFormat, null)).isNull();
  }

  @Test
  public void testAddPatterns() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern pattern1 =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamePattern("sonatype*");
    ProprietaryComponentNamePattern pattern2 =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM)
            .withNamespacePattern("@sonatype");
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
            new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM)
                .withNamePattern("sonatype*")))).isEqualTo(0);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern1.getId(), pattern2.getId());
  }

  @Test
  public void testAddPatterns_DoesNotChangePatternEnabledStatus() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern pattern =
        tempEntity.newProprietaryComponentNamePattern(repo, null /* namespacePattern */, "testNamePattern");
    assertThat(pattern.isEnabled()).isTrue();

    // Enabled pattern
    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(pattern)))
        .isEqualTo(0);
    pattern = proprietaryComponentNamePatternDAO.getById(pattern.getId());
    assertThat(pattern.isEnabled()).isTrue();

    // Disabled pattern
    pattern.setEnabled(false);
    proprietaryComponentNamePatternDAO.update(pattern);
    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(pattern)))
        .isEqualTo(0);
    pattern = proprietaryComponentNamePatternDAO.getById(pattern.getId());
    assertThat(pattern.isEnabled()).isFalse();

    // Back to enabled pattern
    pattern.setEnabled(true);
    proprietaryComponentNamePatternDAO.update(pattern);
    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(pattern)))
        .isEqualTo(0);
    pattern = proprietaryComponentNamePatternDAO.getById(pattern.getId());
    assertThat(pattern.isEnabled()).isTrue();
  }

  @Test
  public void testRemovePatterns_ForSpecificRepo() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    tempEntity.newProprietaryComponentNamePattern(repo, null, "sonatype*");

    proprietaryComponentNameDetector.removePatterns(repo.getId());

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM)).isEmpty();
  }
}
