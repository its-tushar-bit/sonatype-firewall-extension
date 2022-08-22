/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.scan.ScanResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.io.ScanReader;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Runs the default unit tests in {@link DefaultPolicyEvaluatorTest}.
 */
public class JUnitDefaultPolicyEvaluatorTest
    extends DefaultPolicyEvaluatorTest
{
  protected DefaultPolicyEvaluator evaluator;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    evaluator = getCLMServer().getInstance(DefaultPolicyEvaluator.class);
  }

  @Override
  protected AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params) {
    return new JUnitPolicyEvaluatorTestRunner(params, evaluator, logOutput);
  }

  @Test
  public void testScan_UIAndCLIHaveSameConfigurations() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    File scanFile = insightWork.getScanFile(application.getId(), "SCAN-ID");
    File scanDir = getCLMServer().getInstance(InsightWork.class).getScanDir(application.getId());
    scanDir.mkdirs();

    // Scan via the CLI
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123",
        "-i", application.getPublicId(), "--output-directory", tempDir.getRoot().getAbsolutePath(),
        "src/test/data/manifest/conda.txt");
    withTestRunner(params).doPolicyEvaluationRun();

    await().atMost(10, TimeUnit.SECONDS).until(scanFile::exists);
    assertThat(scanFile).isNotNull();
    Scan cliScan = new ScanReader().read(scanFile);
    assertThat(cliScan).isNotNull();
    assertThat(cliScan.getConfiguration()).isNotNull();

    FileUtils.cleanDirectory(scanDir);

    // Scan via upload
    HttpResponse post = restRequest()
        .path(ScanResource.RESOURCE_PATH)
        .query("stageId", Stage.ID_BUILD)
        .parameter(application.getPublicId())
        .part("file", "conda.txt",
            FileUtils.readFileToString(new File("src/test/data/manifest/conda.txt"), StandardCharsets.UTF_8))
        .part("filename", "conda.txt")
        .post();

    assertResponseStatus(200, post);
    await().atMost(10, TimeUnit.SECONDS).until(scanFile::exists);
    assertThat(scanFile).isNotNull();
    Scan uiScan = new ScanReader().read(scanFile);
    assertThat(uiScan).isNotNull();
    assertThat(uiScan.getConfiguration()).isNotNull();

    // Check the UI/CLI scans have the same configuration
    assertThat(cliScan.getItems().get(0).getContentType()).isEqualTo(ItemContentType.CONDA_FILE);
    assertThat(uiScan.getItems().get(0).getContentType()).isEqualTo(ItemContentType.CONDA_FILE);
    assertThat(cliScan.getConfiguration()).usingRecursiveComparison().isEqualTo(uiScan.getConfiguration());
  }
}
