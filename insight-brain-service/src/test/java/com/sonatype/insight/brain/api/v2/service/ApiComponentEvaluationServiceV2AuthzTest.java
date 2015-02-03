/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Assert;
import org.junit.Test;

public class ApiComponentEvaluationServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiComponentEvaluationServiceV2 apiComponentEvaluationService;


  @Test
  public void testEvaluateComponents_Authorized() {
    grantReadPermission(app.getId());

    ApiComponentEvaluationRequestDTOV2 evaluationRequest = createEvaluationRequest();
    apiComponentEvaluationService.evaluateComponents(app.getId(), evaluationRequest);
  }

  @Test
  public void testEvaluateComponents_Unauthenticated() {
    ApiComponentEvaluationRequestDTOV2 evaluationRequest = createEvaluationRequest();
    try {
      apiComponentEvaluationService.evaluateComponents(app.getId(), evaluationRequest);
      Assert.fail("Expected UnauthenticatedException");
    }
    catch (UnauthenticatedException ignore) {

    }
  }

  @Test
  public void testEvaluateComponents_UnauthorizedButAuthenticated() {
    login();

    ApiComponentEvaluationRequestDTOV2 evaluationRequest = createEvaluationRequest();
    try {
      apiComponentEvaluationService.evaluateComponents(app.getId(), evaluationRequest);
      Assert.fail("Expected UnauthorizedException");
    }
    catch (UnauthorizedException ignore) {

    }
  }

  private ApiComponentEvaluationRequestDTOV2 createEvaluationRequest() {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = createComponent(
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1", null, "jar"),
        "1249e25aebb15358bedd");
    request.components.add(component);
    return request;
  }

  private ApiComponentDTOV2 createComponent(final ComponentIdentifier componentIdentifier,
      final String hash)
  {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    component.hash = hash;
    return component;
  }
}
