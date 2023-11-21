/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.time.Instant;
import java.time.Period;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.http.client.HttpResponseException;
import org.assertj.core.util.Files;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

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
    assertThat(client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2",
        Files.currentFolder().getPath())).isEqualTo(200);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_getRecentCommittersNonEmptyMap() throws Exception {
    Map<String, Collection<Instant>> testCommittersMap = new HashMap<>();
    Collection<Instant> instantCollection =
        Collections.unmodifiableList(Arrays.asList(Instant.now(), Instant.now().minus(Period.ofDays(5))));
    testCommittersMap.put("testEmail@test.com", instantCollection);
    SourceControlClient client = spy(new SourceControlClient(getCLMServer().getClientConfiguration()));
    addOrgSourceControlForTest();
    doReturn(testCommittersMap).when(client).getRecentCommitters(null, 90);
    doReturn(testCommittersMap).when(client).getRecentCommitters(Files.currentFolder().getPath(), 90);

    assertThat(client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2")).isEqualTo(200);
    assertThat(client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2",
        Files.currentFolder().getPath())).isEqualTo(200);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_getRecentCommittersEmptyMap() throws Exception {
    SourceControlClient client = spy(new SourceControlClient(getCLMServer().getClientConfiguration()));
    addOrgSourceControlForTest();
    doReturn(Collections.emptyMap()).when(client).getRecentCommitters(null, 90);
    doReturn(Collections.emptyMap()).when(client).getRecentCommitters(Files.currentFolder().getPath(), 90);

    assertThat(client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2")).isEqualTo(200);
    assertThat(client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2",
        Files.currentFolder().getPath())).isEqualTo(200);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_getRecentCommittersGitException() throws Exception {
    SourceControlClient client = spy(new SourceControlClient(getCLMServer().getClientConfiguration()));
    addOrgSourceControlForTest();
    doThrow(new GitException("Cannot get map of recent committers: ")).when(client).getRecentCommitters(null,
        90);
    doThrow(new GitException("Cannot get map of recent committers: ")).when(client)
        .getRecentCommitters(Files.currentFolder().getPath(),
            90);

    assertThat(client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2")).isEqualTo(200);
    assertThat(client.addOrUpdateSourceControlRecord(APP_ID, "https://github.com/org/proj2",
        Files.currentFolder().getPath())).isEqualTo(200);
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_InvalidAppId() {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());

    assertThatExceptionOfType(HttpResponseException.class)
        .isThrownBy(() -> client.addOrUpdateSourceControlRecord("abc-xyz", "https://github.com/org/proj2"))
        .withMessageStartingWith("Cannot find application with public ID: 'abc-xyz'")
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));

    assertThatExceptionOfType(HttpResponseException.class)
        .isThrownBy(() -> client.addOrUpdateSourceControlRecord("abc-xyz", "https://github.com/org/proj2",
            Files.currentFolder().getPath()))
        .withMessageStartingWith("Cannot find application with public ID: 'abc-xyz'")
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_AddWithInvalidUrl() {
    turnOnAutomaticSourceControl();
    Application newApp = tempEntity.newApplicationWithParent("testUpdateSourceControl_AddWithInvalidUrl");

    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    assertThatExceptionOfType(HttpResponseException.class)
        .isThrownBy(() -> client.addOrUpdateSourceControlRecord(newApp.getPublicId(), "https://not good"))
        .withMessage(
            "SourceControl repositoryUrl is invalid: Illegal character in authority at index 8: https://not good")
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(400));

    assertThatExceptionOfType(HttpResponseException.class)
        .isThrownBy(() -> client.addOrUpdateSourceControlRecord(newApp.getPublicId(), "https://not good",
            Files.currentFolder().getPath()))
        .withMessage(
            "SourceControl repositoryUrl is invalid: Illegal character in authority at index 8: https://not good")
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(400));
  }

  @Test
  public void testAddOrUpdateSourceControlRecord_UpdateWithInvalidUrl() throws Exception {
    SourceControlClient client = new SourceControlClient(getCLMServer().getClientConfiguration());
    addOrgSourceControlForTest();

    // expect : update will be ignored since repo URL is already set
    client.addOrUpdateSourceControlRecord(APP_ID, "https://not good");
    client.addOrUpdateSourceControlRecord(APP_ID, "https://not good", Files.currentFolder().getPath());
  }

  private void turnOnAutomaticSourceControl() {
    AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO =
        new AutomaticSourceControlConfigurationDAO();
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
  }

  private void addOrgSourceControlForTest() throws Exception {
    turnOnAutomaticSourceControl();

    ApiSourceControlDTO sourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(application.getId()).setRepositoryUrl("https://github.com/org/proj")
            .setToken("token").build());
    HttpResponse response =
        restRequest().path("api", "v2", "sourceControl", OwnerType.APPLICATION.toString(), application.getId())
            .body(sourceControl).post();
    assertResponseStatus(200, response);
  }
}
