/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentCategory;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.ProxyRepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.repository.component.QuarantinedComponentDto;
import com.sonatype.insight.brain.repository.component.QuarantinedComponentOverviewDto;
import com.sonatype.insight.brain.repository.component.QuarantinedComponentResource;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresQuarantinedComponentResourceTest
{
  private IqTestContext ctx;

  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private Repository repository;

  private Configuration configuration;

  @BeforeEach
  void setup() {
    quarantinedComponentAccessDAO = ctx.lookup(QuarantinedComponentAccessDAO.class);
    repository = ctx.tempEntity().newRepository();
    configuration = ctx.lookup(Configuration.class);
  }

  @Test
  void testGetQuarantinedComponent_AnonymousEnabled() throws Exception {
    // setup
    final ProxyRepositoryComponent proxyRepositoryComponent =
        ctx.tempEntity().newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    final String encodedToken = encodeToken(quarantinedComponentAccess);

    // when anonymous request
    final HttpResponse response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH, QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH)
        .parameter(encodedToken)
        .anon()
        .get();

    // then
    ctx.assertResponseStatus(200, response);
    QuarantinedComponentDto quarantinedComponentDto = response.getBody(QuarantinedComponentDto.class);
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo(proxyRepositoryComponent.getId());
  }

  @Test
  void testGetQuarantinedComponent_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity().newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when anonymous request
    HttpResponse response =
        ctx.restRequest()
            .path(QuarantinedComponentResource.RESOURCE_PATH,
                QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH)
            .parameter(encodedToken)
            .anon()
            .get();
    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH, QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH)
        .parameter(encodedToken)
        .get();

    // then success
    ctx.assertResponseStatus(200, response);
    QuarantinedComponentDto quarantinedComponentDto = response.getBody(QuarantinedComponentDto.class);
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo(proxyRepositoryComponent.getId());
  }

  @Test
  void testGetQuarantinedComponentOverview_AnonymousEnabled() throws Exception {
    // setup
    Date date = new Date();

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2", null /* classifier */, "jar");
    final String encodedToken = setupTestData(componentIdentifier, date);

    // when anonymous request
    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(quarantinedComponentAccessDAO, configuration);
    Date expirationTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    // when
    final HttpResponse response =
        ctx.restRequest()
            .path(QuarantinedComponentResource.RESOURCE_PATH,
                QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH)
            .parameter(encodedToken)
            .anon()
            .get();

    // then
    ctx.assertResponseStatus(200, response);
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        response.getBody(QuarantinedComponentOverviewDto.class);
    assertThat(quarantinedComponentOverviewDto.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(
            ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    assertThat(quarantinedComponentOverviewDto.componentHash).isEqualTo("testHash");
    assertThat(quarantinedComponentOverviewDto.matchState).isEqualTo(MatchState.EXACT.toString());
    assertThat(quarantinedComponentOverviewDto.pathname).isEqualTo("com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar");
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("com.lingocoder : abi.cli : 0.5.2");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isTrue();
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(1);
    assertThat(quarantinedComponentOverviewDto.repositoryId).isNotNull();
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo("repositoryPublicId");
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.componentVersion).isEqualTo("0.5.2");
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).isEqualTo(expirationTime);
  }

  @Test
  void testGetQuarantinedComponentOverview_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2", null /* classifier */, "jar");
    String encodedToken = setupTestData(componentIdentifier, date);

    // when anonymous request
    HttpResponse response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH)
        .parameter(encodedToken)
        .anon()
        .get();
    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(quarantinedComponentAccessDAO, configuration);
    Date expirationTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    // when authenticated request
    response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH)
        .parameter(encodedToken)
        .get();

    // then success
    ctx.assertResponseStatus(200, response);
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        response.getBody(QuarantinedComponentOverviewDto.class);
    assertThat(quarantinedComponentOverviewDto.componentIdentifier).usingRecursiveComparison()
        .isEqualTo(
            ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    assertThat(quarantinedComponentOverviewDto.componentHash).isEqualTo("testHash");
    assertThat(quarantinedComponentOverviewDto.matchState).isEqualTo(MatchState.EXACT.toString());
    assertThat(quarantinedComponentOverviewDto.pathname).isEqualTo("com/lingocoder/abi.cli/0.5.2/abi.cli-0.5.2.jar");
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("com.lingocoder : abi.cli : 0.5.2");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isTrue();
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(1);
    assertThat(quarantinedComponentOverviewDto.repositoryId).isNotNull();
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo("repositoryPublicId");
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.componentVersion).isEqualTo("0.5.2");
    assertThat(quarantinedComponentOverviewDto.tokenExpiryTime).isEqualTo(expirationTime);
  }

  @Test
  void testGetQuarantinedComponentVersionRemediation_AnonymousEnabled() throws Exception {
    // setup
    final Repository repository = ctx.tempEntity().newRepository("repo");
    final ProxyRepositoryComponent proxyRepositoryComponent =
        ctx.tempEntity().newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    final String encodedToken = encodeToken(quarantinedComponentAccess);

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

    ctx.hdsRespondWith(componentDetailsList).atUri("/rest/ci/componentDetails/list");
    ctx.hdsRespondWith(componentDependenciesDTO).atUri("/rest/component/dependencies");

    // when
    final HttpResponse response =
        ctx.restRequest()
            .path(QuarantinedComponentResource.RESOURCE_PATH,
                QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_REMEDIATION_PATH)
            .parameter(encodedToken)
            .anon()
            .get();

    // then
    ctx.assertResponseStatus(200, response);
    ComponentVersionInfoDTO componentVersionInfoDTO = response.getBody(ComponentVersionInfoDTO.class);
    assertThat(componentVersionInfoDTO).isNotNull();
    assertThat(componentVersionInfoDTO.allVersions).isNotEmpty();
    assertThat(componentVersionInfoDTO.remediation).isNotNull();
  }

  @Test
  void testGetQuarantinedComponentVersionRemediation_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    Repository repository = ctx.tempEntity().newRepository("repo");
    ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity().newRepositoryComponent(repository.getId());
    QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

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

    ctx.hdsRespondWith(componentDetailsList).atUri("/rest/ci/componentDetails/list");
    ctx.hdsRespondWith(componentDependenciesDTO).atUri("/rest/component/dependencies");

    // when anonymous request
    HttpResponse response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_REMEDIATION_PATH)
        .parameter(encodedToken)
        .anon()
        .get();

    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_REMEDIATION_PATH)
        .parameter(encodedToken)
        .get();

    // then success
    ctx.assertResponseStatus(200, response);
    ComponentVersionInfoDTO componentVersionInfoDTO = response.getBody(ComponentVersionInfoDTO.class);
    assertThat(componentVersionInfoDTO).isNotNull();
    assertThat(componentVersionInfoDTO.allVersions).isNotEmpty();
    assertThat(componentVersionInfoDTO.remediation).isNotNull();
  }

  private String setupTestData(ComponentIdentifier componentIdentifier, Date date) {
    final RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    final Repository repository = ctx.tempEntity().newRepository(repositoryManager, "repositoryPublicId");
    String hash = "testHash";
    String pathname = "testPathname";
    if (componentIdentifier != null) {
      pathname = componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID).replace(".", "/") + "/"
          + componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + "/"
          + componentIdentifier.get(ComponentIdentifier.VERSION) + "/"
          + componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID) + "-"
          + componentIdentifier.get(ComponentIdentifier.VERSION) + "."
          + componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION);
    }
    final ProxyRepositoryComponent proxyRepositoryComponent = ctx.tempEntity()
        .newRepositoryComponent(
            repository.getId(),
            MatchState.EXACT, pathname, hash, componentIdentifier, date, date);

    ctx.tempEntity()
        .newRepositoryPolicyViolation(repository.getId(), 6,
            proxyRepositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
            proxyRepositoryComponent.getComponentIdentifier(), date);
    final QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId(), date);
    return encodeToken(quarantinedComponentAccess);
  }

  @Test
  void testGetQuarantinedComponentPolicyViolations_AnonymousEnabled() throws Exception {
    // setup
    Date date = new Date();
    final ProxyRepositoryComponent proxyRepositoryComponent =
        ctx.tempEntity().newRepositoryComponent(repository.getId(), "path", date, null);
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact =
        new ConstraintFact(UUID.randomUUID().toString(), "constraintName", "and");
    ConditionFact conditionFact = new ConditionFact(LicenseThreatGroupConditionType.ID,
        0, "some summary", "some reason");
    conditionFact.setTriggerJson("some trigger");
    constraintFact.addConditionFact(conditionFact);
    constraintFacts.add(constraintFact);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity()
            .newRepositoryPolicyViolation(repository.getId(), 5, proxyRepositoryComponent.getPathname(),
                "hash", constraintFacts, false /* isWaived */, Action.ID_FAIL, "policyid", "policyname",
                proxyRepositoryComponent.getComponentIdentifier(), date, null, null,
                null);
    final QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    final String encodedToken = encodeToken(quarantinedComponentAccess);

    // when anonymous request
    final HttpResponse response =
        ctx.restRequest()
            .path(QuarantinedComponentResource.RESOURCE_PATH,
                QuarantinedComponentResource.QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH)
            .parameter(encodedToken)
            .anon()
            .get();

    // then
    ctx.assertResponseStatus(200, response);

    ProxyRepositoryPolicyViolationDTO[] repositoryPolicyViolationDTOs =
        response.getBody(ProxyRepositoryPolicyViolationDTO[].class);
    assertThat(repositoryPolicyViolationDTOs).hasSize(1);

    ProxyRepositoryPolicyViolationDTO policyViolationDTO = repositoryPolicyViolationDTOs[0];
    assertThat(policyViolationDTO.policyViolationId).isEqualTo(proxyRepositoryPolicyViolation.getId());
    assertThat(policyViolationDTO.policyId).isEqualTo(proxyRepositoryPolicyViolation.getPolicyId());
    assertThat(policyViolationDTO.policyName).isEqualTo(proxyRepositoryPolicyViolation.getPolicyName());
    assertThat(policyViolationDTO.policyOwner.ownerId).isNull();
    assertThat(policyViolationDTO.policyOwner.ownerName).isNull();
    assertThat(policyViolationDTO.policyOwner.ownerType).isNull();
    assertThat(policyViolationDTO.policyThreatLevel).isEqualTo(proxyRepositoryPolicyViolation.getThreatLevel());
    assertThat(policyViolationDTO.policyThreatCategory).isEqualTo(proxyRepositoryPolicyViolation.getThreatCategory());

    assertThat(policyViolationDTO.constraints).hasSize(1);
    assertThat(policyViolationDTO.constraints.get(0).constraintId).isEqualTo(constraintFact.getConstraintId());
    assertThat(policyViolationDTO.constraints.get(0).constraintName).isEqualTo(constraintFact.getConstraintName());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionReason).isEqualTo(
        conditionFact.getReason());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionSummary).isEqualTo(
        conditionFact.getSummary());
    assertThat(policyViolationDTO.constraintFactsJson)
        .isEqualTo(proxyRepositoryPolicyViolation.getConstraintFactsJson());

    assertThat(policyViolationDTO.waived).isEqualTo(proxyRepositoryPolicyViolation.isWaived());
    assertThat(policyViolationDTO.policyActionTypeId).isEqualTo(Action.ID_FAIL);
    assertThat(policyViolationDTO.lastReported).isEqualTo(proxyRepositoryPolicyViolation.getTime());
  }

  @Test
  void testGetQuarantinedComponentPolicyViolations_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    Date date = new Date();
    ProxyRepositoryComponent proxyRepositoryComponent =
        ctx.tempEntity().newRepositoryComponent(repository.getId(), "path", date, null);
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName", "and");
    ConditionFact conditionFact =
        new ConditionFact(LicenseThreatGroupConditionType.ID, 0, "some summary", "some reason");
    conditionFact.setTriggerJson("some trigger");
    constraintFact.addConditionFact(conditionFact);
    constraintFacts.add(constraintFact);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity()
            .newRepositoryPolicyViolation(repository.getId(), 5,
                proxyRepositoryComponent.getPathname(), "hash", constraintFacts, false /* isWaived */, Action.ID_FAIL,
                "policyid",
                "policyname", proxyRepositoryComponent.getComponentIdentifier(), date, null, null, null);
    QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when anonymous request
    HttpResponse response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH)
        .parameter(encodedToken)
        .anon()
        .get();
    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH)
        .parameter(encodedToken)
        .get();

    // then success
    ctx.assertResponseStatus(200, response);
    ProxyRepositoryPolicyViolationDTO[] repositoryPolicyViolationDTOs =
        response.getBody(ProxyRepositoryPolicyViolationDTO[].class);
    assertThat(repositoryPolicyViolationDTOs).hasSize(1);

    ProxyRepositoryPolicyViolationDTO policyViolationDTO = repositoryPolicyViolationDTOs[0];
    assertThat(policyViolationDTO.policyViolationId).isEqualTo(proxyRepositoryPolicyViolation.getId());
    assertThat(policyViolationDTO.policyId).isEqualTo(proxyRepositoryPolicyViolation.getPolicyId());
    assertThat(policyViolationDTO.policyName).isEqualTo(proxyRepositoryPolicyViolation.getPolicyName());
    assertThat(policyViolationDTO.policyOwner.ownerId).isNull();
    assertThat(policyViolationDTO.policyOwner.ownerName).isNull();
    assertThat(policyViolationDTO.policyOwner.ownerType).isNull();
    assertThat(policyViolationDTO.policyThreatLevel).isEqualTo(proxyRepositoryPolicyViolation.getThreatLevel());
    assertThat(policyViolationDTO.policyThreatCategory).isEqualTo(proxyRepositoryPolicyViolation.getThreatCategory());

    assertThat(policyViolationDTO.constraints).hasSize(1);
    assertThat(policyViolationDTO.constraints.get(0).constraintId).isEqualTo(constraintFact.getConstraintId());
    assertThat(policyViolationDTO.constraints.get(0).constraintName).isEqualTo(constraintFact.getConstraintName());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionReason)
        .isEqualTo(conditionFact.getReason());
    assertThat(policyViolationDTO.constraints.get(0).conditions.get(0).conditionSummary)
        .isEqualTo(conditionFact.getSummary());
    assertThat(policyViolationDTO.constraintFactsJson)
        .isEqualTo(proxyRepositoryPolicyViolation.getConstraintFactsJson());

    assertThat(policyViolationDTO.waived).isEqualTo(proxyRepositoryPolicyViolation.isWaived());
    assertThat(policyViolationDTO.policyActionTypeId).isEqualTo(Action.ID_FAIL);
    assertThat(policyViolationDTO.lastReported).isEqualTo(proxyRepositoryPolicyViolation.getTime());
  }

  @Test
  void testGetQuarantinedComponentOtherVersions_AnonymousEnabled() throws Exception {
    // setup
    Date date = new Date();
    // Quarantined component
    final ProxyRepositoryComponent proxyRepositoryComponent =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT,
                "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.1.jar", "hash",
                ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1", null /* classifier */,
                    "jar"),
                date, new DateTime(date).minusDays(1).toDate(), null);
    // Never quarantined component
    ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.2.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2", null /* classifier */,
                "jar"),
            date, null);
    // Quarantined component
    ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.3", null /* classifier */,
                "jar"),
            date, date, null);
    // Unquarantined component
    ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.4.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.4", null /* classifier */,
                "jar"),
            date, new DateTime(date).minusDays(1).toDate(), date);
    // Unrelated never quarantined component
    ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "g/a/v/a-v.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", null /* classifier */, "jar"), date, null, null);

    final QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    final String encodedToken = encodeToken(quarantinedComponentAccess);

    // when anonymous request
    final HttpResponse response =
        ctx.restRequest()
            .path(QuarantinedComponentResource.RESOURCE_PATH,
                QuarantinedComponentResource.QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH)
            .parameter(encodedToken)
            .anon()
            .query("page", 1)
            .query("pageSize", 5)
            .query("asc", true)
            .get();

    // then
    ctx.assertResponseStatus(200, response);
    ApiPageResult<String> responseDTO = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<ApiPageResult<String>>()
        {
        });
    assertThat(responseDTO.getTotal()).isEqualTo(2);
    assertThat(responseDTO.getPage()).isEqualTo(1);
    assertThat(responseDTO.getPageSize()).isEqualTo(5);
    assertThat(responseDTO.getPageCount()).isEqualTo(1);
    assertThat(responseDTO.getResults()).containsExactly("com.lingocoder : abi.cli : 0.5.2",
        "com.lingocoder : abi.cli : 0.5.4");
  }

  @Test
  void testGetQuarantinedComponentOtherVersions_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    Date date = new Date();
    // Quarantined component
    ProxyRepositoryComponent proxyRepositoryComponent =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT,
                "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.1.jar", "hash", ComponentIdentifier
                    .createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.1", null /* classifier */, "jar"),
                date, new DateTime(date).minusDays(1).toDate(), null);
    // Never quarantined component
    ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.2.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.2", null /* classifier */,
                "jar"),
            date, null);
    // Quarantined component
    ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.3.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.3", null /* classifier */,
                "jar"),
            date, date, null);
    // Unquarantined component
    ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT,
            "com/lingocoder/abi.cli/0.5.3/abi.cli-0.5.4.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", "0.5.4", null /* classifier */,
                "jar"),
            date, new DateTime(date).minusDays(1).toDate(), date);
    // Unrelated never quarantined component
    ctx.tempEntity()
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "g/a/v/a-v.jar", "hash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", null /* classifier */, "jar"), date, null, null);

    QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

    // when anonymous request
    HttpResponse response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH)
        .parameter(encodedToken)
        .anon()
        .query("page", 1)
        .query("pageSize", 5)
        .query("asc", true)
        .get();
    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH)
        .parameter(encodedToken)
        .query("page", 1)
        .query("pageSize", 5)
        .query("asc", true)
        .get();

    // then success
    ctx.assertResponseStatus(200, response);
    ApiPageResult<String> responseDTO =
        getBodyByTypeReference(response.getBodyBytes(), new TypeReference<ApiPageResult<String>>()
        {
        });
    assertThat(responseDTO.getTotal()).isEqualTo(2);
    assertThat(responseDTO.getPage()).isEqualTo(1);
    assertThat(responseDTO.getPageSize()).isEqualTo(5);
    assertThat(responseDTO.getPageCount()).isEqualTo(1);
    assertThat(responseDTO.getResults()).containsExactly("com.lingocoder : abi.cli : 0.5.2",
        "com.lingocoder : abi.cli : 0.5.4");
  }

  @Test
  void testGetQuarantinedComponentVersionDetails_AnonymousEnabled() throws Exception {
    // setup
    ProxyRepositoryComponent proxyRepositoryComponent =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "testPathname", "testHash",
                ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), true /* quarantined */);
    QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(proxyRepositoryComponent.getComponentIdentifier());

    ctx.hdsRespondWith(namedComponentDetails).atUri("/rest/ci/componentDetails");

    // when anonymous request
    HttpResponse response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_DETAILS_PATH)
        .parameter(encodedToken)
        .query("version", "v")
        .anon()
        .get();

    // then
    ctx.assertResponseStatus(200, response);
    // Have to configure an object mapper this way because of how NamedComponentDetails works.
    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    namedComponentDetails = objectMapper.readValue(response.getBodyStream(), NamedComponentDetails.class);
    assertThat(namedComponentDetails.getDisplayName().toString()).isEqualTo("g : a : v");
    assertThat(namedComponentDetails.getHash()).isEqualTo(proxyRepositoryComponent.getHash());
    assertThat(namedComponentDetails.getMatchState()).isEqualTo(proxyRepositoryComponent.getMatchStateId());
    assertThat(namedComponentDetails.getDeclaredLicenseIds()).containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getObservedLicenseIds()).containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getEffectiveLicenses()).extracting(
        com.sonatype.clm.dto.model.License::getLicenseId)
        .containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getOverriddenLicenses()).isEmpty();
    assertThat(namedComponentDetails.getPolicyMaxThreatLevelsByCategory()).isEmpty();
    assertThat(namedComponentDetails.getEffectiveLicenseStatus()).isNull();
    assertThat(namedComponentDetails.getCatalogDate()).isNull();
    assertThat(namedComponentDetails.getRelativePopularity()).isNull();
    assertThat(namedComponentDetails.getSecurityVulnerabilities()).isEmpty();
    assertThat(namedComponentDetails.getWebsite()).isNull();
    assertThat(namedComponentDetails.getPolicyAlerts()).isEmpty();
    assertThat(namedComponentDetails.getLicenseThreatLevel()).isEqualTo(5);
    assertThat(namedComponentDetails.getLicenseThreatGroupNames()).containsExactly("Sonatype Special Licenses");
    assertThat(namedComponentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(namedComponentDetails.getIdentificationSourceComment()).isNull();
    assertThat(namedComponentDetails.getComponentIdentifier())
        .isEqualTo(proxyRepositoryComponent.getComponentIdentifier());
    assertThat(namedComponentDetails.getComponentCategories()).extracting(ComponentCategory::getPath)
        .containsExactly("Other");
    assertThat(namedComponentDetails.getHygieneRating()).isNull();
    assertThat(namedComponentDetails.getIntegrityRating()).isNull();
    assertThat(namedComponentDetails.getBreakingChangesCount()).isNull();
    assertThat(namedComponentDetails.getAnalyzerFeatures()).isNull();
  }

  @Test
  void testGetQuarantinedComponentVersionDetails_AnonymousDisabled() throws Exception {
    // setup
    disableAnonymousAccess();
    ProxyRepositoryComponent proxyRepositoryComponent =
        ctx.tempEntity()
            .newRepositoryComponent(repository.getId(), MatchState.EXACT, "testPathname", "testHash",
                ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), true /* quarantined */);
    QuarantinedComponentAccess quarantinedComponentAccess =
        ctx.tempEntity().newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
    String encodedToken = encodeToken(quarantinedComponentAccess);

    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(proxyRepositoryComponent.getComponentIdentifier());

    ctx.hdsRespondWith(namedComponentDetails).atUri("/rest/ci/componentDetails");

    // when anonymous request
    HttpResponse response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_DETAILS_PATH)
        .parameter(encodedToken)
        .query("version", "v")
        .anon()
        .get();

    // then 401 is returned
    assertThat(response.getStatusCode()).isEqualTo(401);

    // when authenticated request
    response = ctx.restRequest()
        .path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_VERSION_DETAILS_PATH)
        .parameter(encodedToken)
        .query("version", "v")
        .get();

    // then success
    ctx.assertResponseStatus(200, response);
    // Have to configure an object mapper this way because of how NamedComponentDetails works.
    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    namedComponentDetails = objectMapper.readValue(response.getBodyStream(), NamedComponentDetails.class);
    assertThat(namedComponentDetails.getDisplayName().toString()).isEqualTo("g : a : v");
    assertThat(namedComponentDetails.getHash()).isEqualTo(proxyRepositoryComponent.getHash());
    assertThat(namedComponentDetails.getMatchState()).isEqualTo(proxyRepositoryComponent.getMatchStateId());
    assertThat(namedComponentDetails.getDeclaredLicenseIds()).containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getObservedLicenseIds()).containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getEffectiveLicenses()).extracting(
        com.sonatype.clm.dto.model.License::getLicenseId)
        .containsExactly(License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getOverriddenLicenses()).isEmpty();
    assertThat(namedComponentDetails.getPolicyMaxThreatLevelsByCategory()).isEmpty();
    assertThat(namedComponentDetails.getEffectiveLicenseStatus()).isNull();
    assertThat(namedComponentDetails.getCatalogDate()).isNull();
    assertThat(namedComponentDetails.getRelativePopularity()).isNull();
    assertThat(namedComponentDetails.getSecurityVulnerabilities()).isEmpty();
    assertThat(namedComponentDetails.getWebsite()).isNull();
    assertThat(namedComponentDetails.getPolicyAlerts()).isEmpty();
    assertThat(namedComponentDetails.getLicenseThreatLevel()).isEqualTo(5);
    assertThat(namedComponentDetails.getLicenseThreatGroupNames()).containsExactly("Sonatype Special Licenses");
    assertThat(namedComponentDetails.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(namedComponentDetails.getIdentificationSourceComment()).isNull();
    assertThat(namedComponentDetails.getComponentIdentifier())
        .isEqualTo(proxyRepositoryComponent.getComponentIdentifier());
    assertThat(namedComponentDetails.getComponentCategories()).extracting(ComponentCategory::getPath)
        .containsExactly("Other");
    assertThat(namedComponentDetails.getHygieneRating()).isNull();
    assertThat(namedComponentDetails.getIntegrityRating()).isNull();
    assertThat(namedComponentDetails.getBreakingChangesCount()).isNull();
    assertThat(namedComponentDetails.getAnalyzerFeatures()).isNull();
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
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
  }

  private static String encodeToken(QuarantinedComponentAccess quarantinedComponentAccess) {
    return encodeToken(quarantinedComponentAccess.getId());
  }

  private static String encodeToken(String token) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
  }
}
