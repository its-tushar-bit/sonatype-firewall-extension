/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.List;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.scan.model.io.ScanReader;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BasePolicyEvaluatorTest
    extends AbstractBrainServiceIntegrationTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(1, BasePolicyEvaluatorTest.class);

  protected ScanReader scanReader;

  protected String insightServerUrl;

  /**
   * The TestRunner class is responsible for executing the actual test against the subject with the given parameters,
   * and asserting the results such as exit code/exception, log output, etc...
   * Implementations:
   * <ul>
   *   <li>{@link JUnitPolicyEvaluatorTestRunner} is the main implementation for normal unit tests</li>
   *   <li><a href="http://github.com/sonatype/native-image-nexus-iq-cli>sonatype/native-image-nexus-iq-cli/</a>
   *   contains the native-image implementations
   * </ul>
   */
  protected abstract AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params);

  @Before
  public void setUp() throws Exception {
    System.out.println("--- " + testName.getMethodName() + " ------------------------");
    try {
      String outDir = tempDir.newFolder("scan").getAbsolutePath();
      String timestamp = "20130610-171959";
      System.setProperty(PolicyEvaluatorCli.PROP_OUTPUT_DIRECTORY, outDir);
      System.setProperty(PolicyEvaluatorCli.PROP_START_TIME, timestamp);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }

    // return a valid report zip file when asked
    File smallReportZip = new File(getClass().getClassLoader().getResource("small-report.zip").getFile());
    getHdsServer().respondWith(smallReportZip)
        .atUri("rest/application/analysis/SCAN-ID");

    insightServerUrl = getCLMServer().getClientConfiguration().getServerUrl();

    scanReader = getCLMServer().getInstance(ScanReader.class);
  }

  protected ScanReceipt newReceipt() {
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("the-scan-id");
    receipt.setReportUrl("the-report-url");
    receipt.setPdfUrl("the-pdf-url");
    receipt.setTimeToReport(0L);
    return receipt;
  }

  protected void createPolicy(String ownerId, String policyName, String actionId, int threatLevel) {
    Policy policy = new Policy();
    policy.setName(policyName);
    policy.setOwnerId(ownerId);
    Condition condition = new Condition(MatchStateConditionType.ID, "is");
    condition.setValue(MatchState.EXACT.getId());
    Constraint constraint = new Constraint();
    constraint.setName("test constraint");
    constraint.addCondition(condition);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, actionId);
    policy.setThreatLevel(threatLevel);
    tempEntity.newPolicy(policy);
  }

  protected void createPolicyWithNotifications(String ownerId,
                                               String policyName,
                                               String actionId,
                                               int threatLevel,
                                               Notification ...notifications)
  {
    Policy policy = new Policy();
    policy.setName(policyName);
    policy.setOwnerId(ownerId);
    Condition condition = new Condition(MatchStateConditionType.ID, "is");
    condition.setValue(MatchState.EXACT.getId());
    Constraint constraint = new Constraint();
    constraint.setName("test constraint");
    constraint.addCondition(condition);
    policy.setNotifications(new Notifications(notifications));
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, actionId);
    policy.setThreatLevel(threatLevel);
    tempEntity.newPolicy(policy);
  }

  protected ApplicationSummaryList newApplicationSummaryList(String publicId, String name) {
    ApplicationSummary appSummary = new ApplicationSummary();
    appSummary.setPublicId(publicId);
    appSummary.setName(name);
    ApplicationSummaryList appSummaryList = new ApplicationSummaryList();
    appSummaryList.getApplicationSummaries().add(appSummary);
    return appSummaryList;
  }

  protected PolicyEvaluationResult newPolicyEvaluationResultForOneComponent() {
    PolicyEvaluationResult expectedPolicyEvalutionResult = new PolicyEvaluationResult();
    expectedPolicyEvalutionResult.setTotalComponentCount(1);
    return expectedPolicyEvalutionResult;
  }

  protected void assertLogSummary(PolicyEvaluationResult expectedPolicyEvalutionResult) {
    assertThat(logOutput).atInfoLevel()
        .contains(String.format("Number of components affected: %s critical, %s severe, %s moderate",
            expectedPolicyEvalutionResult.getCriticalComponentCount(),
            expectedPolicyEvalutionResult.getSevereComponentCount(),
            expectedPolicyEvalutionResult.getModerateComponentCount()))
        .contains(String.format("Number of open policy violations: %s critical, %s severe, %s moderate",
            expectedPolicyEvalutionResult.getCriticalPolicyViolationCount(),
            expectedPolicyEvalutionResult.getSeverePolicyViolationCount(),
            expectedPolicyEvalutionResult.getModeratePolicyViolationCount()))
        .contains(String.format("Number of legacy violations: %s",
            expectedPolicyEvalutionResult.getLegacyViolationCount()))
        .contains(String.format("Number of components: %s",
            expectedPolicyEvalutionResult.getTotalComponentCount()));
  }
}
