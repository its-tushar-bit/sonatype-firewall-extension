/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.ConflictException;

import jakarta.ws.rs.BadRequestException;
import jakarta.inject.Inject;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Dashboard metrics service: Lucene integration and coalescing cache (CLM-40927).
 */
public class DashboardMetricsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private DashboardMetricsService dashboardMetricsService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Before
  public void setUpClient() {
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(dashboardMetricsService);
  }

  @Test
  public void testGetMetrics_ApplicationsCountFromIndex() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());

    User reader = tempEntity.newUser("metrics-org-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(org.getId(), readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(request);

    assertThat(metrics.applications.total).isEqualTo(3);
    assertThat(metrics.applications.source).isEqualTo("index");
    assertThat(metrics.applications.breakdown).isNull();
    assertThat(metrics.lastUpdatedAt).isNotNull();
  }

  @Test
  public void testGetMetrics_FailsClosed_UserWithNoReadContexts() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());

    User userWithNoPermissions = tempEntity.newUser("user-with-no-permissions");

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(userWithNoPermissions);

    DashboardMetricsDTO metrics = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());

    assertThat(metrics.applications.total).isZero();
    assertThat(metrics.applications.source).isEqualTo("index");
  }

  @Test
  public void testGetMetrics_NoIndex_ThrowsConflictException() throws Exception {
    DashboardMetricsTestSupport.runWithoutSearchIndex(
        lookup(InsightWork.class).getSearchIndexDir(),
        () -> assertThatExceptionOfType(ConflictException.class)
            .isThrownBy(() -> dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO()))
            .withMessageContaining("Search index not found")
            .withMessageContaining("Re-indexing is required")
            .satisfies(ex -> {
              assertThat(ex.getMessage()).doesNotContain("Exception");
              assertThat(ex.getMessage()).doesNotContain("query");
              assertThat(ex.getMessage()).doesNotContain("stack");
              assertThat(ex.getMessage()).doesNotContain("at com.");
            }));
  }

  @Test
  public void testGetMetrics_CoalescingCacheWithinTtl() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("cache-test-user", "cache-test-user", User.INTERNAL_REALM_ID));
    when(searchIndexClient.count(anyString())).thenReturn(7L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(99_000L);

    DashboardMetricsService service =
        new DashboardMetricsService(searchIndexClient, new MetricFilterValidator(), currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    DashboardMetricsDTO first = service.getMetrics(request);
    DashboardMetricsDTO second = service.getMetrics(request);

    assertThat(first.applications.total).isEqualTo(7);
    assertThat(second.applications.total).isEqualTo(7);
    verify(searchIndexClient, times(1)).count(anyString());
    verify(searchIndexClient, times(1)).getLastIndexTime();
  }

  @Test
  public void testGetMetrics_CacheKeyDifferentiatesNullRealmFromExplicitRealm() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(3L, 11L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    UserPrincipal nullRealmPrincipal = new UserPrincipal("shared-user", "shared-user", null);
    UserPrincipal explicitRealmPrincipal =
        new UserPrincipal("shared-user", "shared-user", User.INTERNAL_REALM_ID);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal())
        .thenReturn(nullRealmPrincipal, explicitRealmPrincipal, nullRealmPrincipal, explicitRealmPrincipal);

    DashboardMetricsService service =
        new DashboardMetricsService(searchIndexClient, new MetricFilterValidator(), currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    assertThat(service.getMetrics(request).applications.total).isEqualTo(3);
    assertThat(service.getMetrics(request).applications.total).isEqualTo(11);

    service.getMetrics(request);
    service.getMetrics(request);

    verify(searchIndexClient, times(2)).count(anyString());
  }

  @Test
  public void testGetMetrics_CacheKeyDifferentiatesSameUsernameDifferentRealm() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(5L, 9L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    // A single Singleton service must isolate cache entries between two principals that share a
    // username but differ in realm. Use ONE service instance (so the same TenantReference cache is
    // consulted) and swap the principal returned by the same CurrentUser mock between calls — the
    // cache key is built from getUserPrincipal() once per getMetrics() call.
    UserPrincipal internalPrincipal =
        new UserPrincipal("shared-user", "shared-user", User.INTERNAL_REALM_ID);
    UserPrincipal ldapPrincipal = new UserPrincipal("shared-user", "shared-user", "ldap-realm-id");

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal())
        .thenReturn(internalPrincipal, ldapPrincipal, internalPrincipal, ldapPrincipal);

    DashboardMetricsService service =
        new DashboardMetricsService(searchIndexClient, new MetricFilterValidator(), currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    // Distinct realms must miss each other's cache entry → two separate index counts.
    assertThat(service.getMetrics(request).applications.total).isEqualTo(5);
    assertThat(service.getMetrics(request).applications.total).isEqualTo(9);

    // Repeating each identity must hit the existing cache entry → no further index counts.
    service.getMetrics(request);
    service.getMetrics(request);

    verify(searchIndexClient, times(2)).count(anyString());
  }

  @Test
  public void testGetMetrics_UnsupportedFiltersRejected() {
    Organization org = tempEntity.newOrganization();

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of(org.getId());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dashboardMetricsService.getMetrics(request))
        .withMessage("Request filters are not supported yet.");
  }

  @Test
  public void testGetMetrics_CacheCoalescesAcrossEmptyFilterRequests() {
    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));
    when(searchIndexClient.count(anyString())).thenReturn(2L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    DashboardMetricsService service =
        new DashboardMetricsService(searchIndexClient, new MetricFilterValidator(), currentUser);

    DashboardMetricsRequestDTO requestA = new DashboardMetricsRequestDTO();
    DashboardMetricsRequestDTO requestB = new DashboardMetricsRequestDTO();

    assertThat(service.getMetrics(requestA).applications.total).isEqualTo(2);
    assertThat(service.getMetrics(requestB).applications.total).isEqualTo(2);

    service.getMetrics(requestA);
    service.getMetrics(requestB);

    verify(searchIndexClient, times(1)).count(anyString());
  }

  private void loginAs(final User user) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add(new UserPrincipal(user.getUsername(), user.getUsername(), User.INTERNAL_REALM_ID),
        User.INTERNAL_REALM_ID);

    SimpleSession session = new SimpleSession();
    session.setId(UUID.randomUUID().toString());
    session.setStartTimestamp(new Date());

    Subject authenticatedSubject = new Subject.Builder(lookup(SecurityManager.class))
        .session(session)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(lookup(SecurityManager.class));
    ThreadContext.bind(authenticatedSubject);
  }
}
