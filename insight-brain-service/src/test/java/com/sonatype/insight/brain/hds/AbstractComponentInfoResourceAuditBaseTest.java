/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;

import org.junit.jupiter.api.Test;

public abstract class AbstractComponentInfoResourceAuditBaseTest
    extends AbstractComponentInfoResourceAuditTest
{
  protected abstract HttpRequest resourceRequest();

  @Test
  public void testGetComponentDetails_CoordinatesOnly() throws Exception {
    detailsRequest(application.getPublicId(), COMPONENT_IDENTIFIER, null).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER, null);
  }

  @Test
  public void testGetComponentDetails_HashOnly() throws Exception {
    detailsRequest(application.getPublicId(), null, COMPONENT_HASH).get();

    assertAuditComponentInfo(application, null, COMPONENT_HASH);
  }

  @Test
  public void testGetComponentDetails_CoordinatesAndHash() throws Exception {
    detailsRequest(application.getPublicId(), COMPONENT_IDENTIFIER, COMPONENT_HASH).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER, COMPONENT_HASH);
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

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
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

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetComponentDetailsForAllVersions_Unauthorized() throws Exception {
    detailsAllVersionsRequest(application.getPublicId(), COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private HttpRequest detailsAllVersionsRequest(String applicationId, ComponentIdentifier componentIdentifier) {
    return detailsRequest(applicationId, componentIdentifier, null).path("/allVersions");
  }

  private HttpRequest detailsListRequest(String applicationId, ComponentIdentifier componentIdentifier) {
    return detailsRequest(applicationId, componentIdentifier, null).path("/list");
  }

  private HttpRequest detailsRequest(String applicationId, ComponentIdentifier componentIdentifier, String hash) {
    return resourceRequest().parameter(applicationId)
        .query("componentIdentifier", componentIdentifier)
        .query("hash", hash);
  }
}
