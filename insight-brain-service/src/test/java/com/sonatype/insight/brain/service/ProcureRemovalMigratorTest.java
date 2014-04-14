/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

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
  public void testPolicyMonitorsRemoved() throws Exception {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();
    dao.insert(createPolicyMonitoring(createApplication(), ProcureRemovalMigrator.ID_PROCURE));
    dao.insert(createPolicyMonitoring(createApplication(), Stage.ID_BUILD));

    procureRemovalMigrator.migrate();

    List<PolicyMonitoring> monitoring = dao.getAll();
    assertEquals(1, monitoring.size());
    assertEquals(Stage.ID_BUILD, monitoring.get(0).getStageTypeId());
  }

  private String createApplication() {
    Application app = tempEntity.newApplicationWithParent("ProcureRemovalMigratorTest" + x++);
    return app.getId();
  }

  private PolicyMonitoring createPolicyMonitoring(String ownerId, String stageTypeId) throws Exception {
    Field field = PolicyMonitoring.class.getDeclaredField("stageTypeId");
    field.setAccessible(true);
    PolicyMonitoring monitoring = new PolicyMonitoring();
    monitoring.setOwnerId(ownerId);
    field.set(monitoring, stageTypeId);
    return monitoring;
  }
}
