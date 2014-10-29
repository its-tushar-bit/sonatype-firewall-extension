/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.WaivedPolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class WaivedPolicyViolationMigratorTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private WaivedPolicyViolationDAO waivedPolicyViolationDAO = new WaivedPolicyViolationDAO();

  private File sonatypeWork;

  private InsightConfig insightConfig;

  private InsightWork insightWork;

  private WaivedPolicyViolationMigrator waivedPolicyViolationMigrator;

  public void setup(String testDataDir) throws IOException {
    sonatypeWork = temporaryFolder.newFolder();
    String tempFolderPath = sonatypeWork.getAbsolutePath();
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempFolderPath);
    insightWork = new InsightWork(insightConfig);
    waivedPolicyViolationMigrator = new WaivedPolicyViolationMigrator(insightWork);

    // provide evaluation logs and dummied up reports(zip has no content and the report.cache files are loaded) for test
    // purposes
    FileUtils.copyDirectory(new File("target/test-classes", testDataDir), sonatypeWork);
  }

  @Test
  public void testMigrateMultipleApps() throws Exception {
    setup("WaivedPolicyViolationMigratorTest/MultipleApplications");

    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplicationWithSpecificId("app1", "WaivedPolicyViolationMigratorTest1",
        "WaivedPolicyViolationMigratorTest1", org.getId());
    Application app2 = tempEntity.newApplicationWithSpecificId("app2", "WaivedPolicyViolationMigratorTest2",
        "WaivedPolicyViolationMigratorTest2", org.getId());

    tempEntity.newPolicy(org.getId(), "policy2", "policy2 Name");
    // Waiver created before the policy evaluation
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver("hash1", "policy2", org.getId(), "my comment");
    tempEntity.newPolicy(org.getId(), "policy3", "policy3 Name");

    // Two evaluations for app1 + scan1, only the second evaluation should get waived violations (because it's the last
    // one)
    PolicyEvaluation oldEvalApp1Scan1 = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "app1scan1");
    Thread.sleep(1);
    PolicyEvaluation newEvalApp1Scan1 = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "app1scan1");
    // One evaluation for app1 + scan2, should not get waived violations
    PolicyEvaluation evalApp1Scan2 = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "app1scan2");
    // One evaluation for app2, should not get waived violations and should not failed because there's no
    // policythreats.json
    PolicyEvaluation evalApp2Scan1 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "app2scan1");
    // Waiver created after the policy evaluation, should not be used
    Thread.sleep(1);
    tempEntity.newWaiver("hash1", "policy3", org.getId(), "my comment");

    waivedPolicyViolationMigrator.migrate();

    List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationId(oldEvalApp1Scan1.getId());
    assertThat(policyViolations, hasSize(0));

    policyViolations = policyViolationDAO.getByEvaluationId(newEvalApp1Scan1.getId());
    assertThat(policyViolations, hasSize(3));
    PolicyViolation policyViolation = policyViolations.get(0);
    assertWaivedPolicyViolation(newEvalApp1Scan1.getId(), policyViolation, "policy2", "Age", 5, "groupId1",
        "artifactId1", "1.0", "hash1", PolicyThreatCategory.SECURITY, policyWaiver2);
    // Waiver not applicable
    policyViolation = policyViolations.get(1);
    assertWaivedPolicyViolation(newEvalApp1Scan1.getId(), policyViolation, "policy3", "Age", 8, "groupId1",
        "artifactId1", "1.0", "hash1", PolicyThreatCategory.SECURITY, null /* policyWaiver */);
    // Policy does not exist anymore
    policyViolation = policyViolations.get(2);
    assertWaivedPolicyViolation(newEvalApp1Scan1.getId(), policyViolation, "policy4", "Age", 8, "groupId1",
        "artifactId1", "1.0", "hash1", PolicyThreatCategory.QUALITY, null /* policyWaiver */);

    // No waived violations in policythreats.json
    policyViolations = policyViolationDAO.getByEvaluationId(evalApp1Scan2.getId());
    assertThat(policyViolations, hasSize(0));

    // No policythreats.json
    policyViolations = policyViolationDAO.getByEvaluationId(evalApp2Scan1.getId());
    assertThat(policyViolations, hasSize(0));
  }

  private void assertWaivedPolicyViolation(String evaluationId, PolicyViolation policyViolation, String policyId,
      String policyName, int threatLevel, String groupId, String artifactId, String version, String hash,
      PolicyThreatCategory threatCategory, PolicyWaiver policyWaiver)
  {
    assertThat(policyViolation.getPolicyEvaluationId(), is(evaluationId));
    assertThat(policyViolation.getPolicyId(), is(policyId));
    assertThat(policyViolation.getPolicyName(), is(policyName));
    assertThat(policyViolation.getThreatLevel(), is(threatLevel));
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
    assertThat(policyViolation.getComponentIdentifier(), is(componentIdentifier));
    assertThat(policyViolation.getHash(), is(hash));
    assertThat(policyViolation.getThreatCategory(), is(threatCategory));
    assertThat(policyViolation.getConstraintFactsJson().length(), greaterThan(0));
    assertThat(policyViolation.isWaived(), is(true));

    WaivedPolicyViolation waivedPolicyViolation = waivedPolicyViolationDAO.getById(policyViolation.getId());
    if (policyWaiver != null) {
      assertThat(waivedPolicyViolation, notNullValue());
      assertThat(waivedPolicyViolation.getPolicyWaiverId(), is(policyWaiver.getId()));
      assertThat(waivedPolicyViolation.getComment(), is(policyWaiver.getComment()));
    }
    else {
      assertThat(waivedPolicyViolation, nullValue());
    }
  }
}
