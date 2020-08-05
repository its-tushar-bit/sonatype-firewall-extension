/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.scan.model.ClientScanType;

/**
 * Resource for integrations points to conform to Application Evaluation.
 */
public interface ApplicationEvaluationResource
{
  /**
   * Starts the evaluation of an scanned file for an application, integration, type and stage. After
   * starting will return a {@link PolicyEvaluationReceipt} for requester to use to check on results
   * via {@link #pollEvaluationResult(String, String)}
   *
   * @param applicationPublicId public shared id
   * @param integrationType     {@link IntegrationType}
   * @param stage               {@link Stage}
   * @param clientScanType      {@link ClientScanType}
   * @param req                 {@link HttpServletRequest}
   * @return PolicyEvaluationReceipt
   * @throws IOException when the scan file, uploaded via the request, is unable to be read or processed
   */
  PolicyEvaluationReceipt evaluateWithPolling(
      final String applicationPublicId,
      final IntegrationType integrationType,
      final Stage stage,
      final ClientScanType clientScanType,
      final HttpServletRequest req) throws IOException;

  /**
   * Retrieve the {@link PolicyEvaluationPollingResult} for an existing request, made
   * through the {@link #evaluateWithPolling(String, IntegrationType, Stage, ClientScanType, HttpServletRequest)}.
   *
   * @param applicationPublicId public shared id
   * @param statusId            id from status, normally gotten from {@link PolicyEvaluationReceipt}
   * @return PolicyEvaluationReceipt
   */
  PolicyEvaluationPollingResult pollEvaluationResult(final String applicationPublicId, final String statusId);
}
