/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.componentsearch.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchPageResultDTO;
import com.sonatype.insight.brain.componentsearch.model.ComponentMatchSortField;
import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.AffectedComponentList;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for CveAffectedComponentSearchService.
 * Tests that users only see CVE-affected components from applications they have READ permission for.
 */
public class CveAffectedComponentSearchServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private CveAffectedComponentSearchService cveAffectedComponentSearchService;

  private static final String CVE_ID = "CVE-2025-55182";

  private static final Set<String> CVE_IDS = Set.of(CVE_ID);

  private Application app2;

  private ComponentIdentifier componentId1;

  private ComponentIdentifier componentId2;

  @Before
  public void setUpHdsMock() {
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
    setBaseUrl("http://localhost:8070");
    setupHdsMocks();
  }

  @Override
  protected void setUpSecurity() {
    super.setUpSecurity();

    // Create second application in same org
    app2 = tempEntity.newApplication("App2", "app2", org.getId());

    // Create components for both apps
    componentId1 = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "vulnerable-lib",
        "1.0.0",
        "",
        "jar"
    );

    componentId2 = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "another-lib",
        "2.0.0",
        "",
        "jar"
    );

    // Setup evaluations and components for app1
    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-app1",
        new Date()
    );

    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-app1",
        componentId1,
        "pkg:maven/com.example/vulnerable-lib@1.0.0?type=jar"
    );

    // Setup evaluations and components for app2
    tempEntity.newPolicyEvaluation(
        app2.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-app2",
        new Date()
    );

    tempEntity.newApplicationComponent(
        app2.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-app2",
        componentId2,
        "pkg:maven/com.example/another-lib@2.0.0?type=jar"
    );
  }

  private void setupHdsMocks() {
    List<AffectedComponentDTO> affectedComponents = List.of(
        new AffectedComponentDTO("maven", "com.example", "vulnerable-lib", "1.0.0", null),
        new AffectedComponentDTO("maven", "com.example", "another-lib", "2.0.0", null)
    );
    hdsMockServer.respondWith(new AffectedComponentList(affectedComponents, null, null))
        .atUri("/rest/vulnerability/affected?refId=" + CVE_ID).withoutLicense();

    String vulnDataJson = """
        {
          "vulnerabilities": {
            "CVE-2025-55182": {
              "identifier": "CVE-2025-55182",
              "severity": "HIGH",
              "cvssScore": 7.5
            }
          }
        }
        """;
    hdsMockServer.respondWith(vulnDataJson)
        .atUri("/rest/vulnerability/details/json");

    hdsMockServer.respondWith(new com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions[0])
        .atUri("/rest/component/vulnerabilities/nearestFixedVersions");
  }

  // ========== searchCveAffectedComponentsPaginated tests ==========

  @Test
  public void testSearchCveAffectedComponentsPaginated_Authorized() {
    grantReadPermission(app.getId());

    ComponentSearchPageResultDTO result = cveAffectedComponentSearchService
        .searchCveAffectedComponentsPaginated(CVE_IDS, 1, 10, null, "asc");

    assertThat(result).isNotNull();
    assertThat(result.getTotalCount()).isEqualTo(1);
    assertThat(result.getResults()).hasSize(1);
    assertThat(result.getResults()).extracting(ApplicationComponentMatchDTO::getApplicationInternalId)
        .containsExactly(app.getId());
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_OnlySeesAuthorizedApps() {
    // User has READ on app1 only
    grantReadPermission(app.getId());

    ComponentSearchPageResultDTO result = cveAffectedComponentSearchService
        .searchCveAffectedComponentsPaginated(CVE_IDS, 1, 10, null, "asc");

    // Should only see app1's component
    assertThat(result).isNotNull();
    assertThat(result.getTotalCount()).isEqualTo(1);
    assertThat(result.getResults()).extracting(ApplicationComponentMatchDTO::getApplicationInternalId)
        .containsExactly(app.getId());

    // Now grant READ on app2
    grantReadPermission(app2.getId());
    result = cveAffectedComponentSearchService
        .searchCveAffectedComponentsPaginated(CVE_IDS, 1, 10, null, "asc");

    // Should now see both apps' components
    assertThat(result.getTotalCount()).isEqualTo(2);
    assertThat(result.getResults()).extracting(ApplicationComponentMatchDTO::getApplicationInternalId)
        .containsExactlyInAnyOrder(app.getId(), app2.getId());
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_Unauthenticated() {
    ComponentSearchPageResultDTO result = cveAffectedComponentSearchService
        .searchCveAffectedComponentsPaginated(CVE_IDS, 1, 10, null, "asc");

    assertThat(result).isNotNull();
    assertThat(result.getTotalCount()).isEqualTo(0);
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_Unauthorized() {
    login();

    ComponentSearchPageResultDTO result = cveAffectedComponentSearchService
        .searchCveAffectedComponentsPaginated(CVE_IDS, 1, 10, null, "asc");

    assertThat(result).isNotNull();
    assertThat(result.getTotalCount()).isEqualTo(0);
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_WithSorting_OnlySeesAuthorizedApps() {
    grantReadPermission(app.getId());

    ComponentSearchPageResultDTO result = cveAffectedComponentSearchService
        .searchCveAffectedComponentsPaginated(
            CVE_IDS, 1, 10, ComponentMatchSortField.APPLICATION_NAME, "asc");

    assertThat(result.getTotalCount()).isEqualTo(1);
    assertThat(result.getResults()).extracting(ApplicationComponentMatchDTO::getApplicationInternalId)
        .containsExactly(app.getId());
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_WithPagination_OnlySeesAuthorizedApps() {
    grantReadPermission(app.getId());
    grantReadPermission(app2.getId());

    // Page 1 with size 1
    ComponentSearchPageResultDTO result = cveAffectedComponentSearchService
        .searchCveAffectedComponentsPaginated(CVE_IDS, 1, 1, null, "asc");

    assertThat(result.getTotalCount()).isEqualTo(2);
    assertThat(result.getResults()).hasSize(1);

    // Page 2 with size 1
    result = cveAffectedComponentSearchService
        .searchCveAffectedComponentsPaginated(CVE_IDS, 2, 1, null, "asc");

    assertThat(result.getTotalCount()).isEqualTo(2);
    assertThat(result.getResults()).hasSize(1);
  }

  // ========== searchCveAffectedComponentsStreaming tests ==========

  @Test
  public void testSearchCveAffectedComponentsStreaming_Authorized() {
    grantReadPermission(app.getId());

    List<ApplicationComponentMatchDTO> results = cveAffectedComponentSearchService
        .searchCveAffectedComponentsStreaming(CVE_IDS)
        .collect(Collectors.toList());

    assertThat(results).hasSize(1);
    assertThat(results).extracting(ApplicationComponentMatchDTO::getApplicationInternalId)
        .containsExactly(app.getId());
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_OnlySeesAuthorizedApps() {
    // User has READ on app1 only
    grantReadPermission(app.getId());

    List<ApplicationComponentMatchDTO> results = cveAffectedComponentSearchService
        .searchCveAffectedComponentsStreaming(CVE_IDS)
        .collect(Collectors.toList());

    // Should only see app1's component
    assertThat(results).hasSize(1);
    assertThat(results).extracting(ApplicationComponentMatchDTO::getApplicationInternalId)
        .containsExactly(app.getId());

    // Now grant READ on app2
    grantReadPermission(app2.getId());
    results = cveAffectedComponentSearchService
        .searchCveAffectedComponentsStreaming(CVE_IDS)
        .collect(Collectors.toList());

    // Should now see both apps' components
    assertThat(results).hasSize(2);
    assertThat(results).extracting(ApplicationComponentMatchDTO::getApplicationInternalId)
        .containsExactlyInAnyOrder(app.getId(), app2.getId());
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_Unauthenticated() {
    List<ApplicationComponentMatchDTO> results = cveAffectedComponentSearchService
        .searchCveAffectedComponentsStreaming(CVE_IDS)
        .collect(Collectors.toList());

    assertThat(results).isEmpty();
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_Unauthorized() {
    login();

    List<ApplicationComponentMatchDTO> results = cveAffectedComponentSearchService
        .searchCveAffectedComponentsStreaming(CVE_IDS)
        .collect(Collectors.toList());

    assertThat(results).isEmpty();
  }

  // ========== Violating column tests ==========

  @Test
  public void testSearchCveAffectedComponents_ViolatingNo_WhenNoPolicyViolation() {
    // Grant permission to see the app
    grantReadPermission(app.getId());

    // Create a component that is affected by CVE but has NO policy violation
    ComponentIdentifier nonViolatingComponent = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "non-violating-lib",
        "1.5.0",
        "",
        "jar"
    );

    // Add this component to the app (without creating a policy violation)
    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-non-violating",
        nonViolatingComponent,
        "pkg:maven/com.example/non-violating-lib@1.5.0?type=jar"
    );

    // Update HDS mock to include this component as affected
    List<AffectedComponentDTO> affectedComponents = List.of(
        new AffectedComponentDTO("maven", "com.example", "vulnerable-lib", "1.0.0", null),
        new AffectedComponentDTO("maven", "com.example", "non-violating-lib", "1.5.0", null)
    );
    hdsMockServer.respondWith(new AffectedComponentList(affectedComponents, null, null))
        .atUri("/rest/vulnerability/affected?refId=" + CVE_ID).withoutLicense();

    List<ApplicationComponentMatchDTO> results = cveAffectedComponentSearchService
        .searchCveAffectedComponentsStreaming(CVE_IDS)
        .collect(Collectors.toList());

    // Should find at least the non-violating component
    assertThat(results).isNotEmpty();

    // Find the non-violating component in results
    ApplicationComponentMatchDTO nonViolatingMatch = results.stream()
        .filter(r -> r.getPackageUrl().contains("non-violating-lib"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected to find non-violating-lib in results"));

    assertThat(nonViolatingMatch.getViolating())
        .as("Component affected by CVE but without policy violation should have Violating=false")
        .isFalse();
  }

  @Test
  public void testSearchCveAffectedComponents_ViolatingYes_WhenPolicyViolationExists() {
    // Grant permission to see the app
    grantReadPermission(app.getId());

    // Create a component with a policy violation
    ComponentIdentifier violatingComponent = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "violating-lib",
        "2.0.0",
        "",
        "jar"
    );

    // Get the existing policy evaluation for the app
    com.sonatype.insight.brain.model.policy.PolicyEvaluation evaluation =
        tempEntity.newPolicyEvaluation(
            app.getId(),
            StageTypes.STAGE_RELEASE.getId(),
            "scan-violating",
            new Date()
        );

    // Add component to app
    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-violating",
        violatingComponent,
        "pkg:maven/com.example/violating-lib@2.0.0?type=jar"
    );

    // Create a policy and violation for this component related to the CVE
    com.sonatype.insight.brain.model.policy.Policy policy =
        tempEntity.newPolicy(app.getOrganizationId(), "Security Policy", 9);

    com.sonatype.insight.brain.model.policy.PolicyViolation violation =
        tempEntity.newPolicyViolation(
            evaluation,
            policy,
            violatingComponent,
            "hash-violating",
            "Security vulnerability detected"
        );

    // Add CVE reference to the violation's constraint facts
    com.sonatype.clm.dto.model.policy.TriggerReference cveRef =
        new com.sonatype.clm.dto.model.policy.TriggerReference(
            com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
            CVE_ID
        );
    com.sonatype.clm.dto.model.policy.ConditionFact conditionFact =
        new com.sonatype.clm.dto.model.policy.ConditionFact(
            com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType.ID,
            0,
            "Security vulnerability",
            "Contains " + CVE_ID,
            cveRef
        );
    com.sonatype.clm.dto.model.policy.ConstraintFact constraintFact =
        new com.sonatype.clm.dto.model.policy.ConstraintFact(
            "security-constraint",
            "Security Constraint",
            "AND"
        );
    constraintFact.addConditionFact(conditionFact);
    violation.setConstraintFacts(List.of(constraintFact));

    // Persist the constraint facts
    tempEntity.updatePolicyViolation(violation);

    // Update HDS mock to include this component as affected
    List<AffectedComponentDTO> affectedComponents = List.of(
        new AffectedComponentDTO("maven", "com.example", "vulnerable-lib", "1.0.0", null),
        new AffectedComponentDTO("maven", "com.example", "violating-lib", "2.0.0", null)
    );
    hdsMockServer.respondWith(new AffectedComponentList(affectedComponents, null, null))
        .atUri("/rest/vulnerability/affected?refId=" + CVE_ID).withoutLicense();

    List<ApplicationComponentMatchDTO> results = cveAffectedComponentSearchService
        .searchCveAffectedComponentsStreaming(CVE_IDS)
        .collect(Collectors.toList());

    // Find the violating component in results
    ApplicationComponentMatchDTO violatingMatch = results.stream()
        .filter(r -> r.getPackageUrl().contains("violating-lib"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected to find violating-lib in results"));

    assertThat(violatingMatch.getViolating())
        .as("Component with policy violation should have Violating=true")
        .isTrue();
  }
}
