/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.junit.rules.TemporaryFolder;

/**
 * Shared seed-and-evaluate helper for {@code PolicyEvaluation*PlaywrightTest} classes. Composes
 * UUID-suffixed org + app + policy + canned-report-zip + evaluate. The {@link TemporaryEntity}
 * rule is supplied by the caller so cleanup remains JUnit-managed.
 */
public class PolicyEvaluationSeeder
{
  private final TemporaryEntity tempEntity;

  private final TemporaryFolder tempDir;

  private final InsightConfig insightConfig;

  private final String baseUrlFromTest;

  private final String cannedReportClasspathDir;

  /** All fields non-null. For the provision-without-evaluating flow, see {@link AppAndScan}. */
  public record SeededEvaluation(
      Application app,
      String scanId,
      Policy policy,
      String policyName,
      TestReportEvaluator evaluator)
  {
  }

  /**
   * Result of {@link #provisionAppAndScan} — caller seeds policies then evaluates. {@code suffix}
   * is the hyphen-free UUID used to namespace the provisioned org/app/scan, exposed here so
   * callers can derive matching unique names for additional policies without re-parsing
   * {@link Application#getName()}.
   */
  public record AppAndScan(Application app, String scanId, String suffix)
  {
  }

  public PolicyEvaluationSeeder(
      TemporaryEntity tempEntity,
      TemporaryFolder tempDir,
      InsightConfig insightConfig,
      String baseUrlFromTest,
      String cannedReportClasspathDir)
  {
    this.tempEntity = tempEntity;
    this.tempDir = tempDir;
    this.insightConfig = insightConfig;
    this.baseUrlFromTest = baseUrlFromTest;
    this.cannedReportClasspathDir = cannedReportClasspathDir;
  }

  /** Seeds org + app + single-condition policy, then evaluates. */
  public SeededEvaluation seedSingleConditionAndEvaluate(
      String orgPrefix,
      String appPrefix,
      String appIdPrefix,
      String scanIdPrefix,
      String policyPrefix,
      String constraintSuffix,
      String conditionTypeId,
      String operator,
      String value,
      int threatLevel) throws IOException
  {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(orgPrefix + "-" + suffix);
    Application app = tempEntity.newApplication(
        appPrefix + "-" + suffix, appIdPrefix + "-" + suffix, org.getId());
    String scanId = scanIdPrefix + "-" + suffix;
    String policyName = policyPrefix + "-" + suffix;

    Policy policy = buildSingleConstraintPolicy(
        policyName, constraintSuffix, conditionTypeId, operator, value, threatLevel, app.getId());
    Policy persisted = tempEntity.newPolicy(policy);

    TestReportEvaluator evaluator = newEvaluator(app, scanId);
    evaluator.evaluatePolicy();
    return new SeededEvaluation(app, scanId, persisted, policyName, evaluator);
  }

  /** Seeds org + app + policy whose one constraint carries multiple conditions, then evaluates. */
  public SeededEvaluation seedMultiConditionAndEvaluate(
      String orgPrefix,
      String appPrefix,
      String appIdPrefix,
      String scanIdPrefix,
      String policyPrefix,
      String constraintSuffix,
      List<Condition> conditions,
      LogicalOperator constraintOperator,
      int threatLevel) throws IOException
  {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(orgPrefix + "-" + suffix);
    Application app = tempEntity.newApplication(
        appPrefix + "-" + suffix, appIdPrefix + "-" + suffix, org.getId());
    String scanId = scanIdPrefix + "-" + suffix;
    String policyName = policyPrefix + "-" + suffix;

    Policy policy = new Policy(null, policyName);
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(app.getId());
    Constraint constraint = new Constraint(null, policyName + "-" + constraintSuffix, constraintOperator);
    constraint.setConditions(conditions);
    policy.setConstraints(Collections.singletonList(constraint));
    Policy persisted = tempEntity.newPolicy(policy);

    TestReportEvaluator evaluator = newEvaluator(app, scanId);
    evaluator.evaluatePolicy();
    return new SeededEvaluation(app, scanId, persisted, policyName, evaluator);
  }

  /** Provisions org + app + scan id; caller seeds N policies and evaluates once. */
  public AppAndScan provisionAppAndScan(
      String orgPrefix,
      String appPrefix,
      String appIdPrefix,
      String scanIdPrefix)
  {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(orgPrefix + "-" + suffix);
    Application app = tempEntity.newApplication(
        appPrefix + "-" + suffix, appIdPrefix + "-" + suffix, org.getId());
    return new AppAndScan(app, scanIdPrefix + "-" + suffix, suffix);
  }

  /** Seeds a single-condition policy onto an existing application (no evaluation). */
  public Policy seedSingleConditionPolicy(
      Application app,
      String policyName,
      String constraintSuffix,
      String conditionTypeId,
      String operator,
      String value,
      int threatLevel)
  {
    return seedSingleConditionPolicyForOwner(app.getId(), policyName, constraintSuffix,
        conditionTypeId, operator, value, threatLevel);
  }

  /** Seeds a single-condition policy onto any owner (org for inheritance, app for local). */
  public Policy seedSingleConditionPolicyForOwner(
      String ownerId,
      String policyName,
      String constraintSuffix,
      String conditionTypeId,
      String operator,
      String value,
      int threatLevel)
  {
    Policy policy = buildSingleConstraintPolicy(
        policyName, constraintSuffix, conditionTypeId, operator, value, threatLevel, ownerId);
    return tempEntity.newPolicy(policy);
  }

  /** Seeds a multi-constraint policy (one condition per constraint). */
  public Policy seedMultiConstraintPolicy(
      Application app,
      String policyName,
      String constraintSuffix,
      List<Condition> oneConditionPerConstraint,
      int threatLevel)
  {
    Policy policy = new Policy(null, policyName);
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(app.getId());
    List<Constraint> constraints = new ArrayList<>(oneConditionPerConstraint.size());
    for (int i = 0; i < oneConditionPerConstraint.size(); i++) {
      constraints.add(oneConditionConstraint(
          policyName + "-" + constraintSuffix + "-" + i, oneConditionPerConstraint.get(i)));
    }
    policy.setConstraints(constraints);
    return tempEntity.newPolicy(policy);
  }

  /** Builds an unevaluated {@link TestReportEvaluator}; caller invokes {@code evaluatePolicy()}. */
  public TestReportEvaluator newEvaluator(Application app, String scanId) throws IOException {
    URL zippedReport = ReportHelper.zipReport(cannedReportClasspathDir, tempDir);
    InsightWork work = new InsightWork(insightConfig);
    return new TestReportEvaluator(app, scanId, zippedReport, baseUrlFromTest, work);
  }

  private Policy buildSingleConstraintPolicy(
      String policyName,
      String constraintSuffix,
      String conditionTypeId,
      String operator,
      String value,
      int threatLevel,
      String ownerId)
  {
    Policy policy = new Policy(null, policyName);
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(ownerId);
    Constraint constraint = oneConditionConstraint(
        policyName + "-" + constraintSuffix, new Condition(conditionTypeId, operator, value));
    policy.setConstraints(Collections.singletonList(constraint));
    return policy;
  }

  /** AND-of-one {@link Constraint} (operator is irrelevant for a single condition; AND is convention). */
  private static Constraint oneConditionConstraint(String constraintName, Condition condition) {
    Constraint constraint = new Constraint(null, constraintName, LogicalOperator.AND);
    constraint.setConditions(Collections.singletonList(condition));
    return constraint;
  }
}
