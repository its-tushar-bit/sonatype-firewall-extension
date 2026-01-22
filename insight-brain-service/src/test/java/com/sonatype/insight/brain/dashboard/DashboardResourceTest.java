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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.ws.rs.core.HttpHeaders;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.DashboardResource.GET_APPLICATION_RISKS_EXPORT_PATH;
import static com.sonatype.insight.brain.dashboard.DashboardResource.GET_COMPONENT_RISKS_EXPORT_PATH;
import static com.sonatype.insight.brain.dashboard.DashboardResource.GET_POLICY_WAIVERS_EXPORT_PATH;
import static com.sonatype.insight.brain.dashboard.DashboardResource.GET_VIOLATION_RISKS_EXPORT_PATH;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardResourceTest
    extends AbstractResourceTest
{
  private final SimpleDateFormat csvTimestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  private final SimpleDateFormat filenameTimestampFormatter = new SimpleDateFormat("yyyyMMdd-HHmmss");

  private DashboardFilterDAO dashboardFilterDAO;

  @Before
  public void before() {
    dashboardFilterDAO = lookup(DashboardFilterDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(DashboardResource.RESOURCE_PATH);
  }

  @Test
  public void testGetViolationRisks() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app);

    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);

    HttpResponse response = restRequest().path(DashboardResource.GET_VIOLATION_RISKS_PATH)
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
    Repository repository1 = tempEntity.newRepository("repo1");
    Repository repository2 = tempEntity.newRepository("repo2");

    // creating a new filter
    NamedDashboardFilterDTO dashboardFilterDTO = createNamedDashboardFilter(app, tag);
    dashboardFilterDTO.filter.repositoryFilters = new ArrayList<>();
    dashboardFilterDTO.filter.repositoryFilters.addAll(Arrays.asList(repository1.getId(), repository2.getId()));

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
    Repository repository1 = tempEntity.newRepository("repo1");
    Repository repository2 = tempEntity.newRepository("repo2");
    String filterName = "";
    NamedDashboardFilterDTO dashboardFilterDTO = createNamedDashboardFilter(app, tag);
    dashboardFilterDTO.filter.repositoryFilters = new ArrayList<>();
    dashboardFilterDTO.filter.repositoryFilters.addAll(Arrays.asList(repository1.getId(), repository2.getId()));

    // creating a new filter
    tempEntity.newDashboardFilter(tempUser.getUsername(), InternalRealm.ID, filterName,
        JsonUtils.format(dashboardFilterDTO));

    // updating the new filter
    dashboardFilterDTO.filter.minPolicyThreatLevel = 4;
    dashboardFilterDTO.filter.maxPolicyThreatLevel = 9;
    dashboardFilterDTO.filter.repositoryFilters = new ArrayList<>(Collections.singletonList(repository2.getId()));
    dashboardFilterDTO.filter.expirationDate = ExpirationDate.NEVER;

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

    assertThat(CollectionUtils.isEqualCollection(actual.organizationFilters, expected.organizationFilters)).isTrue();
    assertThat(CollectionUtils.isEqualCollection(actual.applicationFilters, expected.applicationFilters)).isTrue();
    assertThat(CollectionUtils.isEqualCollection(actual.tagFilters, expected.tagFilters)).isTrue();
    assertThat(CollectionUtils.isEqualCollection(actual.policyThreatCategoryFilters,
        expected.policyThreatCategoryFilters)).isTrue();
    assertThat(CollectionUtils.isEqualCollection(actual.stageTypeFilters, expected.stageTypeFilters)).isTrue();
    assertThat(CollectionUtils.isEqualCollection(actual.repositoryFilters, expected.repositoryFilters)).isTrue();
    assertThat(actual.expirationDate).isEqualTo(expected.expirationDate);
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
    if (tag != null) {
      dashboardFilterDTO.tagFilters.add(tag.getId());
    }

    dashboardFilterDTO.policyThreatCategoryFilters = new ArrayList<>();
    dashboardFilterDTO.policyThreatCategoryFilters.add(PolicyThreatCategory.SECURITY);

    dashboardFilterDTO.stageTypeFilters = new ArrayList<>();
    dashboardFilterDTO.stageTypeFilters.add(Stage.ID_BUILD);
    dashboardFilterDTO.expirationDate = ExpirationDate.IN_30_DAYS;

    return dashboardFilterDTO;
  }

  private NamedDashboardFilterDTO createNamedDashboardFilter(Application application, Tag tag) {
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.filter = createDashboardFilter(application, tag);
    return namedDashboardFilterDTO;
  }

  @Test
  public void testGetViolationRisksExport() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application", "test organization");
    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    PolicyViolation v1 = createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);
    Policy stagePolicy = tempEntity.newPolicy(app.getId(), "stage policy");
    PolicyViolation v2 = createFirstOccurrencePolicyViolation(app, stagePolicy, StageReleaseStageType.ID,
        new Date(v1.getOpenTime().getTime() + 10));

    RisksFilterDTO filter = new RisksFilterDTO();
    filter.orderBy = "-POLICY_NAME";
    HttpResponse response = restRequest().path(GET_VIOLATION_RISKS_EXPORT_PATH).part("filter", filter).post();
    assertResponseOkAndCsvHeadersSet(response, "results-violations");

    String[] lines = response.getBodyText().split("\r\n");
    String expectedFirstLine = format("5,%s,test organization,test application,Group1 : Artifact1 : Version1,%s,,%s",
        "stage policy", getTimestamps(v2), v2.getId());
    String expectedSecondLine = format("5,%s,test organization,test application,Group1 : Artifact1 : Version1,%s,,%s",
        "build policy", getTimestamps(v1), v1.getId());
    assertThat(lines).containsExactly(DashboardViolationRiskDTO.getCsvHeader(), expectedFirstLine, expectedSecondLine);

    filter.stageIds = Sets.newHashSet(StageReleaseStageType.ID);
    response = restRequest().path(GET_VIOLATION_RISKS_EXPORT_PATH).part("filter", filter).post();
    assertResponseOkAndCsvHeadersSet(response, "results-violations");

    lines = response.getBodyText().split("\r\n");
    String expectedLine =
        format("5,stage policy,test organization,test application,Group1 : Artifact1 : Version1,%s,,%s",
            getTimestamps(v2), v2.getId());
    assertThat(lines).containsExactly(DashboardViolationRiskDTO.getCsvHeader(), expectedLine);
  }

  @Test
  public void testGetViolationRisksExport_fileNamePrefix() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());

    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilter(app, tag);
    namedDashboardFilterDTO.name = "test violation risks non dirty";

    createNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);

    HttpResponse exportResponse = restRequest().auth(tempUser).path(GET_VIOLATION_RISKS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "test_violation_risks_non_dirty-violations");

    dirtyNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);

    exportResponse = restRequest().auth(tempUser).path(GET_VIOLATION_RISKS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "results-violations");
  }

  @Test
  public void testGetViolationRisks_InvalidOrderBy() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app);

    createFirstOccurrencePolicyViolation(app, buildPolicy, BuildStageType.ID);

    RisksFilterDTO filter = new RisksFilterDTO();
    filter.orderBy = "Invalid";
    HttpResponse response = restRequest().path(DashboardResource.GET_VIOLATION_RISKS_PATH)
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

  private PolicyViolation createFirstOccurrencePolicyViolation(
      Application app,
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
    String dispositionHeader = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
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
  public void testDeleteDashboardFilterForCurrentUserByFilterName() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag tag = tempEntity.newTag(org.getId());

    String filerName = "Filter XYZ";
    String username = tempUser.getUsername();
    DashboardFilter dashboardFilter1 = tempEntity.newDashboardFilter(username, InternalRealm.ID, filerName,
        JsonUtils.format(createDashboardFilter(app, tag)));

    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.DELETE_NAMED_FILTER_PATH)
        .query("filterName", filerName);
    HttpResponse response = request.post();
    assertResponseStatus(204, response);
    // verify that the filter got deleted
    assertThat(dashboardFilterDAO.getById(dashboardFilter1.getId())).isNull();
  }

  @Test
  public void testDeleteDashboardFilterForCurrentUserByFilterName_returnErrorResponseWhenFilterIsNotFound()
      throws Exception
  {
    User tempUser = tempEntity.newUser();
    String username = tempUser.getUsername();

    HttpRequest request = restRequest().auth(tempUser).path(DashboardResource.DELETE_NAMED_FILTER_PATH)
        .query("filterName", "NotFoundFilter");
    HttpResponse response = request.post();
    assertResponseStatus(404, response);
    String errorMessage = response.getBodyText();
    assertThat(errorMessage).isEqualTo("Cannot find a filter with name NotFoundFilter for user " + username + ".");
  }

  @Test
  public void testGetPolicyWaivers() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), app.getId(), "comment");
    tempEntity.newWaiver("hash1", policy.getId(), org.getId(), "comment");

    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(new RisksFilterDTO()).post();

    assertResponseStatus(200, response);
    DashboardResultsDTO<?> dto = response.getBody(DashboardResultsDTO.class);
    assertThat(dto.dashboardResults).hasSize(2);
    // Due to type erasures at runtime this is the current best way to try to assert the properties of the inner objects
    LinkedHashMap<String, Object> resultAsMap = (LinkedHashMap<String, Object>) dto.dashboardResults.get(0);
    assertThat(resultAsMap.get("id")).isEqualTo(policyWaiver.getId());
  }

  @Test
  public void testGetPolicyWaivers_InvalidOrderBy() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    Policy policy = tempEntity.newPolicy();
    tempEntity.newWaiver("hash", policy.getId(), app.getId(), "comment");

    RisksFilterDTO filter = new RisksFilterDTO();
    filter.orderBy = "Invalid";
    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(filter).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid orderBy property.");
  }

  @Test
  public void testGetPolicyWaivers_EmptyFilter() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    Policy policy = tempEntity.newPolicy();
    tempEntity.newWaiver("hash", policy.getId(), app.getId(), "comment");

    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(null).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid filter supplied for request.");
  }

  @Test
  public void testGetPolicyWaivers_IncludeAutoWaivers() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    Policy policy = tempEntity.newPolicy();
    tempEntity.newWaiver("hash", policy.getId(), app.getId(), "comment");
    tempEntity.newAutoPolicyWaiver(app.getId());

    //Test without including auto waivers query param
    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(new RisksFilterDTO()).post();

    assertResponseStatus(200, response);
    DashboardResultsDTO<?> dto = response.getBody(DashboardResultsDTO.class);
    assertThat(dto.dashboardResults).hasSize(2);

    //Test with auto waivers query param set to false
    response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(new RisksFilterDTO())
        .query("includeAutoWaivers", "false").post();

    assertResponseStatus(200, response);
    dto = response.getBody(DashboardResultsDTO.class);
    assertThat(dto.dashboardResults).hasSize(1);

    //Test without including auto waivers and feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(new RisksFilterDTO()).post();

    assertResponseStatus(200, response);
    dto = response.getBody(DashboardResultsDTO.class);
    assertThat(dto.dashboardResults).hasSize(1);

    //Test with auto waivers query param set to true and feature flag is disabled
    response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(new RisksFilterDTO())
        .query("includeAutoWaivers", "false").post();

    assertResponseStatus(200, response);
    dto = response.getBody(DashboardResultsDTO.class);
    assertThat(dto.dashboardResults).hasSize(1);
  }

  @Test
  public void testGetPolicyWaiversExport() throws Exception {
    Organization org = tempEntity.newOrganization("Main organization");
    Application app = tempEntity.newApplication("New-App", org.getId());

    Policy policy = tempEntity.newPolicy();
    Constraint sourceConstraint = policy.getConstraints().get(0);
    ConstraintFact sourceConstraintFact =
        new ConstraintFact(sourceConstraint.getId(), sourceConstraint.getName(), sourceConstraint.getOperator().name());
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("GroupId", "ArtifactId", "Version1.0.0");
    PolicyWaiver policyWaiver =
        tempEntity.newWaiver("hash", policy.getId(), app.getId(), Collections.singletonList(sourceConstraintFact),
            PackageUrlIdentifier.toPackageUrl(identifier), ComponentMatcherStrategyForWaiver.EXACT_COMPONENT,
            "comment");
    PolicyWaiverReason waiverReason = tempEntity.newWaiverReason("system", "Something");
    PolicyWaiver secondPolicyWaiver =
        tempEntity.newWaiverWithExistingReason("hash2", policy.getId(), org.getId(), null, "waiver at org level",
            waiverReason.getId());

    RisksFilterDTO filter = new RisksFilterDTO();
    HttpResponse response = restRequest().path(GET_POLICY_WAIVERS_EXPORT_PATH).part("filter", filter).post();
    assertResponseOkAndCsvHeadersSet(response, "results-waivers");

    final String expectedConstraints = "\"" + policyWaiver.getConstraintFactsJson().replace("\"", "\"\"") + "\"";
    final ComponentDisplayName expectedComponentName =
        ComponentDisplayNameUtil.fromIdentifier(policyWaiver.getComponentIdentifier());
    String[] lines = response.getBodyText().split("\r\n");
    String expectedFirstLine =
        format("%s,5,%s,%s,%s,%s,%s,application,%s,%s,EXACT_COMPONENT,hash,%s,%s,%s,%s,comment,%s,%s,%s,%s",
            policyWaiver.getId(), csvTimestampFormatter.format(policyWaiver.getCreateTime()),/*no expiry*/"",
            policy.getId(), policy.getName(), expectedConstraints, app.getId(), app.getName(), expectedComponentName,
            "", policyWaiver.getCreatorId(), policyWaiver.getCreatorName(), false, false, "", "");
    String expectedSecondLine =
        format("%s,5,%s,%s,%s,%s,%s,organization,%s,%s,EXACT_COMPONENT,hash2,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            secondPolicyWaiver.getId(), csvTimestampFormatter.format(secondPolicyWaiver.getCreateTime()),/*no expiry*/
            "",
            policy.getId(), policy.getName(), "", org.getId(), org.getName(), "", "",
            secondPolicyWaiver.getCreatorId(), secondPolicyWaiver.getCreatorName(), secondPolicyWaiver.getComment(),
            false,
            false, waiverReason.getId(), waiverReason.getReasonText());

    assertThat(lines).containsExactly(DashboardPolicyWaiverDTO.getCsvHeader(), expectedFirstLine, expectedSecondLine);

    //includeAutoWaivers
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(app.getId());
    response = restRequest().path(GET_POLICY_WAIVERS_EXPORT_PATH).part("filter", filter)
        .query("includeAutoWaivers", true).post();
    assertResponseOkAndCsvHeadersSet(response, "results-waivers");
    lines = response.getBodyText().split("\r\n");
    expectedFirstLine =
        format("%s,5,%s,%s,%s,%s,%s,application,%s,%s,EXACT_COMPONENT,hash,%s,%s,%s,%s,comment,%s,%s,%s,%s",
            policyWaiver.getId(), csvTimestampFormatter.format(policyWaiver.getCreateTime()),/*no expiry*/"",
            policy.getId(), policy.getName(), expectedConstraints, app.getId(), app.getName(), expectedComponentName,
            "", policyWaiver.getCreatorId(), policyWaiver.getCreatorName(), false, false, "", "");
    expectedSecondLine =
        format("%s,5,%s,%s,%s,%s,%s,organization,%s,%s,EXACT_COMPONENT,hash2,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            secondPolicyWaiver.getId(), csvTimestampFormatter.format(secondPolicyWaiver.getCreateTime()),/*no expiry*/
            "",
            policy.getId(), policy.getName(), "", org.getId(), org.getName(), "", "",
            secondPolicyWaiver.getCreatorId(), secondPolicyWaiver.getCreatorName(), secondPolicyWaiver.getComment(),
            false,
            false, waiverReason.getId(), waiverReason.getReasonText());
    String expectedThirdLine = format("%s,7,%s,%s,%s,%s,%s,application,%s,%s,DEFAULT,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
        autoPolicyWaiver.getId(), csvTimestampFormatter.format(autoPolicyWaiver.getCreateTime()),/*no expiry*/"",
        "", "", "", app.getId(), app.getName(), "", "", "",
        autoPolicyWaiver.getCreatorId(), autoPolicyWaiver.getCreatorName(), "", true, false, "", "");

    assertThat(lines).containsExactly(DashboardPolicyWaiverDTO.getCsvHeader(), expectedFirstLine, expectedSecondLine,
        expectedThirdLine);
  }

  @Test
  public void testGetPolicyWaiversExport_fileNamePrefix() throws Exception {
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    NamedDashboardFilterDTO namedDashboardFilterDTO = createNamedDashboardFilter(app, null);
    namedDashboardFilterDTO.name = "test policy waivers non dirty";

    createNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);
    HttpResponse exportResponse = restRequest().auth(tempUser).path(GET_POLICY_WAIVERS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "test_policy_waivers_non_dirty-waivers");

    dirtyNamedFilterForUserAndAssertResponseOk(namedDashboardFilterDTO, tempUser);
    exportResponse = restRequest().auth(tempUser).path(GET_POLICY_WAIVERS_EXPORT_PATH)
        .part("filter", new RisksFilterDTO()).post();
    assertResponseOkAndCsvHeadersSet(exportResponse, "results-waivers");
  }

  @Test
  public void testGetPolicyWaiverRequests() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy();
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest().setPolicyId(policy.getId()).setOwnerId(app.getId());
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);

    HttpResponse response =
        restRequest().path(DashboardResource.GET_POLICY_WAIVER_REQUESTS_PATH).body(new RisksFilterDTO()).post();

    assertResponseStatus(200, response);
    DashboardResultsDTO<?> dto = response.getBody(DashboardResultsDTO.class);
    assertThat(dto.dashboardResults).hasSize(1);
    // Due to type erasure at runtime this is the current best way to try to assert the properties of the inner objects
    LinkedHashMap<String, Object> resultAsMap = (LinkedHashMap<String, Object>) dto.dashboardResults.get(0);
    assertThat(resultAsMap.get("id")).isEqualTo(policyWaiverRequest.getId());
  }

  @Test
  public void testGetPolicyWaiverRequests_EmptyFilter() throws Exception {
    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_WAIVER_REQUESTS_PATH).body(null).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid filter supplied for request.");
  }

  @Test
  public void testGetPolicyWaiverRequestsExport() throws Exception {
    Organization org = tempEntity.newOrganization("Main organization");
    Application app = tempEntity.newApplication("New-App", org.getId());

    Policy policy = tempEntity.newPolicy();
    Constraint sourceConstraint = policy.getConstraints().get(0);
    ConstraintFact sourceConstraintFact =
        new ConstraintFact(sourceConstraint.getId(), sourceConstraint.getName(), sourceConstraint.getOperator().name());
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("GroupId", "ArtifactId", "1.0.0", "Classifier", "Extension");
    PolicyWaiverRequest policyWaiverRequest1 = new PolicyWaiverRequest().setHash("hash1")
        .setPolicyId(policy.getId()).setOwnerId(app.getId())
        .setConstraintFacts(Collections.singletonList(sourceConstraintFact))
        .setAssociatedPackageUrl(PackageUrlIdentifier.toPackageUrl(componentIdentifier))
        .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT).setComment("comment 1");
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1);
    PolicyWaiverReason waiverReason = tempEntity.newWaiverReason("system", "Something");
    PolicyWaiverRequest policyWaiverRequest2 =
        new PolicyWaiverRequest().setHash("hash2").setPolicyId(policy.getId()).setOwnerId(org.getId())
            .setConstraintFacts(Collections.singletonList(sourceConstraintFact))
            .setAssociatedPackageUrl(PackageUrlIdentifier.toPackageUrl(componentIdentifier))
            .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT).setComment("comment 2")
            .setWaiverReasonId(waiverReason.getId());
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest2);

    RisksFilterDTO filter = new RisksFilterDTO();
    HttpResponse response =
        restRequest().path(DashboardResource.GET_POLICY_WAIVER_REQUESTS_EXPORT_PATH).part("filter", filter).post();
    assertResponseOkAndCsvHeadersSet(response, "results-waiver-requests");

    String expectedConstraints = "\"" + policyWaiverRequest1.getConstraintFactsJson().replace("\"", "\"\"") + "\"";
    ComponentDisplayName expectedComponentName =
        ComponentDisplayNameUtil.fromIdentifier(policyWaiverRequest1.getComponentIdentifier());
    String responseContent = response.getBodyText();
    System.err.println(responseContent);
    String[] lines = responseContent.split("\r\n");
    String expectedLine1 =
        format("%s,5,%s,%s,%s,%s,%s,application,%s,%s,EXACT_COMPONENT,hash1,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            policyWaiverRequest1.getId(), csvTimestampFormatter.format(policyWaiverRequest1.getRequestTime()),
            "" /* no expiry */, policy.getId(), policy.getName(), expectedConstraints, app.getId(), app.getName(),
            expectedComponentName, "", policyWaiverRequest1.getRequesterId(), policyWaiverRequest1.getRequesterName(),
            policyWaiverRequest1.getComment(), policyWaiverRequest1.getStatus().name(), false, "", "");
    String expectedLine2 =
        format("%s,5,%s,%s,%s,%s,%s,organization,%s,%s,EXACT_COMPONENT,hash2,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            policyWaiverRequest2.getId(), csvTimestampFormatter.format(policyWaiverRequest2.getRequestTime()),
            "" /* no expiry */, policy.getId(), policy.getName(), expectedConstraints, org.getId(), org.getName(),
            expectedComponentName, "", policyWaiverRequest2.getRequesterId(), policyWaiverRequest2.getRequesterName(),
            policyWaiverRequest2.getComment(), policyWaiverRequest2.getStatus().name(), false, waiverReason.getId(),
            waiverReason.getReasonText());

    assertThat(lines).containsExactly(DashboardPolicyWaiverRequestDTO.getCsvHeader(), expectedLine1, expectedLine2);
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

  /**
   * GET policy waivers includes Repository Manager waivers when filtering by repository. This tests the end-to-end REST
   * endpoint behavior including JSON serialization/deserialization.
   */
  @Test
  public void testGetPolicyWaivers_IncludesRepositoryManagerWaivers() throws Exception {
    Organization org = tempEntity.newOrganization();
    com.sonatype.insight.brain.model.repository.RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(org);

    // Create waiver at Repository level
    PolicyWaiver repositoryWaiver = tempEntity.newWaiver("hash1", policy.getId(), repository.getId(), "repo comment");

    // Create waiver at RepositoryManager level
    PolicyWaiver rmWaiver = tempEntity.newWaiver("hash2", policy.getId(), repositoryManager.getId(), "rm comment");

    // Filter by repository
    RisksFilterDTO filter = new RisksFilterDTO();
    filter.repositoryIds = Set.of(repository.getId());

    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(filter).post();

    assertResponseStatus(200, response);
    DashboardResultsDTO<?> dto = response.getBody(DashboardResultsDTO.class);

    // Should return BOTH repository waiver and repository manager waiver
    assertThat(dto.dashboardResults).hasSize(2);

    // Verify both waivers are in the response
    java.util.List<String> waiverIds = dto.dashboardResults.stream()
        .map(result -> {
          LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) result;
          return (String) resultMap.get("id");
        })
        .collect(java.util.stream.Collectors.toList());

    assertThat(waiverIds).containsExactlyInAnyOrder(repositoryWaiver.getId(), rmWaiver.getId());
  }

  /**
   * Verify JSON response structure for Repository Manager waivers. This tests that RM waivers have correct ownerType,
   * ownerId, and ownerName in JSON response.
   */
  @Test
  public void testGetPolicyWaivers_RepositoryManagerWaiverJsonStructure() throws Exception {
    Organization org = tempEntity.newOrganization();
    com.sonatype.insight.brain.model.repository.RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(org);

    // Create waiver at RepositoryManager level
    PolicyWaiver rmWaiver = tempEntity.newWaiver("hash1", policy.getId(), repositoryManager.getId(), "rm comment");

    // Filter by repository
    RisksFilterDTO filter = new RisksFilterDTO();
    filter.repositoryIds = Set.of(repository.getId());

    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(filter).post();

    assertResponseStatus(200, response);
    DashboardResultsDTO<?> dto = response.getBody(DashboardResultsDTO.class);

    // Find the RM waiver in results
    LinkedHashMap<String, Object> rmWaiverResult = dto.dashboardResults.stream()
        .map(result -> (LinkedHashMap<String, Object>) result)
        .filter(map -> rmWaiver.getId().equals(map.get("id")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("RM waiver not found in response"));

    // Verify JSON structure for RM waiver - focusing on owner-related fields
    assertThat(rmWaiverResult.get("id")).isEqualTo(rmWaiver.getId());
    assertThat(rmWaiverResult.get("ownerId")).isEqualTo(repositoryManager.getId());
    assertThat(rmWaiverResult.get("ownerType")).isEqualTo("repository_manager");
    assertThat(rmWaiverResult.get("ownerName")).isEqualTo(repositoryManager.getName());

    // Verify that essential fields are present (not null)
    assertThat(rmWaiverResult.get("policyId")).isNotNull();
    assertThat(rmWaiverResult.get("policyName")).isNotNull();
  }

  /**
   * Multiple repositories under same RM - verify deduplication via REST endpoint. This tests that RM waivers appear
   * only once when filtering by multiple repos under the same RM.
   */
  @Test
  public void testGetPolicyWaivers_RepositoryManagerWaiverDeduplication() throws Exception {
    Organization org = tempEntity.newOrganization();
    com.sonatype.insight.brain.model.repository.RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(repositoryManager);
    Repository repo2 = tempEntity.newRepository(repositoryManager);
    Repository repo3 = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(org);

    // Create waivers at Repository level
    PolicyWaiver repoWaiver1 = tempEntity.newWaiver("hash1", policy.getId(), repo1.getId(), "repo1 comment");
    PolicyWaiver repoWaiver2 = tempEntity.newWaiver("hash2", policy.getId(), repo2.getId(), "repo2 comment");
    PolicyWaiver repoWaiver3 = tempEntity.newWaiver("hash3", policy.getId(), repo3.getId(), "repo3 comment");

    // Create ONE waiver at RepositoryManager level
    PolicyWaiver rmWaiver = tempEntity.newWaiver("hash4", policy.getId(), repositoryManager.getId(), "rm comment");

    // Filter by all three repositories
    RisksFilterDTO filter = new RisksFilterDTO();
    filter.repositoryIds = Set.of(repo1.getId(), repo2.getId(), repo3.getId());

    HttpResponse response = restRequest().path(DashboardResource.GET_POLICY_WAIVERS_PATH)
        .body(filter).post();

    assertResponseStatus(200, response);
    DashboardResultsDTO<?> dto = response.getBody(DashboardResultsDTO.class);

    // Should return 3 repository waivers + 1 RM waiver (not duplicated) = 4 total
    assertThat(dto.dashboardResults).hasSize(4);

    // Verify all waiver IDs are present
    java.util.List<String> waiverIds = dto.dashboardResults.stream()
        .map(result -> {
          LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) result;
          return (String) resultMap.get("id");
        })
        .collect(java.util.stream.Collectors.toList());

    assertThat(waiverIds).containsExactlyInAnyOrder(
        repoWaiver1.getId(),
        repoWaiver2.getId(),
        repoWaiver3.getId(),
        rmWaiver.getId()
    );

    // Verify RM waiver appears exactly ONCE (deduplication)
    long rmWaiverCount = waiverIds.stream()
        .filter(id -> rmWaiver.getId().equals(id))
        .count();
    assertThat(rmWaiverCount).isEqualTo(1);
  }

  {
    csvTimestampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
  }
}
