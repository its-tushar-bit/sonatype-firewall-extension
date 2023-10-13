/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList.ApiRepositoryComponentEvaluationRequest;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList.ApiRepositoryComponentEvaluationResult;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerListDTO;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.MultiTenantBrainServiceTestService;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assume;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallResourceTest
    extends AbstractResourceTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  private final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private final RepositoryDAO repositoryDAO = new RepositoryDAO();

  @Test
  public void testGetFirewallUnquarantineSummary() throws Exception {
    // when GETing unquarantine summary
    HttpResponse response = restRequest().path(
        PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.RELEASE_QUARANTINE_SUMMARY_PATH).get();

    // then result is OK
    assertResponseStatus(HttpStatus.OK_200, response);

    // and value is present
    ApiFirewallReleaseQuarantineSummaryDTO dto = response.getBody(ApiFirewallReleaseQuarantineSummaryDTO.class);
    assertThat(dto.autoReleaseQuarantineCountMTD).isZero();
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig() throws Exception {
    // when GETing config
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).get();

    // then result is OK
    assertResponseStatus(HttpStatus.OK_200, response);

    // and value is present
    ApiFirewallReleaseQuarantineConfigDTO[] dtos = response.getBody(ApiFirewallReleaseQuarantineConfigDTO[].class);
    assertThat(dtos).isNotNull().isNotEmpty();
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig_MissingFirewallAutoUnquarantineFeature() throws Exception {
    // setup remove firewall feature
    setMissingFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    // when GETing config
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).get();

    // then result is payment required 402
    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig_MissingReleaseIntegrityFeature() throws Exception {
    // setup remove firewall feature
    setMissingFeature(LicensedFeature.RELEASE_INTEGRITY);

    // when GETing config
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).get();

    // then result is payment required 402
    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testSetFirewallAutoUnquarantineConfig() throws Exception {
    // when SETing config
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).body(new ArrayList<>()).put();

    // then result is OK
    assertResponseStatus(HttpStatus.OK_200, response);

    // and value is present
    ApiFirewallReleaseQuarantineConfigDTO[] dtos = response.getBody(ApiFirewallReleaseQuarantineConfigDTO[].class);
    assertThat(dtos).isNotNull().isNotEmpty();
  }

  @Test
  public void testSetFirewallAutoUnquarantineConfig_MissingFirewallAutoUnquarantineFeature() throws Exception {
    // setup remove feature
    setMissingFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    // when SETing config
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).put();

    // then result is payment required 402
    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testSetFirewallAutoUnquarantineConfig_MissingReleaseIntegrityFeature() throws Exception {
    // setup remove feature
    setMissingFeature(LicensedFeature.RELEASE_INTEGRITY);

    // when SETing config
    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).put();

    // then result is payment required 402
    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testGetQuarantineSummary() throws Exception {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    tempEntity.newRepositoryComponent(repo, "hash");
    tempEntity.newRepositoryComponent(repo.getId(), "path", new Date(), null);
    tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo2", true, false);

    HttpResponse response =
        restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINE_SUMMARY_PATH).get();

    assertResponseStatus(HttpStatus.OK_200, response);
    ApiFirewallQuarantineSummaryDTO summary = response.getBody(ApiFirewallQuarantineSummaryDTO.class);
    assertThat(summary).isNotNull();
    assertThat(summary.repositoryCount).isEqualTo(2);
    assertThat(summary.quarantineEnabled).isTrue();
    assertThat(summary.quarantineEnabledRepositoryCount).isEqualTo(1);
    assertThat(summary.totalComponentCount).isEqualTo(2);
    assertThat(summary.quarantinedComponentCount).isEqualTo(1);
  }

  @Test
  public void testGetUnquarantineList() throws Exception {
    Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));
    Date june2nd2020 = Date.from(LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(ZoneOffset.UTC));

    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    Condition condition = new Condition("RelativePopularity", "<=", "10");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy1 = tempEntity.newPolicy("policy1", constraint);

    // ADD COMPONENT
    final RepositoryComponent component1 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june1st2020, june2nd2020, true);

    // ADD ANOTHER COMPONENT
    RepositoryComponent component2 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", june1st2020, june2nd2020, true);
    component2.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "b", "v"));
    repositoryComponentDAO.update(component2);

    // CREATE POLICY VIOLATION
    final RepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component1, tempEntity);

    // CREATE ANOTHER POLICY VIOLATION
    PolicyViolationTestHelper.createPolicyViolationFail(policy1, component2, tempEntity);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("page", 1)
        .query("pageSize", 2)
        .query("policyId", policy1.getId())
        .query("sortBy", FirewallSortableField.RELEASE_QUARANTINE_TIME.getLabel())
        .query("asc", "false")
        .query("componentName", "a")
        .get();

    assertResponseStatus(HttpStatus.OK_200, response);
    ApiPageResult<ApiFirewallComponentDTO> responseDTO = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<ApiPageResult<ApiFirewallComponentDTO>>() { });
    assertThat(responseDTO.getTotal()).isEqualTo(1);
    final ApiFirewallComponentDTO componentDTO1 = responseDTO.getResults().get(0);
    ApiFirewallServiceTest
        .assertRepositoryComponentWithOnePolicyViolation(policyViolation1, componentDTO1, june1st2020, june2nd2020);
  }

  @Test
  public void testGetUnquarantineList_defaultValues() throws Exception {
    Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));
    Date june2nd2020 = Date.from(LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(ZoneOffset.UTC));

    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    Condition condition = new Condition("RelativePopularity", "<=", "10");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy1 = tempEntity.newPolicy("policy1", constraint);

    // ADD COMPONENT
    final RepositoryComponent component1 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june1st2020, june2nd2020, true);

    // CREATE POLICY VIOLATION
    final RepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component1, tempEntity);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH).get();

    assertResponseStatus(HttpStatus.OK_200, response);
    ApiPageResult<ApiFirewallComponentDTO> responseDTO = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<ApiPageResult<ApiFirewallComponentDTO>>() { });
    assertThat(responseDTO.getTotal()).isEqualTo(1);
    final ApiFirewallComponentDTO componentDTO1 = responseDTO.getResults().get(0);
    ApiFirewallServiceTest
        .assertRepositoryComponentWithOnePolicyViolation(policyViolation1, componentDTO1, june1st2020, june2nd2020);
  }

  @Test
  public void testGetUnquarantineList_invalid() throws Exception {
    // pageSize < MIN_PAGE
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("page", ApiFirewallService.MIN_PAGE - 1)
        .get();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Invalid page: " + (ApiFirewallService.MIN_PAGE - 1) + ". Page shouldn't be lower than 1");

    // pageSize < MIN_PAGE_SIZE
    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("pageSize", ApiFirewallService.MIN_PAGE_SIZE - 1)
        .get();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Invalid page size: " + (ApiFirewallService.MIN_PAGE_SIZE - 1) + ". Page size should be between " +
            ApiFirewallService.MIN_PAGE_SIZE + " and " +
            ApiFirewallService.MAX_PAGE_SIZE);

    // pageSize > MAX_PAGE_SIZE
    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("pageSize", ApiFirewallService.MAX_PAGE_SIZE + 1)
        .get();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Invalid page size: " + (ApiFirewallService.MAX_PAGE_SIZE + 1) + ". Page size should be between " +
            ApiFirewallService.MIN_PAGE_SIZE + " and " +
            ApiFirewallService.MAX_PAGE_SIZE);
  }

  @Test
  public void testGetUnquarantineList_invalidSortField() throws Exception {
    // pageSize < 1
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("sortBy", "INVALID")
        .get();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText()).isEqualTo("sortBy field is invalid");
  }

  @Test
  public void testGetQuarantineList() throws Exception {
    Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));
    Date june2st2020 = DateUtils.addDays(june1st2020, 1);

    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    Condition condition = new Condition("RelativePopularity", "<=", "10");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy1 = tempEntity.newPolicy("policy1", constraint);

    // ADD COMPONENT
    final RepositoryComponent component1 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june1st2020, null, false);

    // ADD ANOTHER COMPONENT
    RepositoryComponent component2 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", june2st2020, null, false);
    component2.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "b", "v"));
    repositoryComponentDAO.update(component2);

    // CREATE POLICY VIOLATION
    final RepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component1, tempEntity);

    // CREATE ANOTHER POLICY VIOLATION
    PolicyViolationTestHelper.createPolicyViolationFail(policy1, component2, tempEntity);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINED_PATH)
        .query("page", 1)
        .query("pageSize", 2)
        .query("policyId", policy1.getId())
        .query("asc", "false")
        .query("componentName", "a")
        .get();

    assertResponseStatus(HttpStatus.OK_200, response);
    ApiPageResult<ApiFirewallComponentDTO> responseDTO = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<ApiPageResult<ApiFirewallComponentDTO>>() { });
    assertThat(responseDTO.getTotal()).isEqualTo(1);
    final ApiFirewallComponentDTO componentDTO1 = responseDTO.getResults().get(0);
    ApiFirewallServiceTest
        .assertRepositoryComponentWithOnePolicyViolation(policyViolation1, componentDTO1, june1st2020, null);

    // TEST WITH MULTIPLE POLICIES
    Policy policy2 = tempEntity.newPolicy("policy2", constraint);
    PolicyViolationTestHelper.createPolicyViolationFail(policy2, component2, tempEntity);

    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINED_PATH)
        .query("page", 1)
        .query("pageSize", 2)
        .query("policyId", policy1.getId(), policy2.getId())
        .query("sortBy", FirewallSortableField.QUARANTINE_TIME.getLabel())
        .query("asc", "true")
        .get();

    assertResponseStatus(HttpStatus.OK_200, response);
    responseDTO = getBodyByTypeReference(
        response.getBodyBytes(),
        new TypeReference<ApiPageResult<ApiFirewallComponentDTO>>() { });

    assertThat(responseDTO.getTotal()).isEqualTo(2);

    assertThat(responseDTO.getResults())
        .extracting(dto -> dto.componentIdentifier)
        .containsExactly(component1.getComponentIdentifier(), component2.getComponentIdentifier());
  }

  @Test
  public void testGetQuarantineList_InvalidSortOrder() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINED_PATH)
        .query("sortBy", FirewallSortableField.RELEASE_QUARANTINE_TIME.getLabel())
        .get();

    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Sortable field releaseQuarantineTime is not applicable to component state QUARANTINE");
  }

  @Test
  public void testGetQuarantineList_defaultValues() throws Exception {
    Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));

    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    Condition condition = new Condition("RelativePopularity", "<=", "10");
    Constraint constraint = new Constraint("c1", "constraint1", LogicalOperator.OR);
    constraint.addCondition(condition);
    Policy policy1 = tempEntity.newPolicy("policy1", constraint);

    // ADD COMPONENT
    final RepositoryComponent component1 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june1st2020, null, false);

    // CREATE POLICY VIOLATION
    final RepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component1, tempEntity);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINED_PATH).get();

    assertResponseStatus(HttpStatus.OK_200, response);
    ApiPageResult<ApiFirewallComponentDTO> responseDTO = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<ApiPageResult<ApiFirewallComponentDTO>>() { });
    assertThat(responseDTO.getTotal()).isEqualTo(1);
    final ApiFirewallComponentDTO componentDTO1 = responseDTO.getResults().get(0);
    ApiFirewallServiceTest
        .assertRepositoryComponentWithOnePolicyViolation(policyViolation1, componentDTO1, june1st2020, null);
  }

  @Test
  public void testGetQuarantineList_invalid() throws Exception {
    // pageSize < MIN_PAGE
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINED_PATH)
        .query("page", ApiFirewallService.MIN_PAGE - 1)
        .get();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Invalid page: " + (ApiFirewallService.MIN_PAGE - 1) + ". Page shouldn't be lower than 1");

    // pageSize < MIN_PAGE_SIZE
    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINED_PATH)
        .query("pageSize", ApiFirewallService.MIN_PAGE_SIZE - 1)
        .get();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Invalid page size: " + (ApiFirewallService.MIN_PAGE_SIZE - 1) + ". Page size should be between " +
            ApiFirewallService.MIN_PAGE_SIZE + " and " +
            ApiFirewallService.MAX_PAGE_SIZE);

    // pageSize > MAX_PAGE_SIZE
    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINED_PATH)
        .query("pageSize", ApiFirewallService.MAX_PAGE_SIZE + 1)
        .get();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Invalid page size: " + (ApiFirewallService.MAX_PAGE_SIZE + 1) + ". Page size should be between " +
            ApiFirewallService.MIN_PAGE_SIZE + " and " +
            ApiFirewallService.MAX_PAGE_SIZE);
  }

  @Test
  public void testGetQuarantineList_invalidSortField() throws Exception {
    // pageSize < 1
    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.QUARANTINED_PATH)
        .query("sortBy", "INVALID")
        .get();
    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);
    assertThat(response.getBodyText()).isEqualTo("sortBy field is invalid");
  }

  @Test
  public void testSetQuarantinedComponentViewAnonymousAccess() throws Exception {
    assertThat(new QuarantinedComponentAccessDAO().isAnonymousAccessEnabled()).isTrue();

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
            ApiFirewallResource.QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS_SET)
        .parameter(false).put();
    assertResponseStatus(204, response);
    assertThat(new QuarantinedComponentAccessDAO().isAnonymousAccessEnabled()).isFalse();
  }

  @Test
  public void testGetQuarantinedComponentViewAnonymousAccess() throws Exception {
    Assume.assumeFalse("QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS is banned in MTIQ",
        MultiTenantBrainServiceTestService.isTestingAgainstMtiq());
    // This REST endpoint must work with anonymous access (i.e. no authentication required)

    // Sanity check
    assertThat(new QuarantinedComponentAccessDAO().isAnonymousAccessEnabled()).isTrue();

    HttpResponse response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS).anon().get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).hasToString("true");

    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);

    response = restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
        ApiFirewallResource.QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS).anon().get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).hasToString("false");
  }

  @Test
  public void getAllRepositoryManagers() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager("instanceId", "repoName",
        "repoProductName", "repoProductVersion");
    String repositoryManagerId = repositoryManager.getId();

    HttpResponse response =
        restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGERS_PATH).get();
    ApiRepositoryManagerListDTO apiRepositoryManagerListDTO = response.getBody(ApiRepositoryManagerListDTO.class);

    assertResponseStatus(HttpStatus.OK_200, response);
    assertThat(apiRepositoryManagerListDTO.repositoryManagers.size()).isEqualTo(1);
    assertThat(apiRepositoryManagerListDTO.repositoryManagers.get(0).id).isEqualTo(repositoryManagerId);
    assertThat(apiRepositoryManagerListDTO.repositoryManagers.get(0).instanceId).isEqualTo("instanceId");
    assertThat(apiRepositoryManagerListDTO.repositoryManagers.get(0).name).isEqualTo("repoName");
    assertThat(apiRepositoryManagerListDTO.repositoryManagers.get(0).productName).isEqualTo("repoProductName");
    assertThat(apiRepositoryManagerListDTO.repositoryManagers.get(0).productVersion).isEqualTo("repoProductVersion");
  }

  @Test
  public void testGetConfiguredRepositories() throws Exception {
    Date may5th20239AM = Date.from(LocalDateTime.of(2023, 5, 1, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202310AM = Date.from(LocalDateTime.of(2023, 5, 1, 10, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202311AM = Date.from(LocalDateTime.of(2023, 5, 1, 11, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repositoryManager, "testRepoNpm", RepositoryType.proxy, "npm",
            may5th20239AM);
    Repository repository =
            tempEntity.newRepository(repositoryManager, "testRepoMaven", RepositoryType.proxy, "maven", may5th202311AM);

    HttpResponse response = restRequest()
            .path(PublicApiPaths.FIREWALL_RESOURCE_PATH,
                    ApiFirewallResource.REPOSITORIES_CONFIGURATION_PATH)
            .parameter(repositoryManager.getId())
            .query("sinceUtcTimestamp", may5th202310AM.getTime())
            .get();

    assertResponseStatus(HttpStatus.OK_200, response);
    ApiRepositoryListDTO expected = response.getBody(ApiRepositoryListDTO.class);
    assertThat(expected.repositories).hasSize(1);
    ApiRepositoryDTO repositoryDTO = expected.repositories.get(0);
    assertThat(repositoryDTO.repositoryId).isEqualTo(repository.getId());
    assertThat(repositoryDTO.publicId).isEqualTo(repository.getName());
    assertThat(repositoryDTO.format).isEqualTo(repository.getFormat());
    assertThat(repositoryDTO.type).isEqualTo(repository.getRepositoryType().name());
    assertThat(repositoryDTO.auditEnabled).isEqualTo(repository.isAuditEnabled());
    assertThat(repositoryDTO.quarantineEnabled).isEqualTo(repository.isQuarantineEnabled());
    assertThat(repositoryDTO.policyCompliantComponentSelectionEnabled).isEqualTo(
            repository.isPolicyCompliantComponentSelectionEnabled());
    assertThat(repositoryDTO.namespaceConfusionProtectionEnabled).isEqualTo(
            repository.isNamespaceConfusionProtectionEnabled());
  }

  @Test
  public void testConfigureRepositories() throws Exception {
    Repository repository = tempEntity.newRepository();
    repository.setAuditEnabled(!repository.isAuditEnabled());
    ApiRepositoryListDTO dto = new ApiRepositoryListDTO();
    dto.repositories = Collections.singletonList(ApiRepositoryDTO.fromRepository(repository));
    Date date = new Date();

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORIES_CONFIGURATION_PATH)
        .parameter(repository.getRepositoryManagerId())
        .body(dto)
        .post();

    assertResponseStatus(204, response);
    Repository storedRepository = repositoryDAO.getById(repository.getId());
    assertThat(storedRepository).usingRecursiveComparison().ignoringFields(
        ArrayUtils.add(JPA.IGNORE_FIELDS, "lastManualConfigureTime")).isEqualTo(repository);
    assertThat(storedRepository.getLastManualConfigureTime()).isAfterOrEqualTo(date);
  }

  @Test
  public void testEvaluateComponents() throws Exception {
    // Set up the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "hash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = createSecurityVulnerabilities();
    hdsResult.components.add(componentEvaluationData);
    hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
    Policy policy = tempEntity.newPolicy(ROOT_ORGANIZATION_ID);

    ApiRepositoryComponentEvaluationRequestList dto = new ApiRepositoryComponentEvaluationRequestList();
    dto.format = "npm";
    ApiRepositoryComponentEvaluationRequest request = new ApiRepositoryComponentEvaluationRequest();
    request.pathname = "foobar";
    request.hash = "hash";
    dto.components.add(request);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "repoPublicId", false, false);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repository.getRepositoryManagerId(), repository.getId())
        .body(dto)
        .post();

    assertEvaluateSuccess(response, repository, "hash", "foobar", null, policy);
  }

  @Test
  public void testEvaluateComponents_AlternativeDtoNames() throws Exception {
    // Set up the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = "hash";
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    componentEvaluationData.securityVulnerabilities = createSecurityVulnerabilities();
    hdsResult.components.add(componentEvaluationData);
    hdsRespondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
    Policy policy = tempEntity.newPolicy(ROOT_ORGANIZATION_ID);

    ObjectNode dto = JSON.createObjectNode();
    dto.put("format", "maven");
    ArrayNode components = dto.putArray("components");
    ObjectNode component = JSON.createObjectNode();
    component.put("pathname", "foobar");
    component.put("sha1", "hash");
    components.add(component);

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "repoPublicId", false, false);

    HttpResponse response;

    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repository.getRepositoryManagerId(), repository.getId())
        .body(dto)
        .post();
    assertEvaluateSuccess(response, repository, "hash", "foobar", null, policy);

    component.remove("sha1");
    component.put("sonatypeFingerprint", "hash");
    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repository.getRepositoryManagerId(), repository.getId())
        .body(dto)
        .post();
    assertEvaluateSuccess(response, repository, "hash", "foobar", null, policy);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);

    component.remove("pathname");
    component.put("packageUrl", packageUrl);
    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repository.getRepositoryManagerId(), repository.getId())
        .body(dto)
        .post();
    assertEvaluateSuccess(response, repository, "hash", null, packageUrl, policy);

    component.remove("packageUrl");
    component.put("purl", packageUrl);
    response = restRequest()
        .path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repository.getRepositoryManagerId(), repository.getId())
        .body(dto)
        .post();
    assertEvaluateSuccess(response, repository, "hash", null, packageUrl, policy);
  }

  private void assertEvaluateSuccess(
      HttpResponse response,
      Repository repository,
      String hash,
      String pathname,
      String packageUrl,
      Policy policy)
  {
    assertResponseStatus(200, response);
    ApiRepositoryComponentEvaluationResultList dto =
        response.getBody(ApiRepositoryComponentEvaluationResultList.class);
    assertThat(dto).isNotNull();
    assertThat(dto.repositoryManagerId).isEqualTo(repository.getRepositoryManagerId());
    assertThat(dto.repositoryId).isEqualTo(repository.getId());
    assertThat(dto.repositoryPublicId).isEqualTo(repository.getPublicId());
    assertThat(dto.repositoryType).isEqualTo(repository.getRepositoryType().name());
    assertThat(dto.results).hasSize(1);
    ApiRepositoryComponentEvaluationResult componentEvaluationResult = dto.results.get(0);
    assertThat(componentEvaluationResult.component).isNotNull();
    assertThat(componentEvaluationResult.component.hash).isEqualTo(hash);
    assertThat(componentEvaluationResult.component.pathname).isEqualTo(pathname);
    assertThat(componentEvaluationResult.component.packageUrl).isEqualTo(packageUrl);
    assertThat(componentEvaluationResult.quarantined).isFalse();
    assertThat(componentEvaluationResult.quarantineDate).isNull();
    assertThat(componentEvaluationResult.catalogDate).isNull();
    assertThat(componentEvaluationResult.policyViolations).hasSize(1);
    assertThat(componentEvaluationResult.policyViolations.get(0).threatLevel).isEqualTo(5);
    assertThat(componentEvaluationResult.policyViolations.get(0).policyId).isEqualTo(policy.getId());
  }

  private List<SecurityVulnerability> createSecurityVulnerabilities() {
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("refId");
    securityVulnerability.setSeverity(5.0F);
    securityVulnerability.setSource("source");
    securityVulnerability.setUrl("test-url");
    securityVulnerabilities.add(securityVulnerability);
    return securityVulnerabilities;
  }

  private <T> T getBodyByTypeReference(byte[] bodyBytes, final TypeReference<T> typeRef) {
    try {
      return JSON.readValue(bodyBytes, typeRef);
    }
    catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Test
  public void testAddRepositoryManager() throws Exception {
    ApiRepositoryManagerDTO apiRepositoryManagerDTO = new ApiRepositoryManagerDTO();
    apiRepositoryManagerDTO.instanceId = "testInstanceId";
    apiRepositoryManagerDTO.name = "testName";
    apiRepositoryManagerDTO.productName = "testProductName";
    apiRepositoryManagerDTO.productVersion = "testProductVersion";

    HttpResponse response =
        restRequest().path(PublicApiPaths.FIREWALL_RESOURCE_PATH, ApiFirewallResource.REPOSITORY_MANAGERS_PATH)
            .body(apiRepositoryManagerDTO).post();

    assertResponseStatus(200, response);

    // Assert the repository manager data in the response
    apiRepositoryManagerDTO = response.getBody(ApiRepositoryManagerDTO.class);
    assertThat(apiRepositoryManagerDTO.id).isNotNull();
    assertThat(apiRepositoryManagerDTO.instanceId).isEqualTo("testInstanceId");
    assertThat(apiRepositoryManagerDTO.name).isEqualTo("testName");
    assertThat(apiRepositoryManagerDTO.productName).isEqualTo("testProductName");
    assertThat(apiRepositoryManagerDTO.productVersion).isEqualTo("testProductVersion");

    // Assert the repository manager data in the db
    RepositoryManager repositoryManager = new RepositoryManagerDAO().getById(apiRepositoryManagerDTO.id);
    assertThat(repositoryManager.getInstanceId()).isEqualTo("testInstanceId");
    assertThat(repositoryManager.getName()).isEqualTo("testName");
    assertThat(repositoryManager.getProductName()).isEqualTo("testProductName");
    assertThat(repositoryManager.getProductVersion()).isEqualTo("testProductVersion");
  }
}
