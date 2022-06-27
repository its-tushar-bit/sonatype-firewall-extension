/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuarantinedComponentResourceTest
    extends AbstractResourceTest
{
  private Repository repository;

  private RepositoryManager repositoryManager;

  @Before
  public void setup() {
    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
  }

  @Test
  public void testGetQuarantinedComponent_AnonymousEnabled() throws Exception {
    // setup
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when anonymous request
    final HttpResponse response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH, QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH)
        .parameter(encodedToken).anon().get();

    // then
    assertResponseStatus(200, response);
    QuarantinedComponentDto quarantinedComponentDto = response.getBody(QuarantinedComponentDto.class);
    assertThat(quarantinedComponentDto).isNotNull();
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo(repositoryComponent.getId());
  }

  @Test
  public void testGetQuarantinedComponent_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when anonymous request
    HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter(encodedToken).anon().get();
    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH, QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH)
        .parameter(encodedToken).get();

    // then success
    assertResponseStatus(200, response);
    QuarantinedComponentDto quarantinedComponentDto = response.getBody(QuarantinedComponentDto.class);
    assertThat(quarantinedComponentDto).isNotNull();
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo(repositoryComponent.getId());
  }

  @Test
  public void testGetQuarantinedComponent_invalidToken() throws Exception {
    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter("token").get();

    // then
    assertResponseStatus(Status.BAD_REQUEST.getStatusCode(), response);
    assertThat(response.getBodyText())
        .isEqualTo("The quarantined component view cannot be retrieved because the URL contains invalid characters.");
  }

  @Test
  public void testGetQuarantinedComponent_expiredToken() throws Exception {
    // setup
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(),
            DateUtils.addDays(new Date(), -3));
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter(encodedToken).get();

    // then
    assertResponseStatus(Status.NOT_FOUND.getStatusCode(), response);
    assertThat(response.getBodyText()).startsWith("This report expired on ")
        .endsWith("You may generate a new report by requesting the blocked component again.");
  }

  @Test
  public void testGetQuarantinedComponent_tokenDoesNotExist() throws Exception {
    //setup
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("token".getBytes(StandardCharsets.UTF_8));

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter(encodedToken).get();

    // then
    assertResponseStatus(Status.NOT_FOUND.getStatusCode(), response);
    assertThat(response.getBodyText()).isEqualTo(
        "The quarantined component view for the blocked component you are trying to view could not be found.");
  }

  @Test
  public void testGetQuarantinedComponentOverview_AnonymousEnabled() throws Exception {
    // setup
    Date date = new Date();

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2");
    final String encodedToken = setupTestData(componentIdentifier, date);

    // when anonymous request
    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(new QuarantinedComponentAccessDAO());
    Date expirationTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH).parameter(encodedToken).anon().get();

    // then
    assertResponseStatus(200, response);
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        response.getBody(QuarantinedComponentOverviewDto.class);
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("com.lingocoder : abi.cli : 0.5.2");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(true);
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(1);
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo("repositoryPublicId");
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.cataloguedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.componentVersion).isEqualTo("0.5.2");
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).hasToString(expirationTime.toString());
  }

  @Test
  public void testGetQuarantinedComponentOverview_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2");
    String encodedToken = setupTestData(componentIdentifier, date);

    // when anonymous request
    HttpResponse response = restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
        QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH).parameter(encodedToken).anon().get();
    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(new QuarantinedComponentAccessDAO());
    Date expirationTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    // when authenticated request
    response = restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
        QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH).parameter(encodedToken).get();

    // then success
    assertResponseStatus(200, response);
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        response.getBody(QuarantinedComponentOverviewDto.class);
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("com.lingocoder : abi.cli : 0.5.2");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(true);
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(1);
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo("repositoryPublicId");
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.cataloguedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.componentVersion).isEqualTo("0.5.2");
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).isEqualTo(expirationTime);
  }

  @Test
  public void testGetQuarantinedComponentOverview_componentIdentifierDoesNotExist() throws Exception {
    // setup
    Date date = new Date();
    final String encodedToken = setupTestData(null, date);

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH).parameter(encodedToken).get();

    // then
    assertResponseStatus(Status.BAD_REQUEST.getStatusCode(), response);
    assertThat(response.getBodyText())
        .isEqualTo("The component identifier for the requested component does not exist.");
  }

  @Test
  public void testGetQuarantinedComponentVersionRemediation_AnonymousEnabled() throws Exception {
    // setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);

    ComponentDetails componentDetail = new ComponentDetails();
    componentDetail.setComponentIdentifier(componentIdentifier);

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    dependenciesMap.put(packageUrlIdentifier, Collections.singleton(packageUrlIdentifier));

    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    detailsMap.put(packageUrlIdentifier, componentDetail);

    List<ComponentDetails> componentDetails = new ArrayList<>();
    componentDetails.add(componentDetail);

    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(componentDetails);

    ComponentDependenciesDTO componentDependenciesDTO = new ComponentDependenciesDTO(dependenciesMap, detailsMap);

    hdsRespondWith(componentDetailsList).atUri("/rest/ci/componentDetails/list");
    hdsRespondWith(componentDependenciesDTO).atUri("/rest/component/dependencies");

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_REMEDIATION_PATH).parameter(encodedToken).anon()
            .get();

    // then
    assertResponseStatus(200, response);
    ComponentVersionInfoDTO componentVersionInfoDTO = response.getBody(ComponentVersionInfoDTO.class);
    assertThat(componentVersionInfoDTO).isNotNull();
    assertThat(componentVersionInfoDTO.allVersions).isNotEmpty();
    assertThat(componentVersionInfoDTO.remediation).isNotNull();
  }

  @Test
  public void testGetQuarantinedComponentVersionRemediation_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    Repository repository = tempEntity.newRepository("repo");
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);

    ComponentDetails componentDetail = new ComponentDetails();
    componentDetail.setComponentIdentifier(componentIdentifier);

    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    dependenciesMap.put(packageUrlIdentifier, Collections.singleton(packageUrlIdentifier));

    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    detailsMap.put(packageUrlIdentifier, componentDetail);

    List<ComponentDetails> componentDetails = new ArrayList<>();
    componentDetails.add(componentDetail);

    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(componentDetails);

    ComponentDependenciesDTO componentDependenciesDTO = new ComponentDependenciesDTO(dependenciesMap, detailsMap);

    hdsRespondWith(componentDetailsList).atUri("/rest/ci/componentDetails/list");
    hdsRespondWith(componentDependenciesDTO).atUri("/rest/component/dependencies");

    // when anonymous request
    HttpResponse response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_REMEDIATION_PATH)
        .parameter(encodedToken).anon().get();

    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_REMEDIATION_PATH)
        .parameter(encodedToken).get();

    // then success
    assertResponseStatus(200, response);
    ComponentVersionInfoDTO componentVersionInfoDTO = response.getBody(ComponentVersionInfoDTO.class);
    assertThat(componentVersionInfoDTO).isNotNull();
    assertThat(componentVersionInfoDTO.allVersions).isNotEmpty();
    assertThat(componentVersionInfoDTO.remediation).isNotNull();
  }

  private String setupTestData(ComponentIdentifier componentIdentifier, Date date) {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar",
            "hash", componentIdentifier, date, date);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.3"),
        date, null);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), date);
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_AnonymousEnabled() throws Exception {
    // setup
    Date date = new Date();
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "path", date, null);
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact =
        new ConstraintFact(UUID.randomUUID().toString(), "constraintName", "and");
    ConditionFact conditionFact = new ConditionFact(LicenseThreatGroupConditionType.ID,
        0, "some summary", "some reason");
    conditionFact.setTriggerJson("some trigger");
    constraintFact.addConditionFact(conditionFact);
    constraintFacts.add(constraintFact);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent.getPathname(),
            "hash", constraintFacts, false, "fail", "policyid", "policyname",
            repositoryComponent.getComponentIdentifier(), date, null, null,
            null);
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when anonymous request
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH).parameter(encodedToken).anon()
            .get();

    // then
    assertResponseStatus(200, response);

    RepositoryPolicyThreatDTO dto = response.getBody(RepositoryPolicyThreatDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.activePolicyViolations).hasSize(1);

    RepositoryPolicyViolationDTO policyViolationDTO = dto.activePolicyViolations.get(0);
    assertThat(policyViolationDTO.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(policyViolationDTO.policyThreatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    assertThat(policyViolationDTO.policyName).isEqualTo(repositoryPolicyViolation.getPolicyName());
    assertThat(policyViolationDTO.blocksUnquarantine).isEqualTo(true);

    assertThat(policyViolationDTO.constraints.get(0).constraintId).isEqualTo(constraintFact.getConstraintId());
    assertThat(policyViolationDTO.constraints.get(0).constraintName).isEqualTo(constraintFact.getConstraintName());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionReason).isEqualTo(
        conditionFact.getReason());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionSummary).isEqualTo(
        conditionFact.getSummary());
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    Date date = new Date();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "path", date, null);
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName", "and");
    ConditionFact conditionFact =
        new ConditionFact(LicenseThreatGroupConditionType.ID, 0, "some summary", "some reason");
    conditionFact.setTriggerJson("some trigger");
    constraintFact.addConditionFact(conditionFact);
    constraintFacts.add(constraintFact);
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId(), 5,
        repositoryComponent.getPathname(), "hash", constraintFacts, false, "fail", "policyid", "policyname",
        repositoryComponent.getComponentIdentifier(), date, null, null, null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when anonymous request
    HttpResponse response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH)
        .parameter(encodedToken).anon().get();
    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH)
        .parameter(encodedToken).get();

    // then success
    assertResponseStatus(200, response);
    RepositoryPolicyThreatDTO dto = response.getBody(RepositoryPolicyThreatDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.activePolicyViolations).hasSize(1);

    RepositoryPolicyViolationDTO policyViolationDTO = dto.activePolicyViolations.get(0);
    assertThat(policyViolationDTO.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(policyViolationDTO.policyThreatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    assertThat(policyViolationDTO.policyName).isEqualTo(repositoryPolicyViolation.getPolicyName());
    assertThat(policyViolationDTO.blocksUnquarantine).isEqualTo(true);

    assertThat(policyViolationDTO.constraints.get(0).constraintId).isEqualTo(constraintFact.getConstraintId());
    assertThat(policyViolationDTO.constraints.get(0).constraintName).isEqualTo(constraintFact.getConstraintName());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionReason)
        .isEqualTo(conditionFact.getReason());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionSummary)
        .isEqualTo(conditionFact.getSummary());
  }

  @Test
  public void testGetQuarantinedComponentPolicyViolations_policyViolationsDoesNotExist() throws Exception {
    // setup
    Date date = new Date();
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "path", date, null);
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact =
        new ConstraintFact(UUID.randomUUID().toString(), "constraintName", "and");
    ConditionFact conditionFact = new ConditionFact(LicenseThreatGroupConditionType.ID,
        0, "some summary", "some reason");
    conditionFact.setTriggerJson("some trigger");
    constraintFact.addConditionFact(conditionFact);
    constraintFacts.add(constraintFact);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent.getPathname(),
        "hash", constraintFacts, true, "fail", "policyid", "policyname",
        repositoryComponent.getComponentIdentifier(), date, null, null,
        null);
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH).parameter(encodedToken).get();

    // then
    assertResponseStatus(200, response);
    RepositoryPolicyThreatDTO dto = response.getBody(RepositoryPolicyThreatDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.activePolicyViolations).isEmpty();
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_AnonymousEnabled() throws Exception {
    // setup
    Date date = new Date();
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.1.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1"),
            date, new DateTime(date).minusDays(1).toDate(), date);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.2.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2"),
        date, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.3"),
        date, date, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.4.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.4"),
        date, new DateTime(date).minusDays(1).toDate(), date);

    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when anonymous request
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH).parameter(encodedToken).anon()
            .get();

    // then
    assertResponseStatus(200, response);
    ApiPageResult<String> responseDTO = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<ApiPageResult<String>>() { });
    assertThat(responseDTO.getTotal()).isEqualTo(2);
    assertThat(responseDTO.getPage()).isEqualTo(1);
    assertThat(responseDTO.getPageSize()).isEqualTo(5);
    assertThat(responseDTO.getPageCount()).isEqualTo(1);
    assertThat(responseDTO.getResults()).hasSize(2);
    List<String> resultList = Arrays.asList("com.lingocoder : abi.cli : 0.5.2",
        "com.lingocoder : abi.cli : 0.5.4");
    assertThat(responseDTO.getResults()).isEqualTo(resultList);
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    Date date = new Date();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.1.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1"), date,
        new DateTime(date).minusDays(1).toDate(), date);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.2.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2"), date, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.3"), date, date, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.4.jar", "hash",
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.4"), date,
        new DateTime(date).minusDays(1).toDate(), date);

    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when anonymous request
    HttpResponse response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH)
        .parameter(encodedToken).anon().get();
    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
        QuarantinedComponentResource.QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH).parameter(encodedToken).get();

    // then success
    assertResponseStatus(200, response);
    ApiPageResult<String> responseDTO =
        getBodyByTypeReference(response.getBodyBytes(), new TypeReference<ApiPageResult<String>>()
        {
        });
    assertThat(responseDTO.getTotal()).isEqualTo(2);
    assertThat(responseDTO.getPage()).isEqualTo(1);
    assertThat(responseDTO.getPageSize()).isEqualTo(5);
    assertThat(responseDTO.getPageCount()).isEqualTo(1);
    assertThat(responseDTO.getResults()).hasSize(2);
    List<String> resultList = Arrays.asList("com.lingocoder : abi.cli : 0.5.2", "com.lingocoder : abi.cli : 0.5.4");
    assertThat(responseDTO.getResults()).isEqualTo(resultList);
  }

  @Test
  public void testGetQuarantinedComponentOtherVersions_otherVersionsDoesNotExist() throws Exception {
    // setup
    Date date = new Date();
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "com/lingocoder/abi.cli/0.5.1/abi.cli-0.5.1.jar",
            new DateTime(date).minusDays(1).toDate(), date);
    tempEntity.newRepositoryComponent(repository.getId(),
        "org/apache/maven/plugins/maven-resources-plugin/3.2.0/maven-resources-plugin-3.2.0.jar", null, null);

    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH).parameter(encodedToken)
            .query("page", 1)
            .query("pageSize", 2)
            .query("asc", "false")
            .get();

    // then
    assertResponseStatus(200, response);
    ApiPageResult<String> responseDTO = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<ApiPageResult<String>>() { });
    assertThat(responseDTO.getTotal()).isZero();
    assertThat(responseDTO.getPage()).isEqualTo(1);
    assertThat(responseDTO.getPageSize()).isEqualTo(2);
    assertThat(responseDTO.getPageCount()).isZero();
    assertThat(responseDTO.getResults()).isEmpty();
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails() throws Exception {
    // setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "testPathname", "testHash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), true /* quarantined */);
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(repositoryComponent.getComponentIdentifier());

    hdsRespondWith(namedComponentDetails).atUri("/rest/ci/componentDetails");

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_DETAILS_PATH).parameter(encodedToken).get();

    // then
    assertResponseStatus(200, response);
    // Have to configure an object mapper this way because of how NamedComponentDetails works.
    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    NamedComponentDetails namedComponentDetailsResponse =
        objectMapper.readValue(response.getBodyStream(), NamedComponentDetails.class);

    assertThat(namedComponentDetailsResponse.getHash()).isEqualTo(repositoryComponent.getHash());
    assertThat(namedComponentDetailsResponse.getComponentIdentifier())
        .isEqualTo(repositoryComponent.getComponentIdentifier());
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_AnonymousEnabled() throws Exception {
    // setup
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "testPathname", "testHash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), true /* quarantined */);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(repositoryComponent.getComponentIdentifier());

    hdsRespondWith(namedComponentDetails).atUri("/rest/ci/componentDetails");

    // when anonymous request
    HttpResponse response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_DETAILS_PATH)
        .parameter(encodedToken).query("version", "v").anon().get();

    // then
    assertResponseStatus(200, response);
    // Have to configure an object mapper this way because of how NamedComponentDetails works.
    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    NamedComponentDetails namedComponentDetailsResponse =
        objectMapper.readValue(response.getBodyStream(), NamedComponentDetails.class);
    assertThat(namedComponentDetailsResponse.getHash()).isEqualTo(repositoryComponent.getHash());
    assertThat(namedComponentDetailsResponse.getComponentIdentifier())
        .isEqualTo(repositoryComponent.getComponentIdentifier());
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "testPathname", "testHash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), true /* quarantined */);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(repositoryComponent.getComponentIdentifier());

    hdsRespondWith(namedComponentDetails).atUri("/rest/ci/componentDetails");

    // when anonymous request
    HttpResponse response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_DETAILS_PATH)
        .parameter(encodedToken).query("version", "v").anon().get();

    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_DETAILS_PATH)
        .parameter(encodedToken).query("version", "v").get();

    // then success
    assertResponseStatus(200, response);
    // Have to configure an object mapper this way because of how NamedComponentDetails works.
    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    NamedComponentDetails namedComponentDetailsResponse =
        objectMapper.readValue(response.getBodyStream(), NamedComponentDetails.class);
    assertThat(namedComponentDetailsResponse.getHash()).isEqualTo(repositoryComponent.getHash());
    assertThat(namedComponentDetailsResponse.getComponentIdentifier())
        .isEqualTo(repositoryComponent.getComponentIdentifier());
  }

  @Test
  public void testGetQuarantinedComponentVersionDetails_DifferentVersion() throws Exception {
    // setup
    Repository repository = tempEntity.newRepository("repo");
    RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "testPathname", "testHash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), true /* quarantined */);
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));
    String otherVersion = "otherVersion";
    ComponentIdentifier otherVersionComponentIdentifier =
        repositoryComponent.getComponentIdentifier().createAlternativeVersion(otherVersion);
    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(otherVersionComponentIdentifier);

    hdsRespondWith(namedComponentDetails).atUri("/rest/ci/componentDetails");

    // when
    HttpResponse response = restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_DETAILS_PATH)
        .parameter(encodedToken).query("version", otherVersion).get();

    // then
    assertResponseStatus(200, response);
    // Have to configure an object mapper this way because of how NamedComponentDetails works.
    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    NamedComponentDetails namedComponentDetailsResponse =
        objectMapper.readValue(response.getBodyStream(), NamedComponentDetails.class);

    assertThat(namedComponentDetailsResponse.getHash()).isNull();
    assertThat(namedComponentDetailsResponse.getComponentIdentifier())
        .isEqualTo(otherVersionComponentIdentifier);
  }

  private <T> T getBodyByTypeReference(byte[] bodyBytes, final TypeReference<T> typeRef) {
    try {
      return new ObjectMapper().readValue(bodyBytes, typeRef);
    }
    catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private void disableAnonymousAccess() {
    new QuarantinedComponentAccessDAO().setAnonymousAccess(false);
  }
}

