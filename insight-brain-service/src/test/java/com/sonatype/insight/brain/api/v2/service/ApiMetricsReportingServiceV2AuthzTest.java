/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingFlattenedDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils.FakeDateRule;

import com.google.common.collect.Sets;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiMetricsReportingServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Rule
  public FakeDateRule fakeDateRule = new FakeDateRule();

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ApiMetricsReportingServiceV2 service;

  private LocalDate today;

  private SuccessMetricsTestUtils successMetricsTestUtils;

  @Before
  public void fakeDate() {
    successMetricsTestUtils = new SuccessMetricsTestUtils(policyViolationDAO);
    today = new LocalDate();
  }

  @Test
  public void testGetMetrics_ExplicitApplicationFilter_Unauthorized() {
    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    login();

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, Collections.singleton(app.getId()));

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetMetrics_ExplicitOrganizationFilter_Unauthorized() {
    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    login();

    List<ApiMetricsReportingDTOV2> results = getMetrics(Collections.singleton(org.getId()), null);

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetMetrics_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());
    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, appIds);

    assertResults(results);
  }

  @Test
  public void testGetMetrics_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(Collections.singleton(org.getId()), null);

    assertResults(results);
  }

  @Test
  public void testGetMetrics_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(org.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(orgIds, null);

    assertResults(results);
  }

  @Test
  public void testGetMetrics_ImplicitFilter_Unauthorized() {
    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    login();

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, null);

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetMetrics_ImplicitFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, null);

    assertResults(results);
  }

  @Test
  public void testGetMetrics_ImplicitFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(org.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, null);

    assertResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitApplicationFilter_Unauthorized() {
    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    login();

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, Collections.singleton(app.getId()));

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitOrganizationFilter_Unauthorized() {
    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    login();

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(Collections.singleton(org.getId()), null);

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());
    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, appIds);

    assertFlattenedResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(Collections.singleton(org.getId()), null);

    assertFlattenedResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(orgIds, null);

    assertFlattenedResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ImplicitFilter_Unauthorized() {
    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    login();

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, null);

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetFlattenedMetrics_ImplicitFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, null);

    assertFlattenedResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ImplicitFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, null);

    assertFlattenedResults(results);
  }

  private List<ApiMetricsReportingDTOV2> getMetrics(Set<String> organizationIds, Set<String> applicationIds) {
    return service.getMetrics(makeQueryDTO(organizationIds, applicationIds));
  }

  private List<ApiMetricsReportingFlattenedDTOV2> getFlattenedMetrics(
      Set<String> organizationIds,
      Set<String> applicationIds)
  {
    return service.getFlattenedMetrics(makeQueryDTO(organizationIds, applicationIds));
  }

  private ApiMetricsReportingQueryDTOV2 makeQueryDTO(Set<String> organizationIds, Set<String> applicationIds) {
    return new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-11", null, applicationIds, organizationIds);
  }

  private void assertResults(List<ApiMetricsReportingDTOV2> results) {
    assertThat(results).hasSize(1);
    assertThat(results.get(0).aggregations).hasSize(2);
    assertThat(results.get(0).applicationId).isEqualTo(app.getId());
  }

  private void assertFlattenedResults(List<ApiMetricsReportingFlattenedDTOV2> results) {
    assertThat(results).hasSize(2).extracting(dto -> dto.applicationId).containsOnly(app.getId());
  }
}
