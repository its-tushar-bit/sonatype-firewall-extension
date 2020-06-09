/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationPolicyEvaluationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyEvaluationDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.94.0
 */
@Named
@Singleton
public class ApiPolicyEvaluationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiPolicyEvaluationService.class);

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final ReportService reportService;

  private static final int MAX_POLICY_EVALUATIONS_TO_RETURN = 100;

  @Inject
  public ApiPolicyEvaluationService(
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final ReportService reportService)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.reportService = reportService;
  }

  @Authorize(permission = Permission.READ)
  public ApiApplicationPolicyEvaluationsDTO getAllPolicyEvaluations(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId)
  {
    applicationDAO.getByIdNotNull(applicationId);
    final ApiApplicationPolicyEvaluationsDTO
        apiApplicationPolicyEvaluationsDTO = new ApiApplicationPolicyEvaluationsDTO();
    apiApplicationPolicyEvaluationsDTO.applicationId = applicationId;
    apiApplicationPolicyEvaluationsDTO.policyEvaluations = new ArrayList<>();
    loadPolicyEvaluations(apiApplicationPolicyEvaluationsDTO);
    return apiApplicationPolicyEvaluationsDTO;
  }

  private void loadPolicyEvaluations(ApiApplicationPolicyEvaluationsDTO apiApplicationPolicyEvaluationsDTO) {
    List<PolicyEvaluation> policyEvaluations =
        policyEvaluationDAO.getLimitedAmountByApplicationId(apiApplicationPolicyEvaluationsDTO.applicationId,
            MAX_POLICY_EVALUATIONS_TO_RETURN);
    policyEvaluations.forEach(policyEvaluation -> {
      PolicyEvaluationResult policyEvaluationResult = loadPolicyEvaluationResults(policyEvaluation);
      apiApplicationPolicyEvaluationsDTO.policyEvaluations
          .add(new ApiPolicyEvaluationDTO(policyEvaluation, policyEvaluationResult, policyEvaluationResult != null));
    });
  }

  private PolicyEvaluationResult loadPolicyEvaluationResults(PolicyEvaluation policyEvaluation) {
    try {
      File reportFile = reportService.getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
      List<PolicyAlert> policyAlerts = Arrays.asList(JsonUtils
          .parse(Objects.requireNonNull(Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_ALERTS_FILENAME)).buf,
              PolicyAlert[].class));
      List<PolicyViolation> policyViolations =
          PolicyAlertUtil.getPolicyViolationsFromAlertsAndEvaluation(policyEvaluation, policyAlerts);
      return scanPolicyEvaluator.createPolicyEvaluationResult(policyEvaluation, policyViolations, false);
    }
    catch (IOException | NotFoundException | NullPointerException e) {
      log.debug("Could not load violations from report file for application {} scan {}, report is not available",
          policyEvaluation.getApplicationId(), policyEvaluation.getScanId(), e);
      return null;
    }
  }
}
