/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;

public class ApiReportViolationsDiffServiceAuthzAndLicenseTest
    extends AbstractServiceAuthzTest
{
  private static final String FROM_COMMIT_HASH = "abcdef1234abcdef1234abcdef1234abcdef1234";

  private static final String TO_COMMIT_HASH = "1234567890123456789012345678901234567890";

  private static final String FROM_SCAN_ID = "fromScanId";

  private static final String TO_SCAN_ID = "toScanId";

  @Inject
  private InsightWork insightWork;

  @Inject
  private ApiReportViolationsDiffService apiReportViolationsDiffService;

  @Inject
  private TestProductLicense testProductLicense;

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolationDiff_Unauthenticated() {
    apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolationDiff_Unauthorized() {
    login();
    apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetPolicyViolationDiff_InvalidLicense() {
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION);
    grantReadPermission(app.getId());
    apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false);
  }

  @Test
  public void testGetPolicyViolationDiff_Authorized() throws URISyntaxException, IOException {
    grantReadPermission(app.getId());
    Date date = new Date();
    // setup reports
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/from-report", tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/to-report", tempDir),
        insightWork);

    // setup evaluations
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID, false, false, false,
        date, FROM_COMMIT_HASH);
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID, false, false, false,
        date, TO_COMMIT_HASH);
    apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false);
  }
}
