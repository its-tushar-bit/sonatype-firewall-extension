/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProcureRemovalMigratorTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private File sonatypeWork;

  private InsightConfig insightConfig;

  private InsightWork insightWork;

  private ProcureRemovalMigrator procureRemovalMigrator;

  private int x = 0;

  @Before
  public void setup() throws IOException {
    sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
    procureRemovalMigrator = new ProcureRemovalMigrator(insightWork);
  }

  @Test
  public void testPolicyActionsRemoved() throws Exception {
    String appId = createApplication();
    PolicyDAO dao = new PolicyDAO();
    Policy policy = tempEntity.newPolicy(appId, "testPolicyMonitorsRemoved");
    policy.setActions(ProcureRemovalMigrator.ID_PROCURE, Collections.singletonList(new Action(Action.ID_WARN)));
    policy.setActions(Stage.ID_BUILD, Collections.singletonList(new Action(Action.ID_WARN)));
    dao.update(policy);

    procureRemovalMigrator.migrate();

    policy = dao.getById(policy.getId());

    assertNull(policy.getActions(ProcureRemovalMigrator.ID_PROCURE));
    assertEquals(1, policy.getActions(Stage.ID_BUILD).size());
  }

  @Test
  public void testPolicyMonitorsRemoved() throws IOException {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();
    dao.insert(new PolicyMonitoring(createApplication(), ProcureRemovalMigrator.ID_PROCURE));
    dao.insert(new PolicyMonitoring(createApplication(), Stage.ID_BUILD));

    procureRemovalMigrator.migrate();

    List<PolicyMonitoring> monitoring = dao.getAll();
    assertEquals(1, monitoring.size());
    assertEquals(Stage.ID_BUILD, monitoring.get(0).getStageTypeId());
  }

  private String createApplication() {
    Application app = tempEntity.newApplicationWithParent("ProcureRemovalMigratorTest" + x++);
    return app.getId();
  }
}
