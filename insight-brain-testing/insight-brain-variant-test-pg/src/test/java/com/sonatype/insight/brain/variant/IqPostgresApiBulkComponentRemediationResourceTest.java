/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiBulkComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiBulkComponentRemediationRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiBulkComponentRemediationResultDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.205
 */
@IqPostgresTest
class IqPostgresApiBulkComponentRemediationResourceTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES_V1 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V2 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v2", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V3 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v3", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_OTHER_V1 = ComponentIdentifier.createMavenCoordinates("g2",
      "a2", "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_OTHER_V2 = ComponentIdentifier.createMavenCoordinates("g2",
      "a2", "v2", "", "jar");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private ComponentEvaluationV2Helper componentEvaluationV2Helper;

  private Application app;

  private Organization org;

  @BeforeEach
  void before() throws Exception {
    PolicyDAO policyDAO = ctx.lookup(PolicyDAO.class);
    componentEvaluationV2Helper = new ComponentEvaluationV2Helper(policyDAO);

    org = ctx.tempEntity().newOrganization("Org");
    app = ctx.tempEntity().newApplication(org.getId());
    ctx.setFeatures(LicensedFeature.ADVANCED_RECOMMENDATION_STRATEGIES, LicensedFeature.COMPONENT_EVALUATION);
    mockVersionScoring();
  }

  @Test
  void testBulk_HappyPath_TwoComponents_TwoResults() throws Exception {
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    mockComponentSummary(MAVEN_COORDINATES_OTHER_V1, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails details3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    ComponentDetails details4 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_OTHER_V1);
    ComponentDetails details5 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_OTHER_V2);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(Stream.of(details1, details2, details3, details4, details5).collect(Collectors.toList()));
    mockComponentDetails(detailsList);

    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_OTHER_V1, null);

    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = Arrays.asList(component1, component2);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApiBulkComponentRemediationDTO result = response.getBody(ApiBulkComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.results).hasSize(2);

    // Results are returned in input order.
    ApiBulkComponentRemediationResultDTO first = result.results.get(0);
    assertThat(first.error).isNull();
    assertThat(first.component).isNotNull();
    assertThat(first.component.componentIdentifier.toComponentIdentifier()).isEqualTo(MAVEN_COORDINATES_V1);
    assertThat(first.remediation).isNotNull();
    assertThat(first.remediation.versionChanges).isNotEmpty();
    assertThat(first.remediation.versionChanges.get(0).getType())
        .isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);

    ApiBulkComponentRemediationResultDTO second = result.results.get(1);
    assertThat(second.error).isNull();
    assertThat(second.component.componentIdentifier.toComponentIdentifier()).isEqualTo(MAVEN_COORDINATES_OTHER_V1);
    assertThat(second.remediation).isNotNull();
  }

  @Test
  void testBulk_PartialSuccess_OneBadComponentDoesNotFailTheBatch() throws Exception {
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails details3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(Stream.of(details1, details2, details3).collect(Collectors.toList()));
    mockComponentDetails(detailsList);

    ApiComponentDTOV2 good = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);

    // A component with neither componentIdentifier nor packageUrl is rejected by validateRequest
    // in ApiComponentRemediationService with an InvalidComponentException (a BadRequestException
    // subclass narrowly used to mark per-component input failures). The bulk endpoint catches
    // exactly that type and surfaces it as a per-item error rather than failing the whole batch.
    ApiComponentDTOV2 bad = new ApiComponentDTOV2();

    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = Arrays.asList(good, bad);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApiBulkComponentRemediationDTO result = response.getBody(ApiBulkComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.results).hasSize(2);

    ApiBulkComponentRemediationResultDTO goodResult = result.results.get(0);
    assertThat(goodResult.error).isNull();
    assertThat(goodResult.remediation).isNotNull();

    ApiBulkComponentRemediationResultDTO badResult = result.results.get(1);
    assertThat(badResult.remediation).isNull();
    assertThat(badResult.error).isNotNull();
    assertThat(badResult.error).contains("componentIdentifier");
  }

  @Test
  void testBulk_EmptyRequest_ReturnsBadRequest() throws Exception {
    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = Collections.emptyList();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testBulk_InvalidStageId_FailsWholeBatch() throws Exception {
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = List.of(component);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .query("stageId", "not-a-real-stage")
        .body(request)
        .post();

    // Invalid stageId is a batch-level parameter and is validated synchronously on the request thread
    // before any per-component work is submitted. It must produce HTTP 400, not a 200 with N per-item errors.
    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testBulk_RepositoryOwnerWithScanIdAndNoStageId_ReturnsBadRequest() throws Exception {
    // Repository owners never accept scanId, regardless of whether stageId is supplied. The check must
    // happen before defaulting the stageId, otherwise an omitted stageId short-circuits the scanId
    // rejection and lets an invalid combination through.
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, "test-repo");

    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = List.of(component);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.REPOSITORY, repository.getId())
        .query("scanId", "some-scan-id")
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testBulk_NullComponentsField_ReturnsBadRequest() throws Exception {
    // Distinct from empty-list: the caller may send { "components": null } explicitly. The service must
    // reject it the same way as an empty list rather than NPE.
    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = null;

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testBulk_NullElementInComponentsList_ReportedAsPerItemError() throws Exception {
    // A JSON array with an explicit null element is structurally valid; the service must convert that
    // element into a per-item error rather than either NPE'ing the whole batch or losing the slot.
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails details3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(Stream.of(details1, details2, details3).collect(Collectors.toList()));
    mockComponentDetails(detailsList);

    ApiComponentDTOV2 good = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);

    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    // Mixed: good, null, good — proves ordering is preserved even when the middle slot is a null entry.
    request.components = Arrays.asList(good, null, good);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApiBulkComponentRemediationDTO result = response.getBody(ApiBulkComponentRemediationDTO.class);
    assertThat(result.results).hasSize(3);

    // Index 0 and 2: good components -> remediation, no error.
    assertThat(result.results.get(0).error).isNull();
    assertThat(result.results.get(0).remediation).isNotNull();
    assertThat(result.results.get(2).error).isNull();
    assertThat(result.results.get(2).remediation).isNotNull();

    // Index 1: null entry -> per-item error, no component echo, no remediation.
    ApiBulkComponentRemediationResultDTO nullResult = result.results.get(1);
    assertThat(nullResult.remediation).isNull();
    assertThat(nullResult.component).isNull();
    assertThat(nullResult.error).isEqualTo("Component must not be null.");
  }

  @Test
  void testBulk_ExceedsMaxBatchSize_ReturnsBadRequest() throws Exception {
    // Cap must be enforced synchronously on the request thread before any tasks are submitted.
    // 201 exceeds the current 200-component cap.
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = Collections.nCopies(201, component);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testBulk_MultipleBadItemsScattered_ResultsInOrder() throws Exception {
    // Verify that (a) multiple per-item failures don't abort the batch, (b) results are returned in the
    // exact same order as the input even when the middle items fail synchronously in validation, and
    // (c) success and failure items can interleave freely.
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    mockComponentSummary(MAVEN_COORDINATES_OTHER_V1, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails details3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    ComponentDetails details4 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_OTHER_V1);
    ComponentDetails details5 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_OTHER_V2);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(Stream.of(details1, details2, details3, details4, details5).collect(Collectors.toList()));
    mockComponentDetails(detailsList);

    ApiComponentDTOV2 good1 = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);
    ApiComponentDTOV2 bad1 = new ApiComponentDTOV2();
    ApiComponentDTOV2 good2 = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_OTHER_V1, null);
    ApiComponentDTOV2 bad2 = new ApiComponentDTOV2();

    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = Arrays.asList(good1, bad1, good2, bad2);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApiBulkComponentRemediationDTO result = response.getBody(ApiBulkComponentRemediationDTO.class);
    assertThat(result.results).hasSize(4);

    // Index 0: good1 -> remediation, no error.
    assertThat(result.results.get(0).error).isNull();
    assertThat(result.results.get(0).remediation).isNotNull();
    assertThat(result.results.get(0).component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(MAVEN_COORDINATES_V1);

    // Index 1: bad1 -> error, no remediation.
    assertThat(result.results.get(1).remediation).isNull();
    assertThat(result.results.get(1).error).isNotNull();

    // Index 2: good2 -> remediation, no error.
    assertThat(result.results.get(2).error).isNull();
    assertThat(result.results.get(2).remediation).isNotNull();
    assertThat(result.results.get(2).component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(MAVEN_COORDINATES_OTHER_V1);

    // Index 3: bad2 -> error, no remediation.
    assertThat(result.results.get(3).remediation).isNull();
    assertThat(result.results.get(3).error).isNotNull();
  }

  @Test
  void testBulk_MalformedPurl_ReportedAsPerItemError() throws Exception {
    // A syntactically-invalid packageUrl is a per-component input error, not a batch-level failure.
    // The single-component endpoint lets InvalidPackageURLException propagate (its @HttpStatusCode(400)
    // surfaces the 400); the bulk endpoint catches it in the per-task try/catch alongside
    // InvalidComponentException so a bad purl in one component doesn't fail the whole batch —
    // consistent with the componentIdentifier path, which had always been per-item.
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails details3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(Stream.of(details1, details2, details3).collect(Collectors.toList()));
    mockComponentDetails(detailsList);

    ApiComponentDTOV2 good = componentEvaluationV2Helper.createComponent(MAVEN_COORDINATES_V1, null);

    ApiComponentDTOV2 bad = new ApiComponentDTOV2();
    bad.packageUrl = "this is not a valid purl";

    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = Arrays.asList(good, bad);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApiBulkComponentRemediationDTO result = response.getBody(ApiBulkComponentRemediationDTO.class);
    assertThat(result.results).hasSize(2);

    ApiBulkComponentRemediationResultDTO goodResult = result.results.get(0);
    assertThat(goodResult.error).isNull();
    assertThat(goodResult.remediation).isNotNull();

    ApiBulkComponentRemediationResultDTO badResult = result.results.get(1);
    assertThat(badResult.remediation).isNull();
    assertThat(badResult.error).isNotNull();
  }

  @Test
  void testBulk_PurlOnlyComponent_Works() throws Exception {
    mockComponentSummary(MAVEN_COORDINATES_V1, ComponentSummary.create(true));
    mockGetDependencies(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()));
    createPolicyWithSecurityVulnerabilityConstraint(app.getId());

    ComponentDetails details1 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V1);
    ComponentDetails details2 = createComponentDetailsForSecurityViolation(MAVEN_COORDINATES_V2);
    ComponentDetails details3 = createComponentDetailsForNoViolation(MAVEN_COORDINATES_V3);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(Stream.of(details1, details2, details3).collect(Collectors.toList()));
    mockComponentDetails(detailsList);

    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = "pkg:maven/g1/a1@v1?type=jar";

    ApiBulkComponentRemediationRequestDTO request = new ApiBulkComponentRemediationRequestDTO();
    request.components = List.of(component);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.COMPONENT_REMEDIATION_BULK_PATH_V2)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(request)
        .post();

    ctx.assertResponseStatus(200, response);
    ApiBulkComponentRemediationDTO result = response.getBody(ApiBulkComponentRemediationDTO.class);
    assertThat(result.results).hasSize(1);
    assertThat(result.results.get(0).error).isNull();
    assertThat(result.results.get(0).remediation).isNotNull();
  }

  private void createPolicyWithSecurityVulnerabilityConstraint(final String ownerId) {
    Policy policy = new Policy();
    policy.setName("Policy");
    policy.setThreatLevel(5);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    ctx.tempEntity().newPolicy(policy);
  }

  private ComponentDetails createComponentDetailsForNoViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = new ComponentDetails();
    componentDetails.setComponentIdentifier(componentIdentifier);
    return componentDetails;
  }

  private ComponentDetails createComponentDetailsForSecurityViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = createComponentDetailsForNoViolation(componentIdentifier);
    componentDetails.setLicenseThreatLevel(5);
    componentDetails
        .setSecurityVulnerabilities(Collections.singletonList(new SecurityVulnerability("ref", "source", 5f)));
    return componentDetails;
  }

  private void mockComponentDetails(final ComponentDetailsList componentEvaluationDataList) {
    ctx.hdsRespondWith(componentEvaluationDataList).atUri("rest/ci/componentDetails/list");
  }

  private void mockVersionScoring() {
    ctx.hdsRespondWith(new VersionScoringService[]{}).atUri(HDS_BULK_SCORE_VERSIONING_PATH);
  }

  private void mockComponentSummary(
      final ComponentIdentifier componentIdentifier,
      final ComponentSummary componentSummary) throws Exception
  {
    ctx.hdsRespondWith(componentSummary)
        .atUri(UriBuilder.fromPath("rest/component/summary")
            .queryParam("componentIdentifier",
                URLEncoder.encode(OBJECT_MAPPER.writeValueAsString(componentIdentifier), "UTF-8"))
            .build());
  }

  private void mockGetDependencies(final ComponentDependenciesDTO componentDependenciesDTO) {
    ctx.hdsRespondWith(componentDependenciesDTO).atUri("rest/component/dependencies");
  }
}
