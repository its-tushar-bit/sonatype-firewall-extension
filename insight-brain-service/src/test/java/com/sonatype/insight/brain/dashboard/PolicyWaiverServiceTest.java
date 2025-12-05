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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.RisksFilterDTOBuilder;
import com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper;
import com.sonatype.insight.brain.builders.TestPolicyBuilder;
import com.sonatype.insight.brain.dashboard.DashboardPolicyWaiverDTOComparator.DashboardPolicyWaiverOrderByEnum;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyWaiverResource;
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
import org.mockito.Mockito;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PolicyWaiverServiceTest
    extends AbstractComponentTest
{
  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private Organization parentOrg;

  private Organization org;

  private Application app1;

  private Application app2;

  private Policy policy;

  private RisksFilterDTOBuilder risksFilterDTOBuilder;

  @Inject
  private PolicyWaiverService dashboardPolicyWaiverService;

  @Before
  public void before() {
    parentOrg = tempEntity.newOrganization();
    org = tempEntity.newOrganization(parentOrg);
    app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    app2 = tempEntity.newApplication("Application 2", "Application-2", parentOrg.getId());
    policy = tempEntity.newPolicy(org);

    risksFilterDTOBuilder = new RisksFilterDTOBuilder().withApplicationIds(Collections.emptySet())
        .withOrganizationIds(Collections.emptySet()).withPageSize(1);
  }

  @Test
  public void testGetDashboardPolicyWaivers_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);
    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(functionCall);
  }

  @Test
  public void testGetDashboardPolicyWaivers_worksWhenMissingWaiversDashboardFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.WAIVERS_DASHBOARD);

    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org);
      tempEntity.newWaiver(testPolicy.getId(), app1.getId());
    };
    IntStream.range(0, 10).forEach(intConsumer);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(2);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(true);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(2);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(3).withPage(1);
    dashboardPolicyWaivers = dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(true);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(3);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(10).withPage(1);
    dashboardPolicyWaivers = dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_worksWhenMissingDashboardFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);

    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org);
      tempEntity.newWaiver(testPolicy.getId(), app1.getId());
    };
    IntStream.range(0, 10).forEach(intConsumer);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(2);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(true);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(2);
  }

  @Test
  public void testGetDashboardPolicyWaiversForExport_worksWhenMissingWaiversDashboardFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.WAIVERS_DASHBOARD);

    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org);
      tempEntity.newWaiver(testPolicy.getId(), app1.getId());
    };
    IntStream.range(0, 10).forEach(intConsumer);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(2);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaiversForExport(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(true);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(2);
  }

  @Test
  public void testGetDashboardPolicyWaiversForExport_worksWhenMissingDashboardFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);

    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org);
      tempEntity.newWaiver(testPolicy.getId(), app1.getId());
    };
    IntStream.range(0, 10).forEach(intConsumer);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(2);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaiversForExport(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(true);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(2);
  }

  @Test
  public void testGetDashboardPolicyWaivers_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(functionCall)
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void testGetDashboardPolicyWaivers_returnsInformationWithoutExtraDetails() {
    PolicyWaiver policyWaiverApp1 = tempEntity.newWaiver(policy.getId(), app1.getId());
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(), app2.getId()));
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withApplicationIds(apps).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).id).isEqualTo(policyWaiverApp1.getId());

    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(1), policyWaiverApp1, app1);
  }

  @Test
  public void testGetDashboardPolicyWaivers_shouldReturnOnlyPageSize() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org);
      tempEntity.newWaiver(testPolicy.getId(), app1.getId());
    };
    IntStream.range(0, 10).forEach(intConsumer);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(2);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(true);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(2);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByOrganization() {
    Organization excludedOrganization = tempEntity.newOrganization();
    Policy excludedPolicy = tempEntity.newPolicy(excludedOrganization);
    tempEntity.newWaiver(excludedPolicy.getId(), excludedOrganization.getId());
    PolicyWaiver policyWaiverApp1 = tempEntity.newWaiver(policy.getId(), org.getId());
    PolicyWaiver policyWaiverParentOrg1 = tempEntity.newWaiver(policy.getId(), parentOrg.getId());
    createPolicyWaiverWithFullDetails(app2);

    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp1, org);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(1), policyWaiverParentOrg1, parentOrg);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByApplication() {
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    PolicyWaiver policyWaiverParentOrg1 = tempEntity.newWaiver(policy.getId(), parentOrg.getId());
    tempEntity.newWaiver(policy.getId(), app1.getId());

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId())).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(1), policyWaiverParentOrg1, parentOrg);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByRepository() {
    Repository repository = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    PolicyWaiver policyWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repository.getId());

    tempEntity.newWaiver(policyWaiver);

    PolicyWaiver policyWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(app1.getId());

    tempEntity.newWaiver(policyWaiver1);

    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repository.getId())).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiver, repository);

    PolicyWaiver policyWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    tempEntity.newWaiver(policyWaiver2);

    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(RepositoryContainer.REPOSITORY_CONTAINER_ID));

    dashboardPolicyWaivers = dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    assertPolicyWaiverWithoutDetails(
        dashboardPolicyWaivers.dashboardResults.get(0),
        policyWaiver2,
        RepositoryContainer.SINGLETON,
        "all_repositories");
  }

  @Test
  public void testGetDashboardPolicyWaivers_returnAllWaivers() {
    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    // add app 1 waiver
    PolicyWaiver policyWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(app1.getId());
    tempEntity.newWaiver(policyWaiver1);

    // add app 3 waiver
    Organization org2 = tempEntity.newOrganization("Org3");
    Application application3 = tempEntity.newApplication("Application-3", " Applicatin-3", org2.getId());
    PolicyWaiver policyWaiverOrg2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(application3.getId());
    tempEntity.newWaiver(policyWaiverOrg2);

    // add waiver for empty org
    Organization org3 = tempEntity.newOrganization("Org4");
    PolicyWaiver policyWaiverOrg3 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(org3.getId());
    tempEntity.newWaiver(policyWaiverOrg3);

    // add waiver for repo container
    PolicyWaiver policyWaiverRepoContainer = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    tempEntity.newWaiver(policyWaiverRepoContainer);

    PolicyWaiver policyWaiverRepoContainerComponent = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .setForContainerImageComponent(true);
    tempEntity.newWaiver(policyWaiverRepoContainerComponent);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.withPageSize(10).build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(4);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    // added afterwards to make sure that repo container still shows even with no repo waivers

    Repository repository = tempEntity.newRepository();

    // add repo waiver
    PolicyWaiver policyWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repository.getId());
    tempEntity.newWaiver(policyWaiver);

    RepositoryComponent
        component = tempEntity.newRepositoryComponent(repository.getId());

    PolicyViolationTestHelper.createPolicyViolationWaived(policy, component, tempEntity);

    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.withPageSize(10).build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(5);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByApplicationCategories() {
    Tag applicationCategory = tempEntity.newTag(org.getId());
    Application categorizedApplication = tempEntity.newApplication("categorizedApplication", org.getId());
    tempEntity.newApplicationTag(categorizedApplication.getId(), applicationCategory.getId());

    PolicyWaiver categorizedAppWaiver = tempEntity.newWaiver(policy.getId(), categorizedApplication.getId());
    tempEntity.newWaiver(policy.getId(), app1.getId());
    tempEntity.newWaiver(policy.getId(), app2.getId());

    risksFilterDTOBuilder.withTagIds(Collections.singleton(applicationCategory.getId())).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), categorizedAppWaiver,
        categorizedApplication);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()));
    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(0);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByPolicyTypes() {
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    Policy nonSecurityPolicy = tempEntity.newPolicy();
    tempEntity.newWaiver(nonSecurityPolicy.getId(), app1.getId());

    PolicyThreatCategoryFilter threatCategoryFilter = new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY);
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()))
        .withPolicyThreatCategories(threatCategoryFilter).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByThreatLevel() {
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    Policy lowThreatLevelPolicy = policy;
    tempEntity.newWaiver(lowThreatLevelPolicy.getId(), app1.getId());

    PolicyThreatLevelFilter threatLevelFilter = new PolicyThreatLevelFilter(7, 9);
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()))
        .withPolicyThreatLevelRange(threatLevelFilter).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByExpirationDate() {
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    Date now = new Date();
    PolicyWaiver oneMonthExpiringWaiver = tempEntity.newWaiver("hash1", policy.getId(), app1.getId(), "",
        DateUtils.addDays(now, 30));
    PolicyWaiver neverExpiringWaiver = tempEntity.newWaiver("hash3", policy.getId(), app1.getId(), "", null);
    tempEntity.newWaiver("hash2", policy.getId(), app1.getId(), "", DateUtils.addDays(now, 40));

    risksFilterDTOBuilder.withApplicationIds(new HashSet<>(Arrays.asList(app1.getId(), app2.getId())))
        .withExpirationDate(IN_7_DAYS).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).expiryTime)
        .isBeforeOrEqualTo(DateUtils.addDays(now, IN_7_DAYS.getDays()));
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);

    risksFilterDTOBuilder.withExpirationDate(IN_30_DAYS);
    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).id).isEqualTo(oneMonthExpiringWaiver.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).expiryTime)
        .isBeforeOrEqualTo(DateUtils.addDays(now, IN_30_DAYS.getDays()));
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(1), oneMonthExpiringWaiver, app1);

    risksFilterDTOBuilder.withExpirationDate(NEVER);
    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(1);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(neverExpiringWaiver.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).expiryTime).isNull();
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), neverExpiringWaiver, app1);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByPolicyWaiverReasonIds() {
    // === Given ===
    final var policy = tempEntity.newPolicy(app1.getId(), "some-violation", 9);

    // reason 1
    final var policyWaiver1 = tempEntity.newWaiverWithReason(
        "some-hash-1",
        policy.getId(),
        app1.getId(),
        List.of(),
        "some-comment-1",
        "user",
        "a-reason-for-the-waiver-1"
    );

    // reason 2
    final var policyWaiver2 = tempEntity.newWaiverWithReason(
        "some-hash-2",
        policy.getId(),
        app1.getId(),
        List.of(),
        "some-comment-2",
        "user",
        "a-reason-for-the-waiver-2"
    );

    // no reason given
    final var policyWaiver3 = tempEntity.newWaiver(
        "some-hash-3",
        policy.getId(),
        app1.getId(),
        List.of(),
        "some-comment-2"
    );

    // === When ===
    final var filter = new RisksFilterDTO();
    filter.policyWaiverReasonIds = Sets.newHashSet(policyWaiver1.getWaiverReasonId());
    final var filteredByReason1 = dashboardPolicyWaiverService.getDashboardPolicyWaivers(filter);

    filter.policyWaiverReasonIds = Sets.newHashSet(policyWaiver2.getWaiverReasonId());
    final var filteredByReason2 = dashboardPolicyWaiverService.getDashboardPolicyWaivers(filter);

    filter.policyWaiverReasonIds = Sets.newHashSet(
        policyWaiver1.getWaiverReasonId(), policyWaiver2.getWaiverReasonId());
    final var filteredByReasons1And2 = dashboardPolicyWaiverService.getDashboardPolicyWaivers(filter);

    filter.policyWaiverReasonIds = Sets.newHashSet("no-reason");
    final var filterByNoReasonGiven = dashboardPolicyWaiverService.getDashboardPolicyWaivers(filter);

    filter.policyWaiverReasonIds = Sets.newHashSet();
    final var filterByEmptyReasonList = dashboardPolicyWaiverService.getDashboardPolicyWaivers(filter);

    filter.policyWaiverReasonIds = null;
    final var filterByNullReasonList = dashboardPolicyWaiverService.getDashboardPolicyWaivers(filter);

    // === Then ===
    assertThat(filteredByReason1.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiver1.getPolicyId());

    assertThat(filteredByReason2.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiver2.getPolicyId());

    assertThat(filteredByReasons1And2.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiver1.getPolicyId(), policyWaiver2.getPolicyId());

    assertThat(filterByNoReasonGiven.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(policyWaiver3.getPolicyId());

    assertThat(filterByEmptyReasonList.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(
            policyWaiver1.getPolicyId(),
            policyWaiver2.getPolicyId(),
            policyWaiver3.getPolicyId());

    assertThat(filterByNullReasonList.dashboardResults.stream().map(entry -> entry.policyId))
        .containsExactlyInAnyOrder(
            policyWaiver1.getPolicyId(),
            policyWaiver2.getPolicyId(),
            policyWaiver3.getPolicyId());
  }

  @Test
  public void testGetDashboardPolicyWaivers_defaultOrderByExpiryTimeAndThreatLevel() {
    PolicyWaiver policyWaiverApp1 = tempEntity.newWaiver(policy.getId(), app1.getId());
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    Date waiverExpirationDate = DateUtils.addDays(new Date(), 2);
    PolicyWaiver policyWaiverNearestExpire =
        tempEntity.newWaiver("hash", policy.getId(), app1.getId(), "", waiverExpirationDate);

    risksFilterDTOBuilder.withApplicationIds(new HashSet<>(Arrays.asList(app1.getId(), app2.getId())))
        .withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(3);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverNearestExpire.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).id).isEqualTo(policyWaiverApp2.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).id).isEqualTo(policyWaiverApp1.getId());
  }

  @Test
  public void testGetDashboardPolicyWaivers_ordersByThreatLevel() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId(), "policy with threat " + value, value);
      tempEntity.newWaiver(testPolicy.getId(), app1.getId());
    };
    IntStream.rangeClosed(1, 9).forEach(intConsumer);

    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.THREAT_LEVEL;
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(9);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    IntConsumer assertConsumer =
        value -> assertThat(dashboardPolicyWaivers.dashboardResults.get(9 - value).threatLevel).isEqualTo(value);
    IntStream.rangeClosed(1, 9).forEach(assertConsumer);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ordersByCreateTime() {
    Date now = new Date();
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId());
      Date waiverCreateDate = DateUtils.addDays(now, -value);
      tempEntity.newWaiver("hash", testPolicy.getId(), app1.getId(), null, "", waiverCreateDate);
    };
    IntStream.rangeClosed(0, 4).forEach(intConsumer);

    String orderBy = DashboardPolicyWaiverOrderByEnum.CREATION_DATE.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(5);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).createTime).isEqualTo(DateUtils.addDays(now, -4));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).createTime).isEqualTo(DateUtils.addDays(now, -3));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).createTime).isEqualTo(DateUtils.addDays(now, -2));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).createTime).isEqualTo(DateUtils.addDays(now, -1));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).createTime).isEqualTo(now);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ordersByExpiryTime() {
    Date now = new Date();
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId());
      Date waiverExpirationDate = DateUtils.addDays(now, value);
      tempEntity.newWaiver("hash", testPolicy.getId(), app1.getId(), "", waiverExpirationDate);
    };
    IntStream.rangeClosed(0, 4).forEach(intConsumer);

    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.EXPIRATION_DATE;
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(5);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).expiryTime).isEqualTo(DateUtils.addDays(now, 4));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).expiryTime).isEqualTo(DateUtils.addDays(now, 3));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).expiryTime).isEqualTo(DateUtils.addDays(now, 2));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).expiryTime).isEqualTo(DateUtils.addDays(now, 1));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).expiryTime).isEqualTo(now);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ordersByPolicyName() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId(), "Policy with ordered name " + value);
      tempEntity.newWaiver(testPolicy.getId(), org.getId());
    };
    IntStream.rangeClosed(0, 4).forEach(intConsumer);

    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.POLICY_NAME;
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(5);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    IntConsumer assertConsumer =
        value -> assertThat(dashboardPolicyWaivers.dashboardResults.get(4 - value).policyName).isEqualTo(
            "Policy with ordered name " + value);
    IntStream.rangeClosed(0, 4).forEach(assertConsumer);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ordersByOwnerTypeAndName() {
    Application app3 = tempEntity.newApplication("Application 3", "Application-3", org.getId());
    tempEntity.newWaiver(policy.getId(), app1.getId());
    tempEntity.newWaiver(policy.getId(), app2.getId());
    tempEntity.newWaiver(policy.getId(), app3.getId());
    tempEntity.newWaiver(policy.getId(), org.getId());
    tempEntity.newWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(), app2.getId(), app3.getId()));
    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE;
    risksFilterDTOBuilder.withApplicationIds(apps).withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(5);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).ownerName).isEqualTo("Root Organization");
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).ownerName).isEqualTo(org.getName());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).ownerName).isEqualTo(app3.getName());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).ownerName).isEqualTo(app2.getName());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).ownerName).isEqualTo(app1.getName());
  }

  @Test
  public void testGetDashboardPolicyWaivers_ordersByComponentDisplayNameWithMatchStrategy() {
    String[] chars = "abc".split("");
    String[] formats = {FORMAT_MAVEN, FORMAT_PYPI, FORMAT_GOLANG};
    List<ComponentMatcherStrategyForWaiver> waiverTypes = List.of(EXACT_COMPONENT, EXACT_COMPONENT, ALL_VERSIONS,
        ALL_COMPONENTS, ALL_COMPONENTS, EXACT_COMPONENT, EXACT_COMPONENT);
    List<ComponentIdentifier> componentIdentifiers = new ArrayList<>();

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
        String purl = PackageUrlIdentifier
            .fromComponentIdentifier(componentIdentifier).getPackageUrl();
        tempEntity.newWaiver("hash" + 1, testPolicy.getId(), org.getId(), null, purl, type, null);
      }
      else {
        tempEntity.newWaiver(null, testPolicy.getId(), org.getId(), null, type, null);
      }
    });

    String orderBy = DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE.toString();
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withOrderBy(orderBy).withPageSize(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(7);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).componentIdentifier.toComponentIdentifier().toString())
        .isEqualTo(componentIdentifiers.get(0).toString());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(0));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).componentIdentifier.toComponentIdentifier().toString())
        .isEqualTo(componentIdentifiers.get(1).toString());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(1));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).componentIdentifier.toComponentIdentifier().toString())
        .isEqualTo(componentIdentifiers.get(2).toString());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(2));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(3));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(4));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(5).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaivers.dashboardResults.get(5).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(5));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(6).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaivers.dashboardResults.get(6).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(6));

    orderBy = "-" + DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE;
    risksFilterDTOBuilder.withOrderBy(orderBy);
    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(7);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).componentIdentifier.toComponentIdentifier().toString())
        .isEqualTo(componentIdentifiers.get(2).toString());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(2));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).componentIdentifier.toComponentIdentifier().toString())
        .isEqualTo(componentIdentifiers.get(1).toString());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(1));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).componentIdentifier.toComponentIdentifier().toString())
        .isEqualTo(componentIdentifiers.get(0).toString());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(0));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(3));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(4));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(5).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaivers.dashboardResults.get(5).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(5));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(6).componentIdentifier).isNull();
    assertThat(dashboardPolicyWaivers.dashboardResults.get(6).componentMatchStrategy)
        .isEqualTo(waiverTypes.get(6));
  }

  @Test
  public void testGetDashboardPolicyWaiversForExport_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD, LicensedFeature.WAIVERS_DASHBOARD);

    risksFilterDTOBuilder.withPageSize(Integer.MAX_VALUE);
    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverService.getDashboardPolicyWaiversForExport(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(functionCall);
  }

  @Test
  public void testGetDashboardPolicyWaiversForExport_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    risksFilterDTOBuilder.withPageSize(Integer.MAX_VALUE);
    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverService.getDashboardPolicyWaiversForExport(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(functionCall)
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void testGetDashboardPolicyWaiversForExport_returnsInformationIncludingFullDetails() {
    PolicyWaiver policyWaiverApp1 = tempEntity.newWaiver(policy.getId(), app1.getId());
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(), app2.getId()));
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withApplicationIds(apps).withPageSize(Integer.MAX_VALUE);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaiversForExport(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).id).isEqualTo(policyWaiverApp1.getId());

    assertPolicyWaiverWithFullDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
    assertPolicyWaiverWithFullDetails(dashboardPolicyWaivers.dashboardResults.get(1), policyWaiverApp1, app1);
  }

  @Test
  public void testGetDashboardPolicyWaivers_OrderByExpiryDateWhenScopeIsSame() {
    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(app1.getId())
            .build());

    Policy policy2 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(app1.getId())
            .build());

    PolicyWaiver policyWaiver1App1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy1.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 11));

    PolicyWaiver policyWaiver2App1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy2.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 7));

    PolicyWaiver policyWaiver1App2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy1.getId())
        .setOwnerId(app2.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 1));

    PolicyWaiver policyWaiver2App2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy1.getId())
        .setOwnerId(app2.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 2));

    tempEntity.newWaiver(policyWaiver1App1);
    tempEntity.newWaiver(policyWaiver2App1);
    tempEntity.newWaiver(policyWaiver1App2);
    tempEntity.newWaiver(policyWaiver2App2);

    String orderBy = DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE.name();
    risksFilterDTOBuilder
        .withOrderBy(orderBy).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults)
        .as("It should include all the waivers.")
        .hasSize(4);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).hash)
        .as("Waiver with closest expiry date should come first")
        .isEqualTo(policyWaiver2App1.getHash());

    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).hash)
        .as("Irrespective of the expiry dates, " +
            "waivers created on application 2 should be after all the waivers created on application 1 " +
            "since the first comparison should be done on OWNER scope.")
        .isEqualTo(policyWaiver1App2.getHash());
  }

  @Test
  public void testGetDashboardPolicyWaivers_OrderByExpiryDateWhenPolicyNameIsSame() {
    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withName("Z-Policy-Name")
            .withOwnerId(app1.getId())
            .build());

    Policy policy2 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withName("A-Policy-Name")
            .withOwnerId(app1.getId())
            .build());

    Policy policy3 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withName("A-Policy-Name")
            .withOwnerId(app2.getId())
            .build());

    PolicyWaiver policyWaiver1App1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy1.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 11));

    PolicyWaiver policyWaiver2App1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy2.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 7));

    PolicyWaiver policyWaiver1App2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy3.getId())
        .setOwnerId(app2.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 1));

    tempEntity.newWaiver(policyWaiver1App1);
    tempEntity.newWaiver(policyWaiver2App1);
    tempEntity.newWaiver(policyWaiver1App2);

    String orderBy = DashboardPolicyWaiverOrderByEnum.POLICY_NAME.name();
    risksFilterDTOBuilder
        .withOrderBy(orderBy).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults)
        .as("It should include all the waivers.")
        .hasSize(3);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).hash)
        .as("Waiver with closest expiry date should come first when two waivers have same policy name")
        .isEqualTo(policyWaiver1App2.getHash());

    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).hash)
        .as("Irrespective of the expiry dates, " +
            "waivers first should be sorted by policy names ")
        .isEqualTo(policyWaiver1App1.getHash());
  }

  @Test
  public void testGetDashboardPolicyWaivers_shouldNotAddExpiredWaivers() {
    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withName("Z-Policy-Name")
            .withOwnerId(app1.getId())
            .build());

    Policy policy2 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withName("A-Policy-Name")
            .withOwnerId(app1.getId())
            .build());

    PolicyWaiver expiredPolicyWaiverApp1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy1.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), -11));

    PolicyWaiver activePolicyWaiverApp1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy2.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 5));

    PolicyWaiver activePolicyWaiverApp2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy2.getId())
        .setOwnerId(app2.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 3));

    tempEntity.newWaiver(expiredPolicyWaiverApp1);
    tempEntity.newWaiver(activePolicyWaiverApp1);
    tempEntity.newWaiver(activePolicyWaiverApp2);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(
            risksFilterDTOBuilder
                .withExpirationDate(IN_7_DAYS)
                .withPageSize(10)
                .build());

    assertThat(dashboardPolicyWaivers.dashboardResults)
        .as("It should not add expired waiver to the result")
        .hasSize(2);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    List<String> actualHashList =
        dashboardPolicyWaivers.dashboardResults
            .stream()
            .map(dto -> dto.hash)
            .collect(Collectors.toList());

    assertThat(actualHashList)
        .isNotEmpty()
        .doesNotContain(expiredPolicyWaiverApp1.getHash());
  }

  @Test
  public void testGetDashboardPolicyWaivers_shouldAddExpiredWaiversWhenAllFilterIsSelectedOnExpiryDate() {
    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withName("Z-Policy-Name")
            .withOwnerId(app1.getId())
            .build());

    Policy policy2 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withName("A-Policy-Name")
            .withOwnerId(app1.getId())
            .build());

    PolicyWaiver expiredPolicyWaiverApp1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy1.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), -11));

    PolicyWaiver activePolicyWaiverApp1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy2.getId())
        .setOwnerId(app1.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 5));

    PolicyWaiver activePolicyWaiverApp2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy2.getId())
        .setOwnerId(app2.getId())
        .setExpiryTime(DateUtils.addDays(new Date(), 3));

    tempEntity.newWaiver(expiredPolicyWaiverApp1);
    tempEntity.newWaiver(activePolicyWaiverApp1);
    tempEntity.newWaiver(activePolicyWaiverApp2);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(
            risksFilterDTOBuilder
                .withExpirationDate(ALL)
                .withPageSize(10)
                .build());

    assertThat(dashboardPolicyWaivers.dashboardResults)
        .as("It should add expired waiver(s) to the result if ALL expiration date filter is selected.")
        .hasSize(3);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetDashboardPolicyWaivers_shouldGetAllParentWaiversFromParentOrgs() {
    Organization parentOrg1 = tempEntity.newOrganization();
    Organization parentOrg2 = tempEntity.newOrganization(parentOrg1);
    Organization parentOrg3 = tempEntity.newOrganization(parentOrg2);
    Organization parentOrg4 = tempEntity.newOrganization(parentOrg3);
    Organization org1 = tempEntity.newOrganization(parentOrg4);
    app1 = tempEntity.newApplication(org1.getId());

    Policy policy1 = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withName("Z-Policy-Name")
            .withOwnerId(app1.getId())
            .build());

    tempEntity.newWaiver(policy1.getId(), Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newWaiver(policy1.getId(), parentOrg1.getId());
    tempEntity.newWaiver(policy1.getId(), parentOrg2.getId());
    tempEntity.newWaiver(policy1.getId(), parentOrg3.getId());
    tempEntity.newWaiver(policy1.getId(), parentOrg4.getId());
    tempEntity.newWaiver(policy1.getId(), org1.getId());
    tempEntity.newWaiver(policy1.getId(), app1.getId());

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(
            risksFilterDTOBuilder
                .withApplicationIds(Collections.singleton(app1.getId()))
                .withPageSize(10)
                .build());

    assertThat(dashboardPolicyWaivers.dashboardResults)
        .as("It should get the app and all the parent orgs including the root org")
        .hasSize(7);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetDashboardPolicyWaivers_IncludeAutoWaivers() {
    tempEntity.newWaiver(policy.getId(), app1.getId());
    createAutoPolicyWaiver(app1.getId(), 7, true);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(10);

    // Test without including auto waivers
    DashboardResultsDTO<DashboardPolicyWaiverDTO> result =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), false);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertThat(result.dashboardResults.get(0).isAutoWaiver).isFalse();

    // Test including auto waivers
    result = dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), true);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertThat(result.dashboardResults).extracting("isAutoWaiver").containsExactlyInAnyOrder(false, true);
  }

  @Test
  public void testGetDashboardPolicyWaivers_FilterAutoWaiversByThreatLevel() {
    createAutoPolicyWaiver(app1.getId(), 5, true);
    createAutoPolicyWaiver(app1.getId(), 8, true);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withPolicyThreatLevelRange(new PolicyThreatLevelFilter(7, 10))
        .withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> result =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), true);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertThat(result.dashboardResults.get(0).threatLevel).isEqualTo(8);
  }

  @Test
  public void testGetDashboardPolicyWaivers_FilterAutoWaiversBySecurityThreatCategoryAndReachable() {
    createAutoPolicyWaiver(app1.getId(), 7, true);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withPolicyThreatCategories(new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY))
        .withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> result =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), true);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);

    risksFilterDTOBuilder.withPolicyThreatCategories(new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE));
    result = dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), true);
    assertThat(result.dashboardResults).hasSize(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetDashboardPolicyWaivers_FilterAutoWaiversBySecurityThreatCategoryAndNotReachable() {
    AutoPolicyWaiver autoPolicyWaiver = createAutoPolicyWaiver(app1.getId(), 7, false);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withPolicyThreatCategories(new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY))
        .withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> result =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), true);
    assertThat(result.dashboardResults).hasSize(0);
    assertThat(result.hasNextPage).isEqualTo(false);

    autoPolicyWaiver.setReachability(true);
    autoPolicyWaiverDAO.update(autoPolicyWaiver);

    result = dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), true);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetDashboardPolicyWaivers_OrderAutoWaivers() {
    createAutoPolicyWaiver(app1.getId(), 5, true);
    createAutoPolicyWaiver(app1.getId(), 8, true);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withOrderBy("-THREAT_LEVEL")
        .withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> result =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), true);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.hasNextPage).isEqualTo(false);
    assertThat(result.dashboardResults).extracting("threatLevel").containsExactly(8, 5);
  }

  @Test
  public void testGetDashboardPolicyWaivers_FeatureFlagDisabled() {
    tempEntity.newWaiver(policy.getId(), app1.getId());
    createAutoPolicyWaiver(app1.getId(), 7, true);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withPageSize(10);

    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    // Test including auto waivers but feature flag is disabled
    DashboardResultsDTO resultsDTO =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build(), true);
    assertThat(resultsDTO).isNotNull();
    assertThat(resultsDTO.dashboardResults).hasSize(1);
  }

  @Test
  public void testGetExpiredWaiversWithValidInputs() {
    // Given
    ComponentIdentifier testComponentIdentifier = getTestComponentIdentifier();
    PolicyWaiver expectedWaiver = createTestPolicyWaiver("waiver-id");

    PolicyWaiverDAO mockPolicyWaiverDAO = setupMockPolicyWaiverDAO("test-owner", "test-hash", expectedWaiver);

    PolicyWaiverService policyWaiverService = createPolicyWaiverServiceWithMockDAO(mockPolicyWaiverDAO);

    // When
    List<PolicyWaiverResource.PolicyWaiverDTO> result = policyWaiverService.getExpiredWaivers(
        "test-owner", "test-hash", Mockito.mock(UnaryOperator.class), testComponentIdentifier, null);

    // Then
    assertThat(result).hasSize(1)
        .extracting(PolicyWaiverResource.PolicyWaiverDTO::getId)
        .containsExactly("waiver-id");
  }

  @Test
  public void testGetDashboardPolicyWaivers_includesRepositoryManagerWaivers() {
    // Create a RepositoryManager and Repository hierarchy
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    // Create waiver at Repository level
    PolicyWaiver repositoryLevelWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repository.getId());
    tempEntity.newWaiver(repositoryLevelWaiver);

    // Create waiver at RepositoryManager level
    PolicyWaiver repositoryManagerLevelWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager.getId());
    tempEntity.newWaiver(repositoryManagerLevelWaiver);

    // Create waiver at Application level (should not be returned)
    PolicyWaiver applicationLevelWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(app1.getId());
    tempEntity.newWaiver(applicationLevelWaiver);

    // Filter by repository
    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repository.getId())).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should return both repository and repository manager waivers, but not application waiver
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(2);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    // Verify both waivers are present
    List<String> returnedWaiverIds = dashboardPolicyWaivers.dashboardResults.stream()
        .map(dto -> dto.id)
        .collect(Collectors.toList());
    assertThat(returnedWaiverIds).containsExactlyInAnyOrder(
        repositoryLevelWaiver.getId(),
        repositoryManagerLevelWaiver.getId()
    );

    // Verify the repository manager waiver details
    DashboardPolicyWaiverDTO rmWaiverDto = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> dto.id.equals(repositoryManagerLevelWaiver.getId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("RepositoryManager waiver not found"));

    assertPolicyWaiverWithoutDetails(rmWaiverDto, repositoryManagerLevelWaiver, repositoryManager);
  }

  @Test
  public void testGetDashboardPolicyWaivers_repositoryManagerWaiverAppearsOnceForMultipleRepositories() {
    // Create a RepositoryManager with multiple repositories
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository1 = tempEntity.newRepository(repositoryManager);
    Repository repository2 = tempEntity.newRepository(repositoryManager);
    Repository repository3 = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    // Create waivers at each Repository level
    PolicyWaiver repositoryWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repository1.getId());
    tempEntity.newWaiver(repositoryWaiver1);

    PolicyWaiver repositoryWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repository2.getId());
    tempEntity.newWaiver(repositoryWaiver2);

    PolicyWaiver repositoryWaiver3 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repository3.getId());
    tempEntity.newWaiver(repositoryWaiver3);

    // Create ONE waiver at RepositoryManager level
    PolicyWaiver repositoryManagerWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager.getId());
    tempEntity.newWaiver(repositoryManagerWaiver);

    // Filter by all three repositories
    Set<String> repositoryIds = new HashSet<>(Arrays.asList(
        repository1.getId(),
        repository2.getId(),
        repository3.getId()
    ));
    risksFilterDTOBuilder.withRepositoryIds(repositoryIds).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should return 3 repository waivers + 1 repository manager waiver (not duplicated)
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(4);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    // Verify all waivers are present and repository manager waiver appears only once
    List<String> returnedWaiverIds = dashboardPolicyWaivers.dashboardResults.stream()
        .map(dto -> dto.id)
        .collect(Collectors.toList());
    assertThat(returnedWaiverIds).containsExactlyInAnyOrder(
        repositoryWaiver1.getId(),
        repositoryWaiver2.getId(),
        repositoryWaiver3.getId(),
        repositoryManagerWaiver.getId()
    );

    // Verify repository manager waiver appears exactly once
    long repositoryManagerWaiverCount = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> dto.id.equals(repositoryManagerWaiver.getId()))
        .count();
    assertThat(repositoryManagerWaiverCount).isEqualTo(1);

    // Verify the repository manager waiver has correct owner details
    DashboardPolicyWaiverDTO rmWaiverDto = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> dto.id.equals(repositoryManagerWaiver.getId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("RepositoryManager waiver not found"));

    assertPolicyWaiverWithoutDetails(rmWaiverDto, repositoryManagerWaiver, repositoryManager);
  }

  @Test
  public void testGetDashboardPolicyWaivers_includesMultipleRepositoryManagerWaivers() {
    // Create multiple RepositoryManagers with their own repositories
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager3 = tempEntity.newRepositoryManager();

    Repository repo1 = tempEntity.newRepository(repositoryManager1);
    Repository repo2 = tempEntity.newRepository(repositoryManager2);
    Repository repo3 = tempEntity.newRepository(repositoryManager3);

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    // Create waivers at each Repository level
    PolicyWaiver repoWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo1.getId());
    tempEntity.newWaiver(repoWaiver1);

    PolicyWaiver repoWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo2.getId());
    tempEntity.newWaiver(repoWaiver2);

    PolicyWaiver repoWaiver3 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo3.getId());
    tempEntity.newWaiver(repoWaiver3);

    // Create waivers at each RepositoryManager level
    PolicyWaiver rmWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager1.getId());
    tempEntity.newWaiver(rmWaiver1);

    PolicyWaiver rmWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager2.getId());
    tempEntity.newWaiver(rmWaiver2);

    PolicyWaiver rmWaiver3 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager3.getId());
    tempEntity.newWaiver(rmWaiver3);

    // Filter by all three repositories from different repository managers
    Set<String> repositoryIds = new HashSet<>(Arrays.asList(
        repo1.getId(),
        repo2.getId(),
        repo3.getId()
    ));
    risksFilterDTOBuilder.withRepositoryIds(repositoryIds).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should return 3 repository waivers + 3 repository manager waivers = 6 total
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(6);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    // Verify all waivers are present
    List<String> returnedWaiverIds = dashboardPolicyWaivers.dashboardResults.stream()
        .map(dto -> dto.id)
        .collect(Collectors.toList());
    assertThat(returnedWaiverIds).containsExactlyInAnyOrder(
        repoWaiver1.getId(),
        repoWaiver2.getId(),
        repoWaiver3.getId(),
        rmWaiver1.getId(),
        rmWaiver2.getId(),
        rmWaiver3.getId()
    );

    // Verify each repository manager waiver has correct owner details
    DashboardPolicyWaiverDTO rmWaiverDto1 = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> dto.id.equals(rmWaiver1.getId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("RepositoryManager1 waiver not found"));
    assertPolicyWaiverWithoutDetails(rmWaiverDto1, rmWaiver1, repositoryManager1);

    DashboardPolicyWaiverDTO rmWaiverDto2 = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> dto.id.equals(rmWaiver2.getId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("RepositoryManager2 waiver not found"));
    assertPolicyWaiverWithoutDetails(rmWaiverDto2, rmWaiver2, repositoryManager2);

    DashboardPolicyWaiverDTO rmWaiverDto3 = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> dto.id.equals(rmWaiver3.getId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("RepositoryManager3 waiver not found"));
    assertPolicyWaiverWithoutDetails(rmWaiverDto3, rmWaiver3, repositoryManager3);
  }

  @Test
  public void testGetDashboardPolicyWaivers_mixedRepositoriesFromSameAndDifferentManagers() {
    // Create 2 repository managers
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();

    // RepositoryManager1 has 3 repositories
    Repository repo1a = tempEntity.newRepository(repositoryManager1);
    Repository repo1b = tempEntity.newRepository(repositoryManager1);
    Repository repo1c = tempEntity.newRepository(repositoryManager1);

    // RepositoryManager2 has 2 repositories
    Repository repo2a = tempEntity.newRepository(repositoryManager2);
    Repository repo2b = tempEntity.newRepository(repositoryManager2);

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    // Create waivers for all repositories
    PolicyWaiver waiver1a = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo1a.getId());
    tempEntity.newWaiver(waiver1a);

    PolicyWaiver waiver1b = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo1b.getId());
    tempEntity.newWaiver(waiver1b);

    PolicyWaiver waiver1c = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo1c.getId());
    tempEntity.newWaiver(waiver1c);

    PolicyWaiver waiver2a = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo2a.getId());
    tempEntity.newWaiver(waiver2a);

    PolicyWaiver waiver2b = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo2b.getId());
    tempEntity.newWaiver(waiver2b);

    // Create waivers for both repository managers
    PolicyWaiver rmWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager1.getId());
    tempEntity.newWaiver(rmWaiver1);

    PolicyWaiver rmWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager2.getId());
    tempEntity.newWaiver(rmWaiver2);

    // Filter by all 5 repositories (3 from RM1, 2 from RM2)
    Set<String> repositoryIds = new HashSet<>(Arrays.asList(
        repo1a.getId(),
        repo1b.getId(),
        repo1c.getId(),
        repo2a.getId(),
        repo2b.getId()
    ));
    risksFilterDTOBuilder.withRepositoryIds(repositoryIds).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should return 5 repository waivers + 2 repository manager waivers = 7 total
    // Each RM waiver should appear only once despite multiple repos from same manager
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(7);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    // Verify all waivers are present
    List<String> returnedWaiverIds = dashboardPolicyWaivers.dashboardResults.stream()
        .map(dto -> dto.id)
        .collect(Collectors.toList());
    assertThat(returnedWaiverIds).containsExactlyInAnyOrder(
        waiver1a.getId(),
        waiver1b.getId(),
        waiver1c.getId(),
        waiver2a.getId(),
        waiver2b.getId(),
        rmWaiver1.getId(),
        rmWaiver2.getId()
    );

    // Verify each repository manager waiver appears exactly once
    long rmWaiver1Count = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> dto.id.equals(rmWaiver1.getId()))
        .count();
    assertThat(rmWaiver1Count).isEqualTo(1);

    long rmWaiver2Count = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> dto.id.equals(rmWaiver2.getId()))
        .count();
    assertThat(rmWaiver2Count).isEqualTo(1);
  }

  @Test
  public void testGetDashboardPolicyWaivers_emptyFiltersIncludesAllRepositoryManagerWaivers() {
    // Create repository managers with repositories
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();

    Repository repo1 = tempEntity.newRepository(repositoryManager1);
    Repository repo2 = tempEntity.newRepository(repositoryManager2);

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    // Create repository waivers
    PolicyWaiver repoWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo1.getId());
    tempEntity.newWaiver(repoWaiver1);

    PolicyWaiver repoWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo2.getId());
    tempEntity.newWaiver(repoWaiver2);

    // Create repository manager waivers
    PolicyWaiver rmWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager1.getId());
    tempEntity.newWaiver(rmWaiver1);

    PolicyWaiver rmWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager2.getId());
    tempEntity.newWaiver(rmWaiver2);

    // Create application waivers (should also be included with empty filters)
    PolicyWaiver appWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(app1.getId());
    tempEntity.newWaiver(appWaiver);

    PolicyWaiver orgWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(org.getId());
    tempEntity.newWaiver(orgWaiver);

    // Use empty filters (no applicationIds, organizationIds, or repositoryIds)
    risksFilterDTOBuilder.withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should return all waivers including repository manager waivers
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isGreaterThanOrEqualTo(6);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    // Verify repository manager waivers are included
    List<String> returnedWaiverIds = dashboardPolicyWaivers.dashboardResults.stream()
        .map(dto -> dto.id)
        .collect(Collectors.toList());

    assertThat(returnedWaiverIds).contains(
        rmWaiver1.getId(),
        rmWaiver2.getId(),
        repoWaiver1.getId(),
        repoWaiver2.getId(),
        appWaiver.getId(),
        orgWaiver.getId()
    );
  }

  @Test
  public void testGetDashboardPolicyWaivers_ordersByOwnerScopeWithRepositoryManagers() {
    // Create repository managers and repositories
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    // Create waivers at different owner levels
    PolicyWaiver rootOrgWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newWaiver(rootOrgWaiver);

    PolicyWaiver parentOrgWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(parentOrg.getId());
    tempEntity.newWaiver(parentOrgWaiver);

    PolicyWaiver orgWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(org.getId());
    tempEntity.newWaiver(orgWaiver);

    PolicyWaiver appWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(app1.getId());
    tempEntity.newWaiver(appWaiver);

    PolicyWaiver repositoryManagerWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager.getId());
    tempEntity.newWaiver(repositoryManagerWaiver);

    PolicyWaiver repositoryWaiver = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repository.getId());
    tempEntity.newWaiver(repositoryWaiver);

    // Order by owner scope descending
    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE;
    risksFilterDTOBuilder.withOrderBy(orderBy).withPageSize(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isGreaterThanOrEqualTo(6);
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);

    // Find the positions of each waiver type in the ordered results
    List<String> orderedOwnerTypes = dashboardPolicyWaivers.dashboardResults.stream()
        .map(dto -> dto.ownerType)
        .collect(Collectors.toList());

    // Verify repository manager waivers are included and ordered correctly
    // Owner scope order (descending): root_organization > organization > application > repository_manager > repository
    assertThat(orderedOwnerTypes).contains("repository_manager", "repository");

    // Verify the specific waivers are present
    List<String> returnedWaiverIds = dashboardPolicyWaivers.dashboardResults.stream()
        .map(dto -> dto.id)
        .collect(Collectors.toList());

    assertThat(returnedWaiverIds).contains(
        repositoryManagerWaiver.getId(),
        repositoryWaiver.getId()
    );
  }

  @Test
  public void testGetDashboardPolicyWaivers_paginationWithRepositoryManagerWaivers() {
    // Create repository managers with repositories
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager3 = tempEntity.newRepositoryManager();

    Repository repo1 = tempEntity.newRepository(repositoryManager1);
    Repository repo2 = tempEntity.newRepository(repositoryManager2);
    Repository repo3 = tempEntity.newRepository(repositoryManager3);

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    // Create 6 repository waivers
    PolicyWaiver repoWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo1.getId());
    tempEntity.newWaiver(repoWaiver1);

    PolicyWaiver repoWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo2.getId());
    tempEntity.newWaiver(repoWaiver2);

    PolicyWaiver repoWaiver3 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repo3.getId());
    tempEntity.newWaiver(repoWaiver3);

    // Create 3 repository manager waivers
    PolicyWaiver rmWaiver1 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager1.getId());
    tempEntity.newWaiver(rmWaiver1);

    PolicyWaiver rmWaiver2 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager2.getId());
    tempEntity.newWaiver(rmWaiver2);

    PolicyWaiver rmWaiver3 = new PolicyWaiver()
        .setHash(TemporaryEntity.uuid().substring(0, 5))
        .setPolicyId(policy.getId())
        .setOwnerId(repositoryManager3.getId());
    tempEntity.newWaiver(rmWaiver3);

    Set<String> repositoryIds = new HashSet<>(Arrays.asList(
        repo1.getId(),
        repo2.getId(),
        repo3.getId()
    ));

    // Test first page with page size 2
    risksFilterDTOBuilder.withRepositoryIds(repositoryIds).withPageSize(2).withPage(0);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> page1 =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(page1.dashboardResults).hasSize(2);
    assertThat(page1.hasNextPage).isTrue();

    // Test second page
    risksFilterDTOBuilder.withPage(1);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> page2 =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(page2.dashboardResults).hasSize(2);
    assertThat(page2.hasNextPage).isTrue();

    // Test third page
    risksFilterDTOBuilder.withPage(2);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> page3 =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(page3.dashboardResults).hasSize(2);
    assertThat(page3.hasNextPage).isFalse();

    // Verify all pages together contain all 6 waivers (3 repos + 3 repo managers)
    List<String> allWaiverIds = new ArrayList<>();
    allWaiverIds.addAll(page1.dashboardResults.stream().map(dto -> dto.id).toList());
    allWaiverIds.addAll(page2.dashboardResults.stream().map(dto -> dto.id).toList());
    allWaiverIds.addAll(page3.dashboardResults.stream().map(dto -> dto.id).toList());

    assertThat(allWaiverIds).hasSize(6);
    assertThat(allWaiverIds).containsExactlyInAnyOrder(
        repoWaiver1.getId(),
        repoWaiver2.getId(),
        repoWaiver3.getId(),
        rmWaiver1.getId(),
        rmWaiver2.getId(),
        rmWaiver3.getId()
    );

    // Verify no duplicates across pages
    Set<String> uniqueWaiverIds = new HashSet<>(allWaiverIds);
    assertThat(uniqueWaiverIds).hasSize(6);
  }

  private ComponentIdentifier getTestComponentIdentifier() {
    TreeMap<String, String> coordinates = new TreeMap<>()
    {
      {
        this.put("artifactId", "Artifact1");
        this.put("groupId", "Group1");
        this.put("version", "1.2.3");
      }
    };

    return new ComponentIdentifier("maven", coordinates);
  }

  private PolicyWaiver createTestPolicyWaiver(String waiverId) {
    PolicyWaiver waiver = new PolicyWaiver();
    waiver.setId(waiverId);
    return waiver;
  }

  private PolicyWaiverDAO setupMockPolicyWaiverDAO(String ownerId, String hash, PolicyWaiver expectedWaiver) {
    PolicyWaiverDAO mockDAO = Mockito.mock(PolicyWaiverDAO.class);
    when(mockDAO.getExpiredToComponentIncludingAllVersions(eq(ownerId), eq(hash), any(PackageUrlIdentifier.class)))
        .thenReturn(List.of(expectedWaiver));
    return mockDAO;
  }

  private PolicyWaiverService createPolicyWaiverServiceWithMockDAO(PolicyWaiverDAO mockDAO) {
    return new PolicyWaiverService(
        null,
        null,
        mockDAO,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );
  }

  @Test
  public void testMapPolicyWaiverToDTO() {
    // Given
    PolicyWaiver waiver = createTestPolicyWaiver();
    UnaryOperator<String> policyNameLoader = setUpPolicyNameLoader();
    Map<String, PolicyWaiverReason> policyWaiverReasonMap = setUpPolicyWaiverReasonMap();

    // When
    PolicyWaiverResource.PolicyWaiverDTO dto =
        dashboardPolicyWaiverService.mapPolicyWaiverToDTO(waiver, policyNameLoader, policyWaiverReasonMap);

    // Then
    assertPolicyWaiverDTO(dto, waiver);
  }

  private PolicyWaiver createTestPolicyWaiver() {
    PolicyWaiver waiver = new PolicyWaiver();
    waiver.setComment("Test Comment");
    waiver.setCreateTime(new Date());
    waiver.setHash("test-hash");
    waiver.setId("test-id");
    waiver.setOwnerId("owner-id");
    waiver.setPolicyId("policy-id");
    waiver.setConstraintFactsJson("constraint-facts-json");
    ConstraintFact constraintFact = new ConstraintFact("1234", "aConstraint", "operator-test");
    waiver.setConstraintFacts(Collections.singletonList(constraintFact));
    waiver.setCreatorId("creator-id");
    waiver.setCreatorName("creator-name");
    waiver.setAssociatedPackageUrl("test-purl");
    waiver.setComponentMatchStrategy(EXACT_COMPONENT);
    waiver.setExpireWhenRemediationAvailable(true);
    waiver.setExpiryTime(new Date());
    waiver.setWaiverReasonId("reason-id");
    return waiver;
  }

  private UnaryOperator<String> setUpPolicyNameLoader() {
    UnaryOperator<String> policyNameLoader = Mockito.mock(UnaryOperator.class);
    when(policyNameLoader.apply("policy-id")).thenReturn("Test Policy Name");
    return policyNameLoader;
  }

  private Map<String, PolicyWaiverReason> setUpPolicyWaiverReasonMap() {
    Map<String, PolicyWaiverReason> policyWaiverReasonMap = new HashMap<>();
    PolicyWaiverReason reason = new PolicyWaiverReason();
    reason.setReasonText("Test Reason");
    policyWaiverReasonMap.put("reason-id", reason);
    return policyWaiverReasonMap;
  }

  private void assertPolicyWaiverDTO(PolicyWaiverResource.PolicyWaiverDTO dto, PolicyWaiver waiver) {
    assertThat(dto.getComment()).isEqualTo(waiver.getComment());
    assertThat(dto.getHash()).isEqualTo(waiver.getHash());
    assertThat(dto.getId()).isEqualTo(waiver.getId());
    assertThat(dto.getOwnerId()).isEqualTo(waiver.getOwnerId());
    assertThat(dto.getPolicyId()).isEqualTo(waiver.getPolicyId());
    assertThat(dto.policyName).isEqualTo("Test Policy Name");
    String constraintFactsJsonString =
        "[{\"constraintId\":\"1234\",\"constraintName\":\"aConstraint\",\"operatorName\":\"operator-test\"," +
            "\"conditionFacts\":[]}]";
    assertThat(dto.getConstraintFactsJson()).isEqualTo(constraintFactsJsonString);
    assertThat(dto.getConstraintFacts()).containsAll(waiver.getConstraintFacts());
    assertThat(dto.getCreatorId()).isEqualTo(waiver.getCreatorId());
    assertThat(dto.getCreatorName()).isEqualTo(waiver.getCreatorName());
    assertThat(dto.getAssociatedPackageUrl()).isEqualTo(waiver.getAssociatedPackageUrl());
    assertThat(dto.getComponentMatchStrategy()).isEqualTo(waiver.getComponentMatchStrategy());
    assertThat(dto.isExpireWhenRemediationAvailable()).isEqualTo(waiver.isExpireWhenRemediationAvailable());
    assertThat(dto.getExpiryTime()).isNotNull();
    assertThat(dto.policyWaiverReasonId).isEqualTo(waiver.getWaiverReasonId());
    assertThat(dto.reasonText).isEqualTo("Test Reason");
    assertThat(dto.isForContainerImage()).isEqualTo(waiver.isForContainerImage());
    assertThat(dto.isForContainerImageComponent()).isEqualTo(waiver.isForContainerImageComponent());
  }

  private PolicyWaiver createPolicyWaiverWithFullDetails(Application application) {
    Date today = new Date();
    Date aWeekFromNow = DateUtils.addDays(today, 7);
    Policy highThreatPolicy = tempEntity.newPolicy(application.getId(), "Very bad security threat", 9);

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact =
        new ConditionFact(ConditionTypes.SecurityVulnerabilityStatusConditionType.getId(), 0, "summary", "reason",
            triggerReference);
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

    PolicyWaiver policyWaiver = new PolicyWaiver()
        .setHash("hash")
        .setPolicyId(highThreatPolicy.getId())
        .setOwnerId(application.getId())
        .setConstraintFacts(singletonList(constraintFact))
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("a comment")
        .setCreateTime(today)
        .setExpiryTime(aWeekFromNow)
        .setComponentUpgradeAvailable(true);

    return tempEntity.newWaiver(policyWaiver);
  }

  private void assertPolicyWaiverWithoutDetails(
      final DashboardPolicyWaiverDTO dashboardPolicyWaiverDTO,
      final PolicyWaiver policyWaiver, Owner owner)
  {
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaiverDTO, policyWaiver, owner, null);
  }

  private void assertPolicyWaiverWithoutDetails(
      final DashboardPolicyWaiverDTO dashboardPolicyWaiverDTO,
      final PolicyWaiver policyWaiver, Owner owner, String customOwnerType)
  {
    assertPolicyWaiverDTOBasicFields(dashboardPolicyWaiverDTO, policyWaiver, owner, customOwnerType);

    assertThat(dashboardPolicyWaiverDTO.constraintFacts).isNull();
    assertThat(dashboardPolicyWaiverDTO.comment).isNull();
    assertThat(dashboardPolicyWaiverDTO.creatorId).isNull();
    assertThat(dashboardPolicyWaiverDTO.creatorName).isNull();
  }

  private void assertPolicyWaiverDTOBasicFields(
      final DashboardPolicyWaiverDTO dashboardPolicyWaiverDTO,
      final PolicyWaiver policyWaiver,
      final Owner owner,
      final String customOwnerType)
  {
    Policy waiverPolicy = policyDAO.getById(policyWaiver.getPolicyId());

    assertThat(dashboardPolicyWaiverDTO.id).isEqualTo(policyWaiver.getId());
    assertThat(dashboardPolicyWaiverDTO.threatLevel).isEqualTo(waiverPolicy.getThreatLevel());
    assertThat(dashboardPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(dashboardPolicyWaiverDTO.expiryTime).isEqualTo(policyWaiver.getExpiryTime());
    assertThat(dashboardPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(dashboardPolicyWaiverDTO.policyName).isEqualTo(waiverPolicy.getName());
    assertThat(dashboardPolicyWaiverDTO.ownerType)
        .isEqualTo(customOwnerType != null ? customOwnerType : owner.getType().toString());
    assertThat(dashboardPolicyWaiverDTO.ownerId).isEqualTo(owner.getId());
    assertThat(dashboardPolicyWaiverDTO.ownerName).isEqualTo(owner.getName());
    assertThat(dashboardPolicyWaiverDTO.componentMatchStrategy).isEqualTo(policyWaiver.getComponentMatchStrategy());
    assertThat(dashboardPolicyWaiverDTO.componentUpgradeAvailable).isEqualTo(
        policyWaiver.isComponentUpgradeAvailable());

    if (policyWaiver.getComponentIdentifier() != null) {
      assertThat(dashboardPolicyWaiverDTO.componentIdentifier.toComponentIdentifier())
          .isEqualTo(policyWaiver.getComponentIdentifier());
      assertThat(dashboardPolicyWaiverDTO.getDisplayName().toString())
          .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(policyWaiver.getComponentIdentifier()).toString());
    }
  }

  private void assertPolicyWaiverWithFullDetails(
      final DashboardPolicyWaiverDTO dashboardPolicyWaiverDTO,
      final PolicyWaiver policyWaiver, Owner owner)
  {
    assertPolicyWaiverDTOBasicFields(dashboardPolicyWaiverDTO, policyWaiver, owner, null);

    if (policyWaiver.getConstraintFacts() != null) {
      assertThat(dashboardPolicyWaiverDTO.constraintFacts).hasSize(policyWaiver.getConstraintFacts().size());
      for (int i = 0; i < policyWaiver.getConstraintFacts().size(); i++) {
        assertThat(dashboardPolicyWaiverDTO.constraintFacts.get(i).getConstraintId()).isEqualTo(
            policyWaiver.getConstraintFacts().get(i).getConstraintId());
      }
    }
    assertThat(dashboardPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(dashboardPolicyWaiverDTO.creatorId).isEqualTo(policyWaiver.getCreatorId());
    assertThat(dashboardPolicyWaiverDTO.creatorName).isEqualTo(policyWaiver.getCreatorName());
  }

  private AutoPolicyWaiver createAutoPolicyWaiver(String ownerId, int threatLevel, boolean reachable) {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver(
        ownerId,
        threatLevel,
        reachable,
        true,
        "testCreator",
        "Test Creator",
        new Date()
    );
    return tempEntity.newAutoPolicyWaiver(autoPolicyWaiver);
  }
}
