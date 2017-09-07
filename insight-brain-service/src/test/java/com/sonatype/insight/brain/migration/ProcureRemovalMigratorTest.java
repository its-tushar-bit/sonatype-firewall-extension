/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.lang.reflect.Field;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternalDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ProcureRemovalMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private ProcureRemovalMigrator procureRemovalMigrator;

  @Test
  public void testMigrate_PolicyAction() throws Exception {
    String appId = tempEntity.newApplicationWithParent().getId();
    PolicyDAO dao = new PolicyDAO();
    Policy policy = tempEntity.newPolicy(appId, "testMigrate_PolicyAction");
    policy.setAction(ProcureRemovalMigrator.ID_PROCURE, Action.ID_WARN);
    policy.setAction(Stage.ID_BUILD, Action.ID_WARN);

    new PolicyInternalDAO().update(PolicyInternal.fromPolicy(policy));

    procureRemovalMigrator.migrate();

    policy = dao.getById(policy.getId());

    assertThat(policy.getActions().keySet(), containsInAnyOrder(Stage.ID_BUILD));
  }

  @Test
  public void testMigrate_PolicyMonitoring() throws Exception {
    tempEntity.newPolicyMonitoring(
        createPolicyMonitoring(tempEntity.newApplicationWithParent().getId(), ProcureRemovalMigrator.ID_PROCURE));
    tempEntity
        .newPolicyMonitoring(createPolicyMonitoring(tempEntity.newApplicationWithParent().getId(), Stage.ID_BUILD));

    procureRemovalMigrator.migrate();

    List<PolicyMonitoring> monitoring = new PolicyMonitoringDAO().getAll();
    assertThat(monitoring, hasSize(1));
    assertThat(monitoring.get(0).getStageTypeId(), is(Stage.ID_BUILD));
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
    PolicyInternalDAO policyInternalDAO = new PolicyInternalDAO();
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
