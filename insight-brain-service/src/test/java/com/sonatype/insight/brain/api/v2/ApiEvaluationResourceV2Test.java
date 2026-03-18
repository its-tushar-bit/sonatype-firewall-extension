/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.APPLICATION_EVALUATION_PATH_V2;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.13.0
 */
public class ApiEvaluationResourceV2Test
    extends AbstractResourceTest
{
  public static final String MISSING_COORDINATES = "The following coordinates are missing for given format: ";

  private static final long RETRY_INTERVAL = 500;

  private static final int NUM_TRIES = 20;

  private SourceControlEventDAO sourceControlEventDAO;

  private Organization org;

  private Application app;

  private ComponentEvaluationV2Helper componentEvaluationV2Helper;

  private static final String SCAN_ID = "scanId";

  private HttpRequest restRequest(String applicationId) {
    return restRequest().path(APPLICATION_EVALUATION_PATH_V2, applicationId);
  }

  @Before
  public void setupApplication() {
    PolicyDAO policyDAO = lookup(PolicyDAO.class);
    componentEvaluationV2Helper = new ComponentEvaluationV2Helper(policyDAO);
    sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testEvaluateComponents_invalidComponentIdentifier_noCoordinates() throws Exception {
    String jsonRequest =
        "{\"components\":[{\"hash\":\"h1\",\"componentIdentifier\":{\"format\":\"maven\"},\"proprietary\":false}]}";
    HttpResponse response = restRequest(app.getId()).body(jsonRequest).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("A component identifier must have at least one coordinate.");
  }

  @Test
  public void testEvaluateComponents_invalidComponentIdentifier_noExtension() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, "h1");
    request.components.add(component);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MISSING_COORDINATES + "[extension]");
  }

  @Test
  public void testEvaluateComponents_invalidPackageUrl() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = "pkg/invalid_package_url";
    request.components.add(component);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid package url");
  }

  @Test
  public void testEvaluateComponents_validation_nullComponentIdentifierAndNullPackageUrl() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.hash = "h1";
    request.components.add(component);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);
  }

  @Test
  public void testEvaluateComponents_validation_nullHashAndNullPackageUrl() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1"));
    request.components.add(component);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);
  }

  @Test
  public void testEvaluateComponents_validation_nullComponentIdentifierAndNullHash() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = "pkg:maven/g1/a1@v1?type=e1";
    request.components.add(component);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);
  }

  @Test
  public void testEvaluateComponents_validation_nullComponentIdentifierAndNullPackageUrlAndNullHash() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    request.components.add(component);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("One of either componentIdentifier, packageUrl, or hash must be supplied.");
  }

  @Test
  public void testEvaluateComponents_nullComponents() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components = null;

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No components provided for evaluation");
  }

  @Test
  public void testEvaluateComponents_emptyComponents() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components = Collections.emptyList();

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No components provided for evaluation");
  }

  @Test
  public void testEvaluateComponents_HdsError() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, "h1");
    request.components.add(component);

    mockHDSInternalServiceError();

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = response.getBody(ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = response.getBody(ApiComponentEvaluationResultDTOV2.class);
    assertThat(details).isNotNull();
    assertThat(details.isError).isTrue();
    assertThat(details.errorMessage).startsWith("Internal Server Error");
    assertThat(details.applicationId).isEqualTo(app.getId());
    assertThat(details.evaluationDate).isNotNull();
    assertThat(details.submittedDate).isNotNull();
    assertThat(details.results).isEmpty();
  }

  @Test
  public void testEvaluateComponents() throws Exception {
    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1");
    String packageUrl1 = PackageUrlIdentifier.toPackageUrl(componentIdentifier1);
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(componentIdentifier1, "h1", packageUrl1);
    request.components.add(component1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "e2");
    String packageUrl2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2).getPackageUrl();
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(componentIdentifier2, "h2", packageUrl2);
    request.components.add(component2);

    // Create a mock return for the first component
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(Collections.singletonList(new License("Apache-2.0",
        "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(
        Collections.singletonList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper.createSecurityVulnerabilities();

    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier1, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 1),
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier2, component2.hash,
            MatchState.UNKNOWN, 1, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 2));
    mockComponentDetails(componentEvaluationDataList);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = response.getBody(ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = response.getBody(ApiComponentEvaluationResultDTOV2.class);
    assertThat(details).isNotNull();
    assertThat(details.errorMessage).isNull();
    assertThat(details.isError).isFalse();
    assertThat(details.applicationId).isEqualTo(app.getId());
    assertThat(details.evaluationDate).isNotNull();
    assertThat(details.submittedDate).isNotNull();
    assertThat(details.results).hasSize(2);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0), request.components.get(0),
        MatchState.EXACT.getId(), declaredLicenseSet, observedLicenseSet,
        securityVulnerabilities, 1, policies);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1), request.components.get(1),
        MatchState.UNKNOWN.getId(), Collections.emptySet(), Collections.emptySet(),
        Collections.emptyList(), 2, Collections.emptyMap());
  }

  @Test
  public void testEvaluateComponents_matchByComponentIdentifier() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, null);
    assertEvaluateComponent(componentIdentifier, component);
  }

  @Test
  public void testEvaluateComponents_matchByPackageUrl() throws Exception {
    PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(packageURLIdentifier.getPackageUrl());

    assertEvaluateComponent(packageURLIdentifier.toComponentIdentifier(), component);
  }

  private void assertEvaluateComponent(
      ComponentIdentifier componentIdentifier,
      ApiComponentDTOV2 component) throws Exception
  {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components.add(component);

    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    LinkedHashSet<License> declaredLicenseSet =
        new LinkedHashSet<>(Collections.singletonList(new License("Apache-2.0", "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(
        Collections.singletonList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper.createSecurityVulnerabilities();

    ComponentEvaluationDataList componentEvaluationDataList =
        createComponentEvaluationDataList(componentEvaluationV2Helper
            .createComponentEvaluationData(componentIdentifier, component.hash, MatchState.EXACT, 0, declaredLicenseSet,
                observedLicenseSet, securityVulnerabilities, 10));
    mockComponentDetails(componentEvaluationDataList);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = response.getBody(ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = response.getBody(ApiComponentEvaluationResultDTOV2.class);
    assertThat(details).isNotNull();
    assertThat(details.errorMessage).isNull();
    assertThat(details.isError).isFalse();
    assertThat(details.applicationId).isEqualTo(app.getId());
    assertThat(details.evaluationDate).isNotNull();
    assertThat(details.submittedDate).isNotNull();
    assertThat(details.results).hasSize(1);
    String expectedPackageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier), null, expectedPackageUrl,
        MatchState.EXACT.getId(), declaredLicenseSet, observedLicenseSet,
        securityVulnerabilities, 10, policies);
  }

  @Test
  public void testEvaluateComponents_multipleMatchByHash() throws Exception {
    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(null, "h1");
    request.components.add(component1);

    // Create a mock return for the first component
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(Collections.singletonList(new License("Apache-2.0",
        "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(
        Collections.singletonList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper.createSecurityVulnerabilities();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1");
    String packageUrl1 = PackageUrlIdentifier.toPackageUrl(componentIdentifier1);
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "e2");
    String packageUrl2 = PackageUrlIdentifier.toPackageUrl(componentIdentifier2);

    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier1, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 11),
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier2, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 22));
    mockComponentDetails(componentEvaluationDataList);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = response.getBody(ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = response.getBody(ApiComponentEvaluationResultDTOV2.class);
    assertThat(details).isNotNull();
    assertThat(details.errorMessage).isNull();
    assertThat(details.isError).isFalse();
    assertThat(details.applicationId).isEqualTo(app.getId());
    assertThat(details.evaluationDate).isNotNull();
    assertThat(details.submittedDate).isNotNull();
    assertThat(details.results).hasSize(2);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0),
        componentEvaluationV2Helper.createComponent(componentIdentifier1, "h1", packageUrl1), MatchState.EXACT.getId(),
        declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 11,
        policies);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1),
        componentEvaluationV2Helper.createComponent(componentIdentifier2, "h1", packageUrl2), MatchState.EXACT.getId(),
        declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 22,
        policies);
  }

  @Test
  public void testEvaluateComponents_withClaimedComponent() throws Exception {
    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1");
    String packageUrl1 = PackageUrlIdentifier.toPackageUrl(componentIdentifier1);
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(componentIdentifier1, "h1", packageUrl1);
    request.components.add(component1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "e2");
    String packageUrl2 = PackageUrlIdentifier.toPackageUrl(componentIdentifier2);
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(componentIdentifier2, "h2", packageUrl2);
    request.components.add(component2);

    tempEntity.newClaimedComponent("h2", componentIdentifier2);

    // Create a mock return for the first component
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(Collections.singletonList(new License("Apache-2.0",
        "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(
        Collections.singletonList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper.createSecurityVulnerabilities();

    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier1, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 21),
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier2, component2.hash,
            MatchState.UNKNOWN, 1, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 22));
    mockComponentDetails(componentEvaluationDataList);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = response.getBody(ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = response.getBody(ApiComponentEvaluationResultDTOV2.class);
    assertThat(details).isNotNull();
    assertThat(details.errorMessage).isNull();
    assertThat(details.isError).isFalse();
    assertThat(details.applicationId).isEqualTo(app.getId());
    assertThat(details.evaluationDate).isNotNull();
    assertThat(details.submittedDate).isNotNull();
    assertThat(details.results).hasSize(2);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0), request.components.get(0),
        MatchState.EXACT.getId(), declaredLicenseSet, observedLicenseSet,
        securityVulnerabilities, 21, policies);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1), request.components.get(1),
        MatchState.EXACT.getId(), Collections.emptySet(), Collections.emptySet(),
        Collections.emptyList(), null, Collections.emptyMap());
  }

  @Test
  public void testEvaluateComponents_withClaimedComponentEmptyVsNullClassifier() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier compIdentifierWithEmptyClassifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1",
        "", "e1");
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(compIdentifierWithEmptyClassifier, null);
    request.components.add(component1);

    ComponentIdentifier compIdentifierWithNullClassifier = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2",
        null, "e2");
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(compIdentifierWithNullClassifier, null);
    request.components.add(component2);

    ComponentIdentifier expectedComponentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "",
        "e1");
    tempEntity.newClaimedComponent("h1", expectedComponentIdentifier1);
    ComponentIdentifier expectedComponentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "",
        "e2");
    tempEntity.newClaimedComponent("h2", expectedComponentIdentifier2);

    // Create a mock return
    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1"), component1.hash,
            MatchState.UNKNOWN, 0, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 1),
        componentEvaluationV2Helper.createComponentEvaluationData(
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "e2"), component1.hash,
            MatchState.UNKNOWN, 1, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 2));
    mockComponentDetails(componentEvaluationDataList);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = response.getBody(ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = response.getBody(ApiComponentEvaluationResultDTOV2.class);
    assertThat(details).isNotNull();
    assertThat(details.errorMessage).isNull();
    assertThat(details.isError).isFalse();
    assertThat(details.applicationId).isEqualTo(app.getId());
    assertThat(details.evaluationDate).isNotNull();
    assertThat(details.submittedDate).isNotNull();
    assertThat(details.results).hasSize(2);
    String expectedPackageUrl1 = PackageUrlIdentifier.toPackageUrl(expectedComponentIdentifier1);
    String expectedPackageUrl2 = PackageUrlIdentifier.toPackageUrl(expectedComponentIdentifier2);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(expectedComponentIdentifier1), "h1", expectedPackageUrl1,
        MatchState.EXACT.getId(), Collections.emptySet(), Collections.emptySet(),
        Collections.emptyList(), null, Collections.emptyMap());
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(expectedComponentIdentifier2), "h2", expectedPackageUrl2,
        MatchState.EXACT.getId(), Collections.emptySet(), Collections.emptySet(),
        Collections.emptyList(), null, Collections.emptyMap());
  }

  @Test
  public void testEvaluateComponents_withClaimedComponentMissingExtension() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier compIdentifierWithNoExtension = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(compIdentifierWithNoExtension, null);
    request.components.add(component1);

    HttpResponse response = restRequest(app.getId()).body(request).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(MISSING_COORDINATES + "[extension]");
  }

  private void mockHDSInternalServiceError() {
    hdsRespondWith("Internal Error").andStatus(500)
        .atUri(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH
            .replace("{purpose: evaluation|integration}", ApiComponentDetailsServiceV2.PURPOSE_EVALUATION));
  }

  @Test
  public void testPromoteScan() throws Exception {
    createScanFile(app.getId(), SCAN_ID);
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2 = ApiPromoteScanRequestDTOV2
        .fromScan(SCAN_ID, Stage.ID_OPERATE);

    HttpResponse response = restRequest()
        .path(APPLICATION_EVALUATION_PATH_V2, ApiEvaluationResourceV2.PROMOTE_SCAN_PATH)
        .parameter(app.getId())
        .body(apiPromoteScanRequestDTOV2)
        .post();

    assertResponseStatus(200, response);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 =
        response.getBody(ApiApplicationEvaluationStatusDTOV2.class);
    assertThat(apiApplicationEvaluationStatusDTOV2).isNotNull();
  }

  @Test
  public void testGetApplicationEvaluationStatus() throws Exception {
    createScanFile(app.getId(), SCAN_ID);
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    HttpResponse response = restRequest()
        .path(APPLICATION_EVALUATION_PATH_V2, ApiEvaluationResourceV2.PROMOTE_SCAN_PATH)
        .parameter(app.getId())
        .body(ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE))
        .post();
    assertResponseStatus(200, response);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 =
        response.getBody(ApiApplicationEvaluationStatusDTOV2.class);
    assertThat(apiApplicationEvaluationStatusDTOV2).isNotNull();

    response = restRequest().path(apiApplicationEvaluationStatusDTOV2.statusUrl).get();

    assertResponseStatus(200, response);
    ApiApplicationEvaluationResultDTOV2 apiApplicationEvaluationResultDTOV2 =
        response.getBody(ApiApplicationEvaluationResultDTOV2.class);
    assertThat(apiApplicationEvaluationResultDTOV2).isNotNull();
    assertThat(apiApplicationEvaluationResultDTOV2.status).isNotNull();
  }

  @Test
  public void testEvaluateSourceControl_NoScanTarget() throws Exception {
    // given an application
    Application app = tempEntity.newApplicationWithParent();

    // and a root-org source control definition
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);

    // and app-level source control
    PasswordHandler pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    tempEntity.newSourceControl(app.getId(), "http://example.com/my/repo.git", null,
        new String(pwHandler.encryptPassword("TOKEN".toCharArray())), null, null, true, "customBranch", null);

    // and events are empty
    assertThat(sourceControlEventDAO.getAll()).isEmpty();

    // when application source control is evaluated
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "customBranch");
    HttpResponse response = restRequest() //
        .path(APPLICATION_EVALUATION_PATH_V2, ApiEvaluationResourceV2.SOURCE_CONTROL_EVALUATION_PATH) //
        .parameter(app.getId()) //
        .body(apiSourceControlEvaluationRequestDTO)
        .post();

    // the response contains status ID
    assertResponseStatus(200, response);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 =
        response.getBody(ApiApplicationEvaluationStatusDTOV2.class);
    assertThat(apiApplicationEvaluationStatusDTOV2.statusUrl).isNotNull();

    // and the event was published
    List<SourceControlEvent> allEvents = sourceControlEventDAO.getAll();
    assertThat(allEvents.size()).isEqualTo(1);

    // and it matches expected values
    SourceControlEvent event = allEvents.get(0);
    assertThat(event.getApplicationId()).isEqualTo(app.getId());
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);
    assertThat(event.getStageTypeId()).isEqualTo("develop");
    assertThat(event.getBranchName()).isEqualTo("customBranch");
    assertThat(event.getScanTargets()).isNull();
  }

  @Test
  public void testEvaluateSourceControl_WithScanTarget() throws Exception {
    // given an application
    Application app = tempEntity.newApplicationWithParent();

    // and a root-org source control definition
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);

    // and app-level source control
    PasswordHandler pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    tempEntity.newSourceControl(app.getId(), "http://example.com/my/repo.git", null,
        new String(pwHandler.encryptPassword("TOKEN".toCharArray())), null, null, true, "customBranch", null);

    // and events are empty
    assertThat(sourceControlEventDAO.getAll()).isEmpty();

    // when application source control is evaluated
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "customBranch",
            Collections.singletonList("testScanTarget"));
    System.err.println(JsonUtils.writeUnformatted(apiSourceControlEvaluationRequestDTO));
    HttpResponse response = restRequest() //
        .path(APPLICATION_EVALUATION_PATH_V2, ApiEvaluationResourceV2.SOURCE_CONTROL_EVALUATION_PATH) //
        .parameter(app.getId()) //
        .body(apiSourceControlEvaluationRequestDTO)
        .post();

    // the response contains status ID
    assertResponseStatus(200, response);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 =
        response.getBody(ApiApplicationEvaluationStatusDTOV2.class);
    assertThat(apiApplicationEvaluationStatusDTOV2.statusUrl).isNotNull();

    // and the event was published
    List<SourceControlEvent> allEvents = sourceControlEventDAO.getAll();
    assertThat(allEvents.size()).isEqualTo(1);

    // and it matches expected values
    SourceControlEvent event = allEvents.get(0);
    assertThat(event.getApplicationId()).isEqualTo(app.getId());
    assertThat(event.getEventType()).isEqualTo(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);
    assertThat(event.getStageTypeId()).isEqualTo("develop");
    assertThat(event.getBranchName()).isEqualTo("customBranch");
    assertThat(event.getScanTargets()).isEqualTo(Collections.singletonList("testScanTarget"));
  }

  private void mockComponentDetails(final ComponentEvaluationDataList componentEvaluationDataList) {
    hdsRespondWith(componentEvaluationDataList).atUri(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH
        .replace("{purpose: evaluation|integration}", ApiComponentDetailsServiceV2.PURPOSE_EVALUATION));
  }

  private ComponentEvaluationDataList createComponentEvaluationDataList(
      final ComponentEvaluationData... componentEvaluationData)
  {
    ComponentEvaluationDataList componentEvaluationDataList = new ComponentEvaluationDataList();
    componentEvaluationDataList.components = new ArrayList<>();
    Collections.addAll(componentEvaluationDataList.components, componentEvaluationData);
    return componentEvaluationDataList;
  }

  private HttpResponse getComponentEvaluationResult(
      final ApiComponentEvaluationTicketDTOV2 evaluationResult) throws Exception
  {
    HttpResponse response = null;
    HttpRequest request = restRequest().path(evaluationResult.resultsUrl);

    for (int tryCount = 1; tryCount <= NUM_TRIES; tryCount++) {
      response = request.get();
      if (response.getStatusCode() == 200) {
        break;
      }
      tryCount++;
      Thread.sleep(RETRY_INTERVAL);
    }
    return response;
  }
}
