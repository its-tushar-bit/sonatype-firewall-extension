/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
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

import javax.inject.Inject;

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
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyWaiverRequestBuilder;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_GOLANG;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PYPI;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.ALL;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.IN_30_DAYS;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.IN_7_DAYS;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DashboardPolicyWaiverRequestServiceTest
    extends AbstractComponentTest
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

  @Before
  public void before() {
    parentOrg = tempEntity.newOrganization();
    org = tempEntity.newOrganization(parentOrg);
    app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    app2 = tempEntity.newApplication("Application 2", "Application-2", parentOrg.getId());
    policy = tempEntity.newPolicy(org.getId(), "A test policy");

    risksFilterDTOBuilder = new RisksFilterDTOBuilder().withApplicationIds(Collections.emptySet())
        .withOrganizationIds(Collections.emptySet()).withPageSize(1);
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
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withApplicationIds(apps)
        .withOrderBy(orderBy).withPageSize(10);
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
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy.getId())
            .setOwnerId(repository.getId()).setExpiryTime(DateUtils.addDays(new Date(), 11)).build();

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);

    PolicyWaiverRequest policyWaiverRequest1 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy.getId())
            .setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(new Date(), 11)).build();

    tempEntity.newPolicyWaiverRequest(policyWaiverRequest1);

    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repository.getId())).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    assertPolicyWaiverRequestWithoutDetails(dashboardPolicyWaiverRequests.dashboardResults.get(0), policyWaiverRequest,
        repository);

    PolicyWaiverRequest policyWaiverRequest2 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy.getId())
            .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID).setExpiryTime(DateUtils.addDays(new Date(), 5))
            .build();

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
    PolicyWaiverRequest policyWaiverRequestApp1 = new PolicyWaiverRequestBuilder().setHash("testHash")
        .setPolicyId(policy.getId()).setOwnerId(app1.getId()).build();
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestApp1);

    // add app 3 waiver request
    Organization org2 = tempEntity.newOrganization("Org3");
    Application application3 = tempEntity.newApplication("Application-3", " Applicatin-3", org2.getId());
    PolicyWaiverRequest policyWaiverRequestOrg2 = new PolicyWaiverRequestBuilder().setHash("testHash")
        .setPolicyId(policy.getId()).setOwnerId(application3.getId()).build();
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestOrg2);

    // add waiver request for empty org
    Organization org3 = tempEntity.newOrganization("Org4");
    PolicyWaiverRequest policyWaiverRequestOrg3 = new PolicyWaiverRequestBuilder().setHash("testHash")
        .setPolicyId(policy.getId()).setOwnerId(org3.getId()).build();
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestOrg3);

    // add waiver request for repo container
    PolicyWaiverRequest policyWaiverRequestRepoContainer = new PolicyWaiverRequestBuilder().setHash("testHash")
        .setPolicyId(policy.getId()).setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID).build();
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestRepoContainer);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService
            .getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.withPageSize(10).build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.id).containsExactlyInAnyOrder(
        policyWaiverRequestApp1.getId(), policyWaiverRequestOrg2.getId(), policyWaiverRequestOrg3.getId(),
        policyWaiverRequestRepoContainer.getId());
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    // added afterwards to make sure that repo container still shows even with no repo waiver requests
    // add waiver request for repo manager
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    PolicyWaiverRequest policyWaiverRequestRepoManager = new PolicyWaiverRequestBuilder().setHash("testHash")
        .setPolicyId(policy.getId()).setOwnerId(repoManager.getId()).build();
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestRepoManager);

    // add repo waiver request
    Repository repository = tempEntity.newRepository();
    PolicyWaiverRequest policyWaiverRequestRepo = new PolicyWaiverRequestBuilder().setHash("testHash")
        .setPolicyId(policy.getId()).setOwnerId(repository.getId()).build();
    tempEntity.newPolicyWaiverRequest(policyWaiverRequestRepo);

    dashboardPolicyWaiverRequests = dashboardPolicyWaiverRequestService
        .getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.withPageSize(10).build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.id).containsExactlyInAnyOrder(
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
        .withPolicyThreatCategories(threatCategoryFilter).withPageSize(10);
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
        .withPolicyThreatLevelRange(threatLevelFilter).withPageSize(10);
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
        .withExpirationDate(IN_7_DAYS).withPageSize(10);
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
  public void testGetDashboardPolicyWaiverRequests_ordersByCreateTime() {
    Date now = new Date();
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId());
      Date waiverRequestDate = DateUtils.addDays(now, -value);
      PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequestBuilder().setHash("hash")
          .setPolicyId(testPolicy.getId()).setOwnerId(app1.getId()).setRequestTime(waiverRequestDate).build();
      tempEntity.newPolicyWaiverRequest(policyWaiverRequest);
    };
    IntStream.rangeClosed(0, 4).forEach(intConsumer);

    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.CREATION_DATE.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(5);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).requestTime).isEqualTo(DateUtils.addDays(now, -4));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).requestTime).isEqualTo(DateUtils.addDays(now, -3));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(2).requestTime).isEqualTo(DateUtils.addDays(now, -2));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(3).requestTime).isEqualTo(DateUtils.addDays(now, -1));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(4).requestTime).isEqualTo(now);
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

    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.STATUS.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(3);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).status)
        .isEqualTo(PolicyWaiverRequestStatus.APPROVED);
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).status)
        .isEqualTo(PolicyWaiverRequestStatus.REJECTED);
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(2).status)
        .isEqualTo(PolicyWaiverRequestStatus.REQUESTED);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ordersByPolicyName() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId(), "Policy with ordered name " + value);
      createPolicyWaiverRequest(testPolicy, org.getId());
    };
    IntStream.rangeClosed(0, 4).forEach(intConsumer);

    String orderBy = "-" + DashboardPolicyWaiverRequestOrderByEnum.POLICY_NAME;
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(5);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();

    IntConsumer assertConsumer =
        value -> assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(4 - value).policyName)
            .isEqualTo("Policy with ordered name " + value);
    IntStream.rangeClosed(0, 4).forEach(assertConsumer);
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
  public void testGetDashboardPolicyWaiverRequests_ordersByComponentDisplayNameWithMatchStrategy() {
    String[] chars = "abc".split("");
    String[] formats = {FORMAT_MAVEN, FORMAT_PYPI, FORMAT_GOLANG};
    ArrayList<ComponentMatcherStrategyForWaiver> waiverTypes = new ArrayList<>()
    {
      {
        this.add(EXACT_COMPONENT);
        this.add(EXACT_COMPONENT);
        this.add(ALL_VERSIONS);
        this.add(ALL_COMPONENTS);
        this.add(ALL_COMPONENTS);
        this.add(EXACT_COMPONENT);
        this.add(EXACT_COMPONENT);
      }
    };
    ArrayList<ComponentIdentifier> componentIdentifiers = new ArrayList<>();

    IntStream.range(0, waiverTypes.size()).forEachOrdered(i -> {
      TreeMap<String, String> coordinates = new TreeMap<>();
      Policy testPolicy = tempEntity.newPolicy(org.getId(), "Policy with ordered name " + i);
      ComponentMatcherStrategyForWaiver type = waiverTypes.get(i);
      if (i < chars.length) {
        String format = formats[i % formats.length];
        switch (format) {
          case FORMAT_GOLANG:
          case FORMAT_PYPI:
            coordinates.put("name", chars[i] + (i + 1));
            coordinates.put("version", "v1");
            break;
          default:
            coordinates.put("artifactId", chars[i] + (i + 1));
            coordinates.put("groupId", chars[i] + (i + 1));
            coordinates.put("version", "v1");
            format = FORMAT_MAVEN;
            break;
        }
        ComponentIdentifier componentIdentifier = new ComponentIdentifier(format, coordinates);
        componentIdentifiers.add(componentIdentifier);
        String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
        tempEntity
            .newPolicyWaiverRequest(new PolicyWaiverRequestBuilder().setHash("hash" + 1).setPolicyId(testPolicy.getId())
                .setOwnerId(org.getId()).setComponentMatchStrategy(type).setAssociatedPackageUrl(purl).build());
      }
      else {
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequestBuilder().setHash(null).setPolicyId(testPolicy.getId())
            .setOwnerId(org.getId()).setComponentMatchStrategy(type).setAssociatedPackageUrl(null).build());
      }
    });

    String orderBy = DashboardPolicyWaiverRequestOrderByEnum.COMPONENT_SCOPE.toString();
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(7);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(
        dashboardPolicyWaiverRequests.dashboardResults.get(0).componentIdentifier.toComponentIdentifier().toString())
            .isEqualTo(componentIdentifiers.get(0).toString());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(0));
    assertThat(
        dashboardPolicyWaiverRequests.dashboardResults.get(1).componentIdentifier.toComponentIdentifier().toString())
            .isEqualTo(componentIdentifiers.get(1).toString());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(1));
    assertThat(
        dashboardPolicyWaiverRequests.dashboardResults.get(2).componentIdentifier.toComponentIdentifier().toString())
            .isEqualTo(componentIdentifiers.get(2).toString());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(2).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(2));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(3).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(3).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(3));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(4).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(4).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(4));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(5).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(5).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(5));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(6).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(6).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(6));

    orderBy = "-" + DashboardPolicyWaiverRequestOrderByEnum.COMPONENT_SCOPE;
    risksFilterDTOBuilder.withOrderBy(orderBy);
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults).hasSize(7);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
    assertThat(
        dashboardPolicyWaiverRequests.dashboardResults.get(0).componentIdentifier.toComponentIdentifier().toString())
            .isEqualTo(componentIdentifiers.get(2).toString());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(0).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(2));
    assertThat(
        dashboardPolicyWaiverRequests.dashboardResults.get(1).componentIdentifier.toComponentIdentifier().toString())
            .isEqualTo(componentIdentifiers.get(1).toString());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(1).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(1));
    assertThat(
        dashboardPolicyWaiverRequests.dashboardResults.get(2).componentIdentifier.toComponentIdentifier().toString())
            .isEqualTo(componentIdentifiers.get(0).toString());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(2).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(0));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(3).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(3).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(3));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(4).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(4).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(4));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(5).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(5).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(5));
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(6).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaiverRequests.dashboardResults.get(6).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(6));
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
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withApplicationIds(apps)
        .withPageSize(Integer.MAX_VALUE).withOrderBy(orderBy);
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
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy1.getId())
            .setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(new Date(), 11)).build();

    PolicyWaiverRequest policyWaiverRequest2App1 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy2.getId())
            .setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(new Date(), 7)).build();

    PolicyWaiverRequest policyWaiverRequest1App2 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy1.getId())
            .setOwnerId(app2.getId()).setExpiryTime(DateUtils.addDays(new Date(), 1)).build();

    PolicyWaiverRequest policyWaiverRequest2App2 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy1.getId())
            .setOwnerId(app2.getId()).setExpiryTime(DateUtils.addDays(new Date(), 2)).build();

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
        .as("Waiver with closest expiry date should come first").isEqualTo(policyWaiverRequest2App1.getHash());

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

    PolicyWaiverRequest policyWaiverRequest1App1 = new PolicyWaiverRequestBuilder().setHash("hash1")
        .setPolicyId(policy1.getId()).setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(now, 11)).build();

    PolicyWaiverRequest policyWaiverRequest2App1 = new PolicyWaiverRequestBuilder().setHash("hash2")
        .setPolicyId(policy2.getId()).setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(now, 7)).build();

    PolicyWaiverRequest policyWaiverRequest1App2 = new PolicyWaiverRequestBuilder().setHash("hash3")
        .setPolicyId(policy3.getId()).setOwnerId(app2.getId()).setExpiryTime(DateUtils.addDays(now, 1)).build();

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
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy1.getId())
            .setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(new Date(), 11)).build();

    PolicyWaiverRequest activePolicyWaiverRequestApp1 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy2.getId())
            .setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(new Date(), 5)).build();

    PolicyWaiverRequest activePolicyWaiverRequestApp2 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy2.getId())
            .setOwnerId(app2.getId()).setExpiryTime(DateUtils.addDays(new Date(), 3)).build();

    tempEntity.newPolicyWaiverRequest(expiredPolicyWaiverRequestApp1);
    tempEntity.newPolicyWaiverRequest(activePolicyWaiverRequestApp1);
    tempEntity.newPolicyWaiverRequest(activePolicyWaiverRequestApp2);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(
            risksFilterDTOBuilder.withExpirationDate(IN_7_DAYS).withPageSize(10).build());

    assertThat(dashboardPolicyWaiverRequests.dashboardResults)
        .as("It should not add expired waiver request to the result").hasSize(2);
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
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy1.getId())
            .setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(new Date(), 11)).build();

    PolicyWaiverRequest activePolicyWaiverRequestApp1 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy2.getId())
            .setOwnerId(app1.getId()).setExpiryTime(DateUtils.addDays(new Date(), 5)).build();

    PolicyWaiverRequest activePolicyWaiverRequestApp2 =
        new PolicyWaiverRequestBuilder().setHash(TemporaryEntity.uuid().substring(0, 5)).setPolicyId(policy2.getId())
            .setOwnerId(app2.getId()).setExpiryTime(DateUtils.addDays(new Date(), 3)).build();

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
        .as("It should get the app and all the parent orgs including the root org").hasSize(7);
    assertThat(dashboardPolicyWaiverRequests.hasNextPage).isFalse();
  }

  private PolicyWaiverRequest createPolicyWaiverRequest(Policy policy, String ownerId) {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequestBuilder().setHash("hash").setPolicyId(policy.getId()).setOwnerId(ownerId)
            .setComponentMatchStrategy(EXACT_COMPONENT).setPolicyViolationId(policyViolation.getId()).build();
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
        new PolicyWaiverRequestBuilder().setHash(hash).setPolicyId(policyId).setOwnerId(ownerId)
            .setComponentMatchStrategy(EXACT_COMPONENT).setComment(comment).setExpiryTime(expiryTime).build();
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
    PolicyWaiverRequest policyWaiverRequest = new PolicyWaiverRequestBuilder().setHash(hash).setPolicyId(policyId)
        .setOwnerId(ownerId).setComponentMatchStrategy(EXACT_COMPONENT).setComment(comment)
        .setWaiverReasonId(newWaiverReason(reasonType, reasonText).getId()).build();
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
        new PolicyWaiverRequestBuilder().setHash("hash").setPolicyId(highThreatPolicy.getId())
            .setOwnerId(application.getId()).setConstraintFacts(singletonList(constraintFact))
            .setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT).setComment("a comment")
            .setRequestTime(today).setExpiryTime(aWeekFromNow).setComponentUpgradeAvailable(true)
            .setPolicyViolationId(policyViolation.getId()).build();

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
