/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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

public class SearchTestHelper
{
  private final TemporaryEntity tempEntity;

  private final TestInsightBrainServiceRule brain;

  public static class ComponentInfo
  {
    public String hash;

    public ComponentIdentifier componentIdentifier;

    public ComponentInfo(final String hash, final ComponentIdentifier componentIdentifier) {
      this.hash = hash;
      this.componentIdentifier = componentIdentifier;
    }
  }

  public Map<String, List<ComponentInfo>> createTestComponentInfoForTwoApps(String appPublicId1, String appPublicId2) {
    Map<String, List<ComponentInfo>> appToComponentMap = new LinkedHashMap<>();

    List<ComponentInfo> app1ComponentInfos = new ArrayList<>();
    app1ComponentInfos.add(new ComponentInfo("1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar")));
    app1ComponentInfos.add(new ComponentInfo("2143b68270b82576110f",
        ComponentIdentifier.createNugetCoordinates("simplejson", "0.38.0")));
    app1ComponentInfos.add(new ComponentInfo("a397f601582e5ccd4b1a",
        ComponentIdentifier.createMavenCoordinates("tomcat", "servlets-default", "5.5.4", "", "jar")));
    app1ComponentInfos.add(new ComponentInfo("69b58197caabec2e0d06", null));
    appToComponentMap.put(appPublicId1, app1ComponentInfos);

    List<ComponentInfo> app2ComponentInfos = new ArrayList<>();
    app2ComponentInfos.add(new ComponentInfo("1249e25aebb15358bedd",
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar")));
    app2ComponentInfos.add(new ComponentInfo("2143b68270b82576110f",
        ComponentIdentifier.createNugetCoordinates("simplejson", "0.38.0")));
    appToComponentMap.put(appPublicId2, app2ComponentInfos);

    return appToComponentMap;
  }

  public SearchTestHelper(TemporaryEntity tempEntity, TestInsightBrainServiceRule brain) {
    this.tempEntity = tempEntity;
    this.brain = brain;
  }

  public Application createAppWithScan(String appPublicId, String stageId, List<ComponentInfo> componentInfos)
      throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(appPublicId.toUpperCase(Locale.ENGLISH), appPublicId, org.getId());
    createScanForApp(app.getId(), stageId, app.getPublicId(), componentInfos);
    return app;
  }

  public void createScanForApp(String appId, String stageId, String resPath, List<ComponentInfo> componentInfos)
      throws Exception {
    String scanId = UUID.randomUUID().toString().replace("-", "");
    File policyAlertsJsonFile = getReportCacheEntry(appId, scanId, PolicyEvaluationUtils.POLICY_ALERTS_FILENAME);
    FileUtils.copyURLToFile(
        getClass().getResource("/ApiSearchResourceTest/" + resPath + "/" + PolicyEvaluationUtils.POLICY_ALERTS_FILENAME),
        policyAlertsJsonFile);
    createReport(appId, scanId);

    for (ComponentInfo componentInfo : componentInfos) {
      tempEntity.newApplicationComponent(appId, stageId, componentInfo.hash, componentInfo.componentIdentifier);
    }

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(appId, stageId, scanId);

    PolicyAlert[] policyAlerts = JsonUtils.parse(FileUtils.fileRead(policyAlertsJsonFile), PolicyAlert[].class);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    for (PolicyAlert policyAlert : policyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      createPolicy(appId, policyFact.getPolicyId(), policyFact.getPolicyName());
      for (ComponentFact componentFact : policyFact.getComponentFacts()) {
        PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policyFact.getPolicyId(),
            policyFact.getPolicyName(), policyFact.getThreatLevel(), PolicyThreatCategory.OTHER,
            componentFact.getHash(), componentFact.getComponentIdentifier(), componentFact.getConstraintFacts(),
            componentFact.getPathnames());
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
