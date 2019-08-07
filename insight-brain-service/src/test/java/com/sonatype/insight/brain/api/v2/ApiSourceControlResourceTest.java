/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlProvider;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApiSourceControlResourceTest
    extends AbstractResourceTest
{
  public static final String VALID_URL = "https://example/com/organization/project";

  private static final ApiSourceControlAdapter apiSourceControlAdapter = new ApiSourceControlAdapter();
  
  private Application app;

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SOURCE_CONTROL_PATH_V2).auth();
  }

  @Test
  public void testGetSourceControl() throws Exception {
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    HttpResponse response = restRequest().path(app.getId()).get();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
  }

  @Test
  public void testAddSourceControl() throws Exception {
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB));
    HttpResponse response = restRequest().path(app.getId()).body(sourceControl).post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.applicationId).isEqualTo(app.getId());
    assertThat(result.ownerId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.repositoryUrl);
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  public void testUpdateSourceControl() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    String updatedUrl = sourceControl.getRepositoryUrl() + ".1";
    sourceControl.setRepositoryUrl(updatedUrl);
    sourceControl.setProvider(SourceControlProvider.GITLAB);
    HttpResponse response = restRequest().path(app.getId())
        .body(apiSourceControlAdapter.convertToDTO(sourceControl)).put();
    assertResponseStatus(200, response);

    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.repositoryUrl).isEqualTo(updatedUrl);
    assertThat(result.provider).isEqualTo(SourceControlProvider.GITLAB);
  }

  @Test
  public void testAddSourceControl_MissingSourceControlProvider() throws Exception {
    SourceControl sourceControl = new SourceControl(app.getId(), VALID_URL, "token", null);
    HttpResponse response = restRequest().path(app.getId()).body(sourceControl).post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.applicationId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
    assertThat(result.provider).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  public void testUpdateSourceControl_MissingSourceControlProvider() throws Exception {
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITLAB);
    String updatedUrl = sourceControl.getRepositoryUrl() + ".1";
    sourceControl.setRepositoryUrl(updatedUrl);
    sourceControl.setProvider(null);
    HttpResponse response = restRequest().path(app.getId())
        .body(apiSourceControlAdapter.convertToDTO(sourceControl)).put();
    assertResponseStatus(200, response);

    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.repositoryUrl).isEqualTo(updatedUrl);
    assertThat(result.provider).isEqualTo(SourceControlProvider.GITHUB);
  }

  @Test
  public void testAddSourceControl_InvalidSourceControlProvider() throws Exception {
    SourceControl sourceControl = new SourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = objectMapper.writeValueAsString(sourceControl);
    ObjectNode node = (ObjectNode) new ObjectMapper().readTree(body);
    node.put("provider", "invalid_scm");
    body = node.toString();

    HttpResponse response = restRequest().path(app.getId()).body(body).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo(
            "SourceControl provider value 'invalid_scm' is invalid, valid options are: github, gitlab");
  }

  @Test
  public void testUpdateSourceControl_InvalidSourceControlProvider() throws Exception {
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    sourceControl.setProvider(null);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = objectMapper.writeValueAsString(sourceControl);
    ObjectNode node = (ObjectNode) new ObjectMapper().readTree(body);
    node.put("provider", "invalid_scm");
    body = node.toString();

    HttpResponse response = restRequest().path(app.getId()).body(body).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo(
            "SourceControl provider value 'invalid_scm' is invalid, valid options are: github, gitlab");
  }

  @Test
  public void testDeleteSourceControl() throws Exception {
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    HttpResponse response = restRequest().path(app.getId()).path(sourceControl.getId()).delete();
    assertResponseStatus(204, response);
  }
}
