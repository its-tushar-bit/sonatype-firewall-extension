/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDefinitionDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentIdentifierValidator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
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

  private static final int NUM_TRIES = 10;

  private Organization org;

  private Application app;

  private PolicyDAO policyDAO = new PolicyDAO();

  @Before
  public void setupApplication() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testEvaluateComponents_invalidComponentIdentifier() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ComponentIdentifier componentIdentifier = createMavenComponentIdentifier("g1", "a1", "v1", null);
    ApiComponentDefinitionDTOV2 component = createComponent(componentIdentifier, "h1");
    request.components.add(component);

    String url = getComponentEvaluationURL(app.getId());
    Response response = AuthedRestAccess.post(url, toJson(request));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is(ApiComponentIdentifierValidator.MISSING_COORDINATES + "[extension]"));
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
    ComponentIdentifier componentIdentifier = createMavenComponentIdentifier("g1", "a1", "v1", "e1");
    ApiComponentDefinitionDTOV2 component = createComponent(componentIdentifier, "h1");
    request.components.add(component);

    mockHDSInternalServiceError(componentIdentifier);

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
    Map<String, Policy> policies = createPolicies();
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    ComponentIdentifier componentIdentifier1 = createMavenComponentIdentifier("g1", "a1", "v1", "e1");
    ApiComponentDefinitionDTOV2 component1 = createComponent(componentIdentifier1, "h1");
    request.components.add(component1);

    ComponentIdentifier componentIdentifier2 = createMavenComponentIdentifier("g2", "a2", "v2", "e2");
    ApiComponentDefinitionDTOV2 component2 = createComponent(componentIdentifier2, "h2");
    request.components.add(component2);

    // Create a mock return for the first component
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(
        Arrays.asList(new License("Apache-2.0", "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(Arrays.asList(new License("ATT", "ATT")));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    mockComponentDetails(componentIdentifier1, component1.hash, declaredLicenseSet, observedLicenseSet,
        securityVulnerabilities);

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
    assertComponentDetails(details.results.get(0), request.components.get(0), MatchState.EXACT.getId(),
        new ArrayList<>(declaredLicenseSet), new ArrayList<>(observedLicenseSet), securityVulnerabilities, policies);
    assertComponentDetails(details.results.get(1), request.components.get(1), MatchState.UNKNOWN.getId(),
        Collections.<License>emptyList(), Collections.<License>emptyList(),
        Collections.<SecurityVulnerability>emptyList(), Collections.<String, Policy>emptyMap());
  }

  private Map<String, Policy> createPolicies() {

    LinkedHashMap<String, Policy> policies = new LinkedHashMap<>();

    Stage stage = new Stage(DevelopStageType.ID);

    // Create org policy
    List<Constraint> constraints = new ArrayList<>();
    Constraint constraintOrg = new Constraint(null, "Constraint Name Org", LogicalOperator.AND);
    constraintOrg.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    constraints.add(constraintOrg);
    Policy policyOrg = new Policy(null, "Policy Name Org");
    policyOrg.setOwnerId(org.getId());
    policyOrg.setConstraints(constraints);
    policyOrg.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));
    policyDAO.insert(policyOrg);
    policies.put(policyOrg.getId(), policyOrg);

    // Create app policy
    constraints = new ArrayList<>();
    Constraint constraintApp = new Constraint(null, "Constraint Name App", LogicalOperator.AND);
    constraintApp.addCondition(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    constraints.add(constraintApp);
    Policy policyApp = new Policy(null, "Policy Name App");
    policyApp.setOwnerId(app.getId());
    policyApp.setConstraints(constraints);
    policyApp.addAction(stage.getStageTypeId(), new Action(FailActionType.ID));
    policyDAO.insert(policyApp);
    policies.put(policyApp.getId(), policyApp);

    return policies;
  }

  private List<SecurityVulnerability> createSecurityVulnerabilities() {
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("refId");
    securityVulnerability.setSeverity(5.0F);
    securityVulnerability.setSource("source");
    securityVulnerabilities.add(securityVulnerability);
    return securityVulnerabilities;
  }

  private void mockHDSInternalServiceError(final ComponentIdentifier componentIdentifier) {
    String uri = UriBuilder.fromPath("rest/ci/componentDetails")
        .queryParam("componentIdentifier", toJson(componentIdentifier)).build().toString();
    setSaasResponseForURI(uri, "Internal Error", 500);

  }

  private void mockComponentDetails(final ComponentIdentifier componentIdentifier, final String hash,
      final Set<License> declaredLicenses, final Set<License> observedLicenses,
      final List<SecurityVulnerability> securityVulnerabilities)
      throws Exception
  {
    NamedComponentDetails componentDetails = new NamedComponentDetails();
    componentDetails.setHash(hash);
    componentDetails.setComponentIdentifier(componentIdentifier);

    componentDetails.setDeclaredLicenses(declaredLicenses);
    componentDetails.setObservedLicenses(observedLicenses);
    componentDetails.setSecurityVulnerabilities(securityVulnerabilities);

    String uri = UriBuilder.fromPath("rest/ci/componentDetails")
        .queryParam("componentIdentifier", toJson(componentIdentifier)).build().toString();
    setSaasResponseForURI(uri, toJson(componentDetails), 200);
  }

  private void assertComponentDetails(final ApiComponentDetailsDTOV2 resultComponentDTO,
      final ApiComponentDefinitionDTOV2 requestComponentDTO, final String matchState,
      final List<License> declaredLicenses, final List<License> observedLicenses,
      final List<SecurityVulnerability> securityVulnerabilities, final Map<String, Policy> policies)
  {
    assertThat(resultComponentDTO, notNullValue());
    assertThat(resultComponentDTO.component, notNullValue());
    assertThat(resultComponentDTO.component.componentIdentifier.getFormat(),
        is(requestComponentDTO.componentIdentifier.getFormat()));
    assertThat(resultComponentDTO.component.componentIdentifier.getCoordinates(),
        is(requestComponentDTO.componentIdentifier.getCoordinates()));
    assertThat(resultComponentDTO.component.hash, is(requestComponentDTO.hash));
    assertThat(resultComponentDTO.matchState, is(matchState));


    assertThat(resultComponentDTO.licenseData, notNullValue());
    assertThat(resultComponentDTO.licenseData.declaredLicenses.size(), is(declaredLicenses.size()));
    for (int i = 0; i < declaredLicenses.size(); i++) {
      assertThat(resultComponentDTO.licenseData.declaredLicenses.get(i).licenseId,
          is(declaredLicenses.get(i).getLicenseId()));
      assertThat(resultComponentDTO.licenseData.declaredLicenses.get(i).licenseName,
          is(declaredLicenses.get(i).getLicenseName()));
    }

    assertThat(resultComponentDTO.licenseData.observedLicenses.size(), is(observedLicenses.size()));
    for (int i = 0; i < observedLicenses.size(); i++) {
      assertThat(resultComponentDTO.licenseData.observedLicenses.get(i).licenseId,
          is(observedLicenses.get(i).getLicenseId()));
      assertThat(resultComponentDTO.licenseData.observedLicenses.get(i).licenseName,
          is(observedLicenses.get(i).getLicenseName()));
    }
    assertThat(resultComponentDTO.licenseData.overriddenLicenses.size(), is(0));

    assertThat(resultComponentDTO.securityData, notNullValue());
    assertThat(resultComponentDTO.securityData.securityIssues.size(), is(securityVulnerabilities.size()));
    for (int i = 0; i < securityVulnerabilities.size(); i++) {
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).source,
          is(securityVulnerabilities.get(i).getSource()));
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).reference,
          is(securityVulnerabilities.get(i).getRefId()));
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).severity,
          is(securityVulnerabilities.get(i).getSeverity()));
    }

    assertThat(resultComponentDTO.policyData, notNullValue());
    assertThat(resultComponentDTO.policyData.policyViolations.size(), is(policies.size()));
    for (ApiComponentPolicyViolationDTOV2 violation : resultComponentDTO.policyData.policyViolations) {
      assertThat(violation.policyId, is(policies.get(violation.policyId).getId()));
      assertThat(violation.policyName, is(policies.get(violation.policyId).getName()));
    }
  }

  private ComponentIdentifier createMavenComponentIdentifier(final String groupId, final String artifactId,
      final String version, final String extension)
  {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.MAVEN_GROUP_ID, groupId);
    coordinates.put(ComponentIdentifier.MAVEN_ARTIFACT_ID, artifactId);
    coordinates.put(ComponentIdentifier.VERSION, version);
    if (extension != null) {
      coordinates.put(ComponentIdentifier.MAVEN_EXTENSION, extension);
    }
    return new ComponentIdentifier(ComponentIdentifier.FORMAT_MAVEN, coordinates);
  }

  private ApiComponentDefinitionDTOV2 createComponent(final ComponentIdentifier componentIdentifier,
      final String hash)
  {
    ApiComponentDefinitionDTOV2 component = new ApiComponentDefinitionDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    component.hash = hash;
    return component;
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
