/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.File;
import java.nio.file.Files;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReleaseGraphResourceTest
    extends AbstractResourceTest
{
  private String appId;

  private String scanId;

  private File reportDir;

  private HttpRequest getRequest(String appId, String scanId) {
    return restRequest().path(ReleaseGraphResource.RESOURCE_PATH).parameter(appId, scanId);
  }

  private HttpRequest addCoords(HttpRequest request, ComponentIdentifier componentIdentifier) {
    return request.query("componentIdentifier", componentIdentifier);
  }

  private HttpRequest addCoords(HttpRequest request, String groupId, String artifactId, String version) {
    return request.query("groupId", groupId).query("artifactId", artifactId).query("version", version);
  }

  private void copyReport(String filename) throws Exception {
    reportDir.mkdirs();
    Files.copy(getClass().getResourceAsStream("/ReleaseGraphResourceTest/" + filename), new File(reportDir,
        "report.zip").toPath());
  }

  @Before
  public void init() throws Exception {
    appId = getClass().getSimpleName();
    String internalAppId = tempEntity.newApplicationWithParent(appId).getId();
    scanId = tempEntity.uuid();
    reportDir = getCLMServer().getReportDir(internalAppId, scanId);
  }

  @Test
  public void testGetImage_NeitherIdentifierNorGav() throws Exception {
    HttpResponse response = getRequest(appId, scanId).get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid component identifier");
  }

  @Test
  public void testGetImage_ByComponentIdentifier() throws Exception {
    copyReport("report.zip");
    HttpResponse response = addCoords(
        addCoords(getRequest(appId, scanId),
            ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar")), "ignored",
        "ignored", "ignored").get();
    assertResponseStatus(200, response);
    byte[] image = response.getBodyBytes();
    assertThat(image).isNotEmpty();
  }

  @Test
  public void testGetImage_ByGav() throws Exception {
    copyReport("report-legacy.zip");
    HttpResponse response = addCoords(getRequest(appId, scanId), "tomcat", "tomcat-util", "5.5.23").get();
    assertResponseStatus(200, response);
    byte[] image = response.getBodyBytes();
    assertThat(image).isNotEmpty();
  }
}
