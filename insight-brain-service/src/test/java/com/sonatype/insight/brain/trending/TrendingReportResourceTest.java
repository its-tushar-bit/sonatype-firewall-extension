/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.service.TestInsightBrainService;
import com.sonatype.insight.brain.trending.TrendingReportProcessor.ProgressMonitor;
import com.sonatype.insight.test.RestAccess;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TrendingReportResourceTest
    extends AbstractResourceAuthzTest
{
  private static enum Checkpoint
  {
    /**
     * Report (re)generation was requested but worker thread has not started yet, generating==false. Triggered
     * synchronously from client thread.
     */
    SCHEDULED,

    /**
     * Worker thread started report generation, generating==true
     */
    STARTED,

    /**
     * Worker thread started processing an application.
     */
    APPLICATION,

    /**
     * Worked thread finished report generation, generating==false
     */
    FINISHED;
  }

  private final Map<Checkpoint, CountDownLatch> checkpoints = new ConcurrentHashMap<Checkpoint, CountDownLatch>();
  private final Map<Checkpoint, AtomicInteger> executions = new ConcurrentHashMap<Checkpoint, AtomicInteger>();
  private final Map<Checkpoint, CountDownLatch> breakpoints = new ConcurrentHashMap<Checkpoint, CountDownLatch>();

  private void awaitCheckpoint(Checkpoint checkpoint) throws InterruptedException {
    Assert.assertTrue(checkpoint + " await", checkpoints.get(checkpoint).await(20, TimeUnit.SECONDS));
    // rearm checkpoint latch. this assumes tests are executed serially
    checkpoints.put(checkpoint, new CountDownLatch(1));
  }

  private void assertCheckpointExecutionCount(int expected, Checkpoint checkpint) {
    Assert.assertEquals(checkpint + " execution count", expected, executions.get(checkpint).get());
  }

  private void enableBreakpoint(Checkpoint checkpoint) {
    Assert.assertTrue(checkpoint + " unique breakpoint", breakpoints.put(checkpoint, new CountDownLatch(1)) == null);
  }

  private void releaseBreakpoint(Checkpoint checkpoint) {
    final CountDownLatch breakpoint = breakpoints.remove(checkpoint);
    Assert.assertNotNull(checkpoint + " breakpoint enabled", breakpoint);
    breakpoint.countDown();
  }

  private void enterCheckpoint(Checkpoint checkpoint) {
    executions.get(checkpoint).incrementAndGet();
    checkpoints.get(checkpoint).countDown();
    final CountDownLatch breakpoint = breakpoints.get(checkpoint);
    if (breakpoint != null) {
      try {
        breakpoint.await(20, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        throw new AssertionError(checkpoint + " enter", e);
      }
    }
  }

  @Before
  public void setupCheckpoints() {
    for (Checkpoint checkpoint : Checkpoint.values()) {
      checkpoints.put(checkpoint, new CountDownLatch(1));
      executions.put(checkpoint, new AtomicInteger());
    }
  }

  @Override
  protected void configureBrain(TestInsightBrainService brain) {
    super.configureBrain(brain);
    brain.addModule(new AbstractModule()
    {
      @Override
      protected void configure() {
        final Provider<TrendingReportProcessor> processorProvider = getProvider(TrendingReportProcessor.class);
        final Provider<TrendingReportCache> cacheProvider = getProvider(TrendingReportCache.class);
        bind(TrendingReportAsyncProcessor.class).toProvider(new Provider<TrendingReportAsyncProcessor>()
        {
          private TrendingReportAsyncProcessor asyncProcessor;

          @Override
          public TrendingReportAsyncProcessor get() {
            if (asyncProcessor == null) {
              asyncProcessor = new TrendingReportAsyncProcessor(cacheProvider.get(), processorProvider.get())
              {
                @Override
                public void calculate() {
                  enterCheckpoint(Checkpoint.SCHEDULED);
                  super.calculate();
                }

                @Override
                protected void beforeStart() {
                  super.beforeStart();
                  enterCheckpoint(Checkpoint.STARTED);
                }

                @Override
                protected void afterFinish() {
                  super.afterFinish();
                  enterCheckpoint(Checkpoint.FINISHED);
                }

                @Override
                protected ProgressMonitor newProgressMonitor() {
                  final ProgressMonitor monitor = super.newProgressMonitor();
                  return new ProgressMonitor()
                  {
                    @Override
                    public void tick(int total, int current) {
                      monitor.tick(total, current);
                      enterCheckpoint(Checkpoint.APPLICATION);
                    }
                  };
                }
              };
            }
            return asyncProcessor;
          }
        });
      }
    });
  }

  @Override
  public void startService() throws Exception {
    super.startService();
    brain.getInjector().getInstance(TrendingReportCache.class).getCacheFile().delete();
  }

  @Rule
  public TemporaryEntity temporaryEntity = new TemporaryEntity();

  @Test
  public void testBasic() throws Exception {
    Response response;

    // initial generation
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response); 
    awaitCheckpoint(Checkpoint.FINISHED); // waits generation complete, fails with timeout if generation didn't happen

    // from cache
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    assertCheckpointExecutionCount(1, Checkpoint.SCHEDULED);
    TrendingReport report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);

    // from cache again
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    assertCheckpointExecutionCount(1, Checkpoint.SCHEDULED);
    TrendingReport cached = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals("generatedOn", report.getMeta().getGeneratedOn(), cached.getMeta().getGeneratedOn());
  }

  @Test
  public void testRegenerate() throws Exception {

    Organization organization = temporaryEntity.newOrganization();
    temporaryEntity.newApplication("app1", "app1", organization.getId());
    temporaryEntity.newApplication("app2", "app2", organization.getId());
    temporaryEntity.newApplication("app3", "app3", organization.getId());

    Response response;
    TrendingReport report;

    // prime cached report data
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertNull("report.getMeta()", report.getMeta());
    awaitCheckpoint(Checkpoint.FINISHED); // waits generation complete, fails with timeout if generation didn't happen

    // get cached report, make sure regeneration is allowed
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    assertCheckpointExecutionCount(1, Checkpoint.SCHEDULED);
    report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals("canGenerate", true, report.getGeneration().isEnabled());

    // trigger report regeneration
    enableBreakpoint(Checkpoint.APPLICATION);
    response = AuthedRestAccess.get(getServiceURL() + "?force=true");
    assertResponseStatus(200, AuthedRestAccess.get(getServiceURL()));
    TrendingReport cached = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals("generation", true, cached.getGeneration().isRunning());

    // get cached report data while regeneration is still running
    awaitCheckpoint(Checkpoint.APPLICATION);
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, AuthedRestAccess.get(getServiceURL()));
    cached = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals("generation", true, cached.getGeneration().isRunning());
    Assert.assertEquals("generation total", 4, cached.getGeneration().getApplicationsTotal());
    Assert.assertEquals("generation current", 0, cached.getGeneration().getApplicationsCurrent());
    releaseBreakpoint(Checkpoint.APPLICATION);

    // get regenerated report data
    awaitCheckpoint(Checkpoint.FINISHED);
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, AuthedRestAccess.get(getServiceURL()));
    TrendingReport regenerated = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertNotEquals("generatedOn", cached.getMeta().getGeneratedOn(), regenerated.getMeta().getGeneratedOn());
    Assert.assertEquals("generating", false, regenerated.getGeneration().isRunning());
  }

  @Test
  public void testRegenerateNonAdminUser() throws Exception {
    Response response;
    TrendingReport report;

    // initial generation
    response = RestAccess.get(getServiceURL(), unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(200, response);
    report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    awaitCheckpoint(Checkpoint.FINISHED); // waits generation complete, fails with timeout if generation didn't happen

    // from cache
    response = RestAccess.get(getServiceURL(), unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(200, response);
    assertCheckpointExecutionCount(1, Checkpoint.SCHEDULED);
    report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals("canGenerate", false, report.getGeneration().isEnabled());

    // force regeneration forbidden
    response = RestAccess.get(getServiceURL() + "?force=true", unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);
    assertCheckpointExecutionCount(1, Checkpoint.SCHEDULED);
  }

  @Test
  public void testAnonymous() throws Exception {
    assertResponseStatus(401, RestAccess.get(getServiceURL()));
  }

  private String getServiceURL() {
    return getRestBaseUrl() + TrendingReportResource.SERVICE_PATH;
  }
}
