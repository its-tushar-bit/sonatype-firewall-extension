/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

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
 * RBAC isolation for dashboard metrics (CLM-40927 Task 8).
 */
public class DashboardMetricsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private DashboardMetricsService dashboardMetricsService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private Organization orgA;

  private Organization orgB;

  private User userA;

  private User userB;

  @Before
  public void setUpClient() {
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    DashboardMetricsTestSupport.resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");
    DashboardMetricsTestSupport.clearDashboardMetricsCache(dashboardMetricsService);
  }

  @Override
  protected void setUpSecurity() {
    orgA = tempEntity.newOrganization();
    orgB = tempEntity.newOrganization();
    tempEntity.newApplication(orgA.getId());
    tempEntity.newApplication(orgA.getId());
    tempEntity.newApplication(orgB.getId());
    tempEntity.newApplication(orgB.getId());
    tempEntity.newApplication(orgB.getId());

    userA = tempEntity.newUser();
    userB = tempEntity.newUser();

    Role roleA = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(orgA.getId(), roleA.getId(), userA.getUsername());
    Role roleB = tempEntity.newRole(false /* global */, Permission.READ);
    tempEntity.newMembershipMapping(orgB.getId(), roleB.getId(), userB.getUsername());

    subject = new Subject.Builder(lookup(SecurityManager.class)).buildSubject();
    ThreadContext.bind(lookup(SecurityManager.class));
    ThreadContext.bind(subject);
  }

  @Test
  public void testGetMetrics_RbacIsolation_ScopesCountsToReadableOrganizations() {
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(userA);
    DashboardMetricsDTO metricsForUserA = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());
    assertThat(metricsForUserA.applications.total).isEqualTo(2);
    assertThat(metricsForUserA.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);

    loginAs(userB);
    DashboardMetricsDTO metricsForUserB = dashboardMetricsService.getMetrics(new DashboardMetricsRequestDTO());
    assertThat(metricsForUserB.applications.total).isEqualTo(3);
    assertThat(metricsForUserB.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);
  }

  @Test
  public void testGetMetrics_ExplicitOrganizationFilterCannotProbeUnreadableOrg() {
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(userA);

    DashboardMetricsRequestDTO filterToUnreadableOrg = new DashboardMetricsRequestDTO();
    filterToUnreadableOrg.organizationIds = Set.of(orgB.getId());
    DashboardMetricsDTO unreadableOrgMetrics = dashboardMetricsService.getMetrics(filterToUnreadableOrg);
    assertThat(unreadableOrgMetrics.applications.total).isZero();
    assertThat(unreadableOrgMetrics.applications.source).isEqualTo(DashboardMetricsService.METRIC_SOURCE_INDEX);

    DashboardMetricsRequestDTO filterToReadableOrg = new DashboardMetricsRequestDTO();
    filterToReadableOrg.organizationIds = Set.of(orgA.getId());
    assertThat(dashboardMetricsService.getMetrics(filterToReadableOrg).applications.total).isEqualTo(2);
  }

  @Test
  public void testGetMetrics_ExplicitApplicationFilterCannotProbeUnreadableApp() {
    String unreadableAppId = tempEntity.newApplication(orgB.getId()).getId();
    DashboardMetricsTestSupport.populateIndex(luceneSearchIndexClient);

    loginAs(userA);

    DashboardMetricsRequestDTO filterToUnreadableApp = new DashboardMetricsRequestDTO();
    filterToUnreadableApp.applicationIds = Set.of(unreadableAppId);
    assertThat(dashboardMetricsService.getMetrics(filterToUnreadableApp).applications.total).isZero();
  }

  private void loginAs(User user) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add(new UserPrincipal(user.getUsername(), user.getUsername(), User.INTERNAL_REALM_ID),
        User.INTERNAL_REALM_ID);

    SimpleSession session = new SimpleSession();
    session.setId(UUID.randomUUID().toString());
    session.setStartTimestamp(new Date());

    subject = new Subject.Builder(lookup(SecurityManager.class))
        .session(session)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(lookup(SecurityManager.class));
    ThreadContext.bind(subject);
  }
}
