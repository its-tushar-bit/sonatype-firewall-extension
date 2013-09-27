/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.InsightWork;

public class SaasIdeResourcePerformance
{
  // private static final int CONNECTIONS_PER_CLIENT = 4;
  //
  // private static final int CLIENTS = 1;

  public static void main(String... args) throws Exception {
    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);

    Policy[] policies = new Policy[1];
    for (int i = 0; i < policies.length; i++) {
      policies[i] = SaasIdeResourcePerformanceUtils.createSvPolicy();
    }

    List<Long> results = new SaasIdeResourcePerformance(32, 32, "http://localhost:8080/insight-portal", policies)
        .execute("c8e086158a709a128ff5");

    long sum = 0l;
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

  private ExecutorService pool;

  private IdeResource resource;

  private InsightWork work;

  private Application testApplication;

  private final int iterations;

  private SaasIdeResourcePerformance(int connections, int iterations, String server, Policy... policies)
      throws Exception
  {

    pool = Executors.newFixedThreadPool(connections);

    this.iterations = iterations * connections;
    work = SaasIdeResourcePerformanceUtils.createInsightWork();
    resource = new IdeResource(work, null, SaasIdeResourcePerformanceUtils.createSaasClient(server));

    // trigger db
    testApplication = new Application();
    testApplication.setPublicId("bom1-12345678");
    testApplication.setName("perf-test");
    new ApplicationDAO().insert(testApplication);

    SaasIdeResourcePerformanceUtils.addPolicy(testApplication, policies, work);
  }

  List<Long> execute(String hash) throws Exception {
    List<ClientRunnable> callables = new ArrayList<ClientRunnable>();
    HttpServletRequest request = SaasIdeResourcePerformanceUtils.createRequest();
    new ClientRunnable(resource, testApplication.getPublicIdLowercase(), hash, request).call();
    try {
      for (int i = 0; i < iterations; i++) {
        callables.add(new ClientRunnable(resource, testApplication.getPublicIdLowercase(), hash, request));
      }

      List<Future<Long>> resultFutures = pool.invokeAll(callables, 10, TimeUnit.MINUTES);
      List<Long> results = new ArrayList<Long>(resultFutures.size());
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

    private HttpServletRequest request;

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