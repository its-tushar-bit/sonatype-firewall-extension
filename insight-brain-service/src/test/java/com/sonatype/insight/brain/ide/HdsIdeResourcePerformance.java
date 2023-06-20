/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;

import static org.mockito.Mockito.mock;

public class HdsIdeResourcePerformance
{
  // private static final int CONNECTIONS_PER_CLIENT = 4;
  //
  // private static final int CLIENTS = 1;

  public static void main(String... args) throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);

    Policy[] policies = new Policy[1];
    for (int i = 0; i < policies.length; i++) {
      policies[i] = HdsIdeResourcePerformanceUtils.createSvPolicy();
    }

    List<Long> results = new HdsIdeResourcePerformance(32, 32, "http://localhost:8080/insight-portal", policies)
        .execute("c8e086158a709a128ff5");

    long sum = 0L;
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;

    for (Long value : results) {
      sum += value;
      max = Math.max(max, value);
      min = Math.min(min, value);
      System.out.print("," + value);
    }
    System.out.println("\nMin: " + min);
    System.out.println("Max: " + max);
    System.out.println("Avg: " + sum / results.size());
    System.out.println("Sum: " + sum);
  }

  private final ExecutorService pool;

  private final IdeResource resource;

  private final Application testApplication;

  private final int iterations;

  private HdsIdeResourcePerformance(int connections, int iterations, String server, Policy... policies)
      throws Exception
  {

    pool = Executors.newFixedThreadPool(connections);

    this.iterations = iterations * connections;
    resource = new IdeResource(null, HdsIdeResourcePerformanceUtils.createHdsClient(server),
        new ComponentPolicyEvaluator(), HdsIdeResourcePerformanceUtils.createTelemetrySender(),
        new UserIdePolicyEvaluationDAO(), new CurrentUser(), mock(Configuration.class));

    // trigger db
    testApplication = new Application();
    testApplication.setPublicId("bom1-12345678");
    testApplication.setName("perf-test");
    new ApplicationDAO().insert(testApplication);

    HdsIdeResourcePerformanceUtils.addPolicy(testApplication, policies);
  }

  List<Long> execute(String hash) throws Exception {
    List<ClientRunnable> callables = new ArrayList<>();
    HttpServletRequest request = HdsIdeResourcePerformanceUtils.createRequest();
    new ClientRunnable(resource, testApplication.getPublicIdLowercase(), hash, request).call();
    try {
      for (int i = 0; i < iterations; i++) {
        callables.add(new ClientRunnable(resource, testApplication.getPublicIdLowercase(), hash, request));
      }

      List<Future<Long>> resultFutures = pool.invokeAll(callables, 10, TimeUnit.MINUTES);
      List<Long> results = new ArrayList<>(resultFutures.size());
      for (Future<Long> result : resultFutures) {
        results.add(result.get());
      }
      return results;
    }
    finally {
      pool.shutdownNow();
    }
  }

  private static class ClientRunnable
      implements Callable<Long>
  {
    private final String applicationId;

    private final String hash;

    private final IdeResource resource;

    private final HttpServletRequest request;

    ClientRunnable(IdeResource resource, String applicationId, String hash, HttpServletRequest request) {
      this.applicationId = applicationId;
      this.resource = resource;
      this.request = request;
      this.hash = hash;
    }

    @Override
    public Long call() throws Exception {
      long start = System.currentTimeMillis();
      resource.doScan("simple", applicationId, hash, false, request);
      return System.currentTimeMillis() - start;
    }
  }
}
