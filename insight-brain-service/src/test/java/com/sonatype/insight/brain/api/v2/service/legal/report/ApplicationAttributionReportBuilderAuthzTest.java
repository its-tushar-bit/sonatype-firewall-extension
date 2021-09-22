/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApplicationAttributionReportBuilderAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationAttributionReportBuilder applicationAttributionReportBuilder;

  @Test(expected = UnauthenticatedException.class)
  public void testGenerateLegalAttributionApplicationReport_Unauthenticated() {
    applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
        app,
        BuildStageType.ID,
        LegalCustomReportParameters.builder().buildWithDefaults("app"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGenerateLegalAttributionApplicationReport_Unauthorized() {
    login();
    applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
        app,
        BuildStageType.ID,
        LegalCustomReportParameters.builder().buildWithDefaults("app"));
  }

  @Test(expected = NotFoundException.class)
  public void testGenerateLegalAttributionApplicationReport_Authorized() {
    grantLegalReviewerPermission(app.getId());
    applicationAttributionReportBuilder.generateCustomLegalApplicationAttributionReport(
        app,
        BuildStageType.ID,
        LegalCustomReportParameters.builder().buildWithDefaults("app"));
  }
}
