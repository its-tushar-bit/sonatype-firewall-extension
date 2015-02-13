/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.rhc.RepoHealthCheckSecurityVulnerability;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ComponentEvaluationV2Helper
{
  private PolicyDAO policyDAO = new PolicyDAO();

  public ComponentEvaluationData createComponentEvaluationData(final ComponentIdentifier componentIdentifier,
      final String hash, final MatchState matchState, final int index,
      final Set<License> declaredLicenses, final Set<License> observedLicenses,
 final List<RepoHealthCheckSecurityVulnerability> securityVulnerabilities)
  {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.requestIndex = index;
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.matchState = matchState.getId();
    componentEvaluationData.declaredLicenses = declaredLicenses;
    componentEvaluationData.observedLicenses = observedLicenses;
    componentEvaluationData.catalogDate = new Date().getTime();
    componentEvaluationData.securityVulnerabilities = securityVulnerabilities;

    return componentEvaluationData;
  }

  public ApiComponentDTOV2 createComponent(final ComponentIdentifier componentIdentifier,
      final String hash)
  {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    component.hash = hash;
    return component;
  }

  public ComponentIdentifier createMavenComponentIdentifier(final String groupId, final String artifactId,
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

  public List<RepoHealthCheckSecurityVulnerability> createSecurityVulnerabilities() {
    List<RepoHealthCheckSecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    RepoHealthCheckSecurityVulnerability securityVulnerability = new RepoHealthCheckSecurityVulnerability();
    securityVulnerability.setRefId("refId");
    securityVulnerability.setSeverity(5.0F);
    securityVulnerability.setSource("source");
    securityVulnerability.setUrl("test-url");
    securityVulnerabilities.add(securityVulnerability);
    return securityVulnerabilities;
  }

  public Map<String, Policy> createPolicies(final Organization org, final Application app) {

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

  public void assertComponentDetails(final ApiComponentDetailsDTOV2 resultComponentDTO,
      final ApiComponentDTOV2 requestComponentDTO, final String matchState, final List<License> declaredLicenses,
      final List<License> observedLicenses, final List<RepoHealthCheckSecurityVulnerability> securityVulnerabilities,
      final Map<String, Policy> policies)
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
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).url, is(securityVulnerabilities.get(i).getUrl()));
    }

    assertThat(resultComponentDTO.policyData, notNullValue());
    assertThat(resultComponentDTO.policyData.policyViolations.size(), is(policies.size()));
    for (ApiPolicyViolationDTOV2 violation : resultComponentDTO.policyData.policyViolations) {
      assertThat(violation.policyId, is(policies.get(violation.policyId).getId()));
      assertThat(violation.policyName, is(policies.get(violation.policyId).getName()));
    }
  }
}
