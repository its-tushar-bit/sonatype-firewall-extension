/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.v2.DefaultApiApplicationCategoryResource;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tag.TagService.fromDTO;
import static com.sonatype.insight.brain.tag.TagService.toDTO;

public class ApiApplicationCategoryResourceAuditTest
    extends AbstractAuditTest
{
  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testAddTag() throws Exception {
    ApiApplicationCategoryDTO dto = restRequest().body(toDTO(tag())).post().getBody(ApiApplicationCategoryDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION_CATEGORY, null);
    assertOrganizationData(auditDTO, organization);
    assertTagData(auditDTO, fromDTO(dto, dto.organizationId));
  }

  @Test
  public void testAddTag_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(toDTO(tag())).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION_CATEGORY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdateTag() throws Exception {
    ApiApplicationCategoryDTO dto =
        restRequest().body(toDTO(tag(saveTag().getId()))).put().getBody(ApiApplicationCategoryDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION_CATEGORY, null);
    assertOrganizationData(auditDTO, organization);
    assertTagData(auditDTO, fromDTO(dto, dto.organizationId));
  }

  @Test
  public void testUpdateTag_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(toDTO(tag(saveTag().getId()))).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION_CATEGORY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testDeleteTag() throws Exception {
    Tag tag = saveTag();

    restRequest().path(tag.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION_CATEGORY, null);
    assertOrganizationData(auditDTO, organization);
    assertTagData(auditDTO, tag);
  }

  @Test
  public void testDeleteTag_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).path(saveTag().getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION_CATEGORY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  private Tag tag() {
    return tag(null);
  }

  private Tag tag(String id) {
    Tag tag = new Tag(organization.getId(), "name1", "description1", Color.yellow);
    tag.setId(id);
    return tag;
  }

  private Tag saveTag() {
    return tempEntity.newTag(organization.getId(), "name2", "description2", Color.dark_blue);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest()
        .path(DefaultApiApplicationCategoryResource.RESOURCE_PATH,
            DefaultApiApplicationCategoryResource.ORGANIZATION_PATH)
        .parameter(organization.getId());
  }

  private void assertTagData(AuditDTO auditDTO, Tag tag) {
    assertCustomData(auditDTO, "applicationCategoryId", tag.getId());
    assertCustomData(auditDTO, "applicationCategoryName", tag.getName());
    assertCustomData(auditDTO, "applicationCategoryDescription", tag.getDescription());
    assertCustomData(auditDTO, "applicationCategoryColor", tag.getColor().toValue());
  }
}
