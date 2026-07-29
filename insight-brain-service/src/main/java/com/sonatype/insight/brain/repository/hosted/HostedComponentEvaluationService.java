/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class HostedComponentEvaluationService
{
  private static final Logger log = LoggerFactory.getLogger(HostedComponentEvaluationService.class);

  private final HostedComponentScanStorageService hostedComponentScanStorageService;

  private final HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  private final HostedComponentScanQueueConsumer hostedComponentScanQueueConsumer;

  private final ScanPersistenceService scanPersistenceService;

  @Inject
  public HostedComponentEvaluationService(
      final HostedComponentScanStorageService hostedComponentScanStorageService,
      final HostedComponentScanQueueDAO hostedComponentScanQueueDAO,
      final HostedComponentScanQueueConsumer hostedComponentScanQueueConsumer,
      final ScanPersistenceService scanPersistenceService)
  {
    this.hostedComponentScanStorageService = hostedComponentScanStorageService;
    this.hostedComponentScanQueueDAO = hostedComponentScanQueueDAO;
    this.hostedComponentScanQueueConsumer = hostedComponentScanQueueConsumer;
    this.scanPersistenceService = scanPersistenceService;
  }

  public String queueScan(
      final String repositoryId,
      final String componentId,
      final String policyEvaluationStage,
      final File scanFile) throws IOException
  {
    log.debug("Queueing scan for repositoryId={}, componentId={}", repositoryId, componentId);

    ScanEntity scanEntity = hostedComponentScanStorageService.storeScanFile(repositoryId, scanFile);
    String scanFileId = scanEntity.getName();

    HostedComponentScanQueue hostedComponentScanQueueEntity = new HostedComponentScanQueue(
        componentId,
        scanFileId,
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repositoryId);
    hostedComponentScanQueueEntity.setPolicyEvaluationStage(policyEvaluationStage);

    try {
      hostedComponentScanQueueDAO.insert(hostedComponentScanQueueEntity);
    }
    catch (RuntimeException e) {
      try {
        scanPersistenceService.deleteScan(scanEntity);
      }
      catch (IOException deleteEx) {
        log.warn("Failed to clean up scan file scanFileId={} after DB insert failure", scanFileId, deleteEx);
      }
      throw e;
    }

    String jobId = hostedComponentScanQueueEntity.getId();
    log.debug("Enqueued scan job id={} for componentId={}, scanFileId={}, repositoryId={}",
        jobId, componentId, scanFileId, repositoryId);

    hostedComponentScanQueueConsumer.triggerProcessing();
    return jobId;
  }
}
