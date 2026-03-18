/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiAgeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRetentionPolicyDTO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiDataRetentionPolicyResourceTest
    extends AbstractResourceTest
{
  private DataRetentionPolicyDAO dataRetentionPolicyDAO;

  @Before
  public void setUp() {
    dataRetentionPolicyDAO = lookup(DataRetentionPolicyDAO.class);
  }

  private HttpRequest restRequest(String organizationId) {
    return restRequest()
        .path(PublicApiPaths.DATA_RETENTION_POLICY_RESOURCE_PATH,
            ApiDataRetentionPolicyResource.ORGANIZATION_PATH)
        .parameter(organizationId);
  }

  @Test
  public void testGetDataRetentionPolicies() throws Exception {
    HttpResponse response = restRequest(Organization.ROOT_ORGANIZATION_ID).get();
    assertResponseStatus(200, response);
    ApiDataRetentionPoliciesDTO dto = response.getBody(ApiDataRetentionPoliciesDTO.class);

    assertThat(dto).isNotNull();
    assertThat(dto.applicationReports).isNotNull();
    assertThat(dto.applicationReports.stages).containsOnlyKeys(Stage.ID_DEVELOP, Stage.ID_SOURCE, Stage.ID_BUILD,
        Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE, Stage.ID_OPERATE,
        DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING);
    assertThat(dto.applicationReports.stages.values()).allSatisfy(policyDTO -> {
      assertThat(policyDTO).isNotNull();
      assertThat(policyDTO.inheritPolicy).isFalse();
      assertThat(policyDTO.maxAge).isNotNull();
    });
    assertThat(dto.successMetrics).isNotNull();
    assertThat(dto.successMetrics.inheritPolicy).isFalse();
    assertThat(dto.successMetrics.maxAge).isNotNull();
  }

  @Test
  public void testGetParentDataRetentionPolicies() throws Exception {
    Organization organization = tempEntity.newOrganization();

    HttpResponse response = restRequest().path(PublicApiPaths.DATA_RETENTION_POLICY_RESOURCE_PATH,
        ApiDataRetentionPolicyResource.PARENT_ORGANIZATION_PATH).parameter(organization.getId()).get();

    assertResponseStatus(200, response);
    ApiDataRetentionPoliciesDTO dto = response.getBody(ApiDataRetentionPoliciesDTO.class);

    assertThat(dto).isNotNull();
    assertThat(dto.applicationReports).isNotNull();
    assertThat(dto.applicationReports.stages).containsOnlyKeys(Stage.ID_DEVELOP, Stage.ID_SOURCE, Stage.ID_BUILD,
        Stage.ID_STAGE_RELEASE, Stage.ID_RELEASE, Stage.ID_OPERATE,
        DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING);
    assertThat(dto.applicationReports.stages.values()).allSatisfy(policyDTO -> {
      assertThat(policyDTO).isNotNull();
      assertThat(policyDTO.inheritPolicy).isFalse();
      assertThat(policyDTO.maxAge).isNotNull();
    });
    assertThat(dto.successMetrics).isNotNull();
    assertThat(dto.successMetrics.inheritPolicy).isFalse();
    assertThat(dto.successMetrics.maxAge).isNotNull();
  }

  @Test
  public void testGetParentDataRetentionPolicies_BadRequest() throws Exception {
    HttpResponse response = restRequest().path(PublicApiPaths.DATA_RETENTION_POLICY_RESOURCE_PATH,
        ApiDataRetentionPolicyResource.PARENT_ORGANIZATION_PATH)
        .parameter(Organization.ROOT_ORGANIZATION_ID)
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Organization with id ROOT_ORGANIZATION_ID does not have a parent organization.");
  }

  @Test
  public void testSetDataRetentionPolicies() throws Exception {
    Organization org = tempEntity.newOrganization();
    ApiDataRetentionPoliciesDTO dto = new ApiDataRetentionPoliciesDTO();
    dto.applicationReports = new ApiReportRetentionPoliciesDTO();
    dto.applicationReports.stages.put(Stage.ID_BUILD,
        new ApiReportRetentionPolicyDTO(false, true, 30, ApiAgeDTO.fromString("2 weeks")));

    HttpResponse response = restRequest(org.getId()).body(dto).put();
    assertResponseStatus(204, response);

    Map<String, DataRetentionPolicy> policies = dataRetentionPolicyDAO.getByOwnerId(org.getId());
    assertThat(policies).containsOnlyKeys(Stage.ID_BUILD);
    DataRetentionPolicy policy = policies.get(Stage.ID_BUILD);
    assertThat(policy.isPurgingEnabled()).isTrue();
    assertThat(policy.getMaxCount()).isEqualTo(30);
    assertThat(policy.getMaxAgeInDays()).isEqualTo(14);
  }
}
