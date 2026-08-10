/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SloViolationEnricher} (delegating to {@link SloRemediationEnricher}) populates the
 * report-derived {@code dependencyType} and the bulk-table {@code recommendedRemediationVersion} on
 * {@link SloViolation}s. Uses the real {@code DevelopmentPrioritiesReportService} against a seeded report file plus a
 * real {@code development_prioritization_component_info} row, so the dependency-type values match the Priorities page.
 */
@ComponentH2Test
public class SloViolationEnricherRemediationTest
    extends AbstractComponentH2Test
{
  private static final String SCAN_ID = "slo-remediation-scan";

  // Components present in the seeded report-1 fixture (see ApiReportDataServiceTest/report-1/bom.json).
  private static final ComponentIdentifier GSON =
      ComponentIdentifier.createMavenCoordinates("com.google.code.gson", "gson", "2.8.1", "", "jar");

  private static final ComponentIdentifier INSIGHT_SCANNER_ARCHIVE = ComponentIdentifier
      .createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-archive", "1.0.0-SNAPSHOT", "", "jar");

  @Inject
  private SloViolationEnricher enricher;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Test
  public void populatesTransitiveDependencyTypeAndRecommendedVersion() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);
    seedReport(application);

    String prioritizationId = tempEntity.newDevelopmentPrioritization(SCAN_ID).getId();
    tempEntity.newDevelopmentPrioritizationComponentInfo(prioritizationId, SCAN_ID, GSON.toSyntheticHash(),
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "2.8.9");

    PolicyViolation violation = newViolation(evaluation, policy, GSON);

    List<SloViolation> results = enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(violation));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.dependencyType).isEqualTo("Transitive");
    assertThat(result.recommendedRemediationVersion).isEqualTo("2.8.9");
  }

  @Test
  public void populatesInnerSourceDirectDependencyTypeFromReport() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);
    seedReport(application);

    PolicyViolation violation = newViolation(evaluation, policy, INSIGHT_SCANNER_ARCHIVE);

    List<SloViolation> results = enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(violation));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.dependencyType).isEqualTo("Inner Source Direct");
    // No bulk component-info row seeded for this component, so no recommended version is available.
    assertThat(result.recommendedRemediationVersion).isNull();
  }

  @Test
  public void defaultsDependencyTypeUnknownWhenComponentNotInReport() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);
    seedReport(application);

    ComponentIdentifier absent =
        ComponentIdentifier.createMavenCoordinates("org.example", "not-in-report", "9.9.9", "", "jar");
    PolicyViolation violation = newViolation(evaluation, policy, absent);

    List<SloViolation> results = enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(violation));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.dependencyType).isEqualTo("Unknown");
    assertThat(result.recommendedRemediationVersion).isNull();
  }

  @Test
  public void degradesToUnknownWhenNoReport() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);
    // Deliberately do NOT seed a report for this scan: getDependencyInformation throws NotFoundException, which the
    // enricher narrows and degrades to Unknown rather than failing the feed request.

    PolicyViolation violation = newViolation(evaluation, policy, GSON);

    List<SloViolation> results = enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(violation));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.dependencyType).isEqualTo(PrioritizedComponent.DEPENDENCY_TYPE_UNKNOWN);
    assertThat(result.recommendedRemediationVersion).isNull();
  }

  private void seedReport(final Application application) throws Exception {
    com.sonatype.insight.brain.utils.ReportHelper.saveMockReport(
        insightWork, tempDir, "/ApiReportDataServiceTest/report-1", application.getId(), SCAN_ID);
  }

  private PolicyViolation newViolation(
      final PolicyEvaluation evaluation,
      final Policy policy,
      final ComponentIdentifier componentIdentifier)
  {
    // policy_violation.hash is varchar(20); bound the low digits of nanoTime so the unique value always fits.
    PolicyViolation violation = new PolicyViolation(evaluation, policy,
        "hash-" + (System.nanoTime() % 100_000_000_000_000L),
        componentIdentifier, Collections.singletonList(new ConstraintFact()), "component.jar");
    violation.setId("pv-" + System.nanoTime());
    policyViolationDAO.insert(violation);
    return policyViolationDAO.getById(violation.getId());
  }
}
