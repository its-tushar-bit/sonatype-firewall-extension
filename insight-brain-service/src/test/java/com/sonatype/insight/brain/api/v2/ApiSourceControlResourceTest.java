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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApiSourceControlResourceTest
    extends AbstractResourceTest
{
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
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), "https://example/com", "token");
    HttpResponse response = restRequest().path(app.getId()).get();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);
    assertThat(result.id).isEqualTo(sourceControl.getId());
  }

  @Test
  public void testAdd() throws Exception {
    SourceControl sourceControl = new SourceControl(app.getId(), "https://example.com", "token");
    HttpResponse response = restRequest().path(app.getId()).body(sourceControl).post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    assertThat(result.id).isNotNull();
    assertThat(result.applicationId).isEqualTo(app.getId());
    assertThat(result.repositoryUrl).isEqualTo(sourceControl.getRepositoryUrl());
    assertThat(result.token).isEqualTo(SourceControl.FAKE_SECRET_KEY);
  }

  @Test
  public void testUpdate() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), "https://example/com", "token");
    String updatedUrl = sourceControl.getRepositoryUrl() + ".1";
    sourceControl.setRepositoryUrl(updatedUrl);
    HttpResponse response = restRequest().path(app.getId()).body(sourceControl).put();
    assertResponseStatus(200, response);

    SourceControl result = response.getBody(SourceControl.class);
    assertThat(result.getRepositoryUrl()).isEqualTo(updatedUrl);
  }

  @Test
  public void testDelete() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), "https://example.com", "token");
    HttpResponse response = restRequest().path(app.getId()).path(sourceControl.getId()).delete();
    assertResponseStatus(204, response);
  }
}
