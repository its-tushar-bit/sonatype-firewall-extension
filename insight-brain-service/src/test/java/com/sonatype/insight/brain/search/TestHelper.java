/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.service.TestInsightBrainServiceRule;
import com.sonatype.insight.json.store.JsonUtils;

import org.codehaus.plexus.util.FileUtils;

class TestHelper
{
  private final TemporaryEntity tempEntity;

  private final TestInsightBrainServiceRule brain;

  public TestHelper(TemporaryEntity tempEntity, TestInsightBrainServiceRule brain) {
    this.tempEntity = tempEntity;
    this.brain = brain;
  }

  public Application createAppWithScan(String appPublicId, String stageId) throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(appPublicId.toUpperCase(Locale.ENGLISH), appPublicId, org.getId());
    createScanForApp(app.getId(), stageId, app.getPublicId());
    return app;
  }

  public void createScanForApp(String appId, String stageId, String resPath) throws Exception {
    String scanId = UUID.randomUUID().toString().replace("-", "");
    FileUtils.copyURLToFile(getClass().getResource("/SearchResourceTest/" + resPath + "/bom.json"),
        getReportCacheEntry(appId, scanId, "bom.json"));
    File policyAlertsJsonFile = getReportCacheEntry(appId, scanId, PolicyEvaluationUtils.POLICY_ALERTS_FILENAME);
    FileUtils.copyURLToFile(
        getClass().getResource("/SearchResourceTest/" + resPath + "/" + PolicyEvaluationUtils.POLICY_ALERTS_FILENAME),
        policyAlertsJsonFile);
    createReport(appId, scanId);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(appId, stageId, scanId);

    PolicyAlert[] policyAlerts = JsonUtils.parse(FileUtils.fileRead(policyAlertsJsonFile), PolicyAlert[].class);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    for (PolicyAlert policyAlert : policyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      createPolicy(appId, policyFact.getPolicyId(), policyFact.getPolicyName());
      for (ComponentFact componentFact : policyFact.getComponentFacts()) {
        PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policyFact.getPolicyId(),
            policyFact.getPolicyName(), policyFact.getThreatLevel(), PolicyThreatCategory.OTHER,
            componentFact.getHash(), componentFact.getGroupId(), componentFact.getArtifactId(),
            componentFact.getVersion(), componentFact.getConstraintFacts(), componentFact.getPathnames());
        policyViolationDAO.insert(policyViolation);
      }
    }
  }

  private File getReportCacheEntry(String appId, String scanId, String name) {
    return new File(new File(brain.getReportDir(appId, scanId), "report.cache"), name);
  }

  private void createReport(String appId, String scanId) throws Exception {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(brain.getReportDir(appId, scanId),
        "report.zip")));
    try {
      zos.putNextEntry(new ZipEntry("index.html"));
    }
    finally {
      zos.close();
    }
  }

  private void createPolicy(String appId, String policyId, String policyName) {
    if (new PolicyDAO().getById(policyId) == null) {
      tempEntity.newPolicy(appId, policyId, policyName);
    }
  }
}
