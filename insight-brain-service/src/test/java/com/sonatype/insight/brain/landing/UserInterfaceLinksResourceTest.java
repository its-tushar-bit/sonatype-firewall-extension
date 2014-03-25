/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UserInterfaceLinksResourceTest
    extends AbstractResourceTest
{
  private void assertRedirect(Response response, String expected) throws Exception {
    assertResponseStatus(307, response);
    assertThat(response.getHeader("Location"), is(getRestBaseUrl() + expected));
  }

  private Response get(String path, Object... params) throws Exception {
    return RestAccess.get(getRestUrl(UserInterfaceLinksResource.SERVICE_PATH + '/' + path, params));
  }

  @Test
  public void testLinkToManagement_App() throws Exception {
    Response response = get(UserInterfaceLinksResource.MANAGEMENT_PATH, "application", "test id");
    assertRedirect(response, "assets/index.html#/management/application/test%20id/policies");
  }

  @Test
  public void testLinkToManagement_Org() throws Exception {
    Response response = get(UserInterfaceLinksResource.MANAGEMENT_PATH, "organization", "test id");
    assertRedirect(response, "assets/index.html#/management/organization/test%20id/policies");
  }

  @Test
  public void testLinkToReport() throws Exception {
    assertThat(UserInterfaceLinksResource.getReportUrl("app id", "scan id"), is(UserInterfaceLinksResource.SERVICE_PATH
        + "/application/app%20id/report/scan%20id"));
    Response response = get(UserInterfaceLinksResource.REPORT_PATH, "app id", "scan id");
    assertRedirect(response, "assets/index.html#/reports/app%20id/scan%20id");
  }

  @Test
  public void testLinkToPdf() throws Exception {
    assertThat(UserInterfaceLinksResource.getPdfUrl("app id", "scan id"), is(UserInterfaceLinksResource.SERVICE_PATH
        + "/application/app%20id/report/scan%20id/pdf"));
    Response response = get(UserInterfaceLinksResource.PDF_PATH, "app id", "scan id");
    assertRedirect(response, "rest/report/app%20id/scan%20id/printReport");
  }
}
