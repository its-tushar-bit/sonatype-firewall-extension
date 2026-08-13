/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class HierarchyMetricsTelemetryCollectorTest
    extends AbstractComponentH2Test
{
  @Inject
  private HierarchyMetricsTelemetryCollector telemetryCollector;

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_TelemetryPurpose() {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.HIERARCHY_METRICS);
  }

  @Test
  public void testCollectData_ZeroApps() {
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APP_COMPONENTS, 0L)
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APP_COMPONENT_VIOLATIONS, 0L)
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "0");
  }

  @Test
  public void testCollectData_ZeroRepos() {
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_REPOS, 0L)
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_REPO_COMPONENTS, 0L)
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_REPO_COMPONENT_VIOLATIONS, 0L);
  }

  @Test
  public void testCollectData_MaxOneApp() {
    createAppsAndOrgs(2);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "2")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "1.0");
  }

  @Test
  public void testCollectData_MaxTenApps() {
    createAppsAndOrgs(11);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "11")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "55")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "10")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "9.8");
  }

  @Test
  public void testCollectData_MaxTwentyApps() {
    createAppsAndOrgs(21);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "21")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "210")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "20")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "18.8");
  }

  @Test
  public void testCollectData_MinOneApp() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newApplication(organization.getId());
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "1.0");
  }

  @Test
  public void testCollectData_AppAndComponentsAndEvaluationsAndViolations() {
    createAppAndComponentsAndEvaluationAndViolations(5, 50);

    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APP_COMPONENTS, 5L)
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APP_COMPONENT_VIOLATIONS, 50L)
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APP_EVALUATIONS, 1L);
  }

  @Test
  public void testCollectData_RepoAndComponentsAndViolations() {
    createRepoAndComponentsAndViolations(5, 50);

    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_REPOS, 1L)
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_REPO_COMPONENTS, 5L)
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_REPO_COMPONENT_VIOLATIONS, 50L);
  }

  private void createAppsAndOrgs(int numberOfOrgs) {
    for (int i = 0; i < numberOfOrgs; i++) {
      Organization organization = tempEntity.newOrganization();
      for (int j = 0; j < i; j++) {
        tempEntity.newApplication(organization.getId());
      }
    }
  }

  private void createAppAndComponentsAndEvaluationAndViolations(int numOfComponents, int numOfViolations) {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    Policy policy = tempEntity.newPolicy(application);

    for (int i = 0; i < numOfComponents; i++) {
      tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "hash" + i,
          ComponentIdentifier.createMavenCoordinates("g", "a", "" + i), null, MatchState.EXACT, false,
          new Date()).getId();
    }

    for (int i = 0; i < numOfViolations; i++) {
      tempEntity.newPolicyViolation(policyEvaluation, policy);
    }
  }

  private void createRepoAndComponentsAndViolations(int numOfComponents, int numOfViolations) {
    Repository repository = tempEntity.newRepository();
    for (int i = 0; i < numOfComponents; i++) {
      tempEntity.newRepositoryComponent(repository.getId(), "pathName" + i);
    }

    for (int i = 0; i < numOfViolations; i++) {
      tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, "pathname", null);
    }
  }
}
