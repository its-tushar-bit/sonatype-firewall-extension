/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.componentanalysis;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.scan.model.ClientScanType;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

public class ComponentAnalysisServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ComponentAnalysisService componentAnalysisService;

  @Mock
  private HttpServletRequest httpRequest;

  @Mock
  private ScanHandler scanHandler;

  @Test
  public void testAnalyzeComponentsWithPolling_Unauthenticated() {
    assertThatThrownBy(() -> componentAnalysisService.analyzeComponentsWithPolling(IntegrationType.CLI,
        app.getPublicId(), ClientScanType.SONATYPE, httpRequest, new Stage(Stage.ID_BUILD)))
            .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void testAnalyzeComponentsWithPolling_Unauthorized() {
    login();
    assertThatThrownBy(() -> componentAnalysisService.analyzeComponentsWithPolling(IntegrationType.CLI,
        app.getPublicId(), ClientScanType.SONATYPE, httpRequest, new Stage(Stage.ID_BUILD)))
            .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void testAnalyzeComponentsWithPolling_Authorized() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    assertThatCode(() -> componentAnalysisService.analyzeComponentsWithPolling(IntegrationType.CLI,
        app.getPublicId(), ClientScanType.SONATYPE, httpRequest, new Stage(Stage.ID_BUILD)))
            .doesNotThrowAnyException();
  }
}
