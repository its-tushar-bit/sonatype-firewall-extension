/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiComponentEvaluationServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiComponentEvaluationServiceV2 apiComponentEvaluationService;

  @Inject
  private InsightWork work;

  @Test
  public void testEvaluateComponents_Authorized() {
    grantEvaluateComponentPermission(app.getId());

    ApiComponentEvaluationRequestDTOV2 evaluationRequest = createEvaluationRequest();
    apiComponentEvaluationService.evaluateComponents(app.getId(), evaluationRequest);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    ApiComponentEvaluationRequestDTOV2 evaluationRequest = createEvaluationRequest();
    apiComponentEvaluationService.evaluateComponents(app.getId(), evaluationRequest);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_Unauthorized() {
    login();

    ApiComponentEvaluationRequestDTOV2 evaluationRequest = createEvaluationRequest();
    apiComponentEvaluationService.evaluateComponents(app.getId(), evaluationRequest);
  }

  private ApiComponentEvaluationRequestDTOV2 createEvaluationRequest() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = createComponent(
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1", null, "jar"), "1249e25aebb15358bedd");
    request.components.add(component);
    return request;
  }

  private ApiComponentDTOV2 createComponent(final ComponentIdentifier componentIdentifier, final String hash) {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    component.hash = hash;
    return component;
  }

  @Test
  public void testGetComponentEvaluation_Authorized() throws Exception {
    String resultId = "testResultId";
    File resultFile = work.getComponentDetailsFile(app.getId(), resultId);
    resultFile.getParentFile().mkdirs();
    Files.write(resultFile.toPath(), "{}".getBytes(StandardCharsets.UTF_8));
    grantEvaluateComponentPermission(app.getId());
    apiComponentEvaluationService.getComponentEvaluation(app.getId(), resultId);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetComponentEvaluation_Unauthenticated() throws Exception {
    apiComponentEvaluationService.getComponentEvaluation(app.getId(), "resultId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetComponentEvaluation_Unauthorized() throws Exception {
    login();
    apiComponentEvaluationService.getComponentEvaluation(app.getId(), "resultId");
  }
}
