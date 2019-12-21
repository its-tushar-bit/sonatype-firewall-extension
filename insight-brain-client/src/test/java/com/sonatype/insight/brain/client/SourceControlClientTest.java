/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
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

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SourceControlClientTest
    extends AbstractBrainServiceTest
{
  private static final String APP_ID = "SourceControlClientTest_AppId";

  private Application application;

  @Before
  public void createApplication() {

    application = tempEntity.newApplicationWithParent(APP_ID);
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord() throws Exception {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    addOrgSourceControlForTest();
    assertThat(client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2")).isEqualTo(200);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_InvalidAppId() throws Exception {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());

    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.addOrUpdateSourceControlRecord("abc-xyz", "https://github.com/org/proj2");
    }).withMessageStartingWith("Could not find an application with public ID")
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_AddWithInvalidUrl() throws Exception {
    turnOnAutomaticSourceControl();
    Application newApp = tempEntity.newApplicationWithParent("testAddOrUpdateSourceControlRecord_AddWithInvalidUrl");

    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> {
      client.addOrUpdateSourceControlRecord(newApp.getPublicId(), "https://not good");
    }).withMessage(
        "SourceControl repositoryUrl is invalid: Illegal character in authority at index 8: https://not good")
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(400));
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_UpdateWithInvalidUrl() throws Exception {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    addOrgSourceControlForTest();

    // expect : update will be ignored since repo URL is already set
    client.addOrUpdateSourceControlRecord(APP_ID, "https://not good");
  }

  private void turnOnAutomaticSourceControl() {
    AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO =
        new AutomaticSourceControlConfigurationDAO();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
  }

  private void addOrgSourceControlForTest() throws Exception {
    turnOnAutomaticSourceControl();

    ApiSourceControlAdapter apiSourceControlAdapter = new ApiSourceControlAdapter();
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(application.getId()).setRepositoryUrl("https://github.com/org/proj")
            .setToken("token").build());
    HttpResponse response =
        restRequest().path("api", "v2", "sourceControl", OwnerType.APPLICATION.toString(), application.getId())
            .body(sourceControl).post();
    assertResponseStatus(200, response);
  }
}
