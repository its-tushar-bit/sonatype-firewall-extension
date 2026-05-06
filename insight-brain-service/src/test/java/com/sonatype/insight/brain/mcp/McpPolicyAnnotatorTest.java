/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentEvaluationServiceV2;
import com.sonatype.insight.brain.mcp.model.McpPolicyContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class McpPolicyAnnotatorTest
{
  @Mock
  private ApiComponentEvaluationServiceV2 evaluationService;

  private McpPolicyAnnotator underTest;

  @Before
  public void setUp() {
    underTest = new McpPolicyAnnotator(evaluationService);
  }

  @Test
  public void testEvaluatePolicy_nullApplicationId() {
    McpPolicyContext result = underTest.evaluatePolicy("pkg:maven/org/lib@1.0", null, "build");

    assertThat(result).isNull();
  }

  @Test
  public void testEvaluatePolicy_emptyApplicationId() {
    McpPolicyContext result = underTest.evaluatePolicy("pkg:maven/org/lib@1.0", "", "build");

    assertThat(result).isNull();
  }

  @Test
  public void testEvaluatePolicy_successWithViolations() throws Exception {
    String purl = "pkg:maven/org.example/lib@1.0.0";
    String appId = "app-123";

    ApiComponentEvaluationTicketDTOV2 ticket = new ApiComponentEvaluationTicketDTOV2();
    ticket.resultId = "result-1";
    when(evaluationService.evaluateComponents(eq(appId), any(ApiComponentEvaluationRequestDTOV2.class)))
        .thenReturn(ticket);

    ApiComponentEvaluationResultDTOV2 evalResult = buildEvalResult(appId, false);
    addViolation(evalResult, "Security-Critical", 9, null, "CVE-2024-1234");
    addViolation(evalResult, "License-Warn", 5, null, "GPL detected");
    when(evaluationService.getComponentEvaluation(appId, "result-1")).thenReturn(evalResult);

    McpPolicyContext result = underTest.evaluatePolicy(purl, appId, "build");

    assertThat(result).isNotNull();
    assertThat(result.applicationId()).isEqualTo(appId);
    assertThat(result.stage()).isEqualTo("build");
    assertThat(result.violations()).hasSize(2);
    assertThat(result.stageResult().compliant()).isFalse();
    assertThat(result.stageResult().actionType()).isEqualTo("Fail");
    assertThat(result.stageResult().violationCount()).isEqualTo(2);

    assertThat(result.violations().get(0).policyName()).isEqualTo("Security-Critical");
    assertThat(result.violations().get(0).threatLevel()).isEqualTo(9);
    assertThat(result.violations().get(0).actionType()).isEqualTo("Fail");
    assertThat(result.violations().get(0).waived()).isFalse();
    assertThat(result.violations().get(0).reasons()).containsExactly("CVE-2024-1234");

    assertThat(result.violations().get(1).policyName()).isEqualTo("License-Warn");
    assertThat(result.violations().get(1).actionType()).isEqualTo("Warn");
  }

  @Test
  public void testEvaluatePolicy_waivedViolationsAreCompliant() throws Exception {
    String purl = "pkg:maven/org.example/lib@1.0.0";
    String appId = "app-123";

    ApiComponentEvaluationTicketDTOV2 ticket = new ApiComponentEvaluationTicketDTOV2();
    ticket.resultId = "result-1";
    when(evaluationService.evaluateComponents(eq(appId), any(ApiComponentEvaluationRequestDTOV2.class)))
        .thenReturn(ticket);

    ApiComponentEvaluationResultDTOV2 evalResult = buildEvalResult(appId, false);
    addViolation(evalResult, "Security-Critical", 9, new Date(), "CVE-2024-1234");
    when(evaluationService.getComponentEvaluation(appId, "result-1")).thenReturn(evalResult);

    McpPolicyContext result = underTest.evaluatePolicy(purl, appId, "build");

    assertThat(result).isNotNull();
    assertThat(result.violations()).hasSize(1);
    assertThat(result.violations().get(0).waived()).isTrue();
    assertThat(result.stageResult().compliant()).isTrue();
    assertThat(result.stageResult().violationCount()).isEqualTo(0);
    assertThat(result.stageResult().actionType()).isEqualTo("None");
  }

  @Test
  public void testEvaluatePolicy_defaultsStageToDevelop() throws Exception {
    String purl = "pkg:maven/org.example/lib@1.0.0";
    String appId = "app-123";

    ApiComponentEvaluationTicketDTOV2 ticket = new ApiComponentEvaluationTicketDTOV2();
    ticket.resultId = "result-1";
    when(evaluationService.evaluateComponents(eq(appId), any(ApiComponentEvaluationRequestDTOV2.class)))
        .thenReturn(ticket);

    ApiComponentEvaluationResultDTOV2 evalResult = buildEvalResult(appId, false);
    when(evaluationService.getComponentEvaluation(appId, "result-1")).thenReturn(evalResult);

    McpPolicyContext result = underTest.evaluatePolicy(purl, appId, null);

    assertThat(result).isNotNull();
    assertThat(result.stageResult().stage()).isEqualTo("develop");
  }

  @Test
  public void testEvaluatePolicy_evaluationError() throws Exception {
    String purl = "pkg:maven/org.example/lib@1.0.0";
    String appId = "app-123";

    ApiComponentEvaluationTicketDTOV2 ticket = new ApiComponentEvaluationTicketDTOV2();
    ticket.resultId = "result-1";
    when(evaluationService.evaluateComponents(eq(appId), any(ApiComponentEvaluationRequestDTOV2.class)))
        .thenReturn(ticket);

    ApiComponentEvaluationResultDTOV2 evalResult = buildEvalResult(appId, true);
    when(evaluationService.getComponentEvaluation(appId, "result-1")).thenReturn(evalResult);

    McpPolicyContext result = underTest.evaluatePolicy(purl, appId, "build");

    assertThat(result).isNull();
  }

  @Test
  public void testEvaluatePolicy_evaluateComponentsThrows() throws Exception {
    String purl = "pkg:maven/org.example/lib@1.0.0";
    String appId = "app-123";

    when(evaluationService.evaluateComponents(eq(appId), any(ApiComponentEvaluationRequestDTOV2.class)))
        .thenThrow(new RuntimeException("Service unavailable"));

    McpPolicyContext result = underTest.evaluatePolicy(purl, appId, "build");

    assertThat(result).isNull();
  }

  private static ApiComponentEvaluationResultDTOV2 buildEvalResult(String appId, boolean isError) {
    ApiComponentEvaluationResultDTOV2 result = new ApiComponentEvaluationResultDTOV2();
    result.applicationId = appId;
    result.isError = isError;

    ApiComponentDetailsDTOV2 details = new ApiComponentDetailsDTOV2();
    details.policyData = new ApiComponentPolicyViolationListDTOV2();
    result.results.add(details);

    return result;
  }

  private static void addViolation(
      ApiComponentEvaluationResultDTOV2 evalResult,
      String policyName,
      int threatLevel,
      Date waiveTime,
      String reason)
  {
    ApiPolicyViolationDTOV2 violation = new ApiPolicyViolationDTOV2();
    violation.policyName = policyName;
    violation.threatLevel = threatLevel;
    violation.waiveTime = waiveTime;

    ApiConstraintViolationReasonDTO reasonDto = new ApiConstraintViolationReasonDTO();
    reasonDto.reason = reason;

    ApiConstraintViolationDTO constraint = new ApiConstraintViolationDTO();
    constraint.reasons.add(reasonDto);
    violation.constraintViolations.add(constraint);

    evalResult.results.get(0).policyData.policyViolations.add(violation);
  }
}
