/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.model.ClientScanResult;

import org.apache.openjpa.persistence.RollbackException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.policy.Stage.ID_BUILD;
import static com.sonatype.insight.scan.model.ClientScanType.SONATYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class CloudPolicyClientTest
    extends AbstractBrainServiceTest
{
  private static final String TEST_APPLICATION_ID = "the-app-id";

  private static final String TEST_SCAN_ID = "the-scan-id";

  protected InsightWork insightWork;

  @Before
  public void setup() {
    insightWork = getCLMServer().getInstance(InsightWork.class);
  }

  @After
  public void after() throws InterruptedException {
    // We need to do this special cleanup because these tests start async policy evaluations but they don't wait for the
    // policy evaluations to finish.
    // This means the tests usually finish before the policy evaluations finish, which creates a race condition with the
    // TemporaryEntity cleanup.
    long start = System.currentTimeMillis();
    while (true) {
      try {
        tempEntity.after();
        return;
      }
      catch (RollbackException e) {
        if (System.currentTimeMillis() - start > 10000) {
          throw e;
        }
        Thread.sleep(50);
      }
    }
  }

  @Test
  public void testEvaluateCLI_CloudPolicyClient_WithoutWaivers() throws IOException {
    CloudPolicyClient policyClient = spyCloudPolicyClient();

    // we expect an exception as we are not setup for testing further that the evaluation
    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> evaluateCLI(policyClient))
        .withMessage("Policy evaluation could not be completed: Could not download the report for scan ID SCAN-ID");

    verify(policyClient).beforePolling(any(), any());
    verify(policyClient).evaluationRequestPathBuilder(any(), any(), any());
    verify(policyClient, never()).withScanTypeAndWaiverBuilder(any(), any());
    verify(policyClient, never()).createWaiversEntity(any());
  }

  @Test
  public void testEvaluateCLI_CloudPolicyClient_WithWaivers() throws IOException {
    CloudPolicyClient policyClient = spyCloudPolicyClient("--waivers", "something");

    try {
      evaluateCLI(policyClient);
    }
    catch (IOException e) {
      // we expect an exception as we are not setup for testing further that the evaluation
      assertThat(e.getMessage())
          .isEqualTo("Resource not found, please check your request URL.");
    }

    verify(policyClient).beforePolling(any(), any());
    verify(policyClient).evaluationRequestPathBuilder(any(), any(), any());
    verify(policyClient).withScanTypeAndWaiverBuilder(any(), any());
    verify(policyClient).createWaiversEntity(any());
  }

  protected void evaluateCLI(final CloudPolicyClient policyClient) throws IOException {
    Application application = tempEntity.newApplicationWithParent(TEST_APPLICATION_ID);
    File scanFile = createScanFile(application, TEST_SCAN_ID);
    ClientScanResult clientScanResult = new ClientScanResult(scanFile, false);

    policyClient.evaluateCLI(clientScanResult, SONATYPE, new Stage(ID_BUILD));
  }

  protected CloudPolicyClient spyCloudPolicyClient(final String... args) {
    return spy(new CloudPolicyClient(
        new CloudParameters(args),
        getCLMServer().getClientConfiguration(),
        TEST_APPLICATION_ID));
  }

  protected File createScanFile(Application app, String scanId) {
    File scanFile = insightWork.getScanFile(app.getId(), scanId);

    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), "test".getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return scanFile;
  }
}
