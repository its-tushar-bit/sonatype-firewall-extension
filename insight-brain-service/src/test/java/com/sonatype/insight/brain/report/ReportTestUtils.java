/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;

public class ReportTestUtils
{
  public static void createReportFile(String appId, String scanId, File reportFile, InsightWork insightWork)
      throws IOException
  {
    FileUtils.copyFile(reportFile, insightWork.getReportFile(appId, scanId));
  }

  public static File zipReportDir(String reportResourceName, TemporaryFolder tempDir) throws URISyntaxException {
    return Paths.get(ReportHelper.zipReport(reportResourceName, tempDir).toURI()).toFile();
  }

  public static void createPolicyThreats(
      String appId,
      String scanId,
      InsightWork insightWork,
      List<PolicyViolation> policyViolations) throws IOException
  {
    PolicyThreats policyThreats = PolicyThreatsAdapter.createPolicyThreats(policyViolations);
    Report.putEntry(insightWork.getReportFile(appId, scanId), ScanPolicyEvaluator.POLICY_THREATS_FILENAME,
        JsonUtils.generate(policyThreats));
  }
}
