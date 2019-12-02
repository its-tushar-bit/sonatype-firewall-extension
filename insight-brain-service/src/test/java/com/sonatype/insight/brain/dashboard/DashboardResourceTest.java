/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.dashboard.DashboardResource.GET_APPLICATION_RISKS_EXPORT_PATH;
import static com.sonatype.insight.brain.dashboard.DashboardResource.GET_COMPONENT_RISKS_EXPORT_PATH;
import static com.sonatype.insight.brain.dashboard.DashboardResource.GET_NEWEST_RISKS_EXPORT_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DashboardResourceTest
    extends AbstractResourceTest
{
  private final SimpleDateFormat csvTimestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  private final SimpleDateFormat filenameTimestampFormatter = new SimpleDateFormat("yyyyMMdd-HHmmss");

  {
    csvTimestampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  @Test
  public void testGetNewestRisks() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app);

    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);

    HttpResponse response = restRequest().path(DashboardResource.GET_NEWEST_RISKS_PATH)
        .body(new RisksFilterDTO()).post();

    assertResponseStatus(200, response);
    DashboardResultsDTO<?> dto = response.getBody(DashboardResultsDTO.class);
    assertThat(dto.dashboardResults).hasSize(1);
  }

  @Test
  public void testGetActiveDashboardFilterForCurrentUser_ActiveFilter() throws Exception {
    User tempUser = tempEntity.newUser();
    String filterName = "";
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());
    tempEntity.newMembershipMapping(app.getId(), Role.OWNER_ROLE_ID, tempUser.getUsername());
    // creating a new filter
    DashboardFilterDTO dashboardFilterDTO = createDashboardFilter(app, tag);
    tempEntity.newDashboardFilter(tempUser.getUsername(), InternalRealm.ID, filterName,
        JsonUtils.format(dashboardFilterDTO));
    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.FILTERS_PATH);
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    NamedDashboardFilterDTO result = response.getBody(NamedDashboardFilterDTO.class);
    assertThat(result).isNotNull();
    assertDashboardFilterDTO(result.filter, dashboardFilterDTO);
    assertThat(result.name).isEqualTo(filterName);
  }

  @Test
  public void testUpdateDashboardFilterForCurrentUser_Create() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());
    // creating a new filter
    NamedDashboardFilterDTO dashboardFilterDTO = createNamedDashboardFilter(app, tag);
    // Test the create
    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.FILTERS_PATH);
    HttpResponse response = request.body(dashboardFilterDTO).put();
    assertResponseStatus(200, response);

    DashboardFilter dashboardFilter =
        dashboardFilterDAO.getByUsernameAndRealmId(tempUser.getUsername(), InternalRealm.ID).get(0);
    assertThat(dashboardFilter).isNotNull();

    DashboardFilterDTO returnedDashboardFilterDTO = response.getBody(DashboardFilterDTO.class);
    assertThat(returnedDashboardFilterDTO).isNotNull();
    assertDashboardFilterDTO(returnedDashboardFilterDTO, dashboardFilterDTO.filter);
  }

  @Test
  public void testUpdateDashboardFilterForCurrentUser_Update() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());
    String filterName = "";
    NamedDashboardFilterDTO dashboardFilterDTO = createNamedDashboardFilter(app, tag);
    // creating a new filter
    tempEntity.newDashboardFilter(tempUser.getUsername(), InternalRealm.ID, filterName,
        JsonUtils.format(dashboardFilterDTO));
    // updating the new filter
    dashboardFilterDTO.filter.minPolicyThreatLevel = 4;
    dashboardFilterDTO.filter.maxPolicyThreatLevel = 9;
    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.FILTERS_PATH);
    HttpResponse response = request.body(dashboardFilterDTO).put();
    assertResponseStatus(200, response);

    DashboardFilterDTO result = response.getBody(DashboardFilterDTO.class);
    assertThat(result).isNotNull();
    assertDashboardFilterDTO(result, dashboardFilterDTO.filter);
  }

  private void assertDashboardFilterDTO(DashboardFilterDTO actual, DashboardFilterDTO expected) {
    assertThat(actual.minPolicyThreatLevel).isEqualTo(expected.minPolicyThreatLevel);
    assertThat(actual.maxPolicyThreatLevel).isEqualTo(expected.maxPolicyThreatLevel);
    assertThat(actual.applicationFilters).hasSize(1);
    assertThat(actual.applicationFilters.get(0)).isEqualTo(expected.applicationFilters.get(0));
    assertThat(actual.tagFilters).hasSize(1);
    assertThat(actual.tagFilters.get(0)).isEqualTo(expected.tagFilters.get(0));
    assertThat(actual.policyThreatCategoryFilters).hasSize(1);
    assertThat(actual.policyThreatCategoryFilters.get(0)).isEqualTo(expected.policyThreatCategoryFilters.get(0));
    assertThat(actual.stageTypeFilters).hasSize(1);
    assertThat(actual.stageTypeFilters.get(0)).isEqualTo(expected.stageTypeFilters.get(0));
  }

  private DashboardFilterDTO createDashboardFilter(Application application, Tag tag) {
    DashboardFilterDTO dashboardFilterDTO = new DashboardFilterDTO();
    dashboardFilterDTO.minPolicyThreatLevel = 1;
    dashboardFilterDTO.maxPolicyThreatLevel = 10;

    dashboardFilterDTO.applicationFilters = new ArrayList<>();
    dashboardFilterDTO.applicationFilters.add(application.getId());

    dashboardFilterDTO.organizationFilters = new ArrayList<>();
    dashboardFilterDTO.organizationFilters.add(application.getOrganizationId());

    dashboardFilterDTO.tagFilters = new ArrayList<>();
    dashboardFilterDTO.tagFilters.add(tag.getId());

    dashboardFilterDTO.policyThreatCategoryFilters = new ArrayList<>();
    dashboardFilterDTO.policyThreatCategoryFilters.add(PolicyThreatCategory.SECURITY);

    dashboardFilterDTO.stageTypeFilters = new ArrayList<>();
    dashboardFilterDTO.stageTypeFilters.add(Stage.ID_BUILD);

    return dashboardFilterDTO;
  }
  
  private NamedDashboardFilterDTO createNamedDashboardFilter(Application application, Tag tag) {
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.filter = createDashboardFilter(application, tag);
    return namedDashboardFilterDTO;
  }

  @Test
  public void testGetNewestRisksExport() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application", "test organization");
    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    PolicyViolation v1 = createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);
    Policy stagePolicy = tempEntity.newPolicy(app.getId(), "stage policy");
    PolicyViolation v2 = createFirstOccurrencePolicyViolation(app, stagePolicy, StageReleaseStageType.ID,
        new Date(v1.getOpenTime().getTime() + 10));

    RisksFilterDTO filter = new RisksFilterDTO();
    filter.orderBy = "-POLICY_NAME";
    HttpResponse response = restRequest().path(GET_NEWEST_RISKS_EXPORT_PATH).part("filter", filter).post();

    assertResponseOkAndCsvHeadersSet(response, "results-violations");
    String[] lines = response.getBodyText().split("\r\n");
    assertThat(lines).containsExactly(NewestRiskDTO.getCsvHeader(),
        "5,stage policy,test organization,test application,Group1 : Artifact1 : Version1," + getTimestamps(v2),
        "5,build policy,test organization,test application,Group1 : Artifact1 : Version1," + getTimestamps(v1));

    filter.stageIds = Sets.newHashSet(StageReleaseStageType.ID);
    response = restRequest().path(GET_NEWEST_RISKS_EXPORT_PATH).part("filter", filter).post();

    assertResponseOkAndCsvHeadersSet(response, "results-violations");
    lines = response.getBodyText().split("\r\n");
    assertThat(lines).containsExactly(NewestRiskDTO.getCsvHeader(),
        "5,stage policy,test organization,test application,Group1 : Artifact1 : Version1," + getTimestamps(v2));
  }

  @Test
  public void testGetNewestRisksExport_fileNamePrefix() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());

    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilter(app, tag);
    namedDashboardFilterDTO.name = "test newest risks non dirty";

    createNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);

    HttpResponse exportResponse = restRequest().auth(tempUser).path(GET_NEWEST_RISKS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "test_newest_risks_non_dirty-violations");

    dirtyNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);

    exportResponse = restRequest().auth(tempUser).path(GET_NEWEST_RISKS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "results-violations");
  }

  @Test
  public void testGetNewestRisks_InvalidOrderBy() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app);

    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);

    RisksFilterDTO filter = new RisksFilterDTO();
    filter.orderBy = "Invalid";
    HttpResponse response = restRequest().path(DashboardResource.GET_NEWEST_RISKS_PATH)
        .body(filter).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid orderBy property.");
  }

  @Test
  public void testGetApplicationRisksExport() throws Exception {
    Organization org = tempEntity.newOrganization("test organization");
    Application app = tempEntity.newApplication("test application", "app1", org.getId());
    Policy buildPolicy = tempEntity.newPolicy(app);
    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);
    // same app, different stage
    Policy stagePolicy = tempEntity.newPolicy(app);
    createFirstOccurrencePolicyViolation(app, stagePolicy, StageReleaseStageType.ID);
    // different app, same stage
    Organization org2 = tempEntity.newOrganization("test organization 2");
    Application app2 = tempEntity.newApplication("test application 2", "app2", org2.getId());
    Policy buildPolicy2 = tempEntity.newPolicy(app2);
    createFirstOccurrencePolicyViolation(app2, buildPolicy2, BuildStageType.ID);

    RisksFilterDTO filter = new RisksFilterDTO();
    HttpResponse response = restRequest().path(GET_APPLICATION_RISKS_EXPORT_PATH).part("filter", filter).post();

    assertResponseOkAndCsvHeadersSet(response, "results-applications");
    String[] lines = response.getBodyText().split("\r\n");
    assertThat(lines).containsExactly(ApplicationRiskScoreDTO.getCsvHeader(),
        "test organization,test application,10,0,10,0,0", "test organization 2,test application 2,5,0,5,0,0");

    filter.stageIds = Sets.newHashSet(StageReleaseStageType.ID);
    response = restRequest().path(GET_APPLICATION_RISKS_EXPORT_PATH).part("filter", filter).post();

    assertResponseOkAndCsvHeadersSet(response, "results-applications");
    lines = response.getBodyText().split("\r\n");
    assertThat(lines).containsExactly(ApplicationRiskScoreDTO.getCsvHeader(),
        "test organization,test application,5,0,5,0,0");
  }

  @Test
  public void testGetApplicationRisks_InvalidOrderBy() throws Exception {
    RisksFilterDTO filter = new RisksFilterDTO();
    filter.orderBy = "Invalid";
    HttpResponse response = restRequest().path(DashboardResource.GET_APPLICATION_RISKS_PATH)
        .body(filter).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid orderBy property.");
  }

  @Test
  public void testGetApplicationRisksExport_fileNamePrefix() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());

    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilter(app, tag);
    namedDashboardFilterDTO.name = "test application risks non dirty";

    createNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);

    HttpResponse exportResponse = restRequest().auth(tempUser).path(GET_APPLICATION_RISKS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "test_application_risks_non_dirty-applications");

    dirtyNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);

    exportResponse = restRequest().auth(tempUser).path(GET_APPLICATION_RISKS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "results-applications");
  }

  @Test
  public void testGetComponentRisks_InvalidOrderBy() throws Exception {
    RisksFilterDTO filter = new RisksFilterDTO();
    filter.orderBy = "Invalid";
    HttpResponse response = restRequest().path(DashboardResource.GET_COMPONENT_RISKS_PATH)
        .body(filter).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid orderBy property.");
  }

  @Test
  public void testGetComponentRisksExport_returnValidCsvHeadersWithoutAppSetup() throws Exception {
    HttpResponse response = restRequest().path(GET_COMPONENT_RISKS_EXPORT_PATH).part("filter", new RisksFilterDTO())
        .post();

    assertResponseOkAndCsvHeadersSet(response, "results-components");
    String[] lines = response.getBodyText().split("\r\n");
    assertThat(lines).containsExactly(ComponentRiskDTO.getCsvHeader());
  }

  @Test
  public void testGetComponentRisksExport_returnValidCsvContent() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test_app_1", "test app 1");
    Policy buildPolicy = tempEntity.newPolicy(app);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan id");
    tempEntity.newPolicyViolation(evaluation, buildPolicy);
    tempEntity.newPolicyViolation(evaluation, buildPolicy, "Group1", "Artifact2", "Version1", "Hash1", "reason");

    RisksFilterDTO dto = new RisksFilterDTO();
    dto.orderBy = "-NAME";
    HttpResponse response = restRequest().path(GET_COMPONENT_RISKS_EXPORT_PATH).part("filter", dto).post();

    assertResponseOkAndCsvHeadersSet(response, "results-components");
    String[] lines = response.getBodyText().split("\r\n");
    assertThat(lines).containsExactly(ComponentRiskDTO.getCsvHeader(), "Group1 : Artifact2 : Version1,1,5,0,5,0,0",
        "Group1 : Artifact1 : Version1,1,5,0,5,0,0");
  }

  @Test
  public void testGetComponentRisksExport_fileNamePrefix() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());

    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilter(app, tag);
    namedDashboardFilterDTO.name = "test component risks non dirty";

    createNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);

    HttpResponse exportResponse = restRequest().auth(tempUser).path(GET_COMPONENT_RISKS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "test_component_risks_non_dirty-components");

    dirtyNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);

    exportResponse = restRequest().auth(tempUser).path(GET_COMPONENT_RISKS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "results-components");
  }

  private void createNamedFilterForUserAndAssertResponseOk(NamedDashboardFilterDTO namedDashboardFilterDTO, User user)
      throws Exception
  {
    HttpRequest request = restRequest().auth(user).path(DashboardResource.NAMED_FILTERS_PATH);
    HttpResponse response = request.body(namedDashboardFilterDTO).put();
    assertResponseStatus(200, response);
  }

  private void dirtyNamedFilterForUserAndAssertResponseOk(NamedDashboardFilterDTO namedDashboardFilterDTO, User user)
      throws Exception
  {
    namedDashboardFilterDTO.basedOnFilterName = namedDashboardFilterDTO.name;
    namedDashboardFilterDTO.name = null;
    namedDashboardFilterDTO.filter.maxPolicyThreatLevel -= 1;
    HttpRequest request = restRequest().auth(user).path(DashboardResource.FILTERS_PATH);
    HttpResponse response = request.body(namedDashboardFilterDTO).put();
    assertResponseStatus(200, response);
  }

  private PolicyViolation createFirstOccurrencePolicyViolation(Application app, Policy tempPolicy, String stageTypeId) {
    return createFirstOccurrencePolicyViolation(app, tempPolicy, stageTypeId, new Date());
  }

  private PolicyViolation createFirstOccurrencePolicyViolation(Application app,
                                                               Policy tempPolicy,
                                                               String stageTypeId,
                                                               Date time)
  {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "test scan id", time);
    return tempEntity.newPolicyViolation(evaluation, tempPolicy);
  }

  private void assertResponseOkAndCsvHeadersSet(HttpResponse response, String fileNamePrefix) throws ParseException {
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("text/csv");
    String dispositionHeader = response.getHeader("Content-Disposition");
    String headerStart = "attachment; filename=\"" + fileNamePrefix + "-";
    assertThat(dispositionHeader).startsWith(headerStart);
    Matcher matcher = Pattern.compile(headerStart + "([0-9]{8}-[0-9]{6})" + "\\.csv").matcher(dispositionHeader);
    assertThat(matcher.find()).as("Could not find a timestamp in filename attribute: " + dispositionHeader).isTrue();
    Date fileNameTimestamp = filenameTimestampFormatter.parse(matcher.group(1));
    assertThat(new Date().getTime() - fileNameTimestamp.getTime()).isLessThan(5 * 1000);
  }

  private String getTimestamps(PolicyViolation policyViolation) {
    String dateFirstSeen = csvTimestampFormatter.format(policyViolation.getOpenTime());
    long millisSinceFirstSeen = policyViolation.getOpenTime().getTime();
    return dateFirstSeen + "," + millisSinceFirstSeen;
  }
  
  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Insert() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    String filterName = "Filter112233";
    namedDashboardFilterDTO.name = filterName;
    namedDashboardFilterDTO.filter = createDashboardFilter(app, tag);

    //creating a new filter
    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.NAMED_FILTERS_PATH);
    HttpResponse response = request.body(namedDashboardFilterDTO).put();
    assertResponseStatus(200, response);

    NamedDashboardFilterDTO result = response.getBody(NamedDashboardFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.name).isEqualTo(namedDashboardFilterDTO.name);
    assertDashboardFilterDTO(result.filter, namedDashboardFilterDTO.filter);

    // verify what was saved in the db is what's expected
    verifyDbState(tempUser, filterName, namedDashboardFilterDTO);
  }

  @Test
  public void testGetNamedDashboardFiltersForCurrentUser() throws Exception {
    User tempUser = tempEntity.newUser();
    String filterName = "Filter778899";
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.name = filterName;
    namedDashboardFilterDTO.filter = createDashboardFilter(app, tag);
    // creating a new named filter
    tempEntity.newDashboardFilter(tempUser.getUsername(), InternalRealm.ID, filterName,
        JsonUtils.format(namedDashboardFilterDTO.filter));

    NamedDashboardFilterDTO namedDashboardFilterDTO2 = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO2.name = "";
    namedDashboardFilterDTO2.filter = createDashboardFilter(app, tag);
    // creating a new active filter (without a name)
    tempEntity.newDashboardFilter(tempUser.getUsername(), InternalRealm.ID, "",
        JsonUtils.format(namedDashboardFilterDTO.filter));
    
    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.NAMED_FILTERS_PATH);
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    NamedDashboardFilterDTO[] result = response.getBody(NamedDashboardFilterDTO[].class);
    assertThat(result).hasSize(1);
    assertThat(result[0].name).isEqualTo(filterName);
    
    // verify what was saved in the db is what's expected
    verifyDbState(tempUser, filterName, namedDashboardFilterDTO);
  }

  @Test
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Update() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    String filterName = "Filter112233";
    namedDashboardFilterDTO.name = filterName;
    namedDashboardFilterDTO.filter = createDashboardFilter(app, tag);

    // creating a new filter
    tempEntity.newDashboardFilter(tempUser.getUsername(), InternalRealm.ID, filterName,
        JsonUtils.format(namedDashboardFilterDTO.filter));

    // updating the new filter
    namedDashboardFilterDTO.filter.minPolicyThreatLevel = 3;
    namedDashboardFilterDTO.filter.maxPolicyThreatLevel = 7;
    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.NAMED_FILTERS_PATH);
    HttpResponse response = request.body(namedDashboardFilterDTO).put();
    assertResponseStatus(200, response);

    NamedDashboardFilterDTO result = response.getBody(NamedDashboardFilterDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.name).isEqualTo(namedDashboardFilterDTO.name);
    assertThat(result.filter.minPolicyThreatLevel).isEqualTo(3);
    assertThat(result.filter.maxPolicyThreatLevel).isEqualTo(7);
    
    // verify what was saved in the db is what's expected
    verifyDbState(tempUser, filterName, namedDashboardFilterDTO);
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());

    String username = tempUser.getUsername();
    String filterName1 = "Filter XYZ";
    DashboardFilter dashboardFilter1 = tempEntity.newDashboardFilter(username, InternalRealm.ID, filterName1,
        JsonUtils.format(createDashboardFilter(app, tag)));

    String filterName2 = "Filter YYY";
    DashboardFilter dashboardFilter2 = tempEntity.newDashboardFilter(username, InternalRealm.ID, filterName2,
        JsonUtils.format(createDashboardFilter(app, tag)));

    List<String> filterNames = Arrays.asList(filterName1, filterName2);

    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.DELETE_NAMED_FILTERS_PATH)
        .body(filterNames);
    HttpResponse response = request.parameter(filterName1).post();
    assertResponseStatus(204, response);
    // verify that both filters above got deleted
    assertThat(dashboardFilterDAO.getById(dashboardFilter1.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter2.getId())).isNull();
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_returnErrorResponseWhenFilterIsNotFound()
      throws Exception
  {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());

    String username = tempUser.getUsername();
    String filterName = "Filter 1";
    DashboardFilter dashboardFilter = tempEntity.newDashboardFilter(username, InternalRealm.ID, filterName,
        JsonUtils.format(createDashboardFilter(app, tag)));

    List<String> filterNames = Arrays.asList(filterName, "NotFoundFilter");

    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.DELETE_NAMED_FILTERS_PATH)
        .body(filterNames);
    HttpResponse response = request.parameter(filterName).post();
    assertResponseStatus(404, response);
    DashboardFilterErrorResponseDTO[] errorResponseDTOs = response.getBody(DashboardFilterErrorResponseDTO[].class);
    assertThat(errorResponseDTOs).hasSize(1);
    assertThat(errorResponseDTOs[0].status).isEqualTo(404);
    assertThat(errorResponseDTOs[0].name).isEqualTo("NotFoundFilter");
    assertThat(errorResponseDTOs[0].errorMessage)
        .isEqualTo("Cannot find a filter with name NotFoundFilter for user " + username + ".");
    // verify that Filter 1 got deleted
    assertThat(dashboardFilterDAO.getById(dashboardFilter.getId())).isNull();
  }

  @Test
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_returnMaxStatusCodeWhenDifferentFailuresOccur()
      throws Exception
  {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());

    String username = tempUser.getUsername();
    String filterName1 = "Filter 1";
    tempEntity.newDashboardFilter(username, InternalRealm.ID, filterName1,
        JsonUtils.format(createDashboardFilter(app, tag)));

    String filterName2 = "Filter 2";
    tempEntity.newDashboardFilter(username, InternalRealm.ID, filterName2,
        JsonUtils.format(createDashboardFilter(app, tag)));

    List<String> filterNames = Arrays.asList(filterName1, filterName2, "NotFoundFilter");
    
    List<DashboardFilterErrorResponseDTO> expectedResult = new ArrayList<>();
    expectedResult.add(new DashboardFilterErrorResponseDTO(filterName1, "internal server error", 500));
    expectedResult.add(new DashboardFilterErrorResponseDTO("NotFoundFilter", "not found error", 404));

    DashboardFilterService dashboardFilterServiceMock = Mockito.mock(DashboardFilterService.class);
    when(dashboardFilterServiceMock.deleteDashboardFiltersForCurrentUserByFilterName(filterNames))
        .thenReturn(expectedResult);
    
    DashboardResource underTest = new DashboardResource(null, dashboardFilterServiceMock, null, null);
    Response actual = underTest.deleteDashboardFiltersForCurrentUserByFilterName(filterNames);
    assertThat(actual.getStatus()).isEqualTo(500);
    assertThat(actual.getEntity()).asList().hasSize(2);
  }

  private void verifyDbState(final User tempUser, final String filterName, final NamedDashboardFilterDTO expected)
      throws IOException
  {
    DashboardFilter actual =
        dashboardFilterDAO.getByUsernameAndRealmIdAndName(tempUser.getUsername(), InternalRealm.ID, filterName);
    assertThat(actual).isNotNull();
    DashboardFilterDTO actualDto = JsonUtils.parse(actual.getFilter(), DashboardFilterDTO.class);
    assertThat(actual.getName()).isEqualTo(expected.name);
    assertDashboardFilterDTO(actualDto, expected.filter);
  }
}
