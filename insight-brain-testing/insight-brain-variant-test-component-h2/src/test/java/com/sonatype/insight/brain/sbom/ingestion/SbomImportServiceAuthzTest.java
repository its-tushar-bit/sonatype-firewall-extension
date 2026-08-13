/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.ByteArrayInputStream;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class SbomImportServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String TEST_FILENAME = "test-filename.xml";

  @Inject
  private SbomImportService sbomImportService;

  @Inject
  private PolicyEvaluationHelper policyEvaluationHelper;

  @Test
  public void testDetectSbom_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> sbomImportService.detectSbom("abcd", new ByteArrayInputStream(new byte[1]), TEST_FILENAME, false));
  }

  @Test
  public void testDetectSbom_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class,
        () -> sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(new byte[1]),
            TEST_FILENAME, false));
  }

  @Test
  public void testDetectSbom_Authorized() {
    grantWritePermission();
    Application application = tempEntity.newApplicationWithParent();
    SbomDetectionResultDTO dto =
        sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(new byte[1]),
            TEST_FILENAME, false);
    assertThat(dto.getSavedVersion()).isNotEmpty();
  }

  @Test
  public void testImportDetectedSbom_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> sbomImportService.importDetectedSbom("abcd", "abcd", null, "userAgent"));
  }

  @Test
  public void testImportDetectedSbom_Unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(UnauthorizedException.class,
        () -> sbomImportService.importDetectedSbom(application.getId(), "abcd", null, "userAgent"));
  }

  @Test
  public void testImportDetectedSbom_Authorized() {
    grantWritePermission();
    Application application = tempEntity.newApplicationWithParent();
    assertThrows(NotFoundException.class, () -> {
      Response response = sbomImportService.importDetectedSbom(application.getId(), "abcd", null, "userAgent");
      ApiThirdPartyScanTicketDTO status = (ApiThirdPartyScanTicketDTO) response.getEntity();
      policyEvaluationHelper.awaitEvaluationFinished(application.getId(), status.requestId);
    });
  }
}
