/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.trending.TrendingReportService;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

public class TrendingReportServiceTest
    extends AbstractResourceTest
{

  @Test
  public void testBasic() throws Exception {
    TrendingReportService service = brain.getInjector().getInstance(TrendingReportService.class);

    service.purgeCache();

    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(204, response); // no data

    waitForReport(service);

    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    TrendingReport report = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);

    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    TrendingReport cached = JsonHelpers.fromJson(response.getResponseBody(), TrendingReport.class);
    Assert.assertEquals(report.getMeta().getGeneratedOn(), cached.getMeta().getGeneratedOn());
  }

  private String getServiceURL() {
    return getRestBaseUrl() + TrendingReportService.SERVICE_PATH;
  }

  private void waitForReport(TrendingReportService service) throws InterruptedException {
    for (int i = 0; i < 20; i++) {
      if (service.getCacheFile().canRead()) {
        return;
      }
      Thread.sleep(1000L);
    }
    Assert.fail("Report was not generated");
  }

}
