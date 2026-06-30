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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.Configuration;
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

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
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
  public void testGetMetrics_OrganizationFilterHierarchyInclusive() {
    Organization parentOrg = tempEntity.newOrganization("metrics-parent-org");
    Organization childOrg = tempEntity.newOrganization("metrics-child-org", parentOrg);
    Organization siblingOrg = tempEntity.newOrganization("metrics-sibling-org");

    tempEntity.newApplication(parentOrg.getId());
    tempEntity.newApplication(childOrg.getId());
    tempEntity.newApplication(childOrg.getId());
    Application siblingApp = tempEntity.newApplication(siblingOrg.getId());
    tempEntity.newApplication(siblingOrg.getId());
    tempEntity.newApplication(siblingOrg.getId());

    User reader = tempEntity.newUser("metrics-hierarchy-reader");
    Role readRole = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, readRole.getId(), reader.getUsername());

    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);
    loginAs(reader);

    DashboardMetricsDTO unfiltered = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());
    assertThat(unfiltered.applications.total).isEqualTo(6);
    assertIndexSourcedMetric(unfiltered);

    DashboardMetricsRequestDTO filterByChild = new DashboardMetricsRequestDTO();
    filterByChild.organizationIds = Set.of(childOrg.getId());
    DashboardMetricsDTO childScoped = dashboardMetricsService.getMetrics(filterByChild);
    assertThat(childScoped.applications.total).isEqualTo(2);
    assertIndexSourcedMetric(childScoped);

    DashboardMetricsRequestDTO filterByParent = new DashboardMetricsRequestDTO();
    filterByParent.organizationIds = Set.of(parentOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByParent).applications.total).isEqualTo(3);

    DashboardMetricsRequestDTO filterBySibling = new DashboardMetricsRequestDTO();
    filterBySibling.organizationIds = Set.of(siblingOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterBySibling).applications.total).isEqualTo(3);

    DashboardMetricsRequestDTO filterByRoot = new DashboardMetricsRequestDTO();
    filterByRoot.organizationIds = Set.of(Organization.ROOT_ORGANIZATION_ID);
    assertThat(dashboardMetricsService.getMetrics(filterByRoot).applications.total).isEqualTo(6);

    DashboardMetricsRequestDTO filterByMultipleOrgs = new DashboardMetricsRequestDTO();
    filterByMultipleOrgs.organizationIds = Set.of(parentOrg.getId(), siblingOrg.getId());
    assertThat(dashboardMetricsService.getMetrics(filterByMultipleOrgs).applications.total).isEqualTo(6);

    DashboardMetricsRequestDTO applicationOnly = new DashboardMetricsRequestDTO();
    applicationOnly.applicationIds = Set.of(siblingApp.getId());
    assertThat(dashboardMetricsService.getMetrics(applicationOnly).applications.total).isEqualTo(1);

    DashboardMetricsRequestDTO combinedOrgAndApp = new DashboardMetricsRequestDTO();
    combinedOrgAndApp.organizationIds = Set.of(parentOrg.getId());
    combinedOrgAndApp.applicationIds = Set.of(siblingApp.getId());
    assertThat(dashboardMetricsService.getMetrics(combinedOrgAndApp).applications.total).isEqualTo(4);

    DashboardMetricsRequestDTO unknownOrg = new DashboardMetricsRequestDTO();
    unknownOrg.organizationIds = Set.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    assertThat(dashboardMetricsService.getMetrics(unknownOrg).applications.total).isZero();
  }

  @Test
  public void testGetMetrics_OrganizationFilterRejectsOversizedExpansion() {
    OrganizationDAO organizationDAO = mock(OrganizationDAO.class);
    List<Organization> expandedOrgs = new java.util.ArrayList<>();
    for (int i = 0; i < 6; i++) {
      Organization org = mock(Organization.class);
      when(org.getId()).thenReturn("expanded-org-" + i);
      expandedOrgs.add(org);
    }
    when(organizationDAO.getAllChildOrganizations("big-org")).thenReturn(expandedOrgs);

    Configuration configuration = mock(Configuration.class);
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(5);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));

    DashboardMetricsService service =
        new DashboardMetricsService(
            mock(SearchIndexClient.class),
            new MetricFilterValidator(),
            organizationDAO,
            configuration,
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("big-org");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getMetrics(request))
        .withMessageContaining("too many organizations");
  }

  @Test
  public void testGetMetrics_OrganizationFilterAcceptsExpansionAtMaxClauseCount() {
    OrganizationDAO organizationDAO = mock(OrganizationDAO.class);
    List<Organization> expandedOrgs = new java.util.ArrayList<>();
    for (int i = 0; i < 5; i++) {
      Organization org = mock(Organization.class);
      when(org.getId()).thenReturn("expanded-org-" + i);
      expandedOrgs.add(org);
    }
    when(organizationDAO.getAllChildOrganizations("max-org")).thenReturn(expandedOrgs);

    Configuration configuration = mock(Configuration.class);
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(5);

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal()).thenReturn(
        new UserPrincipal("filter-user", "filter-user", User.INTERNAL_REALM_ID));

    SearchIndexClient searchIndexClient = mock(SearchIndexClient.class);
    when(searchIndexClient.count(anyString())).thenReturn(1L);
    when(searchIndexClient.getLastIndexTime()).thenReturn(1L);

    DashboardMetricsService service =
        new DashboardMetricsService(
            searchIndexClient,
            new MetricFilterValidator(),
            organizationDAO,
            configuration,
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of("max-org");

    assertThat(service.getMetrics(request).applications.total).isEqualTo(1);
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
        new DashboardMetricsService(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

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
        new DashboardMetricsService(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

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

    UserPrincipal internalPrincipal =
        new UserPrincipal("shared-user", "shared-user", User.INTERNAL_REALM_ID);
    UserPrincipal ldapPrincipal =
        new UserPrincipal("shared-user", "shared-user", "ldap-realm-id");

    CurrentUser currentUser = mock(CurrentUser.class);
    when(currentUser.getUserPrincipal())
        .thenReturn(internalPrincipal, ldapPrincipal, internalPrincipal, ldapPrincipal);

    DashboardMetricsService service =
        new DashboardMetricsService(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();

    assertThat(service.getMetrics(request).applications.total).isEqualTo(5);
    assertThat(service.getMetrics(request).applications.total).isEqualTo(9);

    service.getMetrics(request);
    service.getMetrics(request);

    verify(searchIndexClient, times(2)).count(anyString());
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
        new DashboardMetricsService(
            searchIndexClient,
            new MetricFilterValidator(),
            mock(OrganizationDAO.class),
            mockConfiguration(),
            currentUser);

    DashboardMetricsRequestDTO requestA = new DashboardMetricsRequestDTO();
    DashboardMetricsRequestDTO requestB = new DashboardMetricsRequestDTO();

    assertThat(service.getMetrics(requestA).applications.total).isEqualTo(2);
    assertThat(service.getMetrics(requestB).applications.total).isEqualTo(2);

    service.getMetrics(requestA);
    service.getMetrics(requestB);

    verify(searchIndexClient, times(1)).count(anyString());
  }

  private static void assertIndexSourcedMetric(DashboardMetricsDTO metrics) {
    assertThat(metrics.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);
    assertThat(metrics.applications.breakdown).isNull();
    assertThat(metrics.lastUpdatedAt).isNotNull();
  }

  private static Configuration mockConfiguration() {
    return mock(Configuration.class);
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
