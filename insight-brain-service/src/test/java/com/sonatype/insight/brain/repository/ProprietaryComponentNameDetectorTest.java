/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import com.google.inject.Binder;
import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ProprietaryComponentNameDetectorTest
    extends AbstractComponentTest
{
  @Inject
  private ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  @Inject
  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private RepositoryManager repoManager;

  private Repository repo;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Before
  public void init() {
    repoManager = tempEntity.newRepositoryManager();
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
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
        .findProprietaryComponentName(ComponentIdentifier.createNpmCoordinates("sonatype-cli", "999")), "sonatype*");
    assertMatch(proprietaryComponentNameDetector
        .findProprietaryComponentName(ComponentIdentifier.createNpmCoordinates("@sonatype/cli", "999")), "@sonatype/*");
    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(ComponentIdentifier.createNpmCoordinates("NOTsonatype-cli", "99"))).isNull();
    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(ComponentIdentifier.createNpmCoordinates("@NOTsonatype/cli", "99"))).isNull();
  }

  @Test
  public void testFindProprietaryComponentName_Namespacing_Maven() {
    repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    tempEntity.newProprietaryComponentNamePattern(repo, "org.sonatype", null);
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(
        ComponentIdentifier.createMavenCoordinates("org.sonatype", "test", "1.0", "", "jar")), "org.sonatype/*");
  }

  @Test
  public void testFindProprietaryComponentName_Namespacing_Npm() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    tempEntity.newProprietaryComponentNamePattern(repo, "@sonatype", null);
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(
        ComponentIdentifier.createNpmCoordinates("@sonatype/test", "1.0")), "@sonatype/*");
  }

  @Test
  public void testFindProprietaryComponentName_Namespacing_Nuget() {
    repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NUGET);
    tempEntity.newProprietaryComponentNamePattern(repo, null, "Sonatype.*");
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(
        ComponentIdentifier.createNugetCoordinates("Sonatype.Type", "1.0")), "sonatype.*");
  }

  @Test
  public void testFindProprietaryComponentName_NullIdentifier() {
    assertThat(proprietaryComponentNameDetector.findProprietaryComponentName(null)).isNull();
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

    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(proprietaryComponentNameDetector);
    Mockito.reset(mockTaskScheduler);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern1.getId(), pattern2.getId());

    assertMatch(proprietaryComponentNameDetector
        .findProprietaryComponentName(
            ComponentIdentifier.createNpmCoordinates("sonatype-cli", "999")),
        "sonatype*");
    assertMatch(proprietaryComponentNameDetector
        .findProprietaryComponentName(
            ComponentIdentifier.createNpmCoordinates("@sonatype/cli", "999")),
        "@sonatype/*");
    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(
            ComponentIdentifier.createNpmCoordinates("NOTsonatype-cli", "99"))).isNull();
    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(
            ComponentIdentifier.createNpmCoordinates("@NOTsonatype/cli", "99"))).isNull();

    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM,
        Collections.singletonList(
            new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM)
                .withNamePattern("sonatype*")))).isEqualTo(0);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(pattern1.getId(), pattern2.getId());

    verify(mockTaskScheduler, never()).scheduleOneTimeTaskForAllOtherNodes(proprietaryComponentNameDetector);
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
    assertMatch(proprietaryComponentNameDetector
        .findProprietaryComponentName(ComponentIdentifier.createNpmCoordinates("sonatype-cli", "999")), "sonatype*");

    proprietaryComponentNameDetector.removePatterns(repo.getId());

    assertThat(proprietaryComponentNameDetector
        .findProprietaryComponentName(ComponentIdentifier.createNpmCoordinates("sonatype-cli", "999"))).isNull();
    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM)).isEmpty();
    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(proprietaryComponentNameDetector);
  }

  @Test
  public void testExecute() throws Exception {
    ProprietaryComponentNameDetector spy = spy(proprietaryComponentNameDetector);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spy).invalidateMatchers();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spy.execute(mock(JobExecutionContext.class));
    }

    verify(spy).invalidateMatchers();
  }

  @Test
  public void testInvalidateMatchers() {
    repo =
        tempEntity.newRepository(repoManager, "testPublicId1", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern proprietaryComponentNamePattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo, null, "sonatype*");
    Repository repo2 =
        tempEntity.newRepository(repoManager, "testPublicId2", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern proprietaryComponentNamePattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo2, "@sonatype", null);
    assertMatch(proprietaryComponentNameDetector.findProprietaryComponentName(
        ComponentIdentifier.createNpmCoordinates("sonatype-cli", "999")), "sonatype*");
    proprietaryComponentNamePatternDAO.delete(proprietaryComponentNamePattern1);
    proprietaryComponentNamePatternDAO.delete(proprietaryComponentNamePattern2);

    proprietaryComponentNameDetector.invalidateMatchers();

    assertThat(proprietaryComponentNameDetector.findProprietaryComponentName(
        ComponentIdentifier.createNpmCoordinates("sonatype-cli", "999"))).isNull();
  }

  @Test
  public void testGetMatcher_isTenantAware() {
    String format = "maven";
    AtomicReference<ComponentNameMatcher> matcher1 = new AtomicReference<>();
    AtomicReference<ComponentNameMatcher> matcher2 = new AtomicReference<>();

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      // a maven matcher is created for tenant 1 since none exists
      matcher1.set(proprietaryComponentNameDetector.getMatcher(format));
    });

    Tenant tenant2 = testAsNewTenant(testName, t2 -> {
      // a maven matcher is created for tenant 2 since none exists
      matcher2.set(proprietaryComponentNameDetector.getMatcher(format));
    });

    testAsTenant(tenant1, t -> {
      // Ensure the original matcher created for tenant 1 is returned
      assertThat(proprietaryComponentNameDetector.getMatcher(format)).isEqualTo(matcher1.get());
    });

    testAsTenant(tenant2, t -> {
      // Ensure the original matcher created for tenant 2 is returned
      assertThat(proprietaryComponentNameDetector.getMatcher(format)).isEqualTo(matcher2.get());
    });

    assertThat(matcher1.get()).isNotEqualTo(matcher2.get());
  }

  @Test
  public void testInvalidateMatchers_isTenantAware() {
    String format = "maven";
    AtomicReference<ComponentNameMatcher> matcher = new AtomicReference<>();

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      // a maven matcher is created for tenant 1 since none exists
      matcher.set(proprietaryComponentNameDetector.getMatcher(format));
    });

    testAsNewTenant(testName, t2 -> {
      // clean up all matchers for tenant 2
      proprietaryComponentNameDetector.invalidateMatchers();
    });

    testAsTenant(tenant1, t -> {
      // Ensure the original matcher created for tenant 1 is still returned
      assertThat(proprietaryComponentNameDetector.getMatcher(format)).isEqualTo(matcher.get());
    });
  }
}
