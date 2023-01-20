/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import com.sonatype.insight.brain.api.v2.service.PolicyViolationTestHelper;
import com.sonatype.insight.brain.builders.TestPolicyBuilder;
import com.sonatype.insight.brain.builders.TestPolicyWaiverBuilder;
import com.sonatype.insight.brain.dashboard.DashboardPolicyWaiverDTOComparator.DashboardPolicyWaiverOrderByEnum;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.RandomStringUtils;
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

public class DashboardPolicyWaiverServiceTest
    extends AbstractComponentTest
{
  @Inject
  private DashboardPolicyWaiverService dashboardPolicyWaiverService;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private PolicyDAO policyDAO;

  private Organization org;

  private Application app1;

  private Application app2;

  private Policy policy;

  private RisksFilterDTOBuilder risksFilterDTOBuilder;

  @Before
  public void beforeEach() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("Application 1", "Application-1", org.getId());
    app2 = tempEntity.newApplication("Application 2", "Application-2", org.getId());
    policy = tempEntity.newPolicy(org);

    risksFilterDTOBuilder = new RisksFilterDTOBuilder().withApplicationIds(Collections.emptySet())
        .withOrganizationIds(Collections.emptySet()).withMaxResults(1);
  }

  @Test
  public void getDashboardPolicyWaivers_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);
    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(functionCall);
  }

  @Test
  public void getDashboardPolicyWaivers_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(functionCall)
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void getDashboardPolicyWaivers_returnsInformationWithoutExtraDetails() {
    PolicyWaiver policyWaiverApp1 = tempEntity.newWaiver(policy.getId(), app1.getId());
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(), app2.getId()));
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withApplicationIds(apps).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(2);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).id).isEqualTo(policyWaiverApp1.getId());

    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(1), policyWaiverApp1, app1);
  }

  @Test
  public void getDashboardPolicyWaivers_shouldReturnOnlyMaxResults() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org);
      tempEntity.newWaiver(testPolicy.getId(), app1.getId());
    };
    IntStream.range(0, 10).forEach(intConsumer);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId())).withMaxResults(2);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(10);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(2);
  }

  @Test
  public void getDashboardPolicyWaivers_filtersByOrganization() {
    Organization excludedOrganization = tempEntity.newOrganization();
    Policy excludedPolicy = tempEntity.newPolicy(excludedOrganization);
    tempEntity.newWaiver(policy.getId(), excludedPolicy.getId());
    PolicyWaiver policyWaiverApp1 = tempEntity.newWaiver(policy.getId(), org.getId());
    createPolicyWaiverWithFullDetails(app2);

    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId())).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(1);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp1, org);
  }

  @Test
  public void getDashboardPolicyWaivers_filtersByApplication() {
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    tempEntity.newWaiver(policy.getId(), app1.getId());

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId())).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(1);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
  }

  @Test
  public void testGetDashboardPolicyWaivers_filtersByRepository() {
    Repository repository = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(
        new TestPolicyBuilder()
            .withSampleTestValues()
            .withOwnerId(org.getId())
            .build());

    PolicyWaiver policyWaiver = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy.getId())
        .withOwnerId(repository.getId())
        .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
        .build();

    tempEntity.newWaiver(policyWaiver);

    PolicyWaiver policyWaiver1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
        .build();

    tempEntity.newWaiver(policyWaiver1);

    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repository.getId())).withMaxResults(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults)
        .isEqualTo(1);

    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiver, repository);

    PolicyWaiver policyWaiver2 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy.getId())
        .withOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .withExpiryTime(Date.from(Instant.now().plus(5, ChronoUnit.DAYS)))
        .build();

    tempEntity.newWaiver(policyWaiver2);

    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(RepositoryContainer.REPOSITORY_CONTAINER_ID));

    dashboardPolicyWaivers = dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults)
        .isEqualTo(1);

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
    PolicyWaiver policyWaiver1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
        .build();
    tempEntity.newWaiver(policyWaiver1);

    // add app 3 waiver
    Organization org2 = tempEntity.newOrganization("Org3");
    Application application3 = tempEntity.newApplication("Application-3", " Applicatin-3", org2.getId());
    PolicyWaiver policyWaiverOrg2 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy.getId())
        .withOwnerId(application3.getId())
        .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
        .build();
    tempEntity.newWaiver(policyWaiverOrg2);

    // add waiver for empty org
    Organization org3 = tempEntity.newOrganization("Org4");
    PolicyWaiver policyWaiverOrg3 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy.getId())
        .withOwnerId(org3.getId())
        .withExpiryTime(Date.from(Instant.now().plus(5, ChronoUnit.DAYS)))
        .build();
    tempEntity.newWaiver(policyWaiverOrg3);

    // add waiver for repo container
    PolicyWaiver policyWaiverRepoContainer = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy.getId())
        .withOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .withExpiryTime(Date.from(Instant.now().plus(10, ChronoUnit.DAYS)))
        .build();
    tempEntity.newWaiver(policyWaiverRepoContainer);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.withMaxResults(10).build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(4);

    // added afterwards to make sure that repo container still shows even with no repo waivers

    Repository repository = tempEntity.newRepository();

    // add repo waiver
    PolicyWaiver policyWaiver = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy.getId())
        .withOwnerId(repository.getId())
        .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
        .build();
    tempEntity.newWaiver(policyWaiver);

    RepositoryComponent
        component = tempEntity.newRepositoryComponent(repository.getId());

    PolicyViolationTestHelper.createPolicyViolationWaived(policy, component, tempEntity);

    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.withMaxResults(10).build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(5);
  }

  @Test
  public void getDashboardPolicyWaivers_filtersByApplicationCategories() {
    Tag applicationCategory = tempEntity.newTag(org.getId());
    Application categorizedApplication = tempEntity.newApplication("categorizedApplication", org.getId());
    tempEntity.newApplicationTag(categorizedApplication.getId(), applicationCategory.getId());

    PolicyWaiver categorizedAppWaiver = tempEntity.newWaiver(policy.getId(), categorizedApplication.getId());
    tempEntity.newWaiver(policy.getId(), app1.getId());
    tempEntity.newWaiver(policy.getId(), app2.getId());

    risksFilterDTOBuilder.withTagIds(Collections.singleton(applicationCategory.getId())).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(1);
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), categorizedAppWaiver,
        categorizedApplication);

    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()));
    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_filtersByPolicyTypes() {
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    Policy nonSecurityPolicy = tempEntity.newPolicy();
    tempEntity.newWaiver(nonSecurityPolicy.getId(), app1.getId());

    PolicyThreatCategoryFilter threatCategoryFilter = new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY);
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()))
        .withPolicyThreatCategories(threatCategoryFilter).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(1);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
  }

  @Test
  public void getDashboardPolicyWaivers_filtersByThreatLevel() {
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    Policy lowThreatLevelPolicy = policy;
    tempEntity.newWaiver(lowThreatLevelPolicy.getId(), app1.getId());

    PolicyThreatLevelFilter threatLevelFilter = new PolicyThreatLevelFilter(7, 9);
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app2.getId()))
        .withPolicyThreatLevelRange(threatLevelFilter).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(1);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
  }

  @Test
  public void getDashboardPolicyWaivers_filtersByExpirationDate() {
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    Instant now = Instant.now();
    PolicyWaiver oneMonthExpiringWaiver = tempEntity.newWaiver("hash1", policy.getId(), app1.getId(), "",
        Date.from(now.plus(30, ChronoUnit.DAYS)));
    PolicyWaiver neverExpiringWaiver = tempEntity.newWaiver("hash3", policy.getId(), app1.getId(), "", null);
    tempEntity.newWaiver("hash2", policy.getId(), app1.getId(), "", Date.from(now.plus(40, ChronoUnit.DAYS)));

    risksFilterDTOBuilder.withApplicationIds(new HashSet<>(Arrays.asList(app1.getId(), app2.getId())))
        .withExpirationDate(IN_7_DAYS).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(1);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).expiryTime)
        .isBeforeOrEqualTo(Date.from(now.plus(IN_7_DAYS.getDays(), ChronoUnit.DAYS)));
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);

    risksFilterDTOBuilder.withExpirationDate(IN_30_DAYS);
    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(2);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).id).isEqualTo(oneMonthExpiringWaiver.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).expiryTime)
        .isBeforeOrEqualTo(Date.from(now.plus(IN_30_DAYS.getDays(), ChronoUnit.DAYS)));
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(1), oneMonthExpiringWaiver, app1);

    risksFilterDTOBuilder.withExpirationDate(NEVER);
    dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(1);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(neverExpiringWaiver.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).expiryTime).isNull();
    assertPolicyWaiverWithoutDetails(dashboardPolicyWaivers.dashboardResults.get(0), neverExpiringWaiver, app1);
  }

  @Test
  public void getDashboardPolicyWaivers_defaultOrderByExpiryTimeAndThreatLevel() {
    PolicyWaiver policyWaiverApp1 = tempEntity.newWaiver(policy.getId(), app1.getId());
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);
    Date waiverExpirationDate = Date.from(Instant.now().plus(Duration.ofDays(2)));
    PolicyWaiver policyWaiverNearestExpire =
        tempEntity.newWaiver("hash", policy.getId(), app1.getId(), "", waiverExpirationDate);

    risksFilterDTOBuilder.withApplicationIds(new HashSet<>(Arrays.asList(app1.getId(), app2.getId())))
        .withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(3);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverNearestExpire.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).id).isEqualTo(policyWaiverApp2.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).id).isEqualTo(policyWaiverApp1.getId());
  }

  @Test
  public void getDashboardPolicyWaivers_ordersByThreatLevel() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId(), "policy with threat " + value, value);
      tempEntity.newWaiver(testPolicy.getId(), app1.getId());
    };
    IntStream.rangeClosed(1, 9).forEach(intConsumer);

    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.THREAT_LEVEL;
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withOrderBy(orderBy).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(9);

    IntConsumer assertConsumer =
        value -> assertThat(dashboardPolicyWaivers.dashboardResults.get(9 - value).threatLevel).isEqualTo(value);
    IntStream.rangeClosed(1, 9).forEach(assertConsumer);
  }

  @Test
  public void getDashboardPolicyWaivers_ordersByCreateTime() {
    Instant seedDate = Instant.now();
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId());
      Date waiverCreateDate = getDateMinusDays(seedDate, value);
      tempEntity.newWaiver("hash", testPolicy.getId(), app1.getId(), null, "", waiverCreateDate);
    };
    IntStream.rangeClosed(0, 4).forEach(intConsumer);

    String orderBy = DashboardPolicyWaiverOrderByEnum.CREATION_DATE.toString();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withOrderBy(orderBy).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(5);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).createTime).isEqualTo(getDateMinusDays(seedDate, 4));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).createTime).isEqualTo(getDateMinusDays(seedDate, 3));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).createTime).isEqualTo(getDateMinusDays(seedDate, 2));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).createTime).isEqualTo(getDateMinusDays(seedDate, 1));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).createTime).isEqualTo(Date.from(seedDate));
  }

  private Date getDateMinusDays(Instant seedDate, int days) {
    return Date.from(seedDate.minus(Duration.ofDays(days)));
  }

  @Test
  public void getDashboardPolicyWaivers_ordersByExpiryTime() {
    Instant seedDate = Instant.now();
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId());
      Date waiverExpirationDate = getDatePlusDays(seedDate, value);
      tempEntity.newWaiver("hash", testPolicy.getId(), app1.getId(), "", waiverExpirationDate);
    };
    IntStream.rangeClosed(0, 4).forEach(intConsumer);

    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.EXPIRATION_DATE;
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app1.getId()))
        .withOrderBy(orderBy).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(5);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).expiryTime).isEqualTo(getDatePlusDays(seedDate, 4));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).expiryTime).isEqualTo(getDatePlusDays(seedDate, 3));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).expiryTime).isEqualTo(getDatePlusDays(seedDate, 2));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).expiryTime).isEqualTo(getDatePlusDays(seedDate, 1));
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).expiryTime).isEqualTo(Date.from(seedDate));
  }

  private Date getDatePlusDays(Instant seedDate, int days) {
    return Date.from(seedDate.plus(Duration.ofDays(days)));
  }

  @Test
  public void getDashboardPolicyWaivers_ordersByPolicyName() {
    IntConsumer intConsumer = value -> {
      Policy testPolicy = tempEntity.newPolicy(org.getId(), "Policy with ordered name " + value);
      tempEntity.newWaiver(testPolicy.getId(), org.getId());
    };
    IntStream.rangeClosed(0, 4).forEach(intConsumer);

    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.POLICY_NAME;
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withOrderBy(orderBy).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(5);

    IntConsumer assertConsumer =
        value -> assertThat(dashboardPolicyWaivers.dashboardResults.get(4 - value).policyName).isEqualTo(
            "Policy with ordered name " + value);
    IntStream.rangeClosed(0, 4).forEach(assertConsumer);
  }

  @Test
  public void getDashboardPolicyWaivers_ordersByOwnerTypeAndName() {
    Application app3 = tempEntity.newApplication("Application 3", "Application-3", org.getId());
    tempEntity.newWaiver(policy.getId(), app1.getId());
    tempEntity.newWaiver(policy.getId(), app2.getId());
    tempEntity.newWaiver(policy.getId(), app3.getId());
    tempEntity.newWaiver(policy.getId(), org.getId());
    tempEntity.newWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(),app2.getId(), app3.getId()));
    String orderBy = "-" + DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE;
    risksFilterDTOBuilder.withApplicationIds(apps).withOrderBy(orderBy).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(5);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).ownerName).isEqualTo("Root Organization");
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).ownerName).isEqualTo(org.getName());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(2).ownerName).isEqualTo(app3.getName());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(3).ownerName).isEqualTo(app2.getName());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(4).ownerName).isEqualTo(app1.getName());
  }

  @Test
  public void getDashboardPolicyWaivers_ordersByComponentDisplayNameWithMatchStrategy() {
    String[] chars = "abc".split("");
    String[] formats = { FORMAT_MAVEN, FORMAT_PYPI, FORMAT_GOLANG };
    ArrayList<ComponentMatcherStrategyForWaiver> waiverTypes = new ArrayList<ComponentMatcherStrategyForWaiver>() {{
        this.add(EXACT_COMPONENT);
        this.add(EXACT_COMPONENT);
        this.add(ALL_VERSIONS);
        this.add(ALL_COMPONENTS);
        this.add(ALL_COMPONENTS);
        this.add(EXACT_COMPONENT);
        this.add(EXACT_COMPONENT);
      }};
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
        .withOrderBy(orderBy).withMaxResults(10);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(7);
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

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(7);
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
  public void getDashboardPolicyWaiversForExport_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.DASHBOARD);

    risksFilterDTOBuilder.withMaxResults(Integer.MAX_VALUE);
    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverService.getDashboardPolicyWaiversForExport(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(functionCall);
  }

  @Test
  public void getDashboardPolicyWaiversForExport_DashboardFeatureDisabled() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");

    risksFilterDTOBuilder.withMaxResults(Integer.MAX_VALUE);
    ThrowingCallable functionCall =
        () -> dashboardPolicyWaiverService.getDashboardPolicyWaiversForExport(risksFilterDTOBuilder.build());
    assertThatExceptionOfType(ConflictException.class).isThrownBy(functionCall)
        .withMessage("The dashboard feature has been disabled.");
  }

  @Test
  public void getDashboardPolicyWaiversForExport_returnsInformationIncludingFullDetails() {
    PolicyWaiver policyWaiverApp1 = tempEntity.newWaiver(policy.getId(), app1.getId());
    PolicyWaiver policyWaiverApp2 = createPolicyWaiverWithFullDetails(app2);

    Set<String> apps = new HashSet<>(Arrays.asList(app1.getId(), app2.getId()));
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()))
        .withApplicationIds(apps).withMaxResults(Integer.MAX_VALUE);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaiversForExport(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(2);
    assertThat(dashboardPolicyWaivers.dashboardResults.get(0).id).isEqualTo(policyWaiverApp2.getId());
    assertThat(dashboardPolicyWaivers.dashboardResults.get(1).id).isEqualTo(policyWaiverApp1.getId());

    assertPolicyWaiverWithFullDetails(dashboardPolicyWaivers.dashboardResults.get(0), policyWaiverApp2, app2);
    assertPolicyWaiverWithFullDetails(dashboardPolicyWaivers.dashboardResults.get(1), policyWaiverApp1, app1);
  }

  @Test
  public void should_order_waivers_by_expiry_date_when_waiver_scope_is_same() {

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

    PolicyWaiver policyWaiver1App1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy1.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver policyWaiver2App1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphanumeric(5))
        .withPolicyId(policy2.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver policyWaiver1App2 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy1.getId())
        .withOwnerId(app2.getId())
        .withExpiryTime(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver policyWaiver2App2 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy1.getId())
        .withOwnerId(app2.getId())
        .withExpiryTime(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
        .build();

    tempEntity.newWaiver(policyWaiver1App1);
    tempEntity.newWaiver(policyWaiver2App1);
    tempEntity.newWaiver(policyWaiver1App2);
    tempEntity.newWaiver(policyWaiver2App2);

    String orderBy = DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE.name();
    risksFilterDTOBuilder
        .withOrderBy(orderBy).withMaxResults(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults)
        .as("It should include all the waivers.")
        .isEqualTo(4);

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
  public void should_order_waivers_by_expiry_date_when_policy_name_is_same() {

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

    PolicyWaiver policyWaiver1App1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy1.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver policyWaiver2App1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphanumeric(5))
        .withPolicyId(policy2.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver policyWaiver1App2 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy3.getId())
        .withOwnerId(app2.getId())
        .withExpiryTime(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
        .build();

    tempEntity.newWaiver(policyWaiver1App1);
    tempEntity.newWaiver(policyWaiver2App1);
    tempEntity.newWaiver(policyWaiver1App2);

    String orderBy = DashboardPolicyWaiverOrderByEnum.POLICY_NAME.name();
    risksFilterDTOBuilder
        .withOrderBy(orderBy).withMaxResults(10);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    assertThat(dashboardPolicyWaivers.numResults)
        .as("It should include all the waivers.")
        .isEqualTo(3);

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

    PolicyWaiver expiredPolicyWaiverApp1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy1.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().minus(11, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver activePolicyWaiverApp1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy2.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().plus(5, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver activePolicyWaiverApp2 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy2.getId())
        .withOwnerId(app2.getId())
        .withExpiryTime(Date.from(Instant.now().plus(3, ChronoUnit.DAYS)))
        .build();

    tempEntity.newWaiver(expiredPolicyWaiverApp1);
    tempEntity.newWaiver(activePolicyWaiverApp1);
    tempEntity.newWaiver(activePolicyWaiverApp2);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(
            risksFilterDTOBuilder
                .withExpirationDate(IN_7_DAYS)
                .withMaxResults(10)
                .build());

    assertThat(dashboardPolicyWaivers.numResults)
        .as("It should not add expired waiver to the result")
        .isEqualTo(2);

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

    PolicyWaiver expiredPolicyWaiverApp1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy1.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().minus(11, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver activePolicyWaiverApp1 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy2.getId())
        .withOwnerId(app1.getId())
        .withExpiryTime(Date.from(Instant.now().plus(5, ChronoUnit.DAYS)))
        .build();

    PolicyWaiver activePolicyWaiverApp2 = new TestPolicyWaiverBuilder()
        .withHash(RandomStringUtils.randomAlphabetic(5))
        .withPolicyId(policy2.getId())
        .withOwnerId(app2.getId())
        .withExpiryTime(Date.from(Instant.now().plus(3, ChronoUnit.DAYS)))
        .build();

    tempEntity.newWaiver(expiredPolicyWaiverApp1);
    tempEntity.newWaiver(activePolicyWaiverApp1);
    tempEntity.newWaiver(activePolicyWaiverApp2);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(
            risksFilterDTOBuilder
                .withExpirationDate(ALL)
                .withMaxResults(10)
                .build());

    assertThat(dashboardPolicyWaivers.numResults)
        .as("It should add expired waiver(s) to the result if ALL expiration date filter is selected.")
        .isEqualTo(3);
  }

  private PolicyWaiver createPolicyWaiverWithFullDetails(Application application) {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    Policy highThreatPolicy = tempEntity.newPolicy(application.getId(), "Very bad security threat", 9);

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact =
        new ConditionFact(ConditionTypes.SecurityVulnerabilityStatusConditionType.getId(), 0, "summary", "reason",
            triggerReference);
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);
    TreeMap<String, String> coordinates = new TreeMap<String, String>() {{
        this.put("artifactId", "a1");
        this.put("groupId", "g1");
        this.put("version", "v1");
        this.put("classifier", "c1");
        this.put("extension", "jar");
      }};
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    return tempEntity.newWaiver("hash", highThreatPolicy.getId(), application.getId(),
        singletonList(constraintFact), purl, EXACT_COMPONENT, "a comment", today, aWeekFromNow);
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
}
