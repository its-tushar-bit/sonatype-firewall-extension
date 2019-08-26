/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlProvider;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SourceControlClientTest
    extends AbstractBrainServiceTest
{
  private static final String APP_ID = "SourceControlClientTest_AppId";

  private Application application;

  @Before
  public void createApplication() {
    application = tempEntity.newApplicationWithParent(APP_ID);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord() throws Exception {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    addOrgSourceControlForTest();
    int status = client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2");
    assertEquals(200, status);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_InvalidAppId() throws Exception {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    int status = client.addOrUpdateSourceControlRecord("abc-xyz", "https://github.com/org/proj2");
    assertEquals(404, status);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_InvalidUrl() throws Exception {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    addOrgSourceControlForTest();
    int status = client.addOrUpdateSourceControlRecord(APP_ID, "https://not good");
    assertEquals(400, status);
  }

  private void addOrgSourceControlForTest() throws Exception {
    ApiSourceControlAdapter apiSourceControlAdapter = new ApiSourceControlAdapter();
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(application.getId(), "https://github.com/org/proj", "token", SourceControlProvider.GITHUB));
    HttpResponse response = restRequest().path("api", "v2", "sourceControl", application.getId())
        .body(sourceControl).post();
    assertResponseStatus(200, response);
  }
}
