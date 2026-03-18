/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Arrays;
import java.util.Date;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistedPolicyEvaluationPollingResultTest
{
  @Test
  public void testInitialization() {
    Date now = new Date();
    PersistedPolicyEvaluationPollingResult actual = new PersistedPolicyEvaluationPollingResult(null, null, null);

    assertThat(actual.getCreateTime()).isAfterOrEqualTo(now).isCloseTo(now, 5000);
  }

  @Test
  public void testGetAndSetPolicyEvaluationPollingResult() {
    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        new PersistedPolicyEvaluationPollingResult(null, null, null);

    assertThat(persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult()).isNull();

    PolicyEvaluationPollingResult expected = createPolicyEvaluationPollingResult();
    persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(expected);

    assertThat(persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult()).usingRecursiveComparison()
        .isEqualTo(expected);

    persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(null);

    assertThat(persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult()).isNull();
  }

  private PolicyEvaluationPollingResult createPolicyEvaluationPollingResult() {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    policyEvaluationPollingResult.setReason("reason");
    policyEvaluationPollingResult.setResult(createPolicyEvaluationResult());
    policyEvaluationPollingResult.setScanReceipt(createScanReceipt());
    return policyEvaluationPollingResult;
  }

  private PolicyEvaluationResult createPolicyEvaluationResult() {
    PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    policyEvaluationResult.setAlerts(Arrays.asList(createPolicyAlert(), createPolicyAlert()));
    return policyEvaluationResult;
  }

  private PolicyAlert createPolicyAlert() {
    Action action = new Action("actionTypeId", "target", "targetType");
    return new PolicyAlert(createPolicyFact(), Arrays.asList(action, action));
  }

  private PolicyFact createPolicyFact() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ConditionFact conditionFact = new ConditionFact("conditionTypeId", 0, "summary", "reason",
        new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "value"));
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "any");
    constraintFact.addConditionFact(conditionFact);
    constraintFact.addConditionFact(conditionFact);
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "hash");
    componentFact.addConstraintFact(constraintFact);
    componentFact.addConstraintFact(constraintFact);
    componentFact.addPathnames(Arrays.asList("path1", "/some/path2"));
    componentFact.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier));
    PolicyFact policyFact = new PolicyFact("policyId", "policyName", 0, "policyViolationId");
    policyFact.addComponentFact(componentFact);
    policyFact.addComponentFact(componentFact);
    return policyFact;
  }

  private ScanReceipt createScanReceipt() {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scanId");
    scanReceipt.setTimeToReport(30L);
    scanReceipt.setReportUrl("the-report-ul");
    scanReceipt.setPdfUrl("the-pdf-url");
    scanReceipt.setDataUrl("the-data-url");
    scanReceipt.setReportTimeoutInSeconds(2100);
    return scanReceipt;
  }
}
