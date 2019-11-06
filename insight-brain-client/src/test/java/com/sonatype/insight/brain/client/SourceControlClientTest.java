/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
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

    try {
      client.addOrUpdateSourceControlRecord("abc-xyz", "https://github.com/org/proj2");
      fail("Call should have failed due to invalid App ID");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode()).isEqualTo(404);
      assertThat(e.getMessage()).startsWith("Could not find an application with public ID");
    }
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_InvalidUrl() throws Exception {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    addOrgSourceControlForTest();

    try {
      client.addOrUpdateSourceControlRecord(APP_ID, "https://not good");
      fail("Call should have failed due to invalid URL");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode()).isEqualTo(400);
      assertThat(e.getMessage()).isEqualTo(
          "SourceControl repositoryUrl is invalid: Illegal character in authority at index 8: https://not good");
    }
  }

  private void addOrgSourceControlForTest() throws Exception {
    // make sure automatic scm is on
    AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO =
        new AutomaticSourceControlConfigurationDAO();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    ApiSourceControlAdapter apiSourceControlAdapter = new ApiSourceControlAdapter();
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(application.getId()).setRepositoryUrl("https://github.com/org/proj")
            .setToken("token").setProvider(SourceControlProvider.GITHUB).build());
    HttpResponse response =
        restRequest().path("api", "v2", "sourceControl", OwnerType.APPLICATION.toString(), application.getId())
        .body(sourceControl).post();
    assertResponseStatus(200, response);
  }
}
