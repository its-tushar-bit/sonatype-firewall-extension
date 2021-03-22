/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.scan.model.ClientScanType;

/**
 * Service for Application Evaluation against Policies.
 */
public interface PolicyEvaluateService
{
  /**
   * Evaluate an Application by it's public Application id, Scan id and {@link Stage}
   *
   * @param applicationPublicId public shared id
   * @param scanId the id of the scan
   * @param stage {@link Stage}
   * @param scanTriggerType The trigger type for the scan for this evaluation {@link ScanTriggerType}
   */
  PolicyEvaluationResult evaluate(
      final String applicationPublicId,
      final String scanId,
      final Stage stage,
      final ScanTriggerType scanTriggerType) throws IOException;

  /**
   * Starts the evaluation of an application, integration, type and stage. After starting will
   * return a {@link PolicyEvaluationReceipt} for the requester to use to check on results
   * via {@link #pollEvaluationResult(String, String)}
   *
   * @param integrationType     {@link IntegrationType}
   * @param applicationPublicId public shared id
   * @param clientScanType      {@link ClientScanType}
   * @param req                 {@link HttpServletRequest}
   * @param stage               {@link Stage}
   * @return PolicyEvaluationReceipt
   * @throws IOException when the scan file, uploaded via the request, is unable to be read or processed
   */
  PolicyEvaluationReceipt evaluateWithPolling(
      final IntegrationType integrationType,
      final String applicationPublicId,
      final ClientScanType clientScanType,
      final HttpServletRequest req,
      final Stage stage) throws IOException;

  /**
   * Starts the evaluation of an {@link Application}, type and stage. The passed <code>statusId</code> is passed as
   * reference for the requester to use to check on resultsvia {@link #pollEvaluationResult(String, String)}
   *
   * @param statusId custom unique id, used as a reference for the evaluation done for this request
   * @param application {@link Application}
   * @param clientScanType {@link ClientScanType}
   * @param stage {@link Stage}
   * @param scanTriggerType the type of trigger for the scan for this evaluation {@link ScanTriggerType}
   * @param tempScanFile {@link File} to temporary store scanned result to
   * @param thirdPartyScanType string value of an {@link IntegrationType} if <code>clientScanType</code>
   *          is {@link ClientScanType#SONATYPE_THIRD_PARTY} or null otherwise
   * @param userAgent User agent from {@link HttpServletRequest}
   */
  void evaluateWithPolling(
      final String statusId,
      final Application application,
      final ClientScanType clientScanType,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final File tempScanFile,
      final String thirdPartyScanType,
      final String userAgent);

  /**
   * Retrieve the {@link PolicyEvaluationPollingResult} for an existing request, made
   * through the {@link #evaluateWithPolling(IntegrationType, String, ClientScanType, HttpServletRequest, Stage)}
   *
   * @param applicationPublicId public shared id
   * @param statusId            id from status, normally gotten from {@link PolicyEvaluationReceipt}
   */
  PolicyEvaluationPollingResult pollEvaluationResult(
      final String applicationPublicId,
      final String statusId);
}
