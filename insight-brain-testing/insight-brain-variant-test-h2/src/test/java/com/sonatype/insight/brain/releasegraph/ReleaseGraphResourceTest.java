/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in {@code ReleaseGraphResource}'s own package (rather than {@code com.sonatype.insight.brain.variant}) and
 * named exactly like the legacy {@code ReleaseGraphResourceTest} because its mock report fixtures are resolved via
 * {@code getClass().getSimpleName()} (see the {@code /ReleaseGraphResourceTest/report} and
 * {@code /ReleaseGraphResourceTest/report-legacy} classpath resources).
 */
@IqH2Test
class ReleaseGraphResourceTest
{
  private IqTestContext ctx;

  private Application app;

  private String scanId;

  private HttpRequest getRequest(String appId, String scanId) {
    return ctx.restRequest().path(ReleaseGraphResource.RESOURCE_PATH).parameter(appId, scanId);
  }

  private HttpRequest addCoords(HttpRequest request, ComponentIdentifier componentIdentifier) {
    return request.query("componentIdentifier", componentIdentifier);
  }

  private HttpRequest addCoords(HttpRequest request, String groupId, String artifactId, String version) {
    return request.query("groupId", groupId).query("artifactId", artifactId).query("version", version);
  }

  private void copyReport(String sourceReportDir) throws Exception {
    ctx.createReportFile(app.getId(), scanId, "/" + getClass().getSimpleName() + "/" + sourceReportDir);
  }

  @BeforeEach
  void init() {
    app = ctx.tempEntity().newApplicationWithParent();
    scanId = TemporaryEntity.uuid();
  }

  @Test
  void testGetImage_NeitherIdentifierNorGav() throws Exception {
    HttpResponse response = getRequest(app.getPublicId(), scanId).get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid component identifier");
  }

  @Test
  void testGetImage_ByComponentIdentifier() throws Exception {
    copyReport("report");
    HttpResponse response = addCoords(
        addCoords(getRequest(app.getPublicId(), scanId),
            ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar")),
        "ignored",
        "ignored", "ignored").get();
    ctx.assertResponseStatus(200, response);
    byte[] image = response.getBodyBytes();
    assertThat(image).isNotEmpty();
  }

  @Test
  void testGetImage_ByGav() throws Exception {
    copyReport("report-legacy");
    HttpResponse response = addCoords(getRequest(app.getPublicId(), scanId), "tomcat", "tomcat-util", "5.5.23").get();
    ctx.assertResponseStatus(200, response);
    byte[] image = response.getBodyBytes();
    assertThat(image).isNotEmpty();
  }
}
