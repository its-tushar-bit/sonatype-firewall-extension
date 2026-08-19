/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.InsightJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BranchMonitorExecutor
{
  private static final Logger log = LoggerFactory.getLogger(BranchMonitorExecutor.class);

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  @Inject
  protected BranchMonitorExecutor(
      SourceControlDAO sourceControlDAO,
      SourceControlEventPublisher sourceControlEventPublisher)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
  }

  abstract void schedule(InsightJob job);

  abstract void performScan(InsightJob job);

  protected void updateDefaultBranchScans(long intervalInMinutes) {
    long start = System.currentTimeMillis();
    log.debug("Updating default branch source scans.");

    Date scanLimitDate =
        Date.from(LocalDateTime.now().minusMinutes(intervalInMinutes).atZone(ZoneId.systemDefault()).toInstant());

    List<SourceControl> sourceControlList =
        sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(scanLimitDate);

    for (SourceControl sourceControl : sourceControlList) {
      initiateDefaultBranchSourceScans(sourceControl);
    }

    log.debug("Initiated default branch source scans for {} applications in {} ms.", sourceControlList.size(),
        System.currentTimeMillis() - start);
  }

  private void initiateDefaultBranchSourceScans(SourceControl sourceControl) {
    String statusId = UUID.randomUUID().toString().replace("-", "");

    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .forSourceControlEvaluation()
        .setApplicationId(sourceControl.getOwnerId())
        .setStageTypeId(Stage.ID_SOURCE)
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING)
        .setStatusId(statusId)
        .setBranchName(sourceControl.getBaseBranch());

    String message = String.format(
        "a source control evaluation for application %s, stage %s and branch %s with status ID %s.",
        sourceControlEvent.getApplicationId(), sourceControlEvent.getStageTypeId(),
        sourceControlEvent.getBranchName(), sourceControlEvent.getStatusId());

    try {
      sourceControlEventPublisher.publishEvent(sourceControlEvent);
      log.debug("Initiated " + message);
    }
    catch (Exception e) {
      log.error("Failed to initiate {}", message, e);
    }
  }
}
