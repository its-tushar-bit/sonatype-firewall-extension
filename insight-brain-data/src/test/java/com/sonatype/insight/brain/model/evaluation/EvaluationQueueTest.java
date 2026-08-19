/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.evaluation;

import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class EvaluationQueueTest
{
  @Test
  public void testSetStageTypeId_ValidStage() {
    EvaluationQueue evaluationQueue = new EvaluationQueue();

    evaluationQueue.setStageTypeId(ProxyStageType.ID);
    assertThat(evaluationQueue.getStageTypeId()).isEqualTo(ProxyStageType.ID);

    evaluationQueue.setStageTypeId(ComplianceStageType.ID);
    assertThat(evaluationQueue.getStageTypeId()).isEqualTo(ComplianceStageType.ID);

    evaluationQueue.setStageTypeId(DevelopStageType.ID);
    assertThat(evaluationQueue.getStageTypeId()).isEqualTo(DevelopStageType.ID);

    evaluationQueue.setStageTypeId(SourceStageType.ID);
    assertThat(evaluationQueue.getStageTypeId()).isEqualTo(SourceStageType.ID);

    evaluationQueue.setStageTypeId(BuildStageType.ID);
    assertThat(evaluationQueue.getStageTypeId()).isEqualTo(BuildStageType.ID);

    evaluationQueue.setStageTypeId(StageReleaseStageType.ID);
    assertThat(evaluationQueue.getStageTypeId()).isEqualTo(StageReleaseStageType.ID);

    evaluationQueue.setStageTypeId(ReleaseStageType.ID);
    assertThat(evaluationQueue.getStageTypeId()).isEqualTo(ReleaseStageType.ID);

    evaluationQueue.setStageTypeId(OperateStageType.ID);
    assertThat(evaluationQueue.getStageTypeId()).isEqualTo(OperateStageType.ID);
  }

  @Test
  public void testSetStageTypeId_HostedStageType_Rejected() {
    EvaluationQueue evaluationQueue = new EvaluationQueue();
    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> evaluationQueue.setStageTypeId("hosted"));
  }

  @Test
  public void testSetStageTypeId_InvalidStage() {
    EvaluationQueue evaluationQueue = new EvaluationQueue();

    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> evaluationQueue.setStageTypeId("invalid-stage"));
  }
}
