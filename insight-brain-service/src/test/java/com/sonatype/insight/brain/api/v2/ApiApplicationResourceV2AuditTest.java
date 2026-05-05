/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiApplicationResourceV2AuditTest
    extends AbstractAuditTest
{
  private ApplicationDAO applicationDAO;

  private Organization organization;

  private Organization targetOrganization;

  private Application application;

  @Before
  public void before() {
    applicationDAO = lookup(ApplicationDAO.class);

    tempEntity.newUser("appContactName");
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication("appName", "appPubId", organization.getId(), "appContactName");
    targetOrganization = tempEntity.newOrganization();
  }

  @Test
  public void testAddApplication_WithCategories() throws Exception {
    Tag tag1 = tempEntity.newTag(organization.getId(), "Tag1", Color.dark_red);
    Tag tag2 = tempEntity.newTag(organization.getId(), "Tag2", Color.dark_red);

    ApiApplicationDTO applicationDTO = applicationDTO(tag1, tag2);
    applicationRequest().body(applicationDTO).post();
    Application application = applicationDAO.getByName(applicationDTO.name);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null);
    assertDetailedApplicationData(auditDTO, application, applicationDTO.contactUserName);

    AuditDTO auditDTOCategories = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, null);
    assertApplicationData(auditDTOCategories, application);
    assertCustomObject(auditDTOCategories, "applicationCategories",
        ApplicationCategoryAuditDTO.transcribe(asList(tag1, tag2)));
  }

  @Test
  public void testAddApplication_EmptyCategories() throws Exception {
    ApiApplicationDTO applicationDTO = applicationDTO();
    applicationRequest().body(applicationDTO).post();
    Application application = applicationDAO.getByName(applicationDTO.name);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null);
    assertDetailedApplicationData(auditDTO, application, application.getContactInternalName());

    assertThat(awaitLogEntries(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, 0)).isEmpty();
  }

  @Test
  public void testAddApplication_Unauthorized() throws Exception {
    applicationRequest().with(unauthorizedUser()).body(applicationDTO()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, "unauthorized");
    assertParentOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdateApplication_WithCategories() throws Exception {
    Tag tag1 = tempEntity.newTag(organization.getId(), "Tag1", Color.dark_red);
    Tag tag2 = tempEntity.newTag(organization.getId(), "Tag2", Color.dark_blue);

    ApiApplicationDTO applicationDTO = applicationDTO("updated-name", "updated-public-id", tag1, tag2);
    applicationDTO.id = application.getId();
    applicationRequest().path(application.getId()).body(applicationDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION, null);
    assertDetailedApplicationData(auditDTO, applicationDTO);

    AuditDTO auditDTOCategories = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, null);
    assertApplicationData(auditDTOCategories, applicationDTO.id, applicationDTO.publicId, applicationDTO.name);
    assertCustomObject(auditDTOCategories, "applicationCategories",
        ApplicationCategoryAuditDTO.transcribe(asList(tag1, tag2)));
  }

  @Test
  public void testUpdateApplication_EmptyCategories() throws Exception {
    ApiApplicationDTO applicationDTO = applicationDTO("updated-name", "updated-public-id");
    applicationDTO.id = application.getId();
    applicationRequest().path(application.getId()).body(applicationDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION, null);
    assertDetailedApplicationData(auditDTO, applicationDTO);

    AuditDTO auditDTOCategories = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, null);
    assertApplicationData(auditDTOCategories, applicationDTO.id, applicationDTO.publicId, applicationDTO.name);
    assertCustomObject(auditDTOCategories, "applicationCategories", Collections.emptyList());
  }

  @Test
  public void testUpdateApplication_Unauthorized() throws Exception {
    ApiApplicationDTO applicationDTO = applicationDTO();
    applicationRequest().path(application.getId()).with(unauthorizedUser()).body(applicationDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private void assertDetailedApplicationData(final AuditDTO auditDTO, ApiApplicationDTO applicationDTO) {
    assertDetailedApplicationData(auditDTO, applicationDTO.id, applicationDTO.publicId, applicationDTO.name,
        applicationDTO.contactUserName);
  }

  @Test
  public void testDeleteApplication() throws Exception {
    applicationRequest().path(application.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION, null);
    assertDetailedApplicationData(auditDTO, application, application.getContactInternalName());
    assertParentOrganizationData(auditDTO, organization);
  }

  @Test
  public void testDeleteApplication_Unauthorized() throws Exception {
    applicationRequest().path(application.getId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private void assertDetailedApplicationData(
      final AuditDTO auditDTO,
      final Application application,
      final String contactInternalName)
  {
    assertDetailedApplicationData(auditDTO, application.getId(), application.getPublicId(), application.getName(),
        contactInternalName);
  }

  private void assertDetailedApplicationData(
      final AuditDTO auditDTO,
      final String id,
      final String publicId,
      final String name,
      final String contactInternalName)
  {
    assertApplicationData(auditDTO, id, publicId, name);
    assertCustomData(auditDTO, "contactUsername", contactInternalName);
    assertParentOrganizationData(auditDTO, organization);
  }

  private ApiApplicationDTO applicationDTO(final Tag... tags) {
    return applicationDTO("test-application-name", "public-app-id", tags);
  }

  private ApiApplicationDTO applicationDTO(final String name, final String applicationPublicId, final Tag... tags) {
    ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = name;
    applicationDTO.organizationId = organization.getId();
    User contactUser = tempEntity.newUser("aContact");
    applicationDTO.contactUserName = contactUser.getUsername();
    applicationDTO.applicationTags = Stream.of(tags).map(this::applicationTagDTO).collect(Collectors.toList());
    return applicationDTO;
  }

  private ApiApplicationTagDTO applicationTagDTO(final Tag tag) {
    ApiApplicationTagDTO tagDTO = new ApiApplicationTagDTO();
    tagDTO.tagId = tag.getId();
    return tagDTO;
  }

  private HttpRequest applicationRequest() {
    return restRequest().path(PublicApiPaths.APP_RESOURCE_PATH);
  }

  @Test
  public void testCloneApplication() throws Exception {
    ApiApplicationDTO applicationDTO = applicationRequest().path(ApiApplicationResourceV2.CLONE_PATH)
        .parameter(application.getId())
        .query("clonedApplicationName", "newAppName")
        .query("clonedApplicationPublicId", "newAppId")
        .post()
        .getBody(ApiApplicationDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null);
    assertDetailedApplicationData(auditDTO, applicationDTO);
    assertSourceApplicationData(auditDTO, application);
  }

  @Test
  public void testCloneApplication_Unauthorized() throws Exception {
    applicationRequest().path(ApiApplicationResourceV2.CLONE_PATH)
        .parameter(application.getId())
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, "unauthorized");
    assertParentOrganizationData(auditDTO, organization);
    assertSourceApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "applicationId", null);
  }

  private void assertSourceApplicationData(AuditDTO auditDTO, Application application) {
    assertCustomData(auditDTO, "sourceApplicationId", application.getId());
    assertCustomData(auditDTO, "sourceApplicationPublicId", application.getPublicId());
    assertCustomData(auditDTO, "sourceApplicationName", application.getName());
  }

  @Test
  public void testMoveApplication() throws Exception {
    moveRequest(application.getId(), targetOrganization.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MOVE_APPLICATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "contactUsername", application.getContactInternalName());
    assertParentOrganizationData(auditDTO, targetOrganization);
  }

  @Test
  public void testMoveApplication_UnauthorizedWrite() throws Exception {
    moveRequest(application.getId(), targetOrganization.getId()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MOVE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testMoveApplication_UnauthorizedAddApplication() throws Exception {
    tempEntity.newMembershipMapping(application.getId(), Role.OWNER_ROLE_ID, getUnauthorizedUsername());

    moveRequest(application.getId(), targetOrganization.getId()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MOVE_APPLICATION, "unauthorized");
    assertApplicationData(auditDTO, application);
    assertParentOrganizationData(auditDTO, targetOrganization);
  }

  private HttpRequest moveRequest(String applicationId, String targetOrganizationId) {
    return restRequest().path(PublicApiPaths.APP_RESOURCE_PATH)
        .path(ApiApplicationResourceV2.MOVE_PATH)
        .parameter(applicationId, targetOrganizationId);
  }
}
