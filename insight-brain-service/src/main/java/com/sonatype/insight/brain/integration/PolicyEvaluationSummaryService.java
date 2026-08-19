/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.license.model.LicensedFeature;

/**
 * @since 1.11.0
 */
@Named
public class PolicyEvaluationSummaryService
{
  private final ApplicationDAO applicationDAO;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ProductLicense productLicense;

  @Inject
  public PolicyEvaluationSummaryService(
      final ApplicationDAO applicationDAO,
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ProductLicense productLicense)
  {
    this.applicationDAO = applicationDAO;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.READ)
  public PolicyEvaluationSummary getEvaluationSummaryByApplicationId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final Stage stage)
  {
    validateLicensed();

    Application application = applicationDAO.getByIdNotNull(applicationId);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
        stage.getStageTypeId());
    if (policyEvaluation == null) {
      return null;
    }

    PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(
        policyEvaluation, false);

    PolicyEvaluationSummary summary = new PolicyEvaluationSummary();
    summary.setAffectedComponentCount(policyEvaluationResult.getAffectedComponentCount());
    summary.setCriticalComponentCount(policyEvaluationResult.getCriticalComponentCount());
    summary.setModerateComponentCount(policyEvaluationResult.getModerateComponentCount());
    summary.setSevereComponentCount(policyEvaluationResult.getSevereComponentCount());
    summary.setReportUrl(UserInterfaceLinksHelper.getReportUrl(application.getPublicId(),
        policyEvaluation.getScanId()));

    return summary;
  }

  private void validateLicensed() {
    productLicense.validateFeature(LicensedFeature.QUALITY);
  }
}
