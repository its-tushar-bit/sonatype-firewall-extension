/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

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
    // Spring shares the detector singleton across test methods, so clear any cached matchers before
    // creating method-local patterns to keep the class order-independent.
    proprietaryComponentNameDetector.invalidateMatchers();
    repoManager = tempEntity.newRepositoryManager();
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

  /**
   * The dedup matcher used by addPatterns is cached (3-minute TTL), so a pattern inserted directly into the database
   * after the cache is warmed is NOT visible to the cached matcher within the TTL. We exercise this by warming the
   * cache, inserting a pattern out-of-band, then re-adding it: the cached matcher still considers it new, the batch
   * insert silently tolerates the resulting duplicate (ignoreDuplicateKey), and no duplicate row is created.
   */
  @Test
  public void testAddPatterns_dedupMatcherIsCachedAndToleratesConcurrentDuplicate() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    // Warm the dedup matcher cache for this format.
    ProprietaryComponentNamePattern first =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamePattern("alpha*");
    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(first)))
        .isEqualTo(1);
    Mockito.reset(mockTaskScheduler);

    // Simulate another node inserting a pattern directly into the database, invisible to our cached matcher.
    ProprietaryComponentNamePattern fromOtherNode =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamePattern("beta*");
    proprietaryComponentNamePatternDAO.insert(fromOtherNode);

    // Re-adding the same pattern: the stale cached matcher treats it as new, but the batch insert tolerates the
    // duplicate at the database without throwing, and no second row is persisted. The return value reflects
    // DB-confirmed inserts: although this node's cache considered the pattern new, the database silently skipped it
    // as a duplicate, so the count is 0.
    ProprietaryComponentNamePattern duplicate =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamePattern("beta*");
    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(duplicate)))
        .isEqualTo(0);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getNamePattern)
        .containsExactlyInAnyOrder("alpha*", "beta*");
    // Nothing was actually inserted (the DB skipped the duplicate), so other nodes must not be told to invalidate.
    verify(mockTaskScheduler, never()).scheduleOneTimeTaskForAllOtherNodes(proprietaryComponentNameDetector);
  }

  @Test
  public void testAddPatterns_cacheInvalidatedOnRemove() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern pattern =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamePattern("sonatype*");

    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(pattern)))
        .isEqualTo(1);
    // Re-adding the same pattern is a no-op while the cache holds it.
    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM)
            .withNamePattern("sonatype*")))).isEqualTo(0);

    // Removing all patterns for the repo invalidates the dedup matcher cache.
    proprietaryComponentNameDetector.removePatterns(repo.getId());
    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM)).isEmpty();

    // The same pattern is now considered new again and is persisted.
    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM)
            .withNamePattern("sonatype*")))).isEqualTo(1);
    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM)).hasSize(1);
  }

  @Test
  public void testAddPatterns_batchPersistsAllNewPatterns() {
    repo = tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern p1 =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamePattern("one*");
    ProprietaryComponentNamePattern p2 =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamePattern("two*");
    ProprietaryComponentNamePattern p3 =
        new ProprietaryComponentNamePattern(repo.getId(), ComponentIdentifier.FORMAT_NPM).withNamespacePattern("@org");

    assertThat(proprietaryComponentNameDetector.addPatterns(ComponentIdentifier.FORMAT_NPM,
        Arrays.asList(p1, p2, p3))).isEqualTo(3);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM))
        .extracting(ProprietaryComponentNamePattern::getId)
        .containsExactlyInAnyOrder(p1.getId(), p2.getId(), p3.getId());
    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(proprietaryComponentNameDetector);
  }

  /**
   * When the batch insert fails, the cached all-pattern matcher (already mutated with the would-be-new patterns) must
   * be evicted so the next add rebuilds from the database and re-attempts the insert, rather than silently treating
   * the never-persisted patterns as already present.
   */
  @Test
  public void testAddPatterns_evictsCacheAndRethrowsWhenInsertFails() {
    ProprietaryComponentNamePatternDAO dao = mock(ProprietaryComponentNamePatternDAO.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    ProprietaryComponentNameDetector detector = new ProprietaryComponentNameDetector(dao, taskScheduler);

    when(dao.getByFormat(ComponentIdentifier.FORMAT_NPM)).thenReturn(Collections.emptyList());
    RuntimeException insertFailure = new RuntimeException("insert failed");
    doThrow(insertFailure).when(dao).insertBatch(anyList(), anyBoolean());

    ProprietaryComponentNamePattern pattern =
        new ProprietaryComponentNamePattern("repo1", ComponentIdentifier.FORMAT_NPM).withNamePattern("sonatype*");

    // First add: the matcher is built (one getByFormat), the pattern is recorded, the insert throws and is rethrown.
    assertThatThrownBy(
        () -> detector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(pattern))).isSameAs(insertFailure);
    // No cross-node invalidation when nothing persisted.
    verify(taskScheduler, never()).scheduleOneTimeTaskForAllOtherNodes(detector);

    // Second add of the same pattern: because the cache was evicted, the matcher is rebuilt from the database
    // (a second getByFormat) and the insert is attempted again rather than being short-circuited as "already known".
    assertThatThrownBy(() -> detector.addPatterns(ComponentIdentifier.FORMAT_NPM, Arrays.asList(
        new ProprietaryComponentNamePattern("repo1", ComponentIdentifier.FORMAT_NPM).withNamePattern("sonatype*"))))
            .isSameAs(insertFailure);

    verify(dao, times(2)).getByFormat(ComponentIdentifier.FORMAT_NPM);
    verify(dao, times(2)).insertBatch(anyList(), anyBoolean());
    // Neither failed add reaches the cross-node invalidation (the catch rethrows before it), across both attempts.
    verify(taskScheduler, never()).scheduleOneTimeTaskForAllOtherNodes(detector);
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

  /**
   * The dedup cache backing {@code addPatterns} ({@code allPatternMatchersByFormat}) is a {@code TenantReference}, so
   * a pattern added by one tenant must not make that pattern look "already present" to another tenant. This guards the
   * add path with the same tenant-isolation coverage that {@code getMatcher}/{@code invalidateMatchers} already have.
   * Uses a mocked DAO so the test exercises only the in-memory dedup cache, with no cross-tenant repository/FK setup.
   */
  @Test
  public void testAddPatterns_dedupCacheIsTenantAware() {
    String format = ComponentIdentifier.FORMAT_NPM;
    ProprietaryComponentNamePatternDAO dao = mock(ProprietaryComponentNamePatternDAO.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    ProprietaryComponentNameDetector detector = new ProprietaryComponentNameDetector(dao, taskScheduler);
    // Empty DB for both tenants, so whether a pattern is "new" depends solely on the per-tenant dedup cache.
    when(dao.getByFormat(format)).thenReturn(Collections.emptyList());
    // The mocked DAO has no real duplicates, so report every handed-in row as inserted (insertBatch returns the size).
    when(dao.insertBatch(anyList(), anyBoolean()))
        .thenAnswer(invocation -> invocation.getArgument(0, List.class).size());

    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      // Tenant 1 adds the pattern: its cache is empty, so it is counted as new.
      assertThat(detector.addPatterns(format, Arrays.asList(
          new ProprietaryComponentNamePattern("repo1", format).withNamePattern("sonatype*")))).isEqualTo(1);
    });

    testAsNewTenant(testName, t2 -> {
      // Tenant 2 adds the same pattern: tenant 1's cache must not bleed across, so it is still counted as new.
      assertThat(detector.addPatterns(format, Arrays.asList(
          new ProprietaryComponentNamePattern("repo2", format).withNamePattern("sonatype*")))).isEqualTo(1);
    });

    testAsTenant(tenant1, t -> {
      // Re-adding within tenant 1 is a no-op: its own cache still holds the pattern.
      assertThat(detector.addPatterns(format, Arrays.asList(
          new ProprietaryComponentNamePattern("repo1", format).withNamePattern("sonatype*")))).isEqualTo(0);
    });
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
