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
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.componentsearch.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchAggregatesDTO;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchPageResultDTO;
import com.sonatype.insight.brain.componentsearch.model.ComponentMatchSortField;
import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for CveAffectedComponentSearchService.
 * Tests the service end-to-end from service layer through DAOs to the database,
 * using HdsMockServer to mock external HDS calls.
 */
public class CveAffectedComponentSearchServiceTest extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private CveAffectedComponentSearchService service;

  @Inject
  private ProductLicense productLicense;

  @Before
  public void setUpHdsMock() {
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
    setBaseUrl("http://localhost:8070");
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_ReturnsCorrectPage() {
    Application app1 = tempEntity.newApplication("Application One", "app1", Organization.ROOT_ORGANIZATION_ID);
    Application app2 = tempEntity.newApplication("Application Two", "app2", Organization.ROOT_ORGANIZATION_ID);

    for (Application app : List.of(app1, app2)) {
      tempEntity.newPolicyEvaluation(
          app.getId(),
          StageTypes.STAGE_RELEASE.getId(),
          "scan-" + app.getPublicId(),
          new Date()
      );

      ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
          "com.example",
          "vulnerable-lib",
          "1.0.0"
      );

      tempEntity.newApplicationComponent(
          app.getId(),
          StageTypes.STAGE_RELEASE.getId(),
          "hash-123",
          componentId,
          "pkg:maven/com.example/vulnerable-lib@1.0.0"
      );
    }

    List<AffectedComponentDTO> affectedComponents = List.of(
        new AffectedComponentDTO("maven", "com.example", "vulnerable-lib", "1.0.0")
    );
    hdsMockServer.respondWith(affectedComponents)
        .atUri("/rest/vulnerability/affected/CVE-2025-55182").withoutLicense();

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
        .atUri("/rest/vulnerability/details/json").withoutLicense();

    hdsMockServer.respondWith(List.of())
        .atUri("/api/v2/component/nearestFixedVersions").withoutLicense();

    ComponentSearchPageResultDTO result = service.searchCveAffectedComponentsPaginated(
        Set.of("CVE-2025-55182"),
        1,
        1,
        null,
        "asc"
    );

    assertThat(result.getPageNumber()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(1);
    assertThat(result.getTotalCount()).isEqualTo(2);
    assertThat(result.getResults()).hasSize(1);

    ComponentSearchAggregatesDTO aggregates = result.getAggregates();
    assertThat(aggregates.getTotalAffectedApplications()).isEqualTo(2);
    assertThat(aggregates.getAffectedComponents()).isEqualTo(1);
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_SortsByApplicationName() {
    Application appZ = tempEntity.newApplication("Zebra App", "appz", Organization.ROOT_ORGANIZATION_ID);
    Application appA = tempEntity.newApplication("Alpha App", "appa", Organization.ROOT_ORGANIZATION_ID);

    for (Application app : List.of(appZ, appA)) {
      tempEntity.newPolicyEvaluation(
          app.getId(),
          StageTypes.STAGE_RELEASE.getId(),
          "scan-" + app.getPublicId(),
          new Date()
      );

      ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
          "com.example",
          "vulnerable-lib",
          "1.0.0"
      );

      tempEntity.newApplicationComponent(
          app.getId(),
          StageTypes.STAGE_RELEASE.getId(),
          "hash-123",
          componentId,
          "pkg:maven/com.example/vulnerable-lib@1.0.0"
      );
    }

    List<AffectedComponentDTO> affectedComponents = List.of(
        new AffectedComponentDTO("maven", "com.example", "vulnerable-lib", "1.0.0")
    );
    hdsMockServer.respondWith(affectedComponents)
        .atUri("/rest/vulnerability/affected/CVE-2025-55182").withoutLicense();

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
        .atUri("/rest/vulnerability/details/json").withoutLicense();

    hdsMockServer.respondWith(List.of())
        .atUri("/api/v2/component/nearestFixedVersions").withoutLicense();

    ComponentSearchPageResultDTO result = service.searchCveAffectedComponentsPaginated(
        Set.of("CVE-2025-55182"),
        1,
        10,
        ComponentMatchSortField.APPLICATION_NAME,
        "asc"
    );

    List<ApplicationComponentMatchDTO> results = result.getResults();
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getApplicationName()).isEqualTo("Alpha App");
    assertThat(results.get(1).getApplicationName()).isEqualTo("Zebra App");

    hdsMockServer.respondWith(affectedComponents)
        .atUri("/rest/vulnerability/affected/CVE-2025-55182").withoutLicense();

    hdsMockServer.respondWith(vulnDataJson)
        .atUri("/rest/vulnerability/details/json").withoutLicense();

    hdsMockServer.respondWith(List.of())
        .atUri("/api/v2/component/nearestFixedVersions").withoutLicense();

    result = service.searchCveAffectedComponentsPaginated(
        Set.of("CVE-2025-55182"),
        1,
        10,
        ComponentMatchSortField.APPLICATION_NAME,
        "desc"
    );

    results = result.getResults();
    assertThat(results.get(0).getApplicationName()).isEqualTo("Zebra App");
    assertThat(results.get(1).getApplicationName()).isEqualTo("Alpha App");
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_CalculatesAggregatesCorrectly() {
    Application app1 = tempEntity.newApplication("App 1", "app1", Organization.ROOT_ORGANIZATION_ID);
    Application app2 = tempEntity.newApplication("App 2", "app2", Organization.ROOT_ORGANIZATION_ID);
    Application app3 = tempEntity.newApplication("App 3", "app3", Organization.ROOT_ORGANIZATION_ID);

    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test Policy", 5);

    for (Application app : List.of(app1, app2, app3)) {
      ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("com.example",
          "vulnerable-lib", "1.0.0");

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
          app.getId(),
          StageTypes.STAGE_RELEASE.getId(),
          "scan-" + app.getPublicId(),
          new Date()
      );

      ApplicationComponent component = tempEntity.newApplicationComponent(
          app.getId(),
          StageTypes.STAGE_RELEASE.getId(),
          "hash-" + app.getPublicId(),
          componentId,
          "pkg:maven/com.example/vulnerable-lib@1.0.0"
      );

      PolicyViolation violation = tempEntity.newPolicyViolation(
          evaluation,
          policy,
          componentId,
          component.getHash(),
          "Test violation"
      );

      TriggerReference cveRef = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
          "CVE-2025-55182");
      ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0,
          "Security vulnerability", "Contains CVE-2025-55182", cveRef);
      ConstraintFact constraintFact = new ConstraintFact("security-constraint", "Security Constraint", "AND");
      constraintFact.addConditionFact(conditionFact);
      violation.setConstraintFacts(List.of(constraintFact));

      if (app == app1) {
        PolicyWaiver waiver = tempEntity.newWaiver(
            violation.getHash(),
            policy.getId(),
            app.getId(),
            "Test waiver"
        );
        violation.setWaiveTime(new Date());
        violation.setPolicyWaiverId(waiver.getId());
      }

      tempEntity.updatePolicyViolation(violation);
    }

    setupHdsMocksForStandardSearch();

    ComponentSearchPageResultDTO result = service.searchCveAffectedComponentsPaginated(
        Set.of("CVE-2025-55182"),
        1,
        10,
        null,
        "asc"
    );

    ComponentSearchAggregatesDTO aggregates = result.getAggregates();
    assertThat(aggregates.getTotalAffectedApplications()).isEqualTo(3);
    assertThat(aggregates.getAffectedComponents()).isEqualTo(1);
    assertThat(aggregates.getViolatingComponents()).isEqualTo(1);
    assertThat(aggregates.getActiveWaivers()).isEqualTo(1);
  }

  @Test
  public void testSearchCveAffectedComponentsPaginated_PaginationBeyondBounds() {
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date()
    );

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("com.example",
        "vulnerable-lib", "1.0.0");
    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-123",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0"
    );

    setupHdsMocksForStandardSearch();

    ComponentSearchPageResultDTO result = service.searchCveAffectedComponentsPaginated(
        Set.of("CVE-2025-55182"),
        5,
        10,
        null,
        "asc"
    );

    assertThat(result.getPageNumber()).isEqualTo(5);
    assertThat(result.getTotalCount()).isEqualTo(1);
    assertThat(result.getResults()).isEmpty();
    assertThat(result.getAggregates().getTotalAffectedApplications()).isEqualTo(1);
  }

  private void setupHdsMocksForStandardSearch() {
    List<AffectedComponentDTO> affectedComponents = List.of(
        new AffectedComponentDTO("maven", "com.example", "vulnerable-lib", "1.0.0")
    );
    hdsMockServer.respondWith(affectedComponents)
        .atUri("/rest/vulnerability/affected/CVE-2025-55182").withoutLicense();

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
        .atUri("/rest/vulnerability/details/json").withoutLicense();

    hdsMockServer.respondWith(List.of())
        .atUri("/api/v2/component/nearestFixedVersions").withoutLicense();
  }

  // ========== searchCveAffectedComponentsStreaming tests ==========

  @Test
  public void testSearchCveAffectedComponentsStreaming_FindsMatchingComponent() {
    Application app = tempEntity.newApplication("Test Application", "testapp", Organization.ROOT_ORGANIZATION_ID);

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date()
    );

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "vulnerable-lib",
        "1.0.0"
    );

    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-123",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0"
    );

    setupHdsMocksForStandardSearch();

    Stream<ApplicationComponentMatchDTO> results =
        service.searchCveAffectedComponentsStreaming(Set.of("CVE-2025-55182"));

    List<ApplicationComponentMatchDTO> resultList = results.collect(Collectors.toList());
    assertThat(resultList).hasSize(1);

    ApplicationComponentMatchDTO match = resultList.get(0);
    assertThat(match.getApplicationPublicId()).isEqualTo("testapp");
    assertThat(match.getApplicationName()).isEqualTo("Test Application");
    assertThat(match.getStage()).isEqualTo(StageTypes.STAGE_RELEASE.getId());
    assertThat(match.getPackageUrl()).isEqualTo("pkg:maven/com.example/vulnerable-lib@1.0.0");
    assertThat(match.getComponentDisplayName()).isEqualTo("com.example : vulnerable-lib ");
    assertThat(match.getCveId()).isEqualTo("CVE-2025-55182");
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_EmptyWhenNoAffectedComponents() {
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date()
    );

    hdsMockServer.respondWith(List.of())
        .atUri("/rest/vulnerability/affected/CVE-2025-99999").withoutLicense();

    hdsMockServer.respondWith("{\"vulnerabilities\":{}}")
        .atUri("/rest/vulnerability/details/json").withoutLicense();

    Stream<ApplicationComponentMatchDTO> results =
        service.searchCveAffectedComponentsStreaming(Set.of("CVE-2025-99999"));

    assertThat(results.collect(Collectors.toList())).isEmpty();
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_IncludesViolatingAndWaiverInfo() {
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date()
    );

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "vulnerable-lib",
        "1.0.0"
    );

    ApplicationComponent component = tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-123",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0"
    );

    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test Policy", 5);

    PolicyViolation violation = tempEntity.newPolicyViolation(
        evaluation,
        policy,
        componentId,
        component.getHash(),
        "Test violation"
    );

    TriggerReference cveRef = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "CVE-2025-55182");
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0,
        "Security vulnerability", "Contains CVE-2025-55182", cveRef);
    ConstraintFact constraintFact = new ConstraintFact("security-constraint", "Security Constraint", "AND");
    constraintFact.addConditionFact(conditionFact);
    violation.setConstraintFacts(List.of(constraintFact));

    PolicyWaiver waiver = tempEntity.newWaiver(
        violation.getHash(),
        policy.getId(),
        app.getId(),
        "Test waiver"
    );

    violation.setWaiveTime(new Date());
    violation.setPolicyWaiverId(waiver.getId());
    tempEntity.updatePolicyViolation(violation);

    setupHdsMocksForStandardSearch();

    Stream<ApplicationComponentMatchDTO> results =
        service.searchCveAffectedComponentsStreaming(Set.of("CVE-2025-55182"));

    List<ApplicationComponentMatchDTO> resultList = results.collect(Collectors.toList());
    assertThat(resultList).hasSize(1);

    ApplicationComponentMatchDTO match = resultList.get(0);
    assertThat(match.getViolating()).isTrue();
    assertThat(match.getActiveWaiver()).isTrue();
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_EmptyWhenNoApplications() {
    List<AffectedComponentDTO> affectedComponents = List.of(
        new AffectedComponentDTO("maven", "com.example", "lib", "1.0.0")
    );
    hdsMockServer.respondWith(affectedComponents)
        .atUri("/rest/vulnerability/affected/CVE-2025-55182").withoutLicense();

    hdsMockServer.respondWith("{\"vulnerabilities\":{}}")
        .atUri("/rest/vulnerability/details/json").withoutLicense();

    Stream<ApplicationComponentMatchDTO> results =
        service.searchCveAffectedComponentsStreaming(Set.of("CVE-2025-55182"));

    assertThat(results.collect(Collectors.toList())).isEmpty();
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_EmptyWhenNoEvaluations() {
    Application app = tempEntity.newApplication("Test App", "testapp",
        Organization.ROOT_ORGANIZATION_ID);

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("com.example",
        "vulnerable-lib", "1.0.0");
    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-123",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0"
    );

    setupHdsMocksForStandardSearch();

    Stream<ApplicationComponentMatchDTO> results =
        service.searchCveAffectedComponentsStreaming(Set.of("CVE-2025-55182"));

    assertThat(results.collect(Collectors.toList())).isEmpty();
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_HandlesHdsNotFound() {
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date()
    );

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "vulnerable-lib",
        "1.0.0"
    );

    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-123",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0"
    );

    hdsMockServer.respondWith(new NotFoundException("CVE not found in HDS"))
        .atUri("/rest/vulnerability/affected/CVE-2025-99999").withoutLicense();

    Stream<ApplicationComponentMatchDTO> results =
        service.searchCveAffectedComponentsStreaming(Set.of("CVE-2025-99999"));

    assertThat(results.collect(Collectors.toList())).isEmpty();
  }

  @Test
  public void testSearchCveAffectedComponentsStreaming_OnlyIncludesRelevantStages() {
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-release",
        new Date()
    );

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.DEVELOP.getId(),
        "scan-develop",
        new Date()
    );

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("com.example",
        "vulnerable-lib", "1.0.0");

    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-release",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0"
    );

    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.DEVELOP.getId(),
        "hash-develop",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0"
    );

    setupHdsMocksForStandardSearch();

    Stream<ApplicationComponentMatchDTO> results =
        service.searchCveAffectedComponentsStreaming(Set.of("CVE-2025-55182"));

    List<ApplicationComponentMatchDTO> resultList = results.collect(Collectors.toList());
    assertThat(resultList).hasSize(1);
    assertThat(resultList.get(0).getStage()).isEqualTo(StageTypes.STAGE_RELEASE.getId());
  }
}
