/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.File;
import java.nio.file.Files;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class ReleaseGraphResourceTest
    extends AbstractResourceTest
{

  private String appId;

  private String scanId;

  private File reportDir;

  private String getUrl(String appId, String scanId, ComponentIdentifier componentIdentifier, String groupId,
      String artifactId, String version)
  {
    UriBuilder builder = UriBuilder.fromUri(getRestUrl(ReleaseGraphResource.SERVICE_PATH, appId, scanId));
    if (groupId != null) {
      builder.queryParam("groupId", groupId);
    }
    if (artifactId != null) {
      builder.queryParam("artifactId", artifactId);
    }
    if (version != null) {
      builder.queryParam("version", version);
    }
    if (componentIdentifier != null) {
      builder.queryParam("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier));
    }
    return builder.build().toString();
  }

  private void copyReport(String filename) throws Exception {
    reportDir.mkdirs();
    Files.copy(getClass().getResourceAsStream("/ReleaseGraphResourceTest/" + filename),
        new File(reportDir, "report.zip").toPath());
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
    Response response = AuthedRestAccess.get(getUrl(appId, scanId, null, null, null, null));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Invalid component identifier"));
  }

  @Test
  public void testGetImage_ByComponentIdentifier() throws Exception {
    copyReport("report.zip");
    Response response = AuthedRestAccess.get(getUrl(appId, scanId,
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar"), "ignored", "ignored",
        "ignored"));
    assertResponseStatus(200, response);
    byte[] image = response.getResponseBodyAsBytes();
    assertThat(image.length, is(greaterThan(0)));
  }

  @Test
  public void testGetImage_ByGav() throws Exception {
    copyReport("report-legacy.zip");
    Response response = AuthedRestAccess.get(getUrl(appId, scanId, null, "tomcat", "tomcat-util", "5.5.23"));
    assertResponseStatus(200, response);
    byte[] image = response.getResponseBodyAsBytes();
    assertThat(image.length, is(greaterThan(0)));
  }
}
