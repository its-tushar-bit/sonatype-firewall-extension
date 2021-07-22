/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMoveApplicationResponseDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiApplicationTagAdapter;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationMoveService;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.telemetry.RestEndpointTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetryContainerRequestFilter;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiApplicationResourceV2Test
    extends AbstractResourceTest
{
  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private ApiApplicationAdapter apiApplicationAdapter;

  private final RoleDAO roleDAO = new RoleDAO();

  private Organization organization;

  private Application app;

  private User userA;

  private User userB;

  private Tag tagA;

  private Tag tagB;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  private HttpRequest roleMembersRequest(String applicationId) {
    return restRequest().subpath(DefaultApiApplicationResourceV2.ROLE_MEMBERS_PATH).parameter(applicationId);
  }

  @Before
  public void setUp() throws Exception {
    apiApplicationAdapter = getCLMServer().getInstance(ApiApplicationAdapter.class);

    organization = tempEntity.newOrganization("test-org");
    app = tempEntity.newApplication("test-app", "test-app", organization.getId());
    userA = tempEntity.newUser("user-a", "John", "Doe", "void@void.com");
    userB = tempEntity.newUser("user-b", "Jane", "Doe", "void@void.com");
    tagA = tempEntity.newTag(organization.getId(), "TagA", Color.dark_red);
    tagB = tempEntity.newTag(organization.getId(), "TagB", Color.dark_red);
  }

  @Test
  public void testCRUD() throws Exception {
    ApiApplicationDTO applicationDTO = createApplicationDTO(null);

    // Test the post
    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the get
    response = restRequest().path(applicationResult.id).get();
    assertResponseStatus(200, response);
    applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the update
    applicationDTO = applicationResult;
    applicationDTO.contactUserName = userB.getUsername();
    ApiApplicationTagDTO applicationTagBDTO = new ApiApplicationTagDTO();
    applicationTagBDTO.tagId = tagB.getId();
    applicationDTO.applicationTags.clear();
    applicationDTO.applicationTags.add(applicationTagBDTO);
    response = restRequest().path(applicationResult.id).body(applicationDTO).put();
    assertResponseStatus(200, response);
    applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the delete
    response = restRequest().path(applicationResult.id).delete();
    assertResponseStatus(204, response);

    final Application application = applicationDAO.getById(applicationResult.id);
    assertThat(application).isNull();
  }

  @Test
  public void testGetApplications() throws Exception {
    int numApps = 2;
    tempEntity.newApplications(organization.getId(), numApps);

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    ApiApplicationListDTO applicationListDTO = response.getBody(ApiApplicationListDTO.class);
    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.applications).hasSize(numApps + 1);
  }

  @Test
  public void testGetApplications_ByPublicId() throws Exception {
    int numApps = 2;
    List<Application> applications = tempEntity.newApplications(organization.getId(), numApps);
    Map<String, List<ApplicationTag>> appTagMap = new HashMap<>();
    for (Application application : applications) {
      List<ApplicationTag> applicationTags = new ArrayList<>();
      applicationTags.add(tempEntity.newApplicationTag(application.getId(), tagA.getId()));
      appTagMap.put(application.getId(), applicationTags);
    }

    HttpResponse response = restRequest().query("publicId", applications.get(0).getPublicId(),
        applications.get(1).getPublicId()).get();
    assertResponseStatus(200, response);
    ApiApplicationListDTO applicationListDTO = response.getBody(ApiApplicationListDTO.class);
    assertThat(applicationListDTO).isNotNull();
    List<ApiApplicationDTO> expectedApplications = new ArrayList<>(numApps);
    for (Application application : applications) {
      ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
      apiApplicationDTO.applicationTags = ApiApplicationTagAdapter.convertToDTO(appTagMap.get(apiApplicationDTO.id));
      expectedApplications.add(apiApplicationDTO);
    }
    assertApplications(applicationListDTO.applications, expectedApplications);
  }

  @Test
  public void testUpdateApplication_MismatchedIds() throws Exception {
    ApiApplicationDTO applicationDTO = createApplicationDTO("Junk");
    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(400, response);
    String errorMessage = response.getBodyText();
    assertThat(errorMessage).isEqualTo("The applicationId=" + app.getId() + " provided in the url did not match the id="
        + applicationDTO.id + " provided in the json.");
  }

  @Test
  public void testUpdateApplication_NullId() throws Exception {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    applicationDTO.id = null;
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  public void testUpdateApplication_EmptyId() throws Exception {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    applicationDTO.id = "  ";
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  public void testUpdateApplication_ChangeOrganizationId() throws Exception {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    applicationDTO.id = null;
    Organization anotherOrg = tempEntity.newOrganization("Another Org");
    applicationDTO.organizationId = anotherOrg.getId();
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(400, response);
    String errorMessage = response.getBodyText();
    assertThat(errorMessage).isEqualTo("Cannot change the parent organization of an application.");
  }

  @Test
  public void testUpdateApplication_ChangePublicId() throws Exception {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    applicationDTO.id = null;
    applicationDTO.publicId = "NewPublicId";
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  public void testDeleteNonExistentApplication() throws Exception {
    final String appId = "invalidAppId";
    final HttpResponse response = restRequest().path(appId).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Could not find an application with ID " + appId + ".");
  }

  @Test
  public void testGetNotExistentApplication() throws Exception {
    final HttpResponse response = restRequest().path("invalidId").get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Could not find an application with ID invalidId.");
  }

  @Test
  public void testAddApplicationConflictingPublicId() throws Exception {
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = app.getPublicId();
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = app.getOrganizationId();

    final HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(app.getPublicId() + " is already used as an ID.");
  }

  @Test
  public void testAddApplicationNonNullIdFails() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.id = "BadIdAsWeAreCallingAddApplication";
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = organization.getId();
    applicationDTO.contactUserName = userA.getUsername();

    // Test the post
    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Application must not have an ID set on creation.");
  }

  @Test
  public void testAddApplicationInvalidContact() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";
    final String contactUserName = "testContact";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = organization.getId();
    applicationDTO.contactUserName = contactUserName;

    // Test the post
    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Application has a contactUserName=" + contactUserName
        + " that does not exist.");
  }

  @Test
  public void testAddApplicationExceedsLicense() throws Exception {
    final int appLimit = 1;
    setApplicationLimit(appLimit);

    // Test Add Application, which should fail with 402 since we exceeded the limit
    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.name = "testAddApplication_exceedsLicense_id_new_name";
    applicationDTO.publicId = "testAddApplication_exceedsLicense_id_new_id";
    applicationDTO.organizationId = organization.getId();

    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(402, response);
    assertThat(response.getBodyText())
        .isEqualTo("You have exceeded the licensed limit of " + appLimit + " applications.");
  }

  @Test
  public void testAddApplicationInvalidOrg() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";
    final String orgId = "invalidOrgId";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = orgId;

    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Application references an organization (ID=" + orgId
        + ") that does not exist.");
  }

  @Test
  public void testAddApplicationNullOrg() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = null;

    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Application must have a parent organization.");
  }

  @Test
  public void testLdapAppRoles() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/ApiApplicationResourceV2Test/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Initial state
    HttpRequest request = roleMembersRequest(app.getId());
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    final List<Role> appRoles = roleDAO.getApplicationRoles();

    ApiRoleMemberMappingListDTO roleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings).isNotNull();
    assertThat(roleMemberMappings.memberMappings).hasSameSizeAs(appRoles);

    // Create
    final ApiRoleMemberMappingListDTO roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, "testuser"),
            newMember(MemberType.GROUP, "Alpha")), appRoles.get(0).getId());

    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for created data
    response = request.get();
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);

    assertThat(returnedRoleMemberMappings).isNotNull();
    final List<ApiRoleMemberMappingDTO> returnedRoleMemberMappingList = returnedRoleMemberMappings.memberMappings;
    assertThat(returnedRoleMemberMappingList).isNotNull();
    assertThat(returnedRoleMemberMappingList).hasSameSizeAs(appRoles);

    for (final ApiRoleMemberMappingDTO roleMember : returnedRoleMemberMappingList) {
      if (roleMember.roleId.equals(appRoles.get(0).getId())) {
        assertThat(roleMember.members).hasSize(3);
        final Map<String, MemberType> memberMap = new HashMap<>();
        for (final ApiMemberDTO member : roleMember.members) {
          memberMap.put(member.userOrGroupName, member.type);
        }
        MemberType type = memberMap.get("Alpha");
        assertThat(type).isEqualTo(MemberType.GROUP);
        type = memberMap.get("testuser");
        assertThat(type).isEqualTo(MemberType.USER);
        type = memberMap.get(User.ADMIN_USERNAME);
        assertThat(type).isEqualTo(MemberType.USER);
      }
      else {
        assertThat(roleMember.members).isEmpty();
      }
    }
  }

  @Test
  public void testCRUDAppRoles() throws Exception {
    // Initial state
    HttpRequest request = roleMembersRequest(app.getId());
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    final List<Role> appRoles = roleDAO.getApplicationRoles();

    ApiRoleMemberMappingListDTO roleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings).isNotNull();
    assertThat(roleMemberMappings.memberMappings).hasSameSizeAs(appRoles);

    // Create
    ApiRoleMemberMappingListDTO roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userB.getUsername())), appRoles.get(0).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for created data
    response = request.get();
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);

    assertThat(returnedRoleMemberMappings).isNotNull();
    List<ApiRoleMemberMappingDTO> returnedRoleMemberMappingList = returnedRoleMemberMappings.memberMappings;
    assertThat(returnedRoleMemberMappingList).hasSameSizeAs(appRoles);

    ApiRoleMemberMappingDTO returnedRoleMemberMapping = null;
    for (final ApiRoleMemberMappingDTO roleMemberMapping : returnedRoleMemberMappingList) {
      if (appRoles.get(0).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMapping = roleMemberMapping;
        break;
      }
    }
    assertApiRoleMemberMappingDTO(returnedRoleMemberMapping, appRoles.get(0).getId(), userB, MemberType.USER);

    // Update
    roleMemberMappingListDTO = newMemberMapping(newMemberList(newMember(MemberType.USER, userA.getUsername())),
        appRoles.get(0).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    roleMemberMappingListDTO = newMemberMapping(newMemberList(newMember(MemberType.USER, userB.getUsername())),
        appRoles.get(1).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for updated data
    response = request.get();
    assertResponseStatus(200, response);
    returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(returnedRoleMemberMappings).isNotNull();
    returnedRoleMemberMappingList = returnedRoleMemberMappings.memberMappings;
    assertThat(returnedRoleMemberMappingList).hasSameSizeAs(appRoles);

    ApiRoleMemberMappingDTO[] returnedRoleMemberMappingArray = new ApiRoleMemberMappingDTO[2];
    for (final ApiRoleMemberMappingDTO roleMemberMapping : returnedRoleMemberMappingList) {
      if (appRoles.get(0).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMappingArray[0] = roleMemberMapping;
      }
      else if (appRoles.get(1).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMappingArray[1] = roleMemberMapping;
      }
    }
    assertThat(returnedRoleMemberMappingArray).hasSize(2);
    assertApiRoleMemberMappingDTO(returnedRoleMemberMappingArray[0], appRoles.get(0).getId(), userA, MemberType.USER);
    assertApiRoleMemberMappingDTO(returnedRoleMemberMappingArray[1], appRoles.get(1).getId(), userB, MemberType.USER);
  }

  @Test
  public void testGetApplicationRoles() throws Exception {
    HttpResponse response = restRequest().path(DefaultApiApplicationResourceV2.ROLE_PATH).get();
    assertResponseStatus(200, response);

    ApiRoleListDTO appRoles = response.getBody(ApiRoleListDTO.class);
    assertThat(appRoles).isNotNull();
    assertThat(appRoles.roles).hasSize(5).extracting(dto -> dto.name).containsExactlyInAnyOrder("Owner", "Developer",
        "Application Evaluator", "Component Evaluator", "Legal Reviewer");
  }

  @Test
  public void testCloneApplication() throws Exception {
    TelemetryContainerRequestFilter.REST_ENDPOINT_INVOCATIONS.clear();

    String clonedAppName = "Cloned App Name";
    String clonedAppPublicId = "ClonedAppPublicId";

    HttpResponse response = restRequest().path(DefaultApiApplicationResourceV2.CLONE_PATH).parameter(app.getId())
        .query("clonedApplicationName", clonedAppName).query("clonedApplicationPublicId", clonedAppPublicId).post();

    assertResponseStatus(200, response);
    ApiApplicationDTO returnedDTO = response.getBody(ApiApplicationDTO.class);

    Application clonedApp = applicationDAO.getByPublicId(clonedAppPublicId);
    assertThat(returnedDTO.id).isEqualTo(clonedApp.getId());
    assertThat(returnedDTO.name).isEqualTo(clonedAppName);
    assertThat(returnedDTO.publicId).isEqualTo(clonedAppPublicId);
    assertThat(returnedDTO.organizationId).isEqualTo(app.getOrganizationId());
    assertThat(returnedDTO.contactUserName).isEqualTo(app.getContactInternalName());
    assertThat(returnedDTO.applicationTags).isEmpty();

    String expectedTelemetryPath = "/" + PublicApiPaths.APP_RESOURCE_PATH + "/" +
        DefaultApiApplicationResourceV2.CLONE_PATH;
    TelemetryContainerRequestFilter telemetryContainerRequestFilter =
        getCLMServer().getInstance(TelemetryContainerRequestFilter.class);
    List<TelemetryData> telemetryData = telemetryContainerRequestFilter.collectAllData();
    assertThat(telemetryData).hasSize(1);
    Map<String, Object> telemetryAttributes = telemetryData.get(0).getAttributes();
    assertThat(telemetryAttributes).hasSize(1);
    RestEndpointTelemetry restEndpointTelemetry =
        (RestEndpointTelemetry) telemetryAttributes.get(TelemetryContainerRequestFilter.REST_ENDPOINT_TELEMETRY);
    assertThat(restEndpointTelemetry.method).isEqualTo("POST");
    assertThat(restEndpointTelemetry.path).isEqualTo(expectedTelemetryPath);
    assertThat(restEndpointTelemetry.invocations).isEqualTo(1);
  }

  @Test
  public void testMoveApplication() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent("test-app-id");

    HttpResponse response = restRequest().path(DefaultApiApplicationResourceV2.MOVE_PATH)
        .parameter(app.getId(), org.getId()).post();
    assertResponseStatus(200, response);
    List<String> warnings = response.getBody(ApiMoveApplicationResponseDTOV2.class).warnings;
    assertThat(warnings).isEmpty();
    assertThat(new ApplicationDAO().getById(app.getId()).getOrganizationId()).isEqualTo(org.getId());
  }

  @Test
  public void testMoveApplication_UnsatisfiedPreconditions() throws Exception {
    Organization org1 = tempEntity.newOrganization("New Parent");
    Organization org2 = tempEntity.newOrganization("Old Parent");
    Application app = tempEntity.newApplication("My App", "test-app-id", org2.getId());
    tempEntity.newPolicy(app.getOrganizationId(), "Missing Policy");

    HttpResponse response = restRequest().path(DefaultApiApplicationResourceV2.MOVE_PATH)
        .parameter(app.getId(), org1.getId()).post();
    assertResponseStatus(409, response);
    ApiMoveApplicationResponseDTOV2 issues = response.getBody(ApiMoveApplicationResponseDTOV2.class);
    assertThat(issues.errors)
        .containsExactly(String.format(ApplicationMoveService.POLICY_MISSING_MSG, "Missing Policy", org2.getName()));
    assertThat(new ApplicationDAO().getById(app.getId()).getOrganizationId()).isEqualTo(app.getOrganizationId());
  }

  @Test
  public void testGetApplicationsByOrganizationId() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplication(app1.getOrganizationId());
    tempEntity.newApplicationWithParent();

    HttpResponse response =
        restRequest().path(DefaultApiApplicationResourceV2.ORGANIZATION_PATH).parameter(app1.getOrganizationId()).get();

    assertResponseStatus(200, response);
    ApiApplicationListDTO apiApplicationListDTO = response.getBody(ApiApplicationListDTO.class);
    assertThat(apiApplicationListDTO).isNotNull();
    assertThat(apiApplicationListDTO.applications).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(apiApplicationAdapter.convertToDTO(app1), apiApplicationAdapter.convertToDTO(app2));
  }

  private ApiRoleMemberMappingListDTO newMemberMapping(final List<ApiMemberDTO> memberList, final String roleId) {
    final ApiRoleMemberMappingDTO memberMappingDTO = new ApiRoleMemberMappingDTO();
    memberMappingDTO.members = memberList;
    memberMappingDTO.roleId = roleId;

    ApiRoleMemberMappingListDTO memberMappingListDTO = new ApiRoleMemberMappingListDTO();
    memberMappingListDTO.memberMappings = new ArrayList<>();
    memberMappingListDTO.memberMappings.add(memberMappingDTO);
    return memberMappingListDTO;
  }

  private ApiMemberDTO newMember(final MemberType type, final String name) {
    return new ApiMemberDTO(null /* ownerId */, null /* ownerType */, name, type);
  }

  private List<ApiMemberDTO> newMemberList(final ApiMemberDTO... members) {
    return Arrays.asList(members);
  }

  private void assertApplication(final ApiApplicationDTO returnedDTO, final ApiApplicationDTO sendDTO) {
    assertThat(returnedDTO.publicId).isEqualTo(sendDTO.publicId);
    assertThat(returnedDTO.name).isEqualTo(sendDTO.name);
    assertThat(returnedDTO.organizationId).isEqualTo(sendDTO.organizationId);
    assertThat(returnedDTO.contactUserName).isEqualTo(sendDTO.contactUserName);

    if (sendDTO.applicationTags == null) {
      assertThat(returnedDTO.applicationTags).isNull();
    }
    else {
      assertThat(returnedDTO.applicationTags).hasSameSizeAs(sendDTO.applicationTags);
      assertThat(returnedDTO.applicationTags).hasSize(1);
      assertThat(returnedDTO.applicationTags.get(0).tagId).isEqualTo(sendDTO.applicationTags.get(0).tagId);
      assertThat(returnedDTO.applicationTags.get(0).applicationId).isEqualTo(returnedDTO.id);
    }
  }

  private void assertApiRoleMemberMappingDTO(final ApiRoleMemberMappingDTO apiRoleMemberMappingDTO,
                                             final String roleId,
                                             final User user,
                                             final MemberType type)
  {
    assertThat(apiRoleMemberMappingDTO).isNotNull();
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(roleId);
    assertThat(apiRoleMemberMappingDTO.members).hasSize(1);
    assertThat(apiRoleMemberMappingDTO.members.get(0).type).isEqualTo(type);
    assertThat(apiRoleMemberMappingDTO.members.get(0).userOrGroupName).isEqualTo(user.getUsername());
  }

  private ApiApplicationDTO createApplicationDTO(String applicationId) {
    ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.id = applicationId;
    applicationDTO.publicId = "testID";
    applicationDTO.name = "test-application-name";
    applicationDTO.organizationId = organization.getId();
    applicationDTO.contactUserName = userA.getUsername();
    addApplicationTagDTOs(applicationDTO);
    return applicationDTO;
  }

  private void addApplicationTagDTOs(ApiApplicationDTO applicationDTO) {
    applicationDTO.applicationTags = new ArrayList<>();
    ApiApplicationTagDTO applicationTagADTO = new ApiApplicationTagDTO();
    applicationTagADTO.tagId = tagA.getId();
    applicationDTO.applicationTags.add(applicationTagADTO);
  }

  private void assertApplications(List<ApiApplicationDTO> actualApplications,
                                  List<ApiApplicationDTO> expectedApplications)
  {
    assertThat(actualApplications).hasSameSizeAs(expectedApplications);

    Comparator<ApiApplicationDTO> appComparator = Comparator.comparing(dto -> dto.id);
    actualApplications.sort(appComparator);
    expectedApplications.sort(appComparator);

    for (int i = 0; i < actualApplications.size(); i++) {
      ApiApplicationDTO actualApplication = actualApplications.get(i);
      ApiApplicationDTO expectedApplication = expectedApplications.get(i);
      assertThat(actualApplication.id).isEqualTo(expectedApplication.id);
      assertThat(actualApplication.name).isEqualTo(expectedApplication.name);
      assertThat(actualApplication.organizationId).isEqualTo(expectedApplication.organizationId);
      assertThat(actualApplication.publicId).isEqualTo(expectedApplication.publicId);
      assertThat(actualApplication.contactUserName).isEqualTo(expectedApplication.contactUserName);

      assertTags(actualApplication.applicationTags, expectedApplication.applicationTags);
    }
  }

  private void assertTags(List<ApiApplicationTagDTO> actualTags, List<ApiApplicationTagDTO> expectedTags) {
    if (actualTags == null) {
      assertThat(expectedTags).isNull();
      return;
    }

    Comparator<ApiApplicationTagDTO> tagComparator = Comparator.<ApiApplicationTagDTO, String> comparing(dto -> dto.id)
        .thenComparing(dto -> dto.applicationId).thenComparing(dto -> dto.tagId);
    assertThat(actualTags).usingElementComparator(tagComparator).containsExactlyInAnyOrderElementsOf(expectedTags);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.APP_RESOURCE_PATH);
  }
}
