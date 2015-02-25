/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentEvaluationServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentIdentifierValidator;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.hamcrest.core.StringStartsWith;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * @since 1.13.0
 */
public class ApiComponentEvaluationResourceV2Test
    extends AbstractResourceTest
{
  private static final long RETRY_INTERVAL = 500;

  private static final int NUM_TRIES = 20;

  private Organization org;

  private Application app;

  private ComponentEvaluationV2Helper componentEvaluationV2Helper = new ComponentEvaluationV2Helper();

  @Before
  public void setupApplication() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testEvaluateComponents_invalidComponentIdentifier_noCoordinates() throws Exception {
    String jsonRequest = "{\"components\":[{\"hash\":\"h1\",\"componentIdentifier\":{\"format\":\"maven\"},\"proprietary\":false}]}";
    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, jsonRequest);
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("A component identifier must have at least one coordinate."));
  }

  @Test
  public void testEvaluateComponents_invalidComponentIdentifier_noExtension() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ComponentIdentifier componentIdentifier =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", null);
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, "h1");
    request.components.add(component);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is(ApiComponentIdentifierValidator.MISSING_COORDINATES + "[extension]"));
  }

  @Test
  public void testEvaluateComponents_validation_nullComponentIdentifier() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.hash = "h1";
    request.components.add(component);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(200, response);
  }

  @Test
  public void testEvaluateComponents_validation_nullHash() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", "e1"));
    request.components.add(component);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(200, response);
  }

  @Test
  public void testEvaluateComponents_validation_nullComponentIdentifierAndNullHash() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    request.components.add(component);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("One of either componentIdentifier or hash must be supplied."));
  }

  @Test
  public void testEvaluateComponents_nullComponents() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components = null;

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("No components provided for evaluation"));
  }


  @Test
  public void testEvaluateComponents_emptyComponents() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components = Collections.emptyList();

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("No components provided for evaluation"));
  }

  @Test
  public void testEvaluateComponents_HdsError() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ComponentIdentifier componentIdentifier =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", "e1");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, "h1");
    request.components.add(component);

    mockHDSInternalServiceError();

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = fromJson(response, ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(getRestBaseUrl(), evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = fromJson(response, ApiComponentEvaluationResultDTOV2.class);
    assertThat(details, notNullValue());
    assertThat(details.isError, is(true));
    assertThat(details.errorMessage, startsWith("Internal Server Error (ID "));
    assertThat(details.applicationId, is(app.getId()));
    assertThat(details.evaluationDate, notNullValue());
    assertThat(details.submittedDate, notNullValue());
    assertThat(details.results, notNullValue());
    assertThat(details.results.size(), is(0));
  }

  @Test
  public void testEvaluateComponents() throws Exception {
    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier componentIdentifier1 =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", "e1");
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(componentIdentifier1, "h1");
    request.components.add(component1);

    ComponentIdentifier componentIdentifier2 =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g2", "a2", "v2", "e2");
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(componentIdentifier2, "h2");
    request.components.add(component2);

    // Create a mock return for the first component
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(
        Arrays.asList(new License("Apache-2.0", "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(Arrays.asList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper
        .createSecurityVulnerabilities();

    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier1, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities),
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier2, component2.hash,
            MatchState.UNKNOWN, 1, Collections.<License>emptySet(), Collections.<License>emptySet(),
            Collections.<SecurityVulnerability>emptyList()));
    mockComponentDetails(componentEvaluationDataList);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = fromJson(response, ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(getRestBaseUrl(), evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = fromJson(response, ApiComponentEvaluationResultDTOV2.class);
    assertThat(details, notNullValue());
    assertThat(details.isError, is(false));
    assertThat(details.errorMessage, nullValue());
    assertThat(details.applicationId, is(app.getId()));
    assertThat(details.evaluationDate, notNullValue());
    assertThat(details.submittedDate, notNullValue());
    assertThat(details.results, notNullValue());
    assertThat(details.results.size(), is(2));
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0), request.components.get(0),
        MatchState.EXACT.getId(), new ArrayList<>(declaredLicenseSet), new ArrayList<>(observedLicenseSet),
        securityVulnerabilities, policies);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1), request.components.get(1),
        MatchState.UNKNOWN.getId(), Collections.<License>emptyList(), Collections.<License>emptyList(),
        Collections.<SecurityVulnerability>emptyList(), Collections.<String, Policy>emptyMap());
  }

  @Test
  public void testEvaluateComponents_matchByComponentIdentifier() throws Exception {
    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier componentIdentifier1 =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", "e1");
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(componentIdentifier1, null);
    request.components.add(component1);

    ComponentIdentifier componentIdentifier2 =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g2", "a2", "v2", "e2");
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(componentIdentifier2, null);
    request.components.add(component2);

    // Create a mock return for the first component
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(
        Arrays.asList(new License("Apache-2.0", "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(Arrays.asList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper
        .createSecurityVulnerabilities();

    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier1, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities),
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier2, component2.hash,
            MatchState.UNKNOWN, 1, Collections.<License>emptySet(), Collections.<License>emptySet(),
            Collections.<SecurityVulnerability>emptyList()));
    mockComponentDetails(componentEvaluationDataList);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = fromJson(response, ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(getRestBaseUrl(), evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = fromJson(response, ApiComponentEvaluationResultDTOV2.class);
    assertThat(details, notNullValue());
    assertThat(details.isError, is(false));
    assertThat(details.errorMessage, nullValue());
    assertThat(details.applicationId, is(app.getId()));
    assertThat(details.evaluationDate, notNullValue());
    assertThat(details.submittedDate, notNullValue());
    assertThat(details.results, notNullValue());
    assertThat(details.results.size(), is(2));
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0), request.components.get(0),
        MatchState.EXACT.getId(), new ArrayList<>(declaredLicenseSet), new ArrayList<>(observedLicenseSet),
        securityVulnerabilities, policies);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1), request.components.get(1),
        MatchState.UNKNOWN.getId(), Collections.<License>emptyList(), Collections.<License>emptyList(),
        Collections.<SecurityVulnerability>emptyList(), Collections.<String, Policy>emptyMap());
  }

  @Test
  public void testEvaluateComponents_multipleMatchByHash() throws Exception {
    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(null, "h1");
    request.components.add(component1);

    // Create a mock return for the first component
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(
        Arrays.asList(new License("Apache-2.0", "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(Arrays.asList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper
        .createSecurityVulnerabilities();

    ComponentIdentifier componentIdentifier1 =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", "e1");
    ComponentIdentifier componentIdentifier2 =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g2", "a2", "v2", "e2");

    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier1, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities),
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier2, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities));
    mockComponentDetails(componentEvaluationDataList);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = fromJson(response, ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(getRestBaseUrl(), evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = fromJson(response, ApiComponentEvaluationResultDTOV2.class);
    assertThat(details, notNullValue());
    assertThat(details.isError, is(false));
    assertThat(details.errorMessage, nullValue());
    assertThat(details.applicationId, is(app.getId()));
    assertThat(details.evaluationDate, notNullValue());
    assertThat(details.submittedDate, notNullValue());
    assertThat(details.results, notNullValue());
    assertThat(details.results.size(), is(2));
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0),
        componentEvaluationV2Helper.createComponent(componentIdentifier1, "h1"),
        MatchState.EXACT.getId(), new ArrayList<>(declaredLicenseSet), new ArrayList<>(observedLicenseSet),
        securityVulnerabilities, policies);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1),
        componentEvaluationV2Helper.createComponent(componentIdentifier2, "h1"),
        MatchState.EXACT.getId(), new ArrayList<>(declaredLicenseSet), new ArrayList<>(observedLicenseSet),
        securityVulnerabilities, policies);
  }

  @Test
  public void testEvaluateComponents_withClaimedComponent() throws Exception {
    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier componentIdentifier1 =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", "e1");
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(componentIdentifier1, "h1");
    request.components.add(component1);

    ComponentIdentifier componentIdentifier2 =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g2", "a2", "v2", "e2");
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(componentIdentifier2, "h2");
    request.components.add(component2);

    tempEntity.newClaimedComponent("h2", componentIdentifier2);

    // Create a mock return for the first component
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(
        Arrays.asList(new License("Apache-2.0", "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(Arrays.asList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper.createSecurityVulnerabilities();

    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier1, component1.hash,
            MatchState.EXACT, 0, declaredLicenseSet, observedLicenseSet, securityVulnerabilities),
        componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier2, component2.hash,
            MatchState.UNKNOWN, 1, Collections.<License>emptySet(), Collections.<License>emptySet(),
            Collections.<SecurityVulnerability>emptyList()));
    mockComponentDetails(componentEvaluationDataList);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = fromJson(response, ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(getRestBaseUrl(), evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = fromJson(response, ApiComponentEvaluationResultDTOV2.class);
    assertThat(details, notNullValue());
    assertThat(details.isError, is(false));
    assertThat(details.errorMessage, nullValue());
    assertThat(details.applicationId, is(app.getId()));
    assertThat(details.evaluationDate, notNullValue());
    assertThat(details.submittedDate, notNullValue());
    assertThat(details.results, notNullValue());
    assertThat(details.results.size(), is(2));
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0), request.components.get(0),
        MatchState.EXACT.getId(), new ArrayList<>(declaredLicenseSet), new ArrayList<>(observedLicenseSet),
        securityVulnerabilities, policies);
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1), request.components.get(1),
        MatchState.EXACT.getId(), Collections.<License>emptyList(), Collections.<License>emptyList(),
        Collections.<SecurityVulnerability>emptyList(), Collections.<String, Policy>emptyMap());
  }

  @Test
  public void testEvaluateComponents_withClaimedComponentEmptyVsNullClassifier() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier compIdentifierWithEmptyClassifier =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", "e1", "");
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(compIdentifierWithEmptyClassifier, null);
    request.components.add(component1);

    ComponentIdentifier compIdentifierWithNullClassifier =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g2", "a2", "v2", "e2");
    ApiComponentDTOV2 component2 = componentEvaluationV2Helper.createComponent(compIdentifierWithNullClassifier, null);
    request.components.add(component2);

    ComponentIdentifier expectedComponentIdentifier1 =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1");
    tempEntity.newClaimedComponent("h1", expectedComponentIdentifier1);
    ComponentIdentifier expectedComponentIdentifier2 =
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "e2");
    tempEntity.newClaimedComponent("h2", expectedComponentIdentifier2);

    // Create a mock return
    ComponentEvaluationDataList componentEvaluationDataList = createComponentEvaluationDataList(
        componentEvaluationV2Helper.createComponentEvaluationData(
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "", "e1"), component1.hash,
            MatchState.UNKNOWN, 0, Collections.<License>emptySet(), Collections.<License>emptySet(),
            Collections.<SecurityVulnerability>emptyList()),
        componentEvaluationV2Helper.createComponentEvaluationData(
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "e2"), component1.hash,
            MatchState.UNKNOWN, 1, Collections.<License>emptySet(), Collections.<License>emptySet(),
            Collections.<SecurityVulnerability>emptyList()));
    mockComponentDetails(componentEvaluationDataList);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = fromJson(response, ApiComponentEvaluationTicketDTOV2.class);

    response = getComponentEvaluationResult(getRestBaseUrl(), evaluationResult);
    assertResponseStatus(200, response);

    ApiComponentEvaluationResultDTOV2 details = fromJson(response, ApiComponentEvaluationResultDTOV2.class);
    assertThat(details, notNullValue());
    assertThat(details.isError, is(false));
    assertThat(details.errorMessage, nullValue());
    assertThat(details.applicationId, is(app.getId()));
    assertThat(details.evaluationDate, notNullValue());
    assertThat(details.submittedDate, notNullValue());
    assertThat(details.results, notNullValue());
    assertThat(details.results.size(), is(2));
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(0),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(expectedComponentIdentifier1), "h1",
        MatchState.EXACT.getId(), Collections.<License>emptyList(), Collections.<License>emptyList(),
        Collections.<SecurityVulnerability>emptyList(), Collections.<String, Policy>emptyMap());
    componentEvaluationV2Helper.assertComponentDetails(details.results.get(1),
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(expectedComponentIdentifier2), "h2",
        MatchState.EXACT.getId(), Collections.<License>emptyList(), Collections.<License>emptyList(),
        Collections.<SecurityVulnerability>emptyList(), Collections.<String, Policy>emptyMap());
  }

  @Test
  public void testEvaluateComponents_withClaimedComponentMissingExtension() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier compIdentifierWithNoExtension =
        componentEvaluationV2Helper.createMavenComponentIdentifier("g1", "a1", "v1", null);
    ApiComponentDTOV2 component1 = componentEvaluationV2Helper.createComponent(compIdentifierWithNoExtension, null);
    request.components.add(component1);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), StringStartsWith
        .startsWith("Coordinates missing the following required entries for the given format: [extension]"));
  }

  private void mockHDSInternalServiceError() {
    setSaasResponseForURI(ApiComponentEvaluationServiceV2.HDS_EVALUATION_COMPONENTS_PATH, "Internal Error", 500);

  }

  private void mockComponentDetails(final ComponentEvaluationDataList componentEvaluationDataList) {
    setSaasResponseForURI(ApiComponentEvaluationServiceV2.HDS_EVALUATION_COMPONENTS_PATH,
        toJson(componentEvaluationDataList), 200);
  }

  private ComponentEvaluationDataList createComponentEvaluationDataList(
      final ComponentEvaluationData... componentEvaluationData)
  {
    ComponentEvaluationDataList componentEvaluationDataList = new ComponentEvaluationDataList();
    componentEvaluationDataList.components = new ArrayList<>();
    Collections.addAll(componentEvaluationDataList.components, componentEvaluationData);
    return componentEvaluationDataList;
  }

  private String getComponentEvaluationURL(final String applicationId) {
    return getRestBaseUrl() + PublicApiPaths.APPLICATION_EVALUATION_PATH_V2 + "/" + applicationId;
  }

  private Response getComponentEvaluationResult(final String baseUrl,
      final ApiComponentEvaluationTicketDTOV2 evaluationResult)
      throws Exception
  {
    Response response = null;
    String url = baseUrl + evaluationResult.resultsUrl;

    boolean done = false;
    int tryCount = 1;
    while (!done) {
      response = AuthedRestAccess.get(url);
      if (response.getStatusCode() == 200 || tryCount >= NUM_TRIES) {
        done = true;
      }
      tryCount++;
      Thread.sleep(RETRY_INTERVAL);
    }

    return response;
  }
}
