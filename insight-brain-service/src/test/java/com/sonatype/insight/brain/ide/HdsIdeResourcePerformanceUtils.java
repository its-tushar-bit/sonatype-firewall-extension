/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.DefaultHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.version.VersionService;

import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

public class HdsIdeResourcePerformanceUtils
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

  static HttpServletRequest createRequest() throws Exception {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getMethod()).thenReturn("GET");
    Mockito.when(request.getHeaderNames()).thenReturn(EmptyEnumeration.getInstance());
    return request;
  }

  static HdsClient createHdsClient(String hdsUrl) {
    InsightConfig config = new InsightConfig();
    config.setHdsUrl(hdsUrl);
    ((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors().get(0))
        .setPort(8877);
    return new DefaultHdsClient(new InsightProxy(config, new ProxyServerConfigurationDAO(), new PasswordHandler(null)),
        mock(ProductLicense.class), config, new VersionService(), new TelemetryId(config));
  }

  static TelemetrySender createTelemetrySender() {
    return mock(TelemetrySender.class);
  }

  static void addPolicy(Application app, Policy[] policies) throws Exception {
    String appId = app.getId();
    PolicyDAO policyDAO = new PolicyDAO();

    for (Policy policy : policies) {
      policy.setOwnerId(appId);
      policyDAO.insert(policy);
    }
  }

  static Policy createSvPolicy() {
    Policy policy = new Policy();
    policy.setName("NoSV" + (counter++));
    Constraint constraint = new Constraint();
    constraint.setName("NoSV");
    policy.setConstraints(Collections.singletonList(constraint));
    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint.addCondition(condition);
    return policy;
  }
}
