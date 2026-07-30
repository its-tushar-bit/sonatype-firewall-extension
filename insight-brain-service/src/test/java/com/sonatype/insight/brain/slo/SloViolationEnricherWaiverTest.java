/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SloViolationEnricher} populates the {@link SloWaiver} field for waived violations, covering both
 * manually-applied waivers (with reason text and expiry) and auto waivers (no expiry), while leaving open violations
 * without a waiver. Exercises the real waiver DAO round trips.
 */
public class SloViolationEnricherWaiverTest
    extends AbstractComponentTest
{
  private static final String SCAN_ID = "slo-waiver-scan";

  @Inject
  private SloViolationEnricher enricher;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Test
  public void mapsManualWaiverWithReasonAndCreator() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);

    PolicyWaiver waiver = tempEntity.newWaiverWithReason("hash", policy.getId(), application.getId(), null,
        "manual waiver comment", "OTHER", "No fix available yet");

    PolicyViolation violation = tempEntity.newWaivedPolicyViolation(evaluation, policy, waiver);
    PolicyViolation reloaded = policyViolationDAO.getById(violation.getId());
    assertThat(reloaded.isWaived()).isTrue();
    assertThat(reloaded.getPolicyWaiverId()).isEqualTo(waiver.getId());
    assertThat(reloaded.getAutoPolicyWaiverId()).isNull();

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(reloaded));

    assertThat(results).hasSize(1);
    SloWaiver result = results.get(0).waiver;
    assertThat(result).isNotNull();
    assertThat(result.autoApplied()).isFalse();
    assertThat(result.id()).isEqualTo(waiver.getId());
    assertThat(result.reason()).isEqualTo("No fix available yet");
    assertThat(result.creatorName()).isNotNull();
    assertThat(result.createTime()).isNotNull();
    assertThat(result.expiryTime()).isEqualTo(waiver.getExpiryTime());
  }

  @Test
  public void mapsAutoWaiverWithoutExpiry() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);

    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    PolicyViolation violation = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, autoWaiver);
    PolicyViolation reloaded = policyViolationDAO.getById(violation.getId());
    assertThat(reloaded.isAutoWaived()).isTrue();
    assertThat(reloaded.getAutoPolicyWaiverId()).isEqualTo(autoWaiver.getId());

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(reloaded));

    assertThat(results).hasSize(1);
    SloWaiver result = results.get(0).waiver;
    assertThat(result).isNotNull();
    assertThat(result.autoApplied()).isTrue();
    assertThat(result.id()).isEqualTo(autoWaiver.getId());
    assertThat(result.expiryTime()).isNull();
    assertThat(result.creatorName()).isNotNull();
    assertThat(result.createTime()).isNotNull();
  }

  @Test
  public void mapsManualWaiverReasonFallsBackToComment() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);

    // newWaiver(hash, policyId, ownerId, comment) sets a comment but leaves waiverReasonId null.
    PolicyWaiver waiver =
        tempEntity.newWaiver("hash", policy.getId(), application.getId(), "comment-only waiver");
    assertThat(waiver.getWaiverReasonId()).isNull();

    PolicyViolation violation = tempEntity.newWaivedPolicyViolation(evaluation, policy, waiver);
    PolicyViolation reloaded = policyViolationDAO.getById(violation.getId());

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(reloaded));

    assertThat(results).hasSize(1);
    SloWaiver result = results.get(0).waiver;
    assertThat(result).isNotNull();
    assertThat(result.autoApplied()).isFalse();
    assertThat(result.reason()).isEqualTo("comment-only waiver");
  }

  @Test
  public void waivedButUnresolvableWaiverYieldsNullWaiverButKeepsWaiveTime() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());

    // Waived (waiveTime set) but references a waiver id that has no corresponding PolicyWaiver row.
    PolicyViolation violation = new PolicyViolation();
    violation.setId("pv-unresolvable-" + System.nanoTime());
    violation.setOwnerId(application.getId());
    violation.setStageTypeId(Stage.ID_RELEASE);
    violation.setPolicyId("policy-1");
    violation.setPolicyName("Unresolvable waiver policy");
    violation.setWaiveTime(new Date());
    violation.setPolicyWaiverId("missing-waiver-id");

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(violation));

    assertThat(results).hasSize(1);
    SloViolation result = results.get(0);
    assertThat(result.waiver).isNull();
    assertThat(result.waiveTime).isNotNull();
  }

  @Test
  public void noWaiverWhenNotWaived() {
    Organization org = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_RELEASE, SCAN_ID);

    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy);
    PolicyViolation reloaded = policyViolationDAO.getById(violation.getId());
    assertThat(reloaded.isWaived()).isFalse();

    List<SloViolation> results =
        enricher.enrich(application, Stage.ID_RELEASE, SCAN_ID, List.of(reloaded));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).waiver).isNull();
  }
}
