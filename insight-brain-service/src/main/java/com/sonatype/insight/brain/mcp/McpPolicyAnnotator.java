/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentEvaluationServiceV2;
import com.sonatype.insight.brain.mcp.model.McpPolicyContext;
import com.sonatype.insight.brain.mcp.model.McpPolicyViolation;
import com.sonatype.insight.brain.mcp.model.McpStageResult;
import com.sonatype.insight.brain.mcp.policy.PolicyAnnotator;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class McpPolicyAnnotator
    implements PolicyAnnotator
{
  private static final Logger log = LoggerFactory.getLogger(McpPolicyAnnotator.class);

  private static final int MAX_POLL_ATTEMPTS = 10;

  private static final long POLL_INTERVAL_MS = 500;

  private final ApiComponentEvaluationServiceV2 evaluationService;

  @Inject
  public McpPolicyAnnotator(ApiComponentEvaluationServiceV2 evaluationService) {
    this.evaluationService = evaluationService;
  }

  @Override
  public McpPolicyContext evaluatePolicy(String purl, String applicationId, String stage) {
    if (applicationId == null || applicationId.isBlank()) {
      return null;
    }

    try {
      ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
      componentDto.packageUrl = purl;

      ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
      request.components = List.of(componentDto);

      // Submit evaluation (async internally)
      var ticket = evaluationService.evaluateComponents(applicationId, request);
      String resultId = ticket.resultId;
      if (resultId == null) {
        log.warn("Evaluation ticket missing resultId for purl={}, app={}", purl, applicationId);
        return null;
      }

      // Poll for results
      ApiComponentEvaluationResultDTOV2 evaluationResult = pollForResult(applicationId, resultId);
      if (evaluationResult == null || evaluationResult.isError) {
        log.warn("Policy evaluation returned error for purl={}, app={}", purl, applicationId);
        return null;
      }

      return mapToMcpPolicyContext(evaluationResult, applicationId, stage);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Policy evaluation interrupted for purl={}, app={}", purl, applicationId);
      return null;
    }
    catch (Exception e) {
      log.warn("Policy evaluation failed for purl={}, app={}: {}", purl, applicationId, e.getMessage());
      return null;
    }
  }

  private ApiComponentEvaluationResultDTOV2 pollForResult(
      String applicationId,
      String resultId) throws InterruptedException, IOException
  {
    for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
      try {
        return evaluationService.getComponentEvaluation(applicationId, resultId);
      }
      catch (NotFoundException e) {
        if (attempt < MAX_POLL_ATTEMPTS - 1) {
          Thread.sleep(POLL_INTERVAL_MS);
        }
      }
    }
    log.warn("Policy evaluation timed out after {} attempts for app={}, resultId={}",
        MAX_POLL_ATTEMPTS, applicationId, resultId);
    return null;
  }

  private McpPolicyContext mapToMcpPolicyContext(
      ApiComponentEvaluationResultDTOV2 evaluationResult,
      String applicationId,
      String stage)
  {
    List<McpPolicyViolation> violations = new ArrayList<>();
    int maxThreatLevel = 0;

    for (ApiComponentDetailsDTOV2 details : evaluationResult.results) {
      if (details.policyData == null || details.policyData.policyViolations == null) {
        continue;
      }
      for (ApiPolicyViolationDTOV2 pv : details.policyData.policyViolations) {
        List<String> reasons = extractReasons(pv.constraintViolations);
        boolean waived = pv.waiveTime != null;
        violations.add(new McpPolicyViolation(
            pv.policyName,
            pv.threatLevel,
            deriveActionType(pv.threatLevel),
            reasons,
            waived));
        if (!waived && pv.threatLevel > maxThreatLevel) {
          maxThreatLevel = pv.threatLevel;
        }
      }
    }

    long activeViolationCount = violations.stream().filter(v -> !v.waived()).count();
    boolean compliant = activeViolationCount == 0;
    String overallAction = deriveActionType(maxThreatLevel);

    String effectiveStage = stage != null ? stage : "develop";

    McpStageResult stageResult = new McpStageResult(
        effectiveStage,
        compliant,
        overallAction,
        (int) activeViolationCount);

    return new McpPolicyContext(applicationId, effectiveStage, stageResult, null, violations);
  }

  private static List<String> extractReasons(List<ApiConstraintViolationDTO> constraintViolations) {
    List<String> reasons = new ArrayList<>();
    if (constraintViolations == null) {
      return reasons;
    }
    for (ApiConstraintViolationDTO cv : constraintViolations) {
      if (cv.reasons == null) {
        continue;
      }
      for (ApiConstraintViolationReasonDTO reason : cv.reasons) {
        if (reason.reason != null) {
          reasons.add(reason.reason);
        }
      }
    }
    return reasons;
  }

  private static String deriveActionType(int threatLevel) {
    if (threatLevel >= 8) {
      return "Fail";
    }
    else if (threatLevel >= 4) {
      return "Warn";
    }
    return "None";
  }
}
