/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Sets;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingFlattenedDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils.FakeDateRule;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ApiMetricsReportingServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Rule
  public FakeDateRule fakeDateRule = new FakeDateRule();

  @Inject
  private ApiMetricsReportingServiceV2 service;

  private LocalDate today;

  @Before
  public void fakeDate() {
    today = new LocalDate();
  }

  @Test
  public void testGetMetrics_ExplicitApplicationFilter_Unauthorized() {
    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    login();

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, Collections.singleton(app.getId()));

    assertThat(results, is(empty()));
  }

  @Test
  public void testGetMetrics_ExplicitOrganizationFilter_Unauthorized() {
    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    login();

    List<ApiMetricsReportingDTOV2> results = getMetrics(Collections.singleton(org.getId()), null);

    assertThat(results, is(empty()));
  }

  @Test
  public void testGetMetrics_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());
    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, appIds);

    assertResults(results);
  }

  @Test
  public void testGetMetrics_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(Collections.singleton(org.getId()), null);

    assertResults(results);
  }

  @Test
  public void testGetMetrics_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(org.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(orgIds, null);

    assertResults(results);
  }

  @Test
  public void testGetMetrics_ImplicitFilter_Unauthorized() {
    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    login();

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, null);

    assertThat(results, is(empty()));
  }

  @Test
  public void testGetMetrics_ImplicitFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, null);

    assertResults(results);
  }

  @Test
  public void testGetMetrics_ImplicitFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(org.getId());

    List<ApiMetricsReportingDTOV2> results = getMetrics(null, null);

    assertResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitApplicationFilter_Unauthorized() {
    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    login();

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, Collections.singleton(app.getId()));

    assertThat(results, is(empty()));
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitOrganizationFilter_Unauthorized() {
    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    login();

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(Collections.singleton(org.getId()), null);

    assertThat(results, is(empty()));
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitApplicationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());
    Set<String> appIds = Sets.newHashSet(app.getId(), app2.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, appIds);

    assertFlattenedResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitOrganizationFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(Collections.singleton(org.getId()), null);

    assertFlattenedResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ExplicitOrganizationFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    Set<String> orgIds = Sets.newHashSet(org.getId(), org2.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(orgIds, null);

    assertFlattenedResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ImplicitFilter_Unauthorized() {
    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    login();

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, null);

    assertThat(results, is(empty()));
  }

  @Test
  public void testGetFlattenedMetrics_ImplicitFilter_Authorized() {
    Application app2 = tempEntity.newApplication(org.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, null);

    assertFlattenedResults(results);
  }

  @Test
  public void testGetFlattenedMetrics_ImplicitFilter_AuthorizedOneOrg() {
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app.getId(), today, tempEntity);
    SuccessMetricsTestUtils.createPolicyViolation(app2.getId(), today, tempEntity);
    grantReadPermission(app.getId());

    List<ApiMetricsReportingFlattenedDTOV2> results = getFlattenedMetrics(null, null);

    assertFlattenedResults(results);
  }

  private List<ApiMetricsReportingDTOV2> getMetrics(Set<String> organizationIds, Set<String> applicationIds) {
    return service.getMetrics(makeQueryDTO(organizationIds, applicationIds));
  }

  private List<ApiMetricsReportingFlattenedDTOV2> getFlattenedMetrics(Set<String> organizationIds,
                                                                      Set<String> applicationIds)
  {
    return service.getFlattenedMetrics(makeQueryDTO(organizationIds, applicationIds));
  }

  private ApiMetricsReportingQueryDTOV2 makeQueryDTO(Set<String> organizationIds, Set<String> applicationIds) {
    return new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-11", null, applicationIds, organizationIds);
  }

  private void assertResults(List<ApiMetricsReportingDTOV2> results) {
    assertThat(results, hasSize(1));
    assertThat(results.get(0).aggregations, hasSize(2));
    assertThat(results.get(0).applicationId, is(app.getId()));
  }

  private void assertFlattenedResults(List<ApiMetricsReportingFlattenedDTOV2> results) {
    assertThat(results, hasSize(2));

    Set<String> presentApps = results.stream().map(dto -> dto.applicationId).collect(Collectors.toSet());
    assertThat(presentApps, is(Collections.singleton(app.getId())));
  }
}
