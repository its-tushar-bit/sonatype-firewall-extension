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
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiFirewallContainerImageResource.QUARANTINED_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApiFirewallContainerImageResourceTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private PolicyViolationDAO policyViolationDAO;

  private Application application;

  private Repository repository;

  @BeforeEach
  void setUp() throws Exception {
    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
    policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);

    Organization organization = ctx.tempEntity().newOrganization();
    application = ctx.tempEntity().newApplicationWithParent(organization);
    Policy policy = ctx.tempEntity().newPolicy(application);
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(application.getId(), ProxyStageType.ID, "scanId");
    repository = ctx.tempEntity().newRepository("docker-repo");
    repository.setFormat("docker");
    repository.setQuarantineEnabled(true);
    repositoryDAO.update(repository);
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, 8, PolicyThreatCategory.OTHER, "test-group-id",
            "test-artifact-id1", "v1", "test-hash", FailActionType.ID);
    ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, 9, PolicyThreatCategory.OTHER, "test-group-id",
            "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
    PolicyViolation policyViolation =
        ctx.tempEntity()
            .newPolicyViolation(policyEvaluation, policy, 10, PolicyThreatCategory.OTHER, "test-group-id",
                "test-artifact-id2", "v1", "test-hash", FailActionType.ID);
    policyViolation.setWaiveTime(DateUtils.addDays(new Date(), 1));
    policyViolationDAO.update(policyViolation);

    ctx.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH);
  }

  @Test
  void testGetContainersInQuarantine() throws Exception {
    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "1")
        .query("pageSize", "100")
        .get();

    ctx.assertResponseStatus(200, response);

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
  void testGetContainersInQuarantine_checkMinimumPage() throws Exception {
    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "0")
        .query("pageSize", "100")
        .get();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testGetContainersInQuarantine_checkMinimumPageSize() throws Exception {
    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "1")
        .query("pageSize", "0")
        .get();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testGetContainersInQuarantine_checkMaximumPageSize() throws Exception {
    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "1")
        .query("pageSize", "101")
        .get();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testGetContainersInQuarantine_onlyQuarantineEnabledRepoImagesAppear() throws Exception {
    // Set up a second org/app/repo that is audit-only (quarantine_enabled = false)
    Organization auditOrg = ctx.tempEntity().newOrganization();
    Application auditApp = ctx.tempEntity().newApplicationWithParent(auditOrg);
    Policy auditPolicy = ctx.tempEntity().newPolicy(auditApp);
    PolicyEvaluation auditEval =
        ctx.tempEntity().newPolicyEvaluation(auditApp.getId(), ProxyStageType.ID, "auditScanId");

    Repository auditOnlyRepo = ctx.tempEntity().newRepository("audit-only-repo");
    auditOnlyRepo.setFormat("docker");
    // quarantineEnabled intentionally left false (default)
    repositoryDAO.update(auditOnlyRepo);
    auditOnlyRepo.setRelatedOrganizationId(auditOrg.getId());
    repositoryDAO.update(auditOnlyRepo);
    auditOrg.setRelatedRepositoryId(auditOnlyRepo.getId());
    organizationDAO.update(auditOrg);

    // Add a violation in the audit-only repo — should never appear in quarantine results
    ctx.tempEntity()
        .newPolicyViolation(auditEval, auditPolicy, 10, PolicyThreatCategory.OTHER, "test-group-id",
            "audit-artifact", "v1", "test-hash", FailActionType.ID);

    HttpResponse response = restRequest()
        .path(QUARANTINED_PATH)
        .query("page", "1")
        .query("pageSize", "100")
        .get();

    ctx.assertResponseStatus(200, response);

    TypeReference<ApiPageResult<ContainerImageInQuarantineData>> typeReference =
        new TypeReference<>()
        {
        };
    ApiPageResult<ContainerImageInQuarantineData> data =
        new ObjectMapper().readValue(response.getBodyBytes(), typeReference);

    // Only the quarantine-enabled repo's image must appear; the audit-only repo's image must not
    assertThat(data.getResults())
        .extracting(ContainerImageInQuarantineData::repositoryPublicId)
        .containsOnly(repository.getPublicId())
        .doesNotContain(auditOnlyRepo.getPublicId());
  }
}
