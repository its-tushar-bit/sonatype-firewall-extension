/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.AbstractMembershipMappingAuditTest;
import com.sonatype.insight.brain.tag.TagDTO;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

public class ApiApplicationResourceV2AuditTest
    extends AbstractMembershipMappingAuditTest
{
  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testSetMembershipMappingForRole() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO = apiRoleMemberMappingListDTO();

    setMembershipMappingRequest(application.getId(), apiRoleMemberMappingListDTO).put();

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP,
        apiRoleMemberMappingListDTO.memberMappings.size(), null);
    auditDTOs.forEach(auditDTO -> assertApplicationData(auditDTO, application));
    assertRoleMembershipData(auditDTOs, apiRoleMemberMappingListDTO);
  }

  @Test
  public void testSetMembershipMappingForRole_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    setMembershipMappingRequest(application.getId(), apiRoleMemberMappingListDTO()).with(unauthorizedUser()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testAddApplication_WithCategories() throws Exception {
    Tag tag1 = tempEntity.newTag(organization.getId(), "Tag1", Color.dark_red);
    Tag tag2 = tempEntity.newTag(organization.getId(), "Tag2", Color.dark_red);

    ApiApplicationDTO applicationDTO = applicationDTO(tag1, tag2);
    applicationRequest().body(applicationDTO).post();
    Application application = new ApplicationDAO().getByName(applicationDTO.name);
    tempEntity.register(application);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null);
    assertDetailedApplicationData(application, auditDTO, applicationDTO.contactUserName);

    AuditDTO auditDTOCategories = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CARTEGORY, null);
    assertApplicationData(auditDTOCategories, application);
    assertCustomObject(auditDTOCategories, "applicationCategories", TagDTO.transcribe(asList(tag1, tag2)));
  }

  @Test
  public void testAddApplication_EmptyCategories() throws Exception {
    ApiApplicationDTO applicationDTO = applicationDTO();
    applicationRequest().body(applicationDTO).post();
    Application application = new ApplicationDAO().getByName(applicationDTO.name);
    tempEntity.register(application);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, null);
    assertDetailedApplicationData(application, auditDTO, application.getContactInternalName());

    assertThat(awaitLogEntries(AuditEvent.CONFIGURE_APPLICATION_CARTEGORY, 0), empty());
  }

  @Test
  public void testAddApplication_Unauthorized() throws Exception {
    applicationRequest().with(unauthorizedUser()).body(applicationDTO()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION, "unauthorized");
    assertParentOrganizationData(auditDTO);
  }

  private void assertDetailedApplicationData(final Application application,
                                             final AuditDTO auditDTO,
                                             final String contactInternalName)
  {
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "contactUsername", contactInternalName);
    assertParentOrganizationData(auditDTO);
  }

  private ApiApplicationDTO applicationDTO(final Tag... tags) {
    ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = "public-app-id";
    applicationDTO.name = "test-application-name";
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

  private HttpRequest setMembershipMappingRequest(String applicationId,
                                                  ApiRoleMemberMappingListDTO apiRoleMemberMappingListDTO)
  {
    return applicationRequest().path(ApiApplicationResourceV2.ROLE_MEMBERS_PATH)
        .parameter(applicationId).body(apiRoleMemberMappingListDTO);
  }

  private void assertParentOrganizationData(final AuditDTO auditDTO) {
    assertCustomData(auditDTO, "parentOrganizationId", organization.getId());
    assertCustomData(auditDTO, "parentOrganizationName", organization.getName());
  }
}
