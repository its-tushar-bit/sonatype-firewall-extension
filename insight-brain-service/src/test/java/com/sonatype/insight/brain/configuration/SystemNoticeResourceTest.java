/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SystemNoticeResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SystemNoticeResource.RESOURCE_PATH);
  }

  @Test
  public void testGetSystemNotice_asAnonymous() throws Exception {
    HttpRequest request = restRequest();
    HttpResponse response = request.path(SystemNoticeResource.FETCH_PATH).anon().get();
    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  @Test
  public void testUpdateSystemNotice() throws Exception {
    HttpRequest request = restRequest();
    HttpResponse response = request.body(new SystemNotice()).put();
    assertResponseStatus(HttpStatus.SC_OK, response);
  }

  @Test
  public void testGetSystemNotice_Unlicensed() throws Exception {
    uninstallLicense();
    HttpRequest request = restRequest();
    HttpResponse response = request.path(SystemNoticeResource.FETCH_PATH).get();
    assertResponseStatus(HttpStatus.SC_OK, response);
  }
}
