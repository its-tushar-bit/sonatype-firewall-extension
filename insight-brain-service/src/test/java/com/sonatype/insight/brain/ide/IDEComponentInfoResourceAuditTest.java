/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.convertToHdsUrl;
import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.newComponentDetails;

public class IDEComponentInfoResourceAuditTest
    extends AbstractAuditTest
{
  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private static final String COMPONENT_HASH = "hash";

  private Application application;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testGetComponentDetails_CoordinatesOnly() throws Exception {
    detailsRequest(application.getPublicId(), COMPONENT_IDENTIFIER, null).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomObject(auditDTO, "componentIdentifier", COMPONENT_IDENTIFIER);
    assertCustomObject(auditDTO, "componentHash", null);
  }

  @Test
  public void testGetComponentDetails_HashOnly() throws Exception {
    detailsRequest(application.getPublicId(), null, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
    assertCustomObject(auditDTO, "componentIdentifier", null);
  }

  @Test
  public void testGetComponentDetails_CoordinatesAndHash() throws Exception {
    detailsRequest(application.getPublicId(), COMPONENT_IDENTIFIER, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomObject(auditDTO, "componentIdentifier", COMPONENT_IDENTIFIER);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetComponentDetails_Unauthorized() throws Exception {
    detailsRequest(application.getPublicId(), COMPONENT_IDENTIFIER, null).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    HttpRequest detailsListRequest = detailsListRequest(application.getPublicId(), COMPONENT_IDENTIFIER);
    setupHdsResponseForComponent(detailsListRequest);

    detailsListRequest.get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomObject(auditDTO, "componentIdentifier", COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetComponentDetailsList_Unauthorized() throws Exception {
    detailsListRequest(application.getPublicId(), COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testGetComponentDetailsForAllVersions() throws Exception {
    HttpRequest detailsAllVersionsRequest = detailsAllVersionsRequest(application.getPublicId(), COMPONENT_IDENTIFIER);
    setupHdsResponseForComponent(detailsAllVersionsRequest);

    detailsAllVersionsRequest.get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomObject(auditDTO, "componentIdentifier", COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetComponentDetailsForAllVersions_Unauthorized() throws Exception {
    detailsAllVersionsRequest(application.getPublicId(), COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private void setupHdsResponseForComponent(final HttpRequest httpRequest) {
    ComponentDetails hdsComponentDetails = newComponentDetails(COMPONENT_IDENTIFIER);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));
    setHdsResponseForURI(convertToHdsUrl(httpRequest.getUrl()), hdsComponentDetailsList, 200);
  }

  private HttpRequest detailsAllVersionsRequest(String applicationId,
                                                ComponentIdentifier componentIdentifier)
  {
    return detailsRequest(applicationId, componentIdentifier, null).path("/allVersions");
  }

  private HttpRequest detailsListRequest(String applicationId,
                                         ComponentIdentifier componentIdentifier)
  {
    return detailsRequest(applicationId, componentIdentifier, null).path("/list");
  }

  private HttpRequest detailsRequest(String applicationId,
                                     ComponentIdentifier componentIdentifier,
                                     String hash)
  {
    return ideComponentInfoResourceRequest().parameter(applicationId)
        .query("componentIdentifier", componentIdentifier).query("hash", hash);
  }

  private HttpRequest ideComponentInfoResourceRequest() {
    return restRequest()
        .path(IDEComponentInfoResource.RESOURCE_PATH, IDEComponentInfoResource.APPLICATION_COMPONENT_DETAILS_PATH);
  }
}
