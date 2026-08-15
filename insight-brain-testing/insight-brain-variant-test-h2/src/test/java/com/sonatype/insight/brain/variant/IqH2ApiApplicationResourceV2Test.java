/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.ApiApplicationResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoriesListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMoveApplicationResponseDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiApplicationTagAdapter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationMoveService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.telemetry.RestEndpointTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetryContainerRequestFilter;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiApplicationResourceV2Test
{
  private IqTestContext ctx;

  private ApplicationDAO applicationDAO;

  private Organization organization;

  private Application app;

  private User userA;

  private User userB;

  private Tag tagA;

  private Tag tagB;

  @BeforeEach
  void setUp() {
    applicationDAO = ctx.lookup(ApplicationDAO.class);

    organization = ctx.tempEntity().newOrganization("test-org");
    app = ctx.tempEntity().newApplication("test-app", "test-app", organization.getId());
    userA = ctx.tempEntity().newUser("user-a", "John", "Doe", "void@void.com");
    userB = ctx.tempEntity().newUser("user-b", "Jane", "Doe", "void@void.com");
    tagA = ctx.tempEntity().newTag(organization.getId(), "TagA", Color.dark_red);
    tagB = ctx.tempEntity().newTag(organization.getId(), "TagB", Color.dark_red);
  }

  @Test
  void testCRUD() throws Exception {
    ApiApplicationDTO applicationDTO = createApplicationDTO(null);

    // Test the post
    HttpResponse response = restRequest().body(applicationDTO).post();
    ctx.assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the get
    response = restRequest().path(applicationResult.id).get();
    ctx.assertResponseStatus(200, response);
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
    ctx.assertResponseStatus(200, response);
    applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the delete
    response = restRequest().path(applicationResult.id).delete();
    ctx.assertResponseStatus(204, response);

    final Application application = applicationDAO.getById(applicationResult.id);
    assertThat(application).isNull();
  }

  @Test
  void testGetApplications() throws Exception {
    int numApps = 2;
    ctx.tempEntity().newApplications(organization.getId(), numApps);

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    ApiApplicationListDTO applicationListDTO = response.getBody(ApiApplicationListDTO.class);
    assertThat(applicationListDTO).isNotNull();
    assertThat(applicationListDTO.applications).hasSize(numApps + 1);
  }

  @Test
  void testGetApplications_ByPublicId() throws Exception {
    int numApps = 2;
    List<Application> applications = ctx.tempEntity().newApplications(organization.getId(), numApps);
    Map<String, List<ApplicationTag>> appTagMap = new HashMap<>();
    for (Application application : applications) {
      List<ApplicationTag> applicationTags = new ArrayList<>();
      applicationTags.add(ctx.tempEntity().newApplicationTag(application.getId(), tagA.getId()));
      appTagMap.put(application.getId(), applicationTags);
    }

    HttpResponse response = restRequest().query("publicId", applications.get(0).getPublicId(),
        applications.get(1).getPublicId()).get();
    ctx.assertResponseStatus(200, response);
    ApiApplicationListDTO applicationListDTO = response.getBody(ApiApplicationListDTO.class);
    assertThat(applicationListDTO).isNotNull();
    List<ApiApplicationDTO> expectedApplications = new ArrayList<>(numApps);
    for (Application application : applications) {
      ApiApplicationDTO apiApplicationDTO =
          ApiApplicationAdapter.convertToDTO(application, appTagMap.get(application.getId()));
      apiApplicationDTO.applicationTags = ApiApplicationTagAdapter.convertToDTO(appTagMap.get(apiApplicationDTO.id));
      expectedApplications.add(apiApplicationDTO);
    }
    assertApplications(applicationListDTO.applications, expectedApplications);
  }

  @Test
  void testUpdateApplication_MismatchedIds() throws Exception {
    ApiApplicationDTO applicationDTO = createApplicationDTO("Junk");
    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    ctx.assertResponseStatus(400, response);
    String errorMessage = response.getBodyText();
    assertThat(errorMessage).isEqualTo("The applicationId=" + app.getId() + " provided in the url did not match the id="
        + applicationDTO.id + " provided in the json.");
  }

  @Test
  void testUpdateApplication_NullId() throws Exception {
    ApiApplicationDTO applicationDTO = ApiApplicationAdapter.convertToDTO(app, Collections.emptyList());
    applicationDTO.id = null;
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    ctx.assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  void testUpdateApplication_EmptyId() throws Exception {
    ApiApplicationDTO applicationDTO = ApiApplicationAdapter.convertToDTO(app, Collections.emptyList());
    applicationDTO.id = "  ";
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    ctx.assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  void testUpdateApplication_ChangeOrganizationId() throws Exception {
    ApiApplicationDTO applicationDTO = ApiApplicationAdapter.convertToDTO(app, Collections.emptyList());
    applicationDTO.id = null;
    Organization anotherOrg = ctx.tempEntity().newOrganization("Another Org");
    applicationDTO.organizationId = anotherOrg.getId();
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    ctx.assertResponseStatus(400, response);
    String errorMessage = response.getBodyText();
    assertThat(errorMessage).isEqualTo("Cannot change the parent organization of an application.");
  }

  @Test
  void testUpdateApplication_ChangePublicId() throws Exception {
    ApiApplicationDTO applicationDTO = ApiApplicationAdapter.convertToDTO(app, Collections.emptyList());
    applicationDTO.id = null;
    applicationDTO.publicId = "NewPublicId";
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    ctx.assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  void testDeleteNonExistentApplication() throws Exception {
    final String appId = "invalidAppId";
    final HttpResponse response = restRequest().path(appId).delete();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Application with ID " + appId + " does not exist.");
  }

  @Test
  void testGetNotExistentApplication() throws Exception {
    final HttpResponse response = restRequest().path("invalidId").get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Application with ID invalidId does not exist.");
  }

  @Test
  void testAddApplicationConflictingPublicId() throws Exception {
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = app.getPublicId();
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = app.getOrganizationId();

    final HttpResponse response = restRequest().body(applicationDTO).post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(app.getPublicId() + " is already used as an ID.");
  }

  @Test
  void testAddApplicationNonNullIdFails() throws Exception {
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
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Application must not have an ID set on creation.");
  }

  @Test
  void testAddApplicationInvalidContact() throws Exception {
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
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Application has a contactUserName=" + contactUserName
        + " that does not exist.");
  }

  @Test
  void testAddApplicationExceedsLicense() throws Exception {
    final int appLimit = 1;
    setApplicationLimit(appLimit);

    // Test Add Application, which should fail with 402 since we exceeded the limit
    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.name = "testAddApplication_exceedsLicense_id_new_name";
    applicationDTO.publicId = "testAddApplication_exceedsLicense_id_new_id";
    applicationDTO.organizationId = organization.getId();

    HttpResponse response = restRequest().body(applicationDTO).post();
    ctx.assertResponseStatus(402, response);
    assertThat(response.getBodyText())
        .isEqualTo("You have exceeded the licensed limit of " + appLimit + " applications.");
  }

  @Test
  void testAddApplicationInvalidOrg() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";
    final String orgId = "invalidOrgId";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = orgId;

    HttpResponse response = restRequest().body(applicationDTO).post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Application references an organization (ID=" + orgId
        + ") that does not exist.");
  }

  @Test
  void testAddApplicationNullOrg() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = null;

    HttpResponse response = restRequest().body(applicationDTO).post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Application must have a parent organization.");
  }

  @Test
  void testCloneApplication() throws Exception {
    TelemetryContainerRequestFilter.REST_ENDPOINT_INVOCATIONS.get().clear();

    String clonedAppName = "Cloned App Name";
    String clonedAppPublicId = "ClonedAppPublicId";

    HttpResponse response = restRequest().path(ApiApplicationResourceV2.CLONE_PATH)
        .parameter(app.getId())
        .query("clonedApplicationName", clonedAppName)
        .query("clonedApplicationPublicId", clonedAppPublicId)
        .post();

    ctx.assertResponseStatus(200, response);
    ApiApplicationDTO returnedDTO = response.getBody(ApiApplicationDTO.class);

    Application clonedApp = applicationDAO.getByPublicId(clonedAppPublicId);
    assertThat(returnedDTO.id).isEqualTo(clonedApp.getId());
    assertThat(returnedDTO.name).isEqualTo(clonedAppName);
    assertThat(returnedDTO.publicId).isEqualTo(clonedAppPublicId);
    assertThat(returnedDTO.organizationId).isEqualTo(app.getOrganizationId());
    assertThat(returnedDTO.contactUserName).isEqualTo(app.getContactInternalName());
    assertThat(returnedDTO.applicationTags).isEmpty();

    String expectedTelemetryPath = "/" + PublicApiPaths.APP_RESOURCE_PATH + "/" +
        ApiApplicationResourceV2.CLONE_PATH;
    TelemetryContainerRequestFilter telemetryContainerRequestFilter =
        ctx.lookup(TelemetryContainerRequestFilter.class);
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
  void testMoveApplication() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplicationWithParent("test-app-id");

    HttpResponse response = restRequest().path(ApiApplicationResourceV2.MOVE_PATH)
        .parameter(app.getId(), org.getId())
        .post();
    ctx.assertResponseStatus(200, response);
    List<String> warnings = response.getBody(ApiMoveApplicationResponseDTOV2.class).warnings;
    assertThat(warnings).isEmpty();
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(org.getId());
  }

  @Test
  void testMoveApplication_UnsatisfiedPreconditions() throws Exception {
    Organization org1 = ctx.tempEntity().newOrganization("New Parent");
    Organization org2 = ctx.tempEntity().newOrganization("Old Parent");
    Application app = ctx.tempEntity().newApplication("My App", "test-app-id", org2.getId());
    ctx.tempEntity().newPolicy(app.getOrganizationId(), "Missing Policy");

    HttpResponse response = restRequest().path(ApiApplicationResourceV2.MOVE_PATH)
        .parameter(app.getId(), org1.getId())
        .post();
    ctx.assertResponseStatus(409, response);
    ApiMoveApplicationResponseDTOV2 issues = response.getBody(ApiMoveApplicationResponseDTOV2.class);
    assertThat(issues.errors)
        .containsExactly(String.format(ApplicationMoveService.POLICY_MISSING_MSG, "Missing Policy", org2.getName()));
    assertThat(applicationDAO.getById(app.getId()).getOrganizationId()).isEqualTo(app.getOrganizationId());
  }

  @Test
  void testGetApplicationsByOrganizationId() throws Exception {
    Application app1 = ctx.tempEntity().newApplicationWithParent();
    Application app2 = ctx.tempEntity().newApplication(app1.getOrganizationId());
    ctx.tempEntity().newApplicationWithParent();

    HttpResponse response =
        restRequest().path(ApiApplicationResourceV2.ORGANIZATION_PATH).parameter(app1.getOrganizationId()).get();

    ctx.assertResponseStatus(200, response);
    ApiApplicationListDTO apiApplicationListDTO = response.getBody(ApiApplicationListDTO.class);
    assertThat(apiApplicationListDTO).isNotNull();
    assertThat(apiApplicationListDTO.applications).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(ApiApplicationAdapter.convertToDTO(app1, Collections.emptyList()),
            ApiApplicationAdapter.convertToDTO(app2, Collections.emptyList()));
  }

  @Test
  void testGetApplicationsWithAppliedCategories() throws Exception {
    ctx.tempEntity().newApplicationWithParent();

    HttpResponse response = restRequest().query("includeCategories", true).get();
    ctx.assertResponseStatus(200, response);

    ApiApplicationCategoriesListDTO applicationCategoriesListDTO =
        response.getBody(ApiApplicationCategoriesListDTO.class);
    assertThat(applicationCategoriesListDTO).isNotNull();
    assertThat(applicationCategoriesListDTO.applications).hasSize(2);
  }

  @Test
  void testGetApplicationsWithAppliedCategories_Filtered() throws Exception {
    Application app2 = ctx.tempEntity().newApplicationWithParent();

    HttpResponse response = restRequest().query("includeCategories", true).query("publicId", app2.getPublicId()).get();
    ctx.assertResponseStatus(200, response);

    ApiApplicationCategoriesListDTO applicationCategoriesListDTO =
        response.getBody(ApiApplicationCategoriesListDTO.class);
    assertThat(applicationCategoriesListDTO).isNotNull();
    assertThat(applicationCategoriesListDTO.applications).extracting(dto -> dto.id)
        .containsExactly(app2.getId());
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

  private void assertApplications(
      List<ApiApplicationDTO> actualApplications,
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

    Comparator<ApiApplicationTagDTO> tagComparator = Comparator.<ApiApplicationTagDTO, String>comparing(dto -> dto.id)
        .thenComparing(dto -> dto.applicationId)
        .thenComparing(dto -> dto.tagId);
    assertThat(actualTags).usingElementComparator(tagComparator).containsExactlyInAnyOrderElementsOf(expectedTags);
  }

  /**
   * Mirrors the legacy {@code AbstractBaseIntegrationTest.setApplicationLimit}: mutates the same singleton
   * {@link ProductLicense} bean the running server reads live, so no REST reinstall is needed.
   */
  private void setApplicationLimit(Integer applicationLimit) {
    ((TestProductLicense) ctx.lookup(ProductLicense.class)).setMaxApplications(applicationLimit);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.APP_RESOURCE_PATH);
  }
}
