/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class SuccessMetricsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SuccessMetricsService successMetricsService;

  @Test
  public void testGetSuccessMetricsForCurrentUser_UnauthorizedApps() throws Exception {
    Application app2 = tempEntity.newApplication(org.getId());
    SuccessMetricsScopeDTO successMetricsScopeDto = new SuccessMetricsScopeDTO(
        new HashSet<>(Arrays.asList(app2.getId(), app.getId())), null);
    SuccessMetricsDTO successMetricsDto = new SuccessMetricsDTO("Metrics", successMetricsScopeDto);
    login();

    successMetricsService.createSuccessMetricsForCurrentUser(successMetricsDto);

    grantReadPermission(app.getId());
    List<SuccessMetricsDTO> actual = successMetricsService.getSuccessMetricsForCurrentUser();
    assertThat(actual.get(0).scope.applicationIds, containsInAnyOrder(app.getId()));

    grantReadPermission(app2.getId());
    actual = successMetricsService.getSuccessMetricsForCurrentUser();
    assertThat(actual.get(0).scope.applicationIds, containsInAnyOrder(app.getId(), app2.getId()));
  }

  @Test
  public void testGetSuccessMetricsForCurrentUser_UnauthorizedOrgs() throws Exception {
    Organization org2 = tempEntity.newOrganization("Org2");
    SuccessMetricsScopeDTO successMetricsScopeDto = new SuccessMetricsScopeDTO(null,
        new HashSet<>(Arrays.asList(org.getId(), org2.getId())));
    SuccessMetricsDTO successMetricsDto = new SuccessMetricsDTO("Metrics", successMetricsScopeDto);
    login();

    successMetricsService.createSuccessMetricsForCurrentUser(successMetricsDto);

    grantReadPermission(org.getId());
    List<SuccessMetricsDTO> actual = successMetricsService.getSuccessMetricsForCurrentUser();
    assertThat(actual.get(0).scope.organizationIds, containsInAnyOrder(org.getId()));

    grantReadPermission(org2.getId());
    actual = successMetricsService.getSuccessMetricsForCurrentUser();
    assertThat(actual.get(0).scope.organizationIds, containsInAnyOrder(org.getId(), org2.getId()));
  }
}
