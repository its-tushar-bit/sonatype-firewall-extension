/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.api.PublicApiPaths.SOURCE_CONTROL_PATH_V2;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.UserMapping;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ApiSourceControlResourceAuditTest
    extends AbstractAuditTest
{
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  private Application app;

  private RoleDAO roleDAO;

  @Inject
  private ApiSourceControlAdapter apiSourceControlAdapter;

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Before
  public void setup() {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")
            .withStatus(HttpStatus.SC_OK)));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*/contributors"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("[{\"login\": \"myuser\"},{\"login\": \"othermyuser\"}," +
                "{\"login\": \"anothermyuser\"},{\"login\": \"unknownuser\"}]")));

    roleDAO = lookup(RoleDAO.class);
    automaticSourceControlConfigurationDAO = lookup(AutomaticSourceControlConfigurationDAO.class);
    apiSourceControlAdapter = lookup(ApiSourceControlAdapter.class);
    app = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null,
        "aLkXJ3Ku07gHpyWJ3BPFHxnt1ueuJCBtVq0VBVqLBr8=", SourceControlProvider.GITHUB);
  }

  @Test
  public void testAuditForCRUD() throws Exception {
    // CREATE
    String repositoryUrl = String.format("%s/organization/project", gitService.baseUrl());
    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setOwnerId(app.getId())
            .setRepositoryUrl(repositoryUrl)
            .setToken("token")
            .build());

    HttpResponse response =
        restRequest().path(SOURCE_CONTROL_PATH_V2)
            .path(OwnerType.APPLICATION.toString(), app.getId())
            .body(sourceControl)
            .post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "repositoryUrl", repositoryUrl);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertCustomData(auditDTO, "provider", result.provider);
    assertApplicationData(auditDTO, app);

    // UPDATE
    String updatedUrl = sourceControl.repositoryUrl + ".1";
    result.repositoryUrl = updatedUrl;
    response = restRequest().path(SOURCE_CONTROL_PATH_V2)
        .path(OwnerType.APPLICATION.toString(), app.getId())
        .body(result)
        .put();
    assertResponseStatus(200, response);

    auditDTO = assertAuditLog(AuditEvent.UPDATE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "repositoryUrl", updatedUrl);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertCustomData(auditDTO, "provider", result.provider);
    assertApplicationData(auditDTO, app);

    // DELETE
    response = restRequest().path(SOURCE_CONTROL_PATH_V2)
        .path(OwnerType.APPLICATION.toString(), app.getId())
        .delete();
    assertResponseStatus(204, response);

    auditDTO = assertAuditLog(AuditEvent.DELETE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "sourceControlId", result.id);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testAuditForAddOrUpdate() throws Exception {
    String repositoryUrl = String.format("%s/organization/project", gitService.baseUrl());
    // make sure automatic source control is on
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // CREATE
    HttpResponse response = restRequest().path(SOURCE_CONTROL_PATH_V2)
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", repositoryUrl)
        .post();
    assertResponseStatus(200, response);
    ApiSourceControlDTO result = response.getBody(ApiSourceControlDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.AUTO_CREATE_SOURCE_CONTROL, null);
    assertCustomData(auditDTO, "repositoryUrl", repositoryUrl);
    assertCustomData(auditDTO, "sourceControlId", result.id);

    // UPDATE
    String updatedUrl = repositoryUrl + ".1";
    result.repositoryUrl = updatedUrl;
    response = restRequest().path(SOURCE_CONTROL_PATH_V2)
        .query("publicId", app.getPublicId())
        .query("repositoryUrl", updatedUrl)
        .post();
    assertResponseStatus(200, response);

    // cannot change the repo url if one is already set, so no new audit entry should be produced
    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.AUTO_CREATE_SOURCE_CONTROL, 1, null);
    auditDTO = auditDTOs.get(0);
    assertCustomData(auditDTO, "repositoryUrl", repositoryUrl);
  }

  @Test
  public void testAuditForAddOrUpdateUserMappingByOrg() throws Exception {
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO(null, userMappings);

    HttpResponse response =
        restRequest().path(SOURCE_CONTROL_PATH_V2)
            .path("/automaticRoleAssignment/userMappings/")
            .path(app.getOrganizationId())
            .body(scmUserMappingsDTO)
            .post();
    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_USER_MAPPINGS, null);
    assertCustomData(auditDTO, "organizationId", app.getOrganizationId());
  }

  @Test
  public void testAuditForDeleteUserMapping() throws Exception {
    HttpResponse response =
        restRequest().path(SOURCE_CONTROL_PATH_V2)
            .path("/automaticRoleAssignment/userMappings/")
            .path(app.getOrganizationId())
            .delete();
    assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_MAPPINGS, null);
    assertCustomData(auditDTO, "organizationId", app.getOrganizationId());
  }

  /*
   * MembershipMappingService.grantRoleMembership method is only able to register on the audit event the
   * last user on the list this can be fixed in the optimize bulk insert ticket
   */
  @Test
  public void testAutomaticRoleAssignment() throws Exception {
    String repositoryUrl = String.format("%s/organization/project", gitService.baseUrl());
    tempEntity.newSourceControl(app.getId(), repositoryUrl);
    tempEntity.newUser("myuser");
    tempEntity.newUser("othermyuser");
    tempEntity.newUser("anothermyuser");

    HttpResponse response = roleAssignmentRestRequest()
        .parameter(app.getPublicId())
        .body(new SCMUserMappingsDTO(null, Lists.newArrayList(
            new UserMapping(FromMappingEnum.SCM_USERNAME, ToMappingEnum.IQ_USERNAME))))
        .post();

    assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, null);
    assertRoleMembershipData(auditDTO, DEVELOPER_ROLE_ID, Arrays.asList("myuser", "othermyuser", "anothermyuser"));
  }

  @Test
  public void testAutomaticRoleAssignment_Unauthorized() throws Exception {
    HttpResponse response = roleAssignmentRestRequest().with(unauthorizedUser()).parameter(app.getPublicId()).post();
    assertResponseStatus(403, response);

    assertAuditLog(AuditEvent.GRANT_ROLE_MEMBERSHIP, "unauthorized");
  }

  @Override
  protected HttpRequest restRequest() {
    return HttpRequest.to(getRestBaseUrl().replaceFirst("/$", ""));
  }

  protected HttpRequest roleAssignmentRestRequest() {
    return restRequest().path(PublicApiPaths.SOURCE_CONTROL_PATH_V2)
        .path(ApiSourceControlResource.AUTOMATIC_ROLE_ASSIGNMENT_PATH);
  }

  protected void assertRoleMembershipData(AuditDTO auditDTO, String roleId, List<String> members) {
    assertRoleData(auditDTO, roleId);
    assertThat(auditDTO.data).containsKey("roleMembers");
    assertThat(auditDTO.data.get("roleMembers")).isInstanceOf(List.class);
    List<String> roleMembers = ((List<LinkedHashMap<String, String>>) auditDTO.data.get("roleMembers"))
        .stream()
        .map(username -> username.get("username"))
        .collect(Collectors.toList());
    assertThat(roleMembers).containsExactlyInAnyOrderElementsOf(members);
  }

  private void assertRoleData(final AuditDTO auditDTO, final String roleId) {
    Role role = roleDAO.getByIdNotNull(roleId);
    assertCustomData(auditDTO, "roleId", role.getId());
    assertCustomData(auditDTO, "roleName", role.getName());
  }
}
