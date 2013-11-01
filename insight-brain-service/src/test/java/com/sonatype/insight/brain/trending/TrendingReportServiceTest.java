/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.TestInsightBrainService;
import com.sonatype.insight.test.RestAccess;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

public class TrendingReportServiceTest
    extends AbstractResourceAuthzTest
{
  private final AtomicInteger generationCount = new AtomicInteger();
  private volatile CountDownLatch cacheWriteLatch;

  @Override
  protected void configureBrain(TestInsightBrainService brain) {
    super.configureBrain(brain);
    brain.addModule(new AbstractModule()
    {
      @Override
      protected void configure() {
        final Provider<InsightWork> insightWorkProvider = getProvider(InsightWork.class);
        final Provider<TrendingReportProcessor> processorProvider = getProvider(TrendingReportProcessor.class);
        final Provider<TrendingReportCache> cacheProvider = new Provider<TrendingReportCache>()
        {
          private TrendingReportCache cache;

          @Override
          public TrendingReportCache get() {
            if (cache == null) {
              cache = new TrendingReportCache(insightWorkProvider.get())
              {
                @Override
                public void writeCache(TrendingReport report) throws IOException {
                  super.writeCache(report);
                  if (cacheWriteLatch != null) {
                    cacheWriteLatch.countDown();
                  }
                }
              };
            }
            return cache;
          }
        };
        bind(TrendingReportCache.class).toProvider(cacheProvider);
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
                  generationCount.incrementAndGet();
                  super.calculate();
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
    cacheWriteLatch = null;
    generationCount.set(0);
  }

  @Override
  public void stopService() throws Exception {
    cacheWriteLatch = null;
    super.stopService();
  }

  @Test
  public void testBasic() throws Exception {
    // initial generation
    cacheWriteLatch = new CountDownLatch(1);
    generationCount.set(0);
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(204, response); // no data
    Assert.assertEquals(1, generationCount.get());
    awaitForCacheWriteLatch();

    // from cache
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    Assert.assertEquals(1, generationCount.get());
    TrendingReport report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals(true, report.getMeta().getCanRegenerate());

    // from cache again
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    Assert.assertEquals(1, generationCount.get());
    TrendingReport cached = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals(report.getMeta().getGeneratedOn(), cached.getMeta().getGeneratedOn());

    Thread.sleep(100); // make sure generatedOn changes

    // force regeneration
    cacheWriteLatch = new CountDownLatch(1);
    response = AuthedRestAccess.get(getServiceURL() + "?force=true");
    assertResponseStatus(204, response);
    Assert.assertEquals(2, generationCount.get());
    awaitForCacheWriteLatch();

    // regenerated from cache
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    Assert.assertEquals(2, generationCount.get());
    TrendingReport regenerated = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertNotEquals(report.getMeta().getGeneratedOn(), regenerated.getMeta().getGeneratedOn());
  }

  @Test
  public void testRegenerateNonAdminUser() throws Exception {
    // initial generation
    cacheWriteLatch = new CountDownLatch(1);
    generationCount.set(0);
    Response response = RestAccess.get(getServiceURL(), unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(204, response); // no data
    Assert.assertEquals(1, generationCount.get());
    awaitForCacheWriteLatch();

    // from cache
    response = RestAccess.get(getServiceURL(), unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(200, response);
    Assert.assertEquals(1, generationCount.get());
    TrendingReport report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals(false, report.getMeta().getCanRegenerate());

    // force regeneration forbidden
    cacheWriteLatch = new CountDownLatch(1);
    response = RestAccess.get(getServiceURL() + "?force=true", unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);
    Assert.assertEquals(1, generationCount.get());
  }

  @Test
  public void testAnonymous() throws Exception {
    Response response = RestAccess.get(getServiceURL());
    assertResponseStatus(401, response);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + TrendingReportService.SERVICE_PATH;
  }

  private void awaitForCacheWriteLatch() throws InterruptedException {
    try {
      if (!cacheWriteLatch.await(20, TimeUnit.SECONDS)) {
        Assert.fail("Report was not generated");
      }
    }
    finally {
      cacheWriteLatch = null;
    }
  }

}
