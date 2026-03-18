/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO.ContainerImageInQuarantineData;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiFirewallContainerImageResource.QUARANTINED_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallContainerImageResourceTest
    extends AbstractResourceTest
{
  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private PolicyViolationDAO policyViolationDAO;

  private Application application;

  private Repository repository;

  @Before
  public void setUp() {
    repositoryDAO = lookup(RepositoryDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);

    Organization organization = tempEntity.newOrganization();
    application = tempEntity.newApplicationWithParent(organization);
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ProxyStageType.ID, "scanId");
    repository = tempEntity.newRepository("docker-repo");
    repository.setFormat("docker");
    repositoryDAO.update(repository);
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    tempEntity.newPolicyViolation(policyEvaluation, policy, 8, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    tempEntity.newPolicyViolation(policyEvaluation, policy, 9, PolicyThreatCategory.OTHER, "test-group-id",
        "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, 10, PolicyThreatCategory.OTHER, "test-group-id",
            "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
    policyViolation.setWaiveTime(DateUtils.addDays(new Date(), 1));
    policyViolationDAO.update(policyViolation);

    licenseManager.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
  }

  @Override
  public HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH);
  }

  @Test
  public void testGetContainersInQuarantine() throws Exception {
    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "1")
        .query("pageSize", "100")
        .get();

    assertResponseStatus(200, response);

    TypeReference<ApiPageResult<ContainerImageInQuarantineData>> typeReference =
        new TypeReference<>()
        {
        };
    ApiPageResult<ContainerImageInQuarantineData> data =
        new ObjectMapper().readValue(response.getBodyBytes(), typeReference);

    assertThat(data.getTotal()).isEqualTo(1);
    List<ContainerImageInQuarantineData> containerImageInQuarantineData = data.getResults();
    assertThat(containerImageInQuarantineData.size()).isEqualTo(1);
    assertThat(containerImageInQuarantineData.get(0).threatLevel()).isEqualTo(9);
    assertThat(containerImageInQuarantineData.get(0).applicationName()).isEqualTo(application.getName());
    assertThat(containerImageInQuarantineData.get(0).repositoryPublicId()).isEqualTo(repository.getPublicId());
    assertThat(containerImageInQuarantineData.get(0).scanId()).isEqualTo("scanId");
  }

  @Test
  public void testGetContainersInQuarantine_checkMinimumPage() throws Exception {
    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "0")
        .query("pageSize", "100")
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetContainersInQuarantine_checkMinimumPageSize() throws Exception {
    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "1")
        .query("pageSize", "0")
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetContainersInQuarantine_checkMaximumPageSize() throws Exception {
    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "1")
        .query("pageSize", "101")
        .get();

    assertResponseStatus(400, response);
  }
}
