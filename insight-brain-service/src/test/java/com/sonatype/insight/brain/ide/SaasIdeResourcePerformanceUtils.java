/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;

import org.mockito.Mockito;

public class SaasIdeResourcePerformanceUtils
{
  private static int counter = 0;

  private static class EmptyEnumeration
      implements Enumeration<String>
  {
    private static final EmptyEnumeration instance = new EmptyEnumeration();

    private EmptyEnumeration() {
    }

    public static EmptyEnumeration getInstance() {
      return instance;
    }

    @Override
    public boolean hasMoreElements() {
      return false;
    }

    @Override
    public String nextElement() {
      throw new NoSuchElementException();
    }
  }

  public static HttpServletRequest createRequest() throws Exception {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getMethod()).thenReturn("GET");
    Mockito.when(request.getHeaderNames()).thenReturn(EmptyEnumeration.getInstance());
    return request;
  }

  public static SaasClient createSaasClient(String saasAddress) {
    InsightConfig config = new InsightConfig();
    config.setSaasAddress(saasAddress);
    return new SaasClient(new InsightProxy(config), new CLMLicenseManager(new TestProductLicenseManager(true),
        new TestLicenseFingerprinter()));
  }

  public static InsightWork createInsightWork() throws IOException {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = File.createTempFile("saasIde", "tmp");
    workDir.delete();
    workDir.mkdirs();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    InsightWork work = new InsightWork(insightConfig);
    return work;
  }

  public static void addPolicy(Application app, Policy[] policies, InsightWork work) throws Exception {
    String appId = app.getId();
    PolicyDAO policyDAO = new PolicyDAO(work.getWorkDir());

    for (Policy policy : policies) {
      policyDAO.insert(appId, policy);
    }
  }

  public static Policy createSvPolicy() {
    Policy policy = new Policy();
    policy.setEnabled(true);
    policy.setName("NoSV" + (counter++));
    Constraint constraint = new Constraint();
    constraint.setName("NoSV");
    policy.setConstraints(Collections.singletonList(constraint));
    Condition condition = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint.addCondition(condition);
    return policy;
  }
}
