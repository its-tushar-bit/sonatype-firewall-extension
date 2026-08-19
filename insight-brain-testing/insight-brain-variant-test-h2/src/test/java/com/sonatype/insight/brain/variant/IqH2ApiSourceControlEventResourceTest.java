/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.ApiSourceControlEventDTO;
import com.sonatype.insight.brain.api.experimental.ApiSourceControlEventFilterDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiSourceControlEventResourceTest
{
  private IqTestContext ctx;

  private static final String CREATED_QUERY_PARAM_KEY = "createdOnOrAfter";

  private static final String ASCENDING_QUERY_PARAM_KEY = "ascending";

  private static final String LIMIT_QUERY_PARAM_KEY = "limit";

  private static final String OFFSET_QUERY_PARAM_KEY = "offset";

  @Test
  void testGetSourceControlEventData_ByApplicationId() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Set<String> applicationIds = Collections.singleton(application.getId());
    PolicyEvaluation policyEvaluation = ctx.tempEntity()
        .newPolicyEvaluation(application.getId(),
            BuildStageType.ID,
            "sourceScan",
            new Date(System.currentTimeMillis() - 10000),
            "sourceCommit");
    SourceControlEvent sourceControlEvent = ctx.tempEntity().newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filterDTO = new ApiSourceControlEventFilterDTO(
        applicationIds,
        sourceControlEvent.getCreateTime().getTime(),
        true,
        10,
        0);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SOURCE_CONTROL_EVENTS_RESOURCE_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .query(CREATED_QUERY_PARAM_KEY, filterDTO.getCreatedOnOrAfter())
        .query(ASCENDING_QUERY_PARAM_KEY, filterDTO.isAscending())
        .query(LIMIT_QUERY_PARAM_KEY, filterDTO.getLimit())
        .query(OFFSET_QUERY_PARAM_KEY, filterDTO.getOffset())
        .get();

    ctx.assertResponseStatus(200, response);
    List<ApiSourceControlEventDTO> results = response.getBodyList(ApiSourceControlEventDTO.class);
    assertThat(results).hasSize(1);
    ApiSourceControlEventDTO result = results.get(0);
    assertThat(result.getId()).isEqualTo(sourceControlEvent.getId());
  }

  @Test
  void testGetSourceControlEventData_ByOrganizationId() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    Application application = ctx.tempEntity().newApplication(organization.getPublicId());
    Set<String> applicationIds = Collections.singleton(application.getId());
    PolicyEvaluation policyEvaluation = ctx.tempEntity()
        .newPolicyEvaluation(application.getId(),
            BuildStageType.ID,
            "sourceScan",
            new Date(System.currentTimeMillis() - 10000),
            "sourceCommit");
    SourceControlEvent sourceControlEvent = ctx.tempEntity().newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filterDTO = new ApiSourceControlEventFilterDTO(
        applicationIds,
        sourceControlEvent.getCreateTime().getTime(),
        true,
        10,
        0);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SOURCE_CONTROL_EVENTS_RESOURCE_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .query(CREATED_QUERY_PARAM_KEY, filterDTO.getCreatedOnOrAfter())
        .query(ASCENDING_QUERY_PARAM_KEY, filterDTO.isAscending())
        .query(LIMIT_QUERY_PARAM_KEY, filterDTO.getLimit())
        .query(OFFSET_QUERY_PARAM_KEY, filterDTO.getOffset())
        .get();

    ctx.assertResponseStatus(200, response);
    List<ApiSourceControlEventDTO> results = response.getBodyList(ApiSourceControlEventDTO.class);
    assertThat(results).hasSize(1);
    ApiSourceControlEventDTO result = results.get(0);
    assertThat(result.getId()).isEqualTo(sourceControlEvent.getId());
  }

  @Test
  void testGetSourceControlEventData_ByApplicationIdWithNegativeOffset() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    Set<String> applicationIds = Collections.singleton(application.getId());
    PolicyEvaluation policyEvaluation = ctx.tempEntity()
        .newPolicyEvaluation(application.getId(),
            BuildStageType.ID,
            "sourceScan",
            new Date(System.currentTimeMillis() - 10000),
            "sourceCommit");
    SourceControlEvent sourceControlEvent = ctx.tempEntity().newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filterDTO = new ApiSourceControlEventFilterDTO(
        applicationIds,
        sourceControlEvent.getCreateTime().getTime(),
        true,
        1,
        -1);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SOURCE_CONTROL_EVENTS_RESOURCE_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .query(CREATED_QUERY_PARAM_KEY, filterDTO.getCreatedOnOrAfter())
        .query(ASCENDING_QUERY_PARAM_KEY, filterDTO.isAscending())
        .query(LIMIT_QUERY_PARAM_KEY, filterDTO.getLimit())
        .query(OFFSET_QUERY_PARAM_KEY, filterDTO.getOffset())
        .get();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testGetSourceControlEventData_ByOrganizationIdWithLimitZero() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    Application application = ctx.tempEntity().newApplication(organization.getPublicId());
    Set<String> applicationIds = Collections.singleton(application.getId());
    PolicyEvaluation policyEvaluation = ctx.tempEntity()
        .newPolicyEvaluation(application.getId(),
            BuildStageType.ID,
            "sourceScan",
            new Date(System.currentTimeMillis() - 10000),
            "sourceCommit");
    SourceControlEvent sourceControlEvent = ctx.tempEntity().newSourceControlEvent(application, policyEvaluation);
    ApiSourceControlEventFilterDTO filterDTO = new ApiSourceControlEventFilterDTO(
        applicationIds,
        sourceControlEvent.getCreateTime().getTime(),
        true,
        0,
        0);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.SOURCE_CONTROL_EVENTS_RESOURCE_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .query(CREATED_QUERY_PARAM_KEY, filterDTO.getCreatedOnOrAfter())
        .query(ASCENDING_QUERY_PARAM_KEY, filterDTO.isAscending())
        .query(LIMIT_QUERY_PARAM_KEY, filterDTO.getLimit())
        .query(OFFSET_QUERY_PARAM_KEY, filterDTO.getOffset())
        .get();

    ctx.assertResponseStatus(400, response);
  }
}
