/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class SbomImportServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SbomImportService sbomImportService;

  @Inject
  private PolicyEvaluationHelper policyEvaluationHelper;

  @Test(expected = UnauthenticatedException.class)
  public void testDetectSbom_Unauthenticated() {
    sbomImportService.detectSbom("abcd", new ByteArrayInputStream(new byte[1]));
  }

  @Test(expected = UnauthorizedException.class)
  public void testDetectSbom_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(new byte[1]));
  }

  @Test
  public void testDetectSbom_Authorized() {
    grantWritePermission();
    Application application = tempEntity.newApplicationWithParent();
    sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(new byte[1]));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testImportDetectedSbom_Unauthenticated() {
    sbomImportService.importDetectedSbom("abcd", "abcd", "userAgent");
  }

  @Test(expected = UnauthorizedException.class)
  public void testImportDetectedSbom_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    sbomImportService.importDetectedSbom(application.getId(), "abcd", "userAgent");
  }

  @Test(expected = NotFoundException.class)
  public void testImportDetectedSbom_Authorized() {
    grantWritePermission();
    Application application = tempEntity.newApplicationWithParent();
    Response response = sbomImportService.importDetectedSbom(application.getId(),
        "OTExZDYxOTUxZTk0NDI5NGJhNjA0YjhhOWZkYmQzY2YtYXBwbGljYXRpb24veG1sLUN5Y2xvbmVEeA==", "userAgent");
    ApiThirdPartyScanTicketDTO status = (ApiThirdPartyScanTicketDTO) response.getEntity();
    policyEvaluationHelper.awaitEvaluationFinished(application.getId(), status.requestId);
  }
}
