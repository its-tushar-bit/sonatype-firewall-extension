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
import com.sonatype.insight.brain.hds.ScanUploader;
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
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IqPostgresTest
public class IqPostgresEvaluationQueueIntegrationTest
{
  private IqTestContext ctx;

  @BeforeEach
  public void disableEvaluationQueueProducer() {
    ctx.lookup(EvaluationQueueProducer.class).disableForTesting = true;
    // Clear any stale checkpoint that may have been created by automatic execution during server startup.
    // During startup, register() schedules a periodic task that can fire before disableForTesting is set,
    // creating a completed checkpoint (with no SBOMs) that would cause subsequent manual execute() calls
    // to skip processing.
    ctx.lookup(KeyValueDAO.class).deleteByKey(KeyValue.EVALUATION_QUEUE_PRODUCER_CHECKPOINT);
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
    ctx.lookup(ApiConfigurationService.class)
        .setConfigurationNoAuthz(
            Map.of(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG, JsonUtils.convertValue(config, Map.class)));
    Application app1 = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newPolicyMonitoring(app1.getId(), ComplianceStageType.ID);
    createSbomWithScan(app1.getId(), new Date(1));

    ctx.lookup(EvaluationQueueProducer.class).execute((JobExecutionContext) null);

    EvaluationQueueDAO evaluationQueueDAO = ctx.lookup(EvaluationQueueDAO.class);
    assertThat(evaluationQueueDAO.getCount()).isEqualTo(1);

    EvaluationQueueConsumer consumer = ctx.lookup(EvaluationQueueConsumer.class);
    consumer.run();

    await().atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofSeconds(1))
        .until(() -> evaluationQueueDAO.getCount() == 0);

    PolicyEvaluationDAO policyEvaluationDAO = ctx.lookup(PolicyEvaluationDAO.class);
    PolicyEvaluation evaluation =
        policyEvaluationDAO.getLastByOwnerIdAndStageId(app1.getId(), ComplianceStageType.ID);
    assertThat(evaluation).isNotNull();
    assertThat(evaluation.getScanId()).isNotNull();
  }

  private void createSbomWithScan(final String applicationId, final Date createdDate) throws Exception {
    ThirdPartySbomMetadata sbom = ctx.tempEntity()
        .newThirdPartySbomMetadata(
            applicationId,
            ThirdPartySbomMetadataStatus.ACTIVE,
            createdDate);

    String scanId = "scan-" + System.currentTimeMillis();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    scanReceipt.setTimeToReport(1L);
    ctx.hdsRespondWith(scanReceipt).atUri(ScanUploader.HDS_PATH);
    ctx.mockReport(scanId, "/" + getClass().getSimpleName() + "/report");

    ThirdPartyFile thirdPartyFile =
        ctx.lookup(ThirdPartyFileDAO.class).getById(sbom.getThirdPartyFileId());
    String filteredScanFile = "filtered-scan-" + System.currentTimeMillis();
    ctx.tempEntity()
        .newThirdPartyScan(
            "scanRequestId-" + System.currentTimeMillis(),
            scanId,
            thirdPartyFile,
            filteredScanFile);

    ScanEntity filteredScan =
        ctx.lookup(ScanPersistenceService.class).getScanByName(applicationId, filteredScanFile);
    try (OutputStream outputStream = filteredScan.getOutputStream()) {
      outputStream.write("dummy scan content".getBytes(StandardCharsets.UTF_8));
    }
  }
}
