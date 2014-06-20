/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

/**
 * @since 1.11.0
 */
@Named
public class PolicyEvaluationSummaryService
{
  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationUtils policyEvaluationUtils;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final CLMLicenseManager licenseManager;


  @Inject
  public PolicyEvaluationSummaryService(final ApplicationDAO applicationDAO,
      final PolicyEvaluationUtils policyEvaluationUtils, final PolicyEvaluationDAO policyEvaluationDAO,
      final CLMLicenseManager licenseManager)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationUtils = policyEvaluationUtils;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.licenseManager = licenseManager;
  }

  @Authorize(permission = Permission.READ)
  public PolicyEvaluationSummary getEvaluationSummaryByApplicationId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId, final Stage stage)
      throws IOException
  {
    validateLicensed();

    Application application = applicationDAO.getByIdNotNull(applicationId);

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
        stage.getStageTypeId());
    if (policyEvaluation == null) {
      return null;
    }

    PolicyEvaluationResult policyEvaluationResult = policyEvaluationUtils
        .createPolicyEvaluationResult(policyEvaluation);

    PolicyEvaluationSummary summary = new PolicyEvaluationSummary();
    summary.setAffectedComponentCount(policyEvaluationResult.getAffectedComponentCount());
    summary.setCriticalComponentCount(policyEvaluationResult.getCriticalComponentCount());
    summary.setModerateComponentCount(policyEvaluationResult.getModerateComponentCount());
    summary.setSevereComponentCount(policyEvaluationResult.getSevereComponentCount());
    summary.setReportUrl(
        "ui/links/application/" + application.getPublicId() + "/report/" + policyEvaluation.getScanId());

    return summary;
  }

  private void validateLicensed() {
    if (!licenseManager.hasQuality()) {
      throw new InvalidLicenseException("Invalid license for the Quality feature");
    }
  }
}
