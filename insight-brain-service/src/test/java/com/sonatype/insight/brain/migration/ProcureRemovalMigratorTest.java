/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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

  private static PolicyInternalDAO policyInternalDAO = new PolicyInternalDAO();

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
    policy.setAction(ProcureRemovalMigrator.ID_PROCURE, Action.ID_WARN);
    policy.setAction(Stage.ID_BUILD, Action.ID_WARN);

    policyInternalDAO.update(PolicyInternal.fromPolicy(policy));

    procureRemovalMigrator.migrate();

    policy = dao.getById(policy.getId());

    assertThat(policy.getActions().keySet(), containsInAnyOrder(Stage.ID_BUILD));
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

  @Test
  public void testMigrate_DeprecatedConditionForSecurityVulnerabilities() throws Exception {
    // Verifies that the deprecated condition for security vulnerabilities can be migrated.
    // The migrator should not fail when it encounters this policy condition type.
    String policyId = tempEntity.newPolicy("Test").getId();
    PolicyInternal policyInternal = policyInternalDAO.getById(policyId);
    policyInternal.setContent(getPolicyContent("policy_deprecated_security_vulnerability_condition.json"));
    policyInternalDAO.update(policyInternal);

    procureRemovalMigrator.migrate();

    Policy policy = new PolicyDAO().getById(policyId);
    Condition deprecatedCondition = policy.getConstraints().get(0).getConditions().get(0);
    assertThat(deprecatedCondition.getConditionTypeId(), is("SecurityVulnerability"));
    assertThat(deprecatedCondition.getOperator(), is("present"));
    assertThat(deprecatedCondition.getValue(), is(nullValue()));
  }

  private String getPolicyContent(String filename) throws Exception {
    return IOUtil.toString(getClass().getResourceAsStream("/ProcureRemovalMigratorTest/" + filename), "UTF-8");
  }
}
