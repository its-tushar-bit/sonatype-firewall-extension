/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiPageResult;
import com.sonatype.insight.brain.api.experimental.dto.FirewallConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallResourceTest
    extends AbstractResourceTest
{
  private static final ObjectMapper JSON = new ObjectMapper();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @After
  public void cleanUp() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
  }

  @Before
  public void setup() throws Exception {
    //enable feature flag
    initServer(
        config -> config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true)));
  }

  @Test
  public void testGetFirewallUnquarantineSummary() throws Exception {
    // when GETing unquarantine summary
    HttpResponse response = restRequest().path(
        ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.RELEASE_QUARANTINE_SUMMARY_PATH).get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    ApiFirewallReleaseQuarantineSummaryDTO dto = response.getBody(ApiFirewallReleaseQuarantineSummaryDTO.class);
    assertThat(dto.autoReleaseQuarantineCountMTD).isZero();
  }

  @Test
  public void testGetFirewallConfiguration() throws Exception {
    // when GETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH).get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    FirewallConfigurationDTO firewallConfigurationDTO = response.getBody(FirewallConfigurationDTO.class);
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isFalse();
  }

  @Test
  public void testSetFirewallConfiguration_Enabled() throws Exception {
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = true;

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH)
            .body(firewallConfigurationDTO).put();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    firewallConfigurationDTO = response.getBody(FirewallConfigurationDTO.class);
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isTrue();
  }

  @Test
  public void testSetFirewallConfiguration_Disabled() throws Exception {
    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled = false;

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH)
            .body(firewallConfigurationDTO).put();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    firewallConfigurationDTO = response.getBody(FirewallConfigurationDTO.class);
    assertThat(firewallConfigurationDTO.autoUnquarantineEnabled).isFalse();
  }

  @Test
  public void testGetFirewallConfiguration_FeatureFlagDisabled() throws Exception {
    //disable feature flag
    initServer(
        config -> config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false)));

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH).get();

    // then result is bad request 400
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void testSetFirewallConfiguration_FeatureFlagDisabled() throws Exception {
    //disable feature flag
    initServer(
        config -> config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false)));

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH)
            .body(new FirewallConfigurationDTO()).put();

    // then result is bad request 400
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void testGetFirewallConfiguration_MissingLicensedFeature() throws Exception {
    // setup remove firewall feature
    getTestProductLicenseManager().setFeatures(LicensedFeature.FIREWALL);

    // when GETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH).get();

    // then result is payment required 402
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED_402);
  }

  @Test
  public void testSetFirewallConfiguration_MissingLicensedFeature() throws Exception {
    // setup remove firewall feature
    getTestProductLicenseManager().setFeatures(LicensedFeature.FIREWALL);

    // when SETing config
    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.CONFIGURATION_PATH)
            .body(new FirewallConfigurationDTO()).put();

    // then result is payment required 402
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED_402);
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig() throws Exception {
    // when GETing config
    HttpResponse response = restRequest().path(ApiFirewallResource.RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    List<ApiFirewallReleaseQuarantineConfigDTO> dtos = response.getBody(List.class);
    assertThat(dtos.size()).isGreaterThan(0);
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig_FeatureFlagDisabled() throws Exception {
    //disable feature flag
    initServer(
        config -> config.setExperimentalFeatures(ImmutableMap.of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false)));

    // when SETing config
    HttpResponse response = restRequest().path(ApiFirewallResource.RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).get();

    // then result is bad request 400
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
  }

  @Test
  public void testGetFirewallAutoUnquarantineConfig_MissingLicensedFeature() throws Exception {
    // setup remove firewall feature
    getTestProductLicenseManager().setFeatures(LicensedFeature.FIREWALL);

    // when GETing config
    HttpResponse response = restRequest().path(ApiFirewallResource.RESOURCE_PATH,
        ApiFirewallResource.RELEASE_QUARANTINE_CONFIGURATION_PATH).get();

    // then result is payment required 402
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED_402);
  }

  @Test
  public void testGetQuarantineSummary() throws Exception {
    Repository repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    tempEntity.newRepositoryComponent(repo, "hash");
    tempEntity.newRepositoryComponent(repo.getId(), "path", new Date(), null);
    tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo2", true, false);

    HttpResponse response =
        restRequest().path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.QUARANTINE_SUMMARY_PATH).get();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);
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

    // CREATE POLICY VIOLATION
    final RepositoryPolicyViolation policyViolation1 = PolicyViolationTestHelper
        .createPolicyViolationFail(policy1, component1, tempEntity);

    HttpResponse response = restRequest()
        .path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("page", 1)
        .query("pageSize", 2)
        .query("policyId", policy1.getId())
        .query("sortBy", FirewallSortableField.UNQUARANTINE_TIME)
        .query("asc", "false")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);
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
        .path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH).get();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);
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
        .path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("page", ApiFirewallService.MIN_PAGE - 1)
        .query("pageSize", 2)
        .query("policyId", "policy_id")
        .query("sortBy", FirewallSortableField.UNQUARANTINE_TIME)
        .query("asc", "false")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    assertThat(response.getBodyText()).isEqualTo("Invalid page: " + (ApiFirewallService.MIN_PAGE - 1));

    // pageSize < MIN_PAGE_SIZE
    response = restRequest()
        .path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("page", 1)
        .query("pageSize", ApiFirewallService.MIN_PAGE_SIZE - 1)
        .query("policyId", "policy_id")
        .query("sortBy", FirewallSortableField.UNQUARANTINE_TIME)
        .query("asc", "false")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    assertThat(response.getBodyText()).isEqualTo("Invalid page size: " + (ApiFirewallService.MIN_PAGE_SIZE - 1));

    // pageSize > MAX_PAGE_SIZE
    response = restRequest()
        .path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("page", 1)
        .query("pageSize", ApiFirewallService.MAX_PAGE_SIZE + 1)
        .query("policyId", "policy_id")
        .query("sortBy", FirewallSortableField.UNQUARANTINE_TIME)
        .query("asc", "false")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    assertThat(response.getBodyText()).isEqualTo("Invalid page size: " + (ApiFirewallService.MAX_PAGE_SIZE + 1));
  }

  @Test
  public void testGetUnquarantineList_invalidSortField() throws Exception {
    // pageSize < 1
    HttpResponse response = restRequest()
        .path(ApiFirewallResource.RESOURCE_PATH, ApiFirewallResource.UNQUARANTINE_PATH)
        .query("page", 1)
        .query("pageSize", 2)
        .query("policyId", "policy_id")
        .query("sortBy", "INVALID")
        .query("asc", "false")
        .get();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);
    assertThat(response.getBodyText()).isEqualTo("sortBy field is invalid");
  }

  private <T> T getBodyByTypeReference(byte[] bodyBytes, final TypeReference<T> typeRef) {
    try {
      return JSON.readValue(bodyBytes, typeRef);
    }
    catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
