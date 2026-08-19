/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.RisksFilterDTOBuilder;
import com.sonatype.insight.brain.builders.TestPolicyBuilder;
import com.sonatype.insight.brain.dashboard.DashboardPolicyWaiverRequestDTOComparator.DashboardPolicyWaiverRequestOrderByEnum;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dashboard.ExpirationDate.ALL;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.IN_30_DAYS;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.IN_7_DAYS;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class DashboardPolicyWaiverRequestServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private Organization parentOrg;

  private Organization org;

  private Application app1;

  private Application app2;

  private Policy policy;

  private RisksFilterDTOBuilder risksFilterDTOBuilder;

  @Inject
  private DashboardPolicyWaiverRequestService dashboardPolicyWaiverRequestService;

  @BeforeEach
  public void before() {
    parentOrg = tempEntity.newOrganization();
    org = tempEntity.newOrganization(parentOrg);
    app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    app2 = tempEntity.newApplication("Application 2", "Application-2", parentOrg.getId());
    policy = tempEntity.newPolicy(org.getId(), "A test policy");

    risksFilterDTOBuilder = new RisksFilterDTOBuilder().withApplicationIds(Collections.emptySet())
        .withOrganizationIds(Collections.emptySet())
        .withPageSize(1);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);
    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(functionCall);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_DashboardDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(functionCall)
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_returnsInformationWithoutExtraDetails() {
    PolicyWaiverRequest policyWaiverRequestApp2 = createPolicyWaiverRequestWithFullDetails(app2);
    PolicyWaiverRequest policyWaiverRequestApp1 = createPolicyWaiverRequest(policy, app1.getId());

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(), app2.getId()));
    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.POLICY_NAME.toString();
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withApplicationIds(apps)
        .withOrderBy(orderBy)
        .withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).id).isEqualTo(policyWaiverRequestApp1.getId());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).id).isEqualTo(policyWaiverRequestApp2.getId());

    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        policyWaiverRequestApp1, app1);
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(1),
        policyWaiverRequestApp2, app2);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_shouldReturnOnlyPageSize() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org);
      createPolicyWaiverRequest(testPolicy, app1.getId());
    };
    IntStream.range(0, 10).forEach(intConsumer);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(2);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isEqualTo(true);
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.size()).isEqualTo(2);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_filtersByOrganization() {
    Organization excludedOrganization = tempEntity.newOrganization();
    Policy excludedPolicy = tempEntity.newPolicy(excludedOrganization);
    createPolicyWaiverRequest(excludedPolicy, excludedOrganization.getId());
    PolicyWaiverRequest policyWaiverRequestApp1 = createPolicyWaiverRequest(policy, org.getId());
    PolicyWaiverRequest policyWaiverRequestParentOrg1 = createPolicyWaiverRequest(policy, parentOrg.getId());
    createPolicyWaiverRequestWithFullDetails(app2);

    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        policyWaiverRequestApp1, org);
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(1),
        policyWaiverRequestParentOrg1, parentOrg);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_filtersByApplication() {
    PolicyWaiverRequest policyWaiverRequestApp2 = createPolicyWaiverRequestWithFullDetails(app2);
    PolicyWaiverRequest policyWaiverRequestParentOrg1 = createPolicyWaiverRequest(policy, parentOrg.getId());
    createPolicyWaiverRequest(policy, app1.getId());

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId())).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        policyWaiverRequestApp2, app2);
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(1),
        policyWaiverRequestParentOrg1, parentOrg);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_filtersByRepository() {
    Repository repository = tempEntity.newRepository();

    Policy policy =
        tempEntity.newPolicy(new TestPolicyBuilder().withSampleTestValues().withOwnerId(org.getId()).build());

    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy.getId())
            .setOwnerId(repository.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 11));

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);

    PolicyWaiverRequest policyWaiverRequest1 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy.getId())
            .setOwnerId(app1.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 11));

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1);

    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repository.getId())).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0), policyWaiverRequest,
        repository);

    PolicyWaiverRequest policyWaiverRequest2 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy.getId())
            .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
            .setExpiryTime(DateUtils.addDays(new Date(), 5));

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest2);

    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(RepositoryContainer.REPOSITORY_CONTAINER_ID));

    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0), policyWaiverRequest2,
        RepositoryContainer.SINGLETON, "all_repositories");
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_returnAllWaiverRequests() {
    Policy policy =
        tempEntity.newPolicy(new TestPolicyBuilder().withSampleTestValues().withOwnerId(org.getId()).build());

    // add app 1 waiver request
    PolicyWaiverRequest policyWaiverRequestApp1 = new PolicyWaiverRequest().setHash("testHash")
        .setPolicyId(policy.getId())
        .setOwnerId(app1.getId());
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestApp1);

    // add app 3 waiver request
    Organization org2 = tempEntity.newOrganization("Org3");
    Application application3 = tempEntity.newApplication("Application-3", " Applicatin-3", org2.getId());
    PolicyWaiverRequest policyWaiverRequestOrg2 = new PolicyWaiverRequest().setHash("testHash")
        .setPolicyId(policy.getId())
        .setOwnerId(application3.getId());
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestOrg2);

    // add waiver request for empty org
    Organization org3 = tempEntity.newOrganization("Org4");
    PolicyWaiverRequest policyWaiverRequestOrg3 = new PolicyWaiverRequest().setHash("testHash")
        .setPolicyId(policy.getId())
        .setOwnerId(org3.getId());
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestOrg3);

    // add waiver request for repo container
    PolicyWaiverRequest policyWaiverRequestRepoContainer = new PolicyWaiverRequest().setHash("testHash")
        .setPolicyId(policy.getId())
        .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestRepoContainer);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService
            .getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.withPageSize(10).build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.id)
        .containsExactlyInAnyOrder(
            policyWaiverRequestApp1.getId(), policyWaiverRequestOrg2.getId(), policyWaiverRequestOrg3.getId(),
            policyWaiverRequestRepoContainer.getId());
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    // added afterwards to make sure that repo container still shows even with no repo waiver requests
    // add waiver request for repo manager
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    PolicyWaiverRequest policyWaiverRequestRepoManager = new PolicyWaiverRequest().setHash("testHash")
        .setPolicyId(policy.getId())
        .setOwnerId(repoManager.getId());
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestRepoManager);

    // add repo waiver request
    Repository repository = tempEntity.newRepository();
    PolicyWaiverRequest policyWaiverRequestRepo = new PolicyWaiverRequest().setHash("testHash")
        .setPolicyId(policy.getId())
        .setOwnerId(repository.getId());
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestRepo);

    dashboardPolicyWaiverRequests = dashboardPolicyWaiverRequestService
        .getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.withPageSize(10).build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.id)
        .containsExactlyInAnyOrder(
            policyWaiverRequestApp1.getId(), policyWaiverRequestOrg2.getId(), policyWaiverRequestOrg3.getId(),
            policyWaiverRequestRepoContainer.getId(), policyWaiverRequestRepoManager.getId(),
            policyWaiverRequestRepo.getId());
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_filtersByApplicationCategories() {
    Tag applicationCategory = tempEntity.newTag(org.getId());
    Application categorizedApplication = tempEntity.newApplication("categorizedApplication", org.getId());
    tempEntity.newApplicationTag(categorizedApplication.getId(), applicationCategory.getId());

    PolicyWaiverRequest categorizedAppWaiverRequest = createPolicyWaiverRequest(policy, categorizedApplication.getId());
    createPolicyWaiverRequest(policy, app1.getId());
    createPolicyWaiverRequest(policy, app2.getId());

    risksFilterDTOBuilder.withTagIds(Collections.singleton(applicationCategory.getId())).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        categorizedAppWaiverRequest, categorizedApplication);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()));
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(0);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_filtersByPolicyTypes() {
    PolicyWaiverRequest policyWaiverRequestApp2 = createPolicyWaiverRequestWithFullDetails(app2);
    Policy nonSecurityPolicy = tempEntity.newPolicy();
    createPolicyWaiverRequest(nonSecurityPolicy, app1.getId());

    PolicyThreatCategoryFilter threatCategoryFilter = new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY);
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()))
        .withPolicyThreatCategories(threatCategoryFilter)
        .withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).id).isEqualTo(policyWaiverRequestApp2.getId());
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        policyWaiverRequestApp2, app2);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_filtersByThreatLevel() {
    PolicyWaiverRequest policyWaiverRequestApp2 = createPolicyWaiverRequestWithFullDetails(app2);
    Policy lowThreatLevelPolicy = policy;
    createPolicyWaiverRequest(lowThreatLevelPolicy, app1.getId());

    PolicyThreatLevelFilter threatLevelFilter = new PolicyThreatLevelFilter(7, 9);
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()))
        .withPolicyThreatLevelRange(threatLevelFilter)
        .withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).id).isEqualTo(policyWaiverRequestApp2.getId());
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        policyWaiverRequestApp2, app2);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_filtersByExpirationDate() {
    PolicyWaiverRequest policyWaiverRequestApp2 = createPolicyWaiverRequestWithFullDetails(app2);
    Date now = new Date();
    PolicyWaiverRequest oneMonthExpiringWaiverRequest =
        createPolicyWaiverRequest("hash1", policy.getId(), app1.getId(), "", DateUtils.addDays(now, 30));
    PolicyWaiverRequest neverExpiringWaiverRequest =
        createPolicyWaiverRequest("hash3", policy.getId(), app1.getId(), "", null);
    createPolicyWaiverRequest("hash2", policy.getId(), app1.getId(), "", DateUtils.addDays(now, 40));

    risksFilterDTOBuilder.withApplicationIds(new HashSet<>(Arrays.asList(app1.getId(), app2.getId())))
        .withExpirationDate(IN_7_DAYS)
        .withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).id).isEqualTo(policyWaiverRequestApp2.getId());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).expiryTime)
        .isBeforeOrEqualTo(DateUtils.addDays(now, IN_7_DAYS.getDays()));
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        policyWaiverRequestApp2, app2);

    risksFilterDTOBuilder.withExpirationDate(IN_30_DAYS);
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).id)
        .isEqualTo(oneMonthExpiringWaiverRequest.getId());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).expiryTime)
        .isBeforeOrEqualTo(DateUtils.addDays(now, IN_30_DAYS.getDays()));
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(1),
        oneMonthExpiringWaiverRequest, app1);

    risksFilterDTOBuilder.withExpirationDate(NEVER);
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).id).isEqualTo(neverExpiringWaiverRequest.getId());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).expiryTime).isNull();
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        neverExpiringWaiverRequest, app1);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_filtersByPolicyWaiverReasonIds() {
    // === Given ===
    Policy policy = tempEntity.newPolicy(app1.getId(), "some-violation", 9);

    // reason 1
    PolicyWaiverRequest policyWaiverRequest1 = createPolicyWaiverRequestWithReason("some-hash-1", policy.getId(),
        app1.getId(), "some-comment-1", "user", "a-reason-for-the-waiver-1");

    // reason 2
    PolicyWaiverRequest policyWaiverRequest2 = createPolicyWaiverRequestWithReason("some-hash-2", policy.getId(),
        app1.getId(), "some-comment-2", "user", "a-reason-for-the-waiver-2");

    // no reason given
    PolicyWaiverRequest policyWaiverRequest3 = createPolicyWaiverRequest(policy, app1.getId());

    // === When ===
    RisksFilterDTO filter = new RisksFilterDTO();
    filter.policyWaiverReasonIds = Sets.newHashSet(policyWaiverRequest1.getWaiverReasonId());
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> filteredByReason1 =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(filter);

    filter.policyWaiverReasonIds = Sets.newHashSet(policyWaiverRequest2.getWaiverReasonId());
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> filteredByReason2 =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(filter);

    filter.policyWaiverReasonIds =
        Sets.newHashSet(policyWaiverRequest1.getWaiverReasonId(), policyWaiverRequest2.getWaiverReasonId());
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> filteredByReasons1And2 =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(filter);

    filter.policyWaiverReasonIds = Sets.newHashSet("no-reason");
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> filterByNoReasonGiven =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(filter);

    filter.policyWaiverReasonIds = Sets.newHashSet();
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> filterByEmptyReasonList =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(filter);

    filter.policyWaiverReasonIds = null;
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> filterByNullReasonList =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(filter);

    // === Then ===
    assertThat(filteredByReason1.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiverRequest1.getPolicyId());

    assertThat(filteredByReason2.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiverRequest2.getPolicyId());

    assertThat(filteredByReasons1And2.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiverRequest1.getPolicyId(), policyWaiverRequest2.getPolicyId());

    assertThat(filterByNoReasonGiven.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiverRequest3.getPolicyId());

    assertThat(filterByEmptyReasonList.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiverRequest1.getPolicyId(), policyWaiverRequest2.getPolicyId(),
            policyWaiverRequest3.getPolicyId());

    assertThat(filterByNullReasonList.dashboardResults.stream().map(entry -> entry.policyId)).containsExactlyInAnyOrder(
        policyWaiverRequest1.getPolicyId(), policyWaiverRequest2.getPolicyId(), policyWaiverRequest3.getPolicyId());
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ordersByThreatLevel() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId(), "policy with threat " + value, value);
      createPolicyWaiverRequest(testPolicy, app1.getId());
    };
    IntStream.rangeClosed(1, 9).forEach(intConsumer);

    String orderBy = "-" + DashboardPolicyWaiverRequestOrderByEnum.THREAT_LEVEL;
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(9);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    IntConsumer assertConsumer =
        value -> assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(9 - value).threatLevel).isEqualTo(value);
    IntStream.rangeClosed(1, 9).forEach(assertConsumer);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ordersByRequestTime() {
    Date now = new Date();
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId());
      Date waiverRequestDate = DateUtils.addDays(now, -value);
      PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setHash("hash")
          .setPolicyId(testPolicy.getId())
          .setOwnerId(app1.getId())
          .setRequestTime(waiverRequestDate);
      tempEntity.newPolicyWaiverRequest(policyWaiverRequest);
    };
    IntStream.rangeClosed(1, 3).forEach(intConsumer);

    // asc
    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.REQUEST_TIME.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(row -> row.requestTime)
        .containsExactly(DateUtils.addDays(now, -3), DateUtils.addDays(now, -2), DateUtils.addDays(now, -1));
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    // desc
    orderBy = "-" + DashboardPolicyWaiverRequestOrderByEnum.REQUEST_TIME.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(row -> row.requestTime)
        .containsExactly(DateUtils.addDays(now, -1), DateUtils.addDays(now, -2), DateUtils.addDays(now, -3));
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ordersByStatus() {
    PolicyWaiverRequest policyWaiverRequestRequested = createPolicyWaiverRequest(policy, app1.getId());
    policyWaiverRequestRequested.setStatus(PolicyWaiverRequestStatus.REQUESTED);
    policyWaiverRequestDAO.update(policyWaiverRequestRequested);
    PolicyWaiverRequest policyWaiverRequestRejected = createPolicyWaiverRequest(policy, org.getId());
    policyWaiverRequestRejected.setStatus(PolicyWaiverRequestStatus.REJECTED);
    policyWaiverRequestDAO.update(policyWaiverRequestRejected);
    PolicyWaiverRequest policyWaiverRequestApproved = createPolicyWaiverRequest(policy, parentOrg.getId());
    policyWaiverRequestApproved.setStatus(PolicyWaiverRequestStatus.APPROVED);
    policyWaiverRequestDAO.update(policyWaiverRequestApproved);

    // asc
    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.STATUS.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(row -> row.status)
        .containsExactly(
            PolicyWaiverRequestStatus.APPROVED, PolicyWaiverRequestStatus.REJECTED,
            PolicyWaiverRequestStatus.REQUESTED);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    // desc
    orderBy = "-" + DashboardPolicyWaiverRequestOrderByEnum.STATUS.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(row -> row.status)
        .containsExactly(
            PolicyWaiverRequestStatus.REQUESTED, PolicyWaiverRequestStatus.REJECTED,
            PolicyWaiverRequestStatus.APPROVED);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ordersByRequesterName() {
    PolicyWaiverRequest policyWaiverRequest3 = createPolicyWaiverRequest(policy, app1.getId());
    policyWaiverRequest3.setRequesterName("Test User 3");
    policyWaiverRequestDAO.update(policyWaiverRequest3);
    PolicyWaiverRequest policyWaiverRequest2 = createPolicyWaiverRequest(policy, org.getId());
    policyWaiverRequest2.setRequesterName("Test User 2");
    policyWaiverRequestDAO.update(policyWaiverRequest2);
    PolicyWaiverRequest policyWaiverRequest1 = createPolicyWaiverRequest(policy, parentOrg.getId());
    policyWaiverRequest1.setRequesterName("Test User 1");
    policyWaiverRequestDAO.update(policyWaiverRequest1);

    // Asc
    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.REQUESTER_NAME.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(row -> row.requesterName)
        .containsExactly("Test User 1", "Test User 2", "Test User 3");
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    // Desc
    orderBy = "-" + DashboardPolicyWaiverRequestOrderByEnum.REQUESTER_NAME.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(row -> row.requesterName)
        .containsExactly("Test User 3", "Test User 2", "Test User 1");
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ordersByPolicyName() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId(), "Policy " + value);
      createPolicyWaiverRequest(testPolicy, org.getId());
    };
    IntStream.rangeClosed(1, 3).forEach(intConsumer);

    // desc
    String orderBy = "-" + DashboardPolicyWaiverRequestOrderByEnum.POLICY_NAME;
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(row -> row.policyName)
        .containsExactly("Policy 3", "Policy 2", "Policy 1");
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    // asc
    orderBy = DashboardPolicyWaiverRequestOrderByEnum.POLICY_NAME.toString();
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withOrderBy(orderBy).withPageSize(10);
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(row -> row.policyName)
        .containsExactly("Policy 1", "Policy 2", "Policy 3");
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ordersByOwnerTypeAndName() {
    Application app3 = tempEntity.newApplication("Application 3", "Application-3", org.getId());
    createPolicyWaiverRequest(policy, app1.getId());
    createPolicyWaiverRequest(policy, app2.getId());
    createPolicyWaiverRequest(policy, app3.getId());
    createPolicyWaiverRequest(policy, org.getId());
    createPolicyWaiverRequest(policy, Organization.ROOT_ORGANIZATION_ID);

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(), app2.getId(), app3.getId()));
    String orderBy = "-" + DashboardPolicyWaiverRequestOrderByEnum.OWNER_SCOPE;
    risksFilterDTOBuilder.withApplicationIds(apps).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(5);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).ownerName).isEqualTo("Root Organization");
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).ownerName).isEqualTo(org.getName());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(2).ownerName).isEqualTo(app3.getName());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(3).ownerName).isEqualTo(app2.getName());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(4).ownerName).isEqualTo(app1.getName());
  }

  @Test
  public void testGetDashboardPolicyWaiverRequestsForExport_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);

    risksFilterDTOBuilder.withPageSize(Integer.MAX_VALUE);
    ThrowingCallable functionCall = () -> dashboardPolicyWaiverRequestService
        .getDashboardPolicyWaiverRequestsForExport(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(functionCall);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequestsForExport_DashboardDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    risksFilterDTOBuilder.withPageSize(Integer.MAX_VALUE);
    ThrowingCallable functionCall = () -> dashboardPolicyWaiverRequestService
        .getDashboardPolicyWaiverRequestsForExport(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(functionCall)
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void testGetDashboardPolicyWaiverRequestsForExport_returnsInformationIncludingFullDetails() {
    PolicyWaiverRequest policyWaiverRequestApp2 = createPolicyWaiverRequestWithFullDetails(app2);
    PolicyWaiverRequest policyWaiverRequestApp1 = createPolicyWaiverRequest(policy, app1.getId());

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(), app2.getId()));
    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.POLICY_NAME.toString();
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withApplicationIds(apps)
        .withPageSize(Integer.MAX_VALUE)
        .withOrderBy(orderBy);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequestsForExport(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).id).isEqualTo(policyWaiverRequestApp1.getId());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).id).isEqualTo(policyWaiverRequestApp2.getId());

    assertPolicyWaiverRequestWithFullDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0),
        policyWaiverRequestApp1, app1);
    assertPolicyWaiverRequestWithFullDetails(dashboardPolicyWaiverRequests.dashboardResults.get(1),
        policyWaiverRequestApp2, app2);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_OrderByExpiryDateWhenScopeIsSame() {
    Policy policy1 =
        tempEntity.newPolicy(new TestPolicyBuilder().withSampleTestValues().withOwnerId(app1.getId()).build());

    Policy policy2 =
        tempEntity.newPolicy(new TestPolicyBuilder().withSampleTestValues().withOwnerId(app1.getId()).build());

    PolicyWaiverRequest policyWaiverRequest1App1 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy1.getId())
            .setOwnerId(app1.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 11));

    PolicyWaiverRequest policyWaiverRequest2App1 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy2.getId())
            .setOwnerId(app1.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 7));

    PolicyWaiverRequest policyWaiverRequest1App2 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy1.getId())
            .setOwnerId(app2.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 1));

    PolicyWaiverRequest policyWaiverRequest2App2 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy1.getId())
            .setOwnerId(app2.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 2));

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1App1);
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest2App1);
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1App2);
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest2App2);

    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.OWNER_SCOPE.name();
    risksFilterDTOBuilder.withOrderBy(orderBy).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).as("It should include all the waivers.").hasSize(4);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).hash)
        .as("Waiver with closest expiry date should come first")
        .isEqualTo(policyWaiverRequest2App1.getHash());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(2).hash)
        .as("Irrespective of the expiry dates, "
            + "waiver requests created on application 2 should be after all the waiver requests created on "
            + "application 1 since the first comparison should be done on OWNER scope.")
        .isEqualTo(policyWaiverRequest1App2.getHash());
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_OrderByExpiryDateWhenPolicyNameIsSame() {
    Date now = new Date();
    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder().withSampleTestValues().withName("Z-Policy-Name").withOwnerId(app1.getId()).build());

    Policy policy2 = tempEntity.newPolicy(
        new TestPolicyBuilder().withSampleTestValues().withName("A-Policy-Name").withOwnerId(app1.getId()).build());

    Policy policy3 = tempEntity.newPolicy(
        new TestPolicyBuilder().withSampleTestValues().withName("A-Policy-Name").withOwnerId(app2.getId()).build());

    PolicyWaiverRequest policyWaiverRequest1App1 = new PolicyWaiverRequest().setHash("hash1")
        .setPolicyId(policy1.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(now, 11));

    PolicyWaiverRequest policyWaiverRequest2App1 = new PolicyWaiverRequest().setHash("hash2")
        .setPolicyId(policy2.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(now, 7));

    PolicyWaiverRequest policyWaiverRequest1App2 = new PolicyWaiverRequest().setHash("hash3")
        .setPolicyId(policy3.getId())
        .setOwnerId(app2.getId())
        .setExpiryTime(DateUtils.addDays(now, 1));

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1App1);
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest2App1);
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1App2);

    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.POLICY_NAME.name();
    risksFilterDTOBuilder.withOrderBy(orderBy).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).as("It should include all the waiver requests.")
        .hasSize(3);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).hash)
        .as("Waiver request with closest expiry date should come first when two waivers have same policy name")
        .isEqualTo(policyWaiverRequest1App2.getHash());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(2).hash)
        .as("Irrespective of the expiry dates, waiver requests first should be sorted by policy names")
        .isEqualTo(policyWaiverRequest1App1.getHash());
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_shouldNotAddExpired() {
    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder().withSampleTestValues().withName("Z-Policy-Name").withOwnerId(app1.getId()).build());

    Policy policy2 = tempEntity.newPolicy(
        new TestPolicyBuilder().withSampleTestValues().withName("A-Policy-Name").withOwnerId(app1.getId()).build());

    PolicyWaiverRequest expiredPolicyWaiverRequestApp1 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy1.getId())
            .setOwnerId(app1.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 11));

    PolicyWaiverRequest activePolicyWaiverRequestApp1 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy2.getId())
            .setOwnerId(app1.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 5));

    PolicyWaiverRequest activePolicyWaiverRequestApp2 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy2.getId())
            .setOwnerId(app2.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 3));

    tempEntity.newPolicyWaiverRequest(expiredPolicyWaiverRequestApp1);
    tempEntity.newPolicyWaiverRequest(activePolicyWaiverRequestApp1);
    tempEntity.newPolicyWaiverRequest(activePolicyWaiverRequestApp2);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(
            risksFilterDTOBuilder.withExpirationDate(IN_7_DAYS).withPageSize(10).build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults)
        .as("It should not add expired waiver request to the result")
        .hasSize(2);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    List<String> actualHashList =
        dashboardPolicyWaiverRequests.dashboardResults.stream().map(dto -> dto.hash).collect(Collectors.toList());

    assertThat(actualHashList).isNotEmpty().doesNotContain(expiredPolicyWaiverRequestApp1.getHash());
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_shouldAddExpiredWhenAllFilterIsSelectedOnExpiryDate() {
    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder().withSampleTestValues().withName("Z-Policy-Name").withOwnerId(app1.getId()).build());

    Policy policy2 = tempEntity.newPolicy(
        new TestPolicyBuilder().withSampleTestValues().withName("A-Policy-Name").withOwnerId(app1.getId()).build());

    PolicyWaiverRequest expiredPolicyWaiverRequestApp1 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy1.getId())
            .setOwnerId(app1.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 11));

    PolicyWaiverRequest activePolicyWaiverRequestApp1 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy2.getId())
            .setOwnerId(app1.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 5));

    PolicyWaiverRequest activePolicyWaiverRequestApp2 =
        new PolicyWaiverRequest().setHash(TemporaryEntity.uuid().substring(0, 5))
            .setPolicyId(policy2.getId())
            .setOwnerId(app2.getId())
            .setExpiryTime(DateUtils.addDays(new Date(), 3));

    tempEntity.newPolicyWaiverRequest(expiredPolicyWaiverRequestApp1);
    tempEntity.newPolicyWaiverRequest(activePolicyWaiverRequestApp1);
    tempEntity.newPolicyWaiverRequest(activePolicyWaiverRequestApp2);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService
            .getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.withExpirationDate(ALL).withPageSize(10).build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults)
        .as("It should add expired waiver request(s) to the result if ALL expiration date filter is selected.")
        .hasSize(3);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_shouldGetAllParentWaiverRequestsFromParentOrgs() {
    Organization parentOrg1 = tempEntity.newOrganization();
    Organization parentOrg2 = tempEntity.newOrganization(parentOrg1);
    Organization parentOrg3 = tempEntity.newOrganization(parentOrg2);
    Organization parentOrg4 = tempEntity.newOrganization(parentOrg3);
    Organization org1 = tempEntity.newOrganization(parentOrg4);
    app1 = tempEntity.newApplication(org1.getId());

    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder().withSampleTestValues().withName("Z-Policy-Name").withOwnerId(app1.getId()).build());

    createPolicyWaiverRequest(policy1, Organization.ROOT_ORGANIZATION_ID);
    createPolicyWaiverRequest(policy1, parentOrg1.getId());
    createPolicyWaiverRequest(policy1, parentOrg2.getId());
    createPolicyWaiverRequest(policy1, parentOrg3.getId());
    createPolicyWaiverRequest(policy1, parentOrg4.getId());
    createPolicyWaiverRequest(policy1, org1.getId());
    createPolicyWaiverRequest(policy1, app1.getId());

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(
            risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(10).build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults)
        .as("It should get the app and all the parent orgs including the root org")
        .hasSize(7);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  private PolicyWaiverRequest createPolicyWaiverRequest(Policy policy, String ownerId) {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest().setHash("hash")
            .setPolicyId(policy.getId())
            .setOwnerId(ownerId)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setPolicyViolationId(policyViolation.getId());
    return tempEntity.newPolicyWaiverRequest(policyWaiverRequest);
  }

  private PolicyWaiverRequest createPolicyWaiverRequest(
      String hash,
      String policyId,
      String ownerId,
      String comment,
      Date expiryTime)
  {
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest().setHash(hash)
            .setPolicyId(policyId)
            .setOwnerId(ownerId)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment(comment)
            .setExpiryTime(expiryTime);
    return tempEntity.newPolicyWaiverRequest(policyWaiverRequest);
  }

  private PolicyWaiverRequest createPolicyWaiverRequestWithReason(
      String hash,
      String policyId,
      String ownerId,
      String comment,
      String reasonType,
      String reasonText)
  {
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequest().setHash(hash)
        .setPolicyId(policyId)
        .setOwnerId(ownerId)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment(comment)
        .setWaiverReasonId(newWaiverReason(reasonType, reasonText).getId());
    return tempEntity.newPolicyWaiverRequest(policyWaiverRequest);
  }

  private PolicyWaiverReason newWaiverReason(String type, String reasonText) {
    PolicyWaiverReason policyWaiverReason = new PolicyWaiverReason(type, reasonText);
    policyWaiverReasonDAO.insert(policyWaiverReason);
    return policyWaiverReason;
  }

  private PolicyWaiverRequest createPolicyWaiverRequestWithFullDetails(Application application) {
    Date today = new Date();
    Date aWeekFromNow = DateUtils.addDays(today, 7);
    Policy highThreatPolicy = tempEntity.newPolicy(application.getId(), "Very bad security threat", 9);

    TriggerReference triggerReference =
        new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID, "vulnerability-1");
    ConditionFact conditionFact = new ConditionFact(ConditionTypes.SecurityVulnerabilityStatusConditionType.getId(), 0,
        "summary", "reason", triggerReference);
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);
    TreeMap<String, String> coordinates = new TreeMap<>()
    {
      {
        this.put("artifactId", "a1");
        this.put("groupId", "g1");
        this.put("version", "v1");
        this.put("classifier", "c1");
        this.put("extension", "jar");
      }
    };
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest().setHash("hash")
            .setPolicyId(highThreatPolicy.getId())
            .setOwnerId(application.getId())
            .setConstraintFacts(singletonList(constraintFact))
            .setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("a comment")
            .setRequestTime(today)
            .setExpiryTime(aWeekFromNow)
            .setComponentUpgradeAvailable(true)
            .setPolicyViolationId(policyViolation.getId());

    return tempEntity.newPolicyWaiverRequest(policyWaiverRequest);
  }

  private void assertPolicyWaiverRequestWithoutDetails(
      DashboardPolicyWaiverRequestDTO dashboardPolicyWaiverRequestDTO,
      PolicyWaiverRequest policyWaiverRequest,
      Owner owner)
  {
    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequestDTO, policyWaiverRequest, owner, null);
  }

  private void assertPolicyWaiverRequestWithoutDetails(
      DashboardPolicyWaiverRequestDTO dashboardPolicyWaiverRequestDTO,
      PolicyWaiverRequest policyWaiverRequest,
      Owner owner,
      String customOwnerType)
  {
    assertPolicyWaiverRequestDTOBasicFields(dashboardPolicyWaiverRequestDTO, policyWaiverRequest, owner,
        customOwnerType);

    assertThat(dashboardPolicyWaiverRequestDTO.constraintFacts).isNull();
    assertThat(dashboardPolicyWaiverRequestDTO.comment).isNull();
    assertThat(dashboardPolicyWaiverRequestDTO.policyWaiverReason).isNull();
  }

  private void assertPolicyWaiverRequestDTOBasicFields(
      DashboardPolicyWaiverRequestDTO dashboardPolicyWaiverRequestDTO,
      PolicyWaiverRequest policyWaiverRequest,
      Owner owner,
      String customOwnerType)
  {
    Policy policy = policyDAO.getById(policyWaiverRequest.getPolicyId());

    assertThat(dashboardPolicyWaiverRequestDTO.id).isEqualTo(policyWaiverRequest.getId());
    assertThat(dashboardPolicyWaiverRequestDTO.threatLevel).isEqualTo(policy.getThreatLevel());
    assertThat(dashboardPolicyWaiverRequestDTO.requestTime).isEqualTo(policyWaiverRequest.getRequestTime());
    assertThat(dashboardPolicyWaiverRequestDTO.expiryTime).isEqualTo(policyWaiverRequest.getExpiryTime());
    assertThat(dashboardPolicyWaiverRequestDTO.policyId).isEqualTo(policyWaiverRequest.getPolicyId());
    assertThat(dashboardPolicyWaiverRequestDTO.policyName).isEqualTo(policy.getName());
    assertThat(dashboardPolicyWaiverRequestDTO.ownerType)
        .isEqualTo(customOwnerType != null ? customOwnerType : owner.getType().toString());
    assertThat(dashboardPolicyWaiverRequestDTO.ownerId).isEqualTo(owner.getId());
    assertThat(dashboardPolicyWaiverRequestDTO.ownerName).isEqualTo(owner.getName());
    assertThat(dashboardPolicyWaiverRequestDTO.componentMatchStrategy)
        .isEqualTo(policyWaiverRequest.getComponentMatchStrategy());
    assertThat(dashboardPolicyWaiverRequestDTO.componentUpgradeAvailable)
        .isEqualTo(policyWaiverRequest.isComponentUpgradeAvailable());
    assertThat(dashboardPolicyWaiverRequestDTO.status).isEqualTo(policyWaiverRequest.getStatus());
    assertThat(dashboardPolicyWaiverRequestDTO.requesterId).isEqualTo(policyWaiverRequest.getRequesterId());
    assertThat(dashboardPolicyWaiverRequestDTO.requesterName).isEqualTo(policyWaiverRequest.getRequesterName());

    if (policyWaiverRequest.getComponentIdentifier() != null) {
      assertThat(dashboardPolicyWaiverRequestDTO.componentIdentifier.toComponentIdentifier())
          .isEqualTo(policyWaiverRequest.getComponentIdentifier());
      assertThat(dashboardPolicyWaiverRequestDTO.getDisplayName().toString())
          .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(policyWaiverRequest.getComponentIdentifier()).toString());
    }
  }

  private void assertPolicyWaiverRequestWithFullDetails(
      DashboardPolicyWaiverRequestDTO dashboardPolicyWaiverRequestDTO,
      PolicyWaiverRequest policyWaiverRequest,
      Owner owner)
  {
    assertPolicyWaiverRequestDTOBasicFields(dashboardPolicyWaiverRequestDTO, policyWaiverRequest, owner, null);

    if (policyWaiverRequest.getConstraintFacts() != null) {
      assertThat(dashboardPolicyWaiverRequestDTO.constraintFacts)
          .hasSize(policyWaiverRequest.getConstraintFacts().size());
      for (int i = 0; i < policyWaiverRequest.getConstraintFacts().size(); i++) {
        assertThat(dashboardPolicyWaiverRequestDTO.constraintFacts.get(i).getConstraintId())
            .isEqualTo(policyWaiverRequest.getConstraintFacts().get(i).getConstraintId());
      }
    }
    assertThat(dashboardPolicyWaiverRequestDTO.comment).isEqualTo(policyWaiverRequest.getComment());
    assertThat(dashboardPolicyWaiverRequestDTO.policyWaiverReason).isEqualTo(policyWaiverRequest.getWaiverReasonId());
  }
}
