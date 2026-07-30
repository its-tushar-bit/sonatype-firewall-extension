/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.KeyValueDAO;
import com.sonatype.insight.brain.dataaccess.evaluation.EvaluationQueueDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.KeyValue;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class EvaluationQueueIntegrationTest
    extends AbstractBrainServiceIntegrationTest
{
  @Before
  public void disableEvaluationQueueProducer() {
    getCLMServer().getInstance(EvaluationQueueProducer.class).disableForTesting = true;
    // Clear any stale checkpoint that may have been created by automatic execution during server startup.
    // During startup, register() schedules a periodic task that can fire before disableForTesting is set,
    // creating a completed checkpoint (with no SBOMs) that would cause subsequent manual execute() calls
    // to skip processing.
    getCLMServer().getInstance(KeyValueDAO.class).deleteByKey(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT);
  }

  @Test
  public void testEvaluationQueueProduceAndConsume_multipleSboms() throws Exception {
    EvaluationQueueConfig config = EvaluationQueueConfig.builder()
        .enabled(true)
        .startTimeDelayEnabled(false)
        .producerPeriod(Duration.ofDays(1))
        .consumerPeriod(Duration.ofDays(1))
        .producerMaxQueuedRows(100)
        .consumerThreadsPerTenant(1)
        .consumerMaxQueuedRows(10)
        .build();
    getCLMServer().getInstance(ApiConfigurationService.class)
        .setConfigurationNoAuthz(
            Map.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG, JsonUtils.convertValue(config, Map.class)));
    Application app1 = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyMonitoring(app1.getId(), ComplianceStageType.ID);
    createSbomWithScan(app1.getId(), new Date(1));

    getCLMServer().getInstance(EvaluationQueueProducer.class).execute((JobExecutionContext) null);

    EvaluationQueueDAO evaluationQueueDAO = getCLMServer().getInstance(EvaluationQueueDAO.class);
    assertThat(evaluationQueueDAO.getCount()).isEqualTo(1);

    EvaluationQueueConsumer consumer = getCLMServer().getInstance(EvaluationQueueConsumer.class);
    consumer.run();

    await().atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofSeconds(1))
        .until(() -> evaluationQueueDAO.getCount() == 0);

    PolicyEvaluationDAO policyEvaluationDAO = getCLMServer().getInstance(PolicyEvaluationDAO.class);
    PolicyEvaluation evaluation =
        policyEvaluationDAO.getLastByOwnerIdAndStageId(app1.getId(), ComplianceStageType.ID);
    assertThat(evaluation).isNotNull();
    assertThat(evaluation.getScanId()).isNotNull();
  }

  private void createSbomWithScan(final String applicationId, final Date createdDate) throws Exception {
    ThirdPartySbomMetadata sbom = tempEntity.newThirdPartySbomMetadata(
        applicationId,
        ThirdPartySbomMetadataStatus.ACTIVE,
        createdDate);

    String scanId = "scan-" + System.currentTimeMillis();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setTimeToReport(1L);
    mockScanReceipt(scanReceipt);
    mockReport(scanId, "/" + EvaluationQueueIntegrationTest.class.getSimpleName() + "/report");

    ThirdPartyFile thirdPartyFile =
        getCLMServer().getInstance(ThirdPartyFileDAO.class).getById(sbom.getThirdPartyFileId());
    String filteredScanFile = "filtered-scan-" + System.currentTimeMillis();
    tempEntity.newThirdPartyScan(
        "scanRequestId-" + System.currentTimeMillis(),
        scanId,
        thirdPartyFile,
        filteredScanFile);

    ScanEntity filteredScan =
        getCLMServer().getInstance(ScanPersistenceService.class).getScanByName(applicationId, filteredScanFile);
    try (OutputStream outputStream = filteredScan.getOutputStream()) {
      outputStream.write("dummy scan content".getBytes(StandardCharsets.UTF_8));
    }
  }
}
