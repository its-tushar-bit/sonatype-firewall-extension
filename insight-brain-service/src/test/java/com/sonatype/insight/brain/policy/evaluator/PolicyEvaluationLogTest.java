/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class PolicyEvaluationLogTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  // TODO Move to the tests for the policy evaluation migrator - CLM-2084
  // @Test
  // public void testMigrate() throws Exception {
  // File legacyLog = new File(tmpDir.getRoot(), "policyevaluations.json");
  // FileUtils.copyURLToFile(getClass().getResource("/PolicyEvaluationLogTest/policyevaluations.json"), legacyLog);
  //
  // PolicyEvaluationLog log = new PolicyEvaluationLog(tmpDir.getRoot());
  //
  // PolicyEvaluation eval = log.lastByStage(Stage.ID_BUILD);
  // assertNotNull(eval);
  // assertEquals(Stage.ID_BUILD, eval.getStageTypeId());
  // assertEquals("4ec4edff03b145e38b6915dda1d0b00f", eval.getScanId());
  //
  // eval = log.lastByStage(Stage.ID_RELEASE);
  // assertNotNull(eval);
  // assertEquals(Stage.ID_RELEASE, eval.getStageTypeId());
  // assertEquals("46969a0aa117487aa769b8c550095973", eval.getScanId());
  //
  // assertFalse(legacyLog.exists());
  //
  // assertNull(log.lastByStage(Stage.ID_STAGE_RELEASE));
  // assertNull(log.lastByStage(Stage.ID_OPERATE));
  // assertNull(log.lastByStage(Stage.ID_PROCURE));
  // assertNull(log.lastByStage(Stage.ID_DEVELOP));
  // }
}
