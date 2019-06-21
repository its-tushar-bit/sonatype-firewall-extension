/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
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
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.purl.PurlIdentifier;

import org.assertj.core.groups.Tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ComponentEvaluationV2Helper
{
  private PolicyDAO policyDAO = new PolicyDAO();

  public ComponentEvaluationData createComponentEvaluationData(
      final ComponentIdentifier componentIdentifier,
      final String hash,
      final MatchState matchState,
      final int index,
      final Set<License> declaredLicenses,
      final Set<License> observedLicenses,
      final List<SecurityVulnerability> securityVulnerabilities,
      final Integer relativePopularity)
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
    componentEvaluationData.relativePopularity = relativePopularity;

    return componentEvaluationData;
  }

  private ApiComponentDTOV2 createComponent(final ComponentIdentifier componentIdentifier,
                                            final String hash,
                                            final String packageUrl)
  {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    component.hash = hash;
    component.packageUrl = packageUrl;
    return component;
  }

  public ApiComponentDTOV2 createComponent(final String packageUrl) {
    return createComponent(null, null, packageUrl);
  }

  public ApiComponentDTOV2 createComponent(final ComponentIdentifier componentIdentifier, final String hash) {
    return createComponent(componentIdentifier, hash, null);
  }

  public List<SecurityVulnerability> createSecurityVulnerabilities() {
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
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
    constraintOrg.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    constraints.add(constraintOrg);
    Policy policyOrg = new Policy(null, "Policy Name Org");
    policyOrg.setOwnerId(org.getId());
    policyOrg.setConstraints(constraints);
    policyOrg.setAction(stage.getStageTypeId(), FailActionType.ID);
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
    policyApp.setAction(stage.getStageTypeId(), FailActionType.ID);
    policyDAO.insert(policyApp);
    policies.put(policyApp.getId(), policyApp);

    return policies;
  }

  public void assertComponentDetails(final ApiComponentDetailsDTOV2 resultComponentDTO,
                                     final ApiComponentDTOV2 requestComponentDTO,
                                     final String matchState,
                                     final List<License> declaredLicenses,
                                     final List<License> observedLicenses,
                                     final List<SecurityVulnerability> securityVulnerabilities,
                                     final Integer relativePopularity,
                                     final Map<String, Policy> policies)
  {
    assertComponentDetails(resultComponentDTO, requestComponentDTO.componentIdentifier, requestComponentDTO.hash,
        matchState, declaredLicenses, observedLicenses, securityVulnerabilities, relativePopularity, policies);
  }

  public void assertComponentDetails(final ApiComponentDetailsDTOV2 resultComponentDTO,
                                     final ApiComponentIdentifierDTOV2 expectedComponentIdentifier,
                                     final String expectedHash,
                                     final String matchState,
                                     final List<License> declaredLicenses,
                                     final List<License> observedLicenses,
                                     final List<SecurityVulnerability> securityVulnerabilities,
                                     final Integer relativePopularity,
                                     final Map<String, Policy> policies)
  {
    assertThat(resultComponentDTO).isNotNull();
    assertThat(resultComponentDTO.component).isNotNull();
    assertThat(resultComponentDTO.component.componentIdentifier.getFormat())
        .isEqualTo(expectedComponentIdentifier.getFormat());
    assertThat(resultComponentDTO.component.componentIdentifier.getCoordinates())
        .isEqualTo(expectedComponentIdentifier.getCoordinates());
    assertThat(resultComponentDTO.component.hash).isEqualTo(expectedHash);
    assertThat(resultComponentDTO.matchState).isEqualTo(matchState);
    assertThat(resultComponentDTO.relativePopularity).isEqualTo(relativePopularity);

    assertThat(resultComponentDTO.licenseData).isNotNull();
    assertThat(resultComponentDTO.licenseData.declaredLicenses).extracting(dto -> dto.licenseId, dto -> dto.licenseName)
        .containsExactly(declaredLicenses.stream()
            .map(license -> tuple(license.getLicenseId(), license.getLicenseName())).toArray(Tuple[]::new));
    assertThat(resultComponentDTO.licenseData.observedLicenses).extracting(dto -> dto.licenseId, dto -> dto.licenseName)
        .containsExactly(observedLicenses.stream()
            .map(license -> tuple(license.getLicenseId(), license.getLicenseName())).toArray(Tuple[]::new));
    assertThat(resultComponentDTO.licenseData.overriddenLicenses).isEmpty();

    assertThat(resultComponentDTO.securityData).isNotNull();
    assertThat(resultComponentDTO.securityData.securityIssues).hasSameSizeAs(securityVulnerabilities);
    for (int i = 0; i < securityVulnerabilities.size(); i++) {
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).source).isEqualTo(securityVulnerabilities.get(i)
          .getSource());
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).reference)
          .isEqualTo(securityVulnerabilities.get(i).getRefId());
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).severity)
          .isEqualTo(securityVulnerabilities.get(i).getSeverity());
      assertThat(resultComponentDTO.securityData.securityIssues.get(i).url)
          .isEqualTo(securityVulnerabilities.get(i).getUrl());
    }

    assertThat(resultComponentDTO.policyData).isNotNull();
    assertThat(resultComponentDTO.policyData.policyViolations).hasSize(policies.size());
    for (ApiPolicyViolationDTOV2 violation : resultComponentDTO.policyData.policyViolations) {
      assertThat(violation.policyId).isEqualTo(policies.get(violation.policyId).getId());
      assertThat(violation.policyName).isEqualTo(policies.get(violation.policyId).getName());
    }
  }

  public ComponentEvaluationDataRequestList toHdsRequest(ApiComponentEvaluationRequestDTOV2 clmRequest) {
    ComponentEvaluationDataRequestList hdsRequest = new ComponentEvaluationDataRequestList();
    hdsRequest.components = new ArrayList<>();
    for (ApiComponentDTOV2 componentDTO : clmRequest.components) {
      ComponentEvaluationDataRequest componentEvaluationDataRequest = new ComponentEvaluationDataRequest();
      componentEvaluationDataRequest.hash = componentDTO.hash;
      if (componentDTO.packageUrl != null) {
        componentEvaluationDataRequest.componentIdentifier =
            new PurlIdentifier(componentDTO.packageUrl).ensureCompleteIdentifier();
      }
      else if (componentDTO.componentIdentifier != null) {
        componentEvaluationDataRequest.componentIdentifier =
            new ComponentIdentifier(componentDTO.componentIdentifier.getFormat(),
                componentDTO.componentIdentifier.getCoordinates());
        componentEvaluationDataRequest.componentIdentifier.ensureComplete();
      }
      hdsRequest.components.add(componentEvaluationDataRequest);
    }
    return hdsRequest;
  }
}
