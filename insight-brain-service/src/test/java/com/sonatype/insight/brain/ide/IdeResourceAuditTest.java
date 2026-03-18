/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class IdeResourceAuditTest
    extends AbstractAuditTest
{
  private Application app;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testDoScan() throws Exception {
    restRequest("simple").get();

    assertAuditLog(null);
  }

  @Test
  public void testPostScan() throws Exception {
    restRequest("enhanced").post();

    assertAuditLog(null);
  }

  @Test
  public void testDoScan_Unauthorized() throws Exception {
    restRequest("simple").with(unauthorizedUser()).get();

    assertAuditLog("unauthorized");
  }

  @Test
  public void testPostScan_Unauthorized() throws Exception {
    restRequest("enhanced").with(unauthorizedUser()).post();

    assertAuditLog("unauthorized");
  }

  @Test
  public void testDoCoordinatesScan() throws Exception {
    restCoordinatesRequest().get();

    assertAuditLog(null);
  }

  @Test
  public void testDoCoordinatesScan_Unauthorized() throws Exception {
    restCoordinatesRequest().with(unauthorizedUser()).get();

    assertAuditLog("unauthorized");
  }

  private void assertAuditLog(String error) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_PROJECT, error);
    assertApplicationData(auditDTO, app);
  }

  private HttpRequest restRequest(String scanType) {
    String hash = "abababababababababab";
    HttpRequest request = restRequest().path(IdeResource.RESOURCE_PATH).path("scan", scanType, app.getPublicId(), hash);

    String hdsUrl = "rest/ide/scan/" + scanType + "/" + hash;
    hdsRespondWithResource("/IdeResourceAuditTest/SimpleMatch_abababababababababab.json").atUri(hdsUrl);

    return request;
  }

  private HttpRequest restCoordinatesRequest() {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "jar");
    HttpRequest request = restRequest().path(IdeResource.RESOURCE_PATH)
        .path(IdeResource.COORDINATES_SCAN_PATH)
        .parameter(app.getPublicId())
        .query("componentIdentifier", identifier);

    String hdsUrl = request.getUrl().replaceFirst("(.*/)(rest/ide/scan/coordinates)(/[^?]+)(.*)", "$2$4");
    hdsRespondWithResource("/IdeResourceAuditTest/Coordinates.json").atUri(hdsUrl);

    return request;
  }
}
