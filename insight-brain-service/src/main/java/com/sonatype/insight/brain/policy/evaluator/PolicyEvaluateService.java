/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PolicyEvaluateService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluateService.class);

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Inject
  public PolicyEvaluateService(ScanPolicyEvaluator scanPolicyEvaluator, PolicyAlertNotifier policyAlertNotifier) {
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
  }

  public PolicyEvaluationResult evaluate(String applicationPublicId, String scanId, Stage stage) throws IOException {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    AuditData.get().addApplicationId(application.getId()).addApplicationName(application.getName());
    log.debug("Received request to evaluate policy for app public id {}, scan id {}, stageTypeId {}",
        applicationPublicId, scanId, stage.getStageTypeId());
    return evaluate(application, scanId, stage);
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION, anonymousAllowed = true)
  PolicyEvaluationResult evaluate(@AuthzContext(AuthzContext.Key.APPLICATION) Application application,
                                  String scanId,
                                  Stage stage) throws IOException
  {
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);
    PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation,
        results.allViolations, true);

    if (!results.evaluation.isReevaluation()) {
      policyAlertNotifier.sendNotifications(application, results);
    }

    return policyEvaluationResult;
  }
}
