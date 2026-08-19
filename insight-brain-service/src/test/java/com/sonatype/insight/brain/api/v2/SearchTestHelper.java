/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

public class SearchTestHelper
{
  private final TemporaryEntity tempEntity;

  public static class PolicyViolationInfo
  {
    public final String policyName;

    public final String reason;

    public final int threatLevel;

    public PolicyViolationInfo(final String policyName, final String reason, final int threatLevel) {
      this.policyName = policyName;
      this.reason = reason;
      this.threatLevel = threatLevel;
    }
  }

  public static class ComponentInfo
  {
    public final String hash;

    public final ComponentIdentifier componentIdentifier;

    public final List<PolicyViolationInfo> policyViolationInfos;

    public ComponentInfo(
        final String hash,
        final ComponentIdentifier componentIdentifier,
        final List<PolicyViolationInfo> policyViolationInfos)
    {
      this.hash = hash;
      this.componentIdentifier = componentIdentifier;
      this.policyViolationInfos = policyViolationInfos == null ? Collections.emptyList() : policyViolationInfos;
    }
  }

  public Map<String, List<ComponentInfo>> createTestComponentInfoForTwoApps(String appPublicId1, String appPublicId2) {
    Map<String, List<ComponentInfo>> appToComponentMap = new LinkedHashMap<>();

    List<ComponentInfo> app1ComponentInfos = new ArrayList<>();
    List<PolicyViolationInfo> app1TomcatUtilPolicyViolationInfos = new ArrayList<>();
    app1TomcatUtilPolicyViolationInfos.add(new PolicyViolationInfo("1st Policy", "Match State was exact", 8));
    app1TomcatUtilPolicyViolationInfos.add(new PolicyViolationInfo("2st Policy", "Found red Label", 4));
    app1ComponentInfos.add(new ComponentInfo("1249e25aebb15358bedd", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "jar"), app1TomcatUtilPolicyViolationInfos));
    app1ComponentInfos.add(new ComponentInfo("2aa135385b1f449292e8", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "zip"), app1TomcatUtilPolicyViolationInfos));
    app1ComponentInfos.add(new ComponentInfo("c85713867bef4a3b91c9", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "sources", "jar"), app1TomcatUtilPolicyViolationInfos));
    app1ComponentInfos.add(new ComponentInfo("a18da38b875b4658b4e9", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "sources", "zip"), app1TomcatUtilPolicyViolationInfos));
    List<PolicyViolationInfo> app1SimpleJsonPolicyViolationInfos = new ArrayList<>();
    app1SimpleJsonPolicyViolationInfos.add(new PolicyViolationInfo("1st Policy", "Match State was exact", 8));
    app1ComponentInfos.add(new ComponentInfo("2143b68270b82576110f", ComponentIdentifier.createNugetCoordinates(
        "simplejson", "0.38.0"), app1SimpleJsonPolicyViolationInfos));
    app1ComponentInfos.add(new ComponentInfo("a397f601582e5ccd4b1a", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "servlets-default", "5.5.4", "", "jar"), null));
    app1ComponentInfos.add(new ComponentInfo("69b58197caabec2e0d06", null, null));
    appToComponentMap.put(appPublicId1, app1ComponentInfos);

    List<ComponentInfo> app2ComponentInfos = new ArrayList<>();
    List<PolicyViolationInfo> app2TomcatUtilPolicyViolationInfos = new ArrayList<>();
    app2TomcatUtilPolicyViolationInfos.add(new PolicyViolationInfo("2st Policy", "Found red Label", 4));
    app2ComponentInfos.add(new ComponentInfo("1249e25aebb15358bedd", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "jar"), app2TomcatUtilPolicyViolationInfos));
    app2ComponentInfos.add(new ComponentInfo("2aa135385b1f449292e8", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "zip"), app2TomcatUtilPolicyViolationInfos));
    app2ComponentInfos.add(new ComponentInfo("c85713867bef4a3b91c9", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "sources", "jar"), app2TomcatUtilPolicyViolationInfos));
    app2ComponentInfos.add(new ComponentInfo("a18da38b875b4658b4e9", ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "sources", "zip"), app2TomcatUtilPolicyViolationInfos));
    List<PolicyViolationInfo> app2SimpleJsonPolicyViolationInfos = new ArrayList<>();
    app2SimpleJsonPolicyViolationInfos.add(new PolicyViolationInfo("2st Policy", "Found red Label", 4));
    app2ComponentInfos.add(new ComponentInfo("2143b68270b82576110f", ComponentIdentifier.createNugetCoordinates(
        "simplejson", "0.38.0"), app2SimpleJsonPolicyViolationInfos));
    appToComponentMap.put(appPublicId2, app2ComponentInfos);

    return appToComponentMap;
  }

  public SearchTestHelper(TemporaryEntity tempEntity) {
    this.tempEntity = tempEntity;
  }

  public Application createAppWithScan(
      String appPublicId,
      String stageId,
      List<ComponentInfo> componentInfos,
      String scanId)
  {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(appPublicId.toUpperCase(Locale.ENGLISH), appPublicId, org.getId());
    createScanForApp(app, stageId, componentInfos, scanId);
    return app;
  }

  void createScanForApp(Application app, String stageId, List<ComponentInfo> componentInfos, String scanId) {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), stageId, scanId);

    Map<String, Policy> policies = new HashMap<>();
    for (ComponentInfo componentInfo : componentInfos) {
      tempEntity.newApplicationComponent(app.getId(), stageId, componentInfo.hash, componentInfo.componentIdentifier);
      for (PolicyViolationInfo policyViolationInfo : componentInfo.policyViolationInfos) {
        Policy policy = policies.get(policyViolationInfo.policyName);
        if (policy == null) {
          policy = tempEntity.newPolicy(app.getId(), policyViolationInfo.policyName, policyViolationInfo.threatLevel);
          policies.put(policy.getName(), policy);
        }
        tempEntity.newPolicyViolation(policyEvaluation, policy, componentInfo.componentIdentifier, componentInfo.hash,
            policyViolationInfo.reason);
      }
    }
  }
}
