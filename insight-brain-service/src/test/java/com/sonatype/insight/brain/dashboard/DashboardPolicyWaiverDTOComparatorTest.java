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
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dashboard.DashboardPolicyWaiverDTOComparator.DashboardPolicyWaiverOrderByEnum;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.error.exception.BadRequestException;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_GOLANG;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_NPM;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_NUGET;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PYPI;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DashboardPolicyWaiverDTOComparatorTest
{
  private List<ComponentIdentifier> componentIdentifiers;

  private static final String ORDER_WAIVER_BY_DESC = "-";

  @Before
  public void before() {
    componentIdentifiers = new ArrayList<>();

    String[] chars = "abcdefgh".split("");
    String[] formats = {FORMAT_MAVEN, FORMAT_NPM, FORMAT_PYPI, FORMAT_GOLANG, FORMAT_NUGET};
    componentIdentifiers.addAll(IntStream.range(0, chars.length).mapToObj(i -> {
      TreeMap<String, String> coordinates = new TreeMap<>();
      String format = formats[i % formats.length];
      switch (format) {
        case FORMAT_NPM:
        case FORMAT_NUGET:
          coordinates.put("packageId", chars[i] + (i + 1));
          coordinates.put("version", "v1");
          break;
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
      return new ComponentIdentifier(format, coordinates);
    }).collect(Collectors.toList()));
  }

  @Test
  public void throwsExceptionOnBuild_OrderByNotSupported() {
    ThrowingCallable comparatorConstructorCall = () -> new DashboardPolicyWaiverDTOComparator("randomOrderBy");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(comparatorConstructorCall)
        .withMessage("Invalid orderBy property.");
  }

  @Test
  public void defaultsToExpirationDateAsc_OrderByIsBlank() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO laterExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(Date.from(seed.plus(
        Duration.ofDays(3)))).getBuiltDTO();
    DashboardPolicyWaiverDTO earlierExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(Date.from(seed.plus(
        Duration.ofDays(1)))).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator("");
    assertThat(comparator.compare(laterExpiring, earlierExpiring)).isGreaterThan(0);

    DashboardPolicyWaiverDTO laterComponentNameDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentIdentifier(componentIdentifiers.get(0))
        .withExpiryTime(Date.from(seed.plus(Duration.ofDays(3))))
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();
    DashboardPolicyWaiverDTO earlierComponentNameDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentIdentifier(componentIdentifiers.get(0))
        .withExpiryTime(Date.from(seed.plus(Duration.ofDays(1))))
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();
    assertThat(comparator.compare(laterComponentNameDto, earlierComponentNameDto)).isGreaterThan(0);

    DashboardPolicyWaiverDTO earlierDto = new DashboardPolicyWaiverDTOBuilder().withCreateTime(Date.from(seed.minus(
        Duration.ofDays(3)))).getBuiltDTO();
    DashboardPolicyWaiverDTO laterDto = new DashboardPolicyWaiverDTOBuilder().withCreateTime(Date.from(seed.minus(
        Duration.ofDays(1)))).getBuiltDTO();
    assertThat(comparator.compare(earlierDto, laterDto)).isEqualTo(0);

    DashboardPolicyWaiverDTO app1 =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.APPLICATION, "app1").getBuiltDTO();
    DashboardPolicyWaiverDTO app2 =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.APPLICATION, "app2").getBuiltDTO();
    assertThat(comparator.compare(app1, app2)).isEqualTo(0);

    DashboardPolicyWaiverDTO lowestPolicyName =
        new DashboardPolicyWaiverDTOBuilder().withPolicyName("Policy lvl 1").getBuiltDTO();
    DashboardPolicyWaiverDTO highestPolicyName =
        new DashboardPolicyWaiverDTOBuilder().withPolicyName("Policy lvl 9").getBuiltDTO();
    assertThat(comparator.compare(lowestPolicyName, highestPolicyName)).isEqualTo(0);

    DashboardPolicyWaiverDTO lowestThreat = new DashboardPolicyWaiverDTOBuilder().withThreatLevel(1).getBuiltDTO();
    DashboardPolicyWaiverDTO highestThreat = new DashboardPolicyWaiverDTOBuilder().withThreatLevel(9).getBuiltDTO();
    assertThat(comparator.compare(lowestThreat, highestThreat)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_ASC_ExactComponentsFirst() {
    DashboardPolicyWaiverDTO exactDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentIdentifier(componentIdentifiers.get(0))
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();
    DashboardPolicyWaiverDTO allComponentsDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentMatchStrategy(ALL_COMPONENTS).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE.toString());
    assertThat(comparator.compare(exactDto, allComponentsDto)).isLessThan(0);
    assertThat(comparator.compare(exactDto, exactDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_DESC_ExactComponentsFirst() {
    DashboardPolicyWaiverDTO exactDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentIdentifier(componentIdentifiers.get(0))
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();
    DashboardPolicyWaiverDTO allComponentsDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentMatchStrategy(ALL_COMPONENTS).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE);
    assertThat(comparator.compare(exactDto, allComponentsDto)).isLessThan(0);
    assertThat(comparator.compare(allComponentsDto, allComponentsDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_ASC_AllComponentsFirst() {
    DashboardPolicyWaiverDTO unknownDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();
    DashboardPolicyWaiverDTO allComponentsDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentMatchStrategy(ALL_COMPONENTS).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE.toString());
    assertThat(comparator.compare(allComponentsDto, unknownDto)).isLessThan(0);
    assertThat(comparator.compare(allComponentsDto, allComponentsDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_DESC_AllComponentsFirst() {
    DashboardPolicyWaiverDTO unknownDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();
    DashboardPolicyWaiverDTO allComponentsDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentMatchStrategy(ALL_COMPONENTS).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE);
    assertThat(comparator.compare(allComponentsDto, unknownDto)).isLessThan(0);
    assertThat(comparator.compare(unknownDto, unknownDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_ASC_UnknownLast() {
    DashboardPolicyWaiverDTO exactDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentIdentifier(componentIdentifiers.get(0))
        .withComponentMatchStrategy(ALL_VERSIONS).getBuiltDTO();
    DashboardPolicyWaiverDTO unknownDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE.toString());
    assertThat(comparator.compare(exactDto, unknownDto)).isLessThan(0);
    assertThat(comparator.compare(unknownDto, unknownDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_DESC_UnknownLast() {
    DashboardPolicyWaiverDTO exactDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentIdentifier(componentIdentifiers.get(0))
        .withComponentMatchStrategy(ALL_VERSIONS).getBuiltDTO();
    DashboardPolicyWaiverDTO unknownDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE);
    assertThat(comparator.compare(exactDto, unknownDto)).isLessThan(0);
    assertThat(comparator.compare(exactDto, exactDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_ASC_Alphabetical() {
    List<DashboardPolicyWaiverDTO> dtos = getWaiversToSort(componentIdentifiers);

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE.toString());
    assertAndCompareWaiverDtos(dtos, comparator, false);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_DESC_Alphabetical() {
    List<DashboardPolicyWaiverDTO> dtos = getWaiversToSort(componentIdentifiers);

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE);
    assertAndCompareWaiverDtos(dtos, comparator, true);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_ASC_SameComponentsExpiration() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO earlierDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentIdentifier(componentIdentifiers.get(0))
        .withExpiryTime(Date.from(seed.plus(Duration.ofDays(1))))
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();
    DashboardPolicyWaiverDTO laterDto = new DashboardPolicyWaiverDTOBuilder()
        .withComponentIdentifier(componentIdentifiers.get(0))
        .withExpiryTime(Date.from(seed.plus(Duration.ofDays(3))))
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE.toString());
    assertThat(comparator.compare(earlierDto, laterDto)).isLessThan(0);
    assertThat(comparator.compare(earlierDto, earlierDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_ASC_AllComponentsExpiration() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO earlierDto = new DashboardPolicyWaiverDTOBuilder()
        .withExpiryTime(Date.from(seed.plus(Duration.ofDays(1))))
        .withComponentMatchStrategy(ALL_COMPONENTS).getBuiltDTO();
    DashboardPolicyWaiverDTO laterDto = new DashboardPolicyWaiverDTOBuilder()
        .withExpiryTime(Date.from(seed.plus(Duration.ofDays(3))))
        .withComponentMatchStrategy(ALL_COMPONENTS).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE.toString());
    assertThat(comparator.compare(earlierDto, laterDto)).isLessThan(0);
    assertThat(comparator.compare(earlierDto, earlierDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_COMPONENT_SCOPE_ASC_UnknownExpiration() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO earlierDto = new DashboardPolicyWaiverDTOBuilder()
        .withExpiryTime(Date.from(seed.plus(Duration.ofDays(1))))
        .withComponentMatchStrategy(EXACT_COMPONENT).getBuiltDTO();
    DashboardPolicyWaiverDTO laterDto = new DashboardPolicyWaiverDTOBuilder()
        .withExpiryTime(Date.from(seed.plus(Duration.ofDays(3))))
        .withComponentMatchStrategy(ALL_VERSIONS).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.COMPONENT_SCOPE.toString());
    assertThat(comparator.compare(earlierDto, laterDto)).isLessThan(0);
    assertThat(comparator.compare(earlierDto, earlierDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_CREATION_DATE_ASC_EarlierCreatedFirst() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO earlierDto = new DashboardPolicyWaiverDTOBuilder().withCreateTime(Date.from(seed.minus(
        Duration.ofDays(3)))).getBuiltDTO();
    DashboardPolicyWaiverDTO laterDto = new DashboardPolicyWaiverDTOBuilder().withCreateTime(Date.from(seed.minus(
        Duration.ofDays(1)))).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.CREATION_DATE.toString());
    assertThat(comparator.compare(earlierDto, laterDto)).isLessThan(0);
    assertThat(comparator.compare(earlierDto, earlierDto)).isEqualTo(0);
  }

  @Test
  public void testCompare_CREATION_DATE_DESC_EarlierCreatedLast() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO earlierDTO = new DashboardPolicyWaiverDTOBuilder().withCreateTime(Date.from(seed.minus(
        Duration.ofDays(3)))).getBuiltDTO();
    DashboardPolicyWaiverDTO laterDTO = new DashboardPolicyWaiverDTOBuilder().withCreateTime(Date.from(seed.minus(
        Duration.ofDays(1)))).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.CREATION_DATE);
    assertThat(comparator.compare(earlierDTO, laterDTO)).isGreaterThan(0);
    assertThat(comparator.compare(laterDTO, laterDTO)).isEqualTo(0);
  }

  @Test
  public void testCompare_EXPIRATION_DATE_ASC_SoonerExpiringFirst() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO laterExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(Date.from(seed.plus(
        Duration.ofDays(3)))).getBuiltDTO();
    DashboardPolicyWaiverDTO earlierExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(Date.from(seed.plus(
        Duration.ofDays(1)))).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.EXPIRATION_DATE.toString());
    assertThat(comparator.compare(laterExpiring, earlierExpiring)).isGreaterThan(0);
    assertThat(comparator.compare(laterExpiring, laterExpiring)).isEqualTo(0);
  }

  @Test
  public void testCompare_EXPIRATION_DATE_DESC_SoonerExpiringLast() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO laterExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(Date.from(seed.plus(
        Duration.ofDays(3)))).getBuiltDTO();
    DashboardPolicyWaiverDTO earlierExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(Date.from(seed.plus(
        Duration.ofDays(1)))).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.EXPIRATION_DATE);
    assertThat(comparator.compare(laterExpiring, earlierExpiring)).isLessThan(0);
    assertThat(comparator.compare(earlierExpiring, earlierExpiring)).isEqualTo(0);
  }

  @Test
  public void testCompare_EXPIRATION_DATE_ASC_NeverExpiringLast() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO neverExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(null).getBuiltDTO();
    DashboardPolicyWaiverDTO earlierExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(Date.from(seed.plus(
        Duration.ofDays(1)))).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.EXPIRATION_DATE.toString());
    assertThat(comparator.compare(neverExpiring, earlierExpiring)).isGreaterThan(0);
    assertThat(comparator.compare(neverExpiring, neverExpiring)).isEqualTo(0);
  }

  @Test
  public void testCompare_EXPIRATION_DATE_DESC_NeverExpiringFirst() {
    Instant seed = Instant.now();
    DashboardPolicyWaiverDTO neverExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(null).getBuiltDTO();
    DashboardPolicyWaiverDTO earlierExpiring = new DashboardPolicyWaiverDTOBuilder().withExpiryTime(Date.from(seed.plus(
        Duration.ofDays(1)))).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.EXPIRATION_DATE);
    assertThat(comparator.compare(neverExpiring, earlierExpiring)).isLessThan(0);
    assertThat(comparator.compare(neverExpiring, neverExpiring)).isEqualTo(0);
  }

  @Test
  public void testCompare_OWNER_SCOPE_ASC() {
    DashboardPolicyWaiverDTO app1 =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.APPLICATION, "app1").getBuiltDTO();
    DashboardPolicyWaiverDTO app2 =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.APPLICATION, "app2").getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE.toString());
    assertThat(comparator.compare(app1, app2)).isLessThan(0);
    assertThat(comparator.compare(app1, app1)).isEqualTo(0);

    DashboardPolicyWaiverDTO organization =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.ORGANIZATION, "organization").getBuiltDTO();
    assertThat(comparator.compare(app1, organization)).isLessThan(0);
    assertThat(comparator.compare(organization, organization)).isEqualTo(0);

    DashboardPolicyWaiverDTO repository =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.REPOSITORY, "repository").getBuiltDTO();
    assertThat(comparator.compare(app1, repository)).isLessThan(0);
    assertThat(comparator.compare(organization, repository)).isLessThan(0);
    assertThat(comparator.compare(repository, repository)).isEqualTo(0);

    DashboardPolicyWaiverDTO repositoryContainer =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.REPOSITORY_CONTAINER, "Repository Managers")
            .getBuiltDTO();
    assertThat(comparator.compare(repositoryContainer, app1)).isGreaterThan(0);
    assertThat(comparator.compare(repositoryContainer, organization)).isGreaterThan(0);
    assertThat(comparator.compare(repositoryContainer, repository)).isGreaterThan(0);
    assertThat(comparator.compare(repositoryContainer, repositoryContainer)).isEqualTo(0);
  }

  @Test
  public void testCompare_OWNER_SCOPE_DESC() {
    DashboardPolicyWaiverDTO app1 =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.APPLICATION, "app1").getBuiltDTO();
    DashboardPolicyWaiverDTO app2 =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.APPLICATION, "app2").getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE);
    assertThat(comparator.compare(app1, app2)).isGreaterThan(0);
    assertThat(comparator.compare(app1, app1)).isEqualTo(0);

    DashboardPolicyWaiverDTO organization =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.ORGANIZATION, "organization").getBuiltDTO();
    assertThat(comparator.compare(app1, organization)).isGreaterThan(0);
    assertThat(comparator.compare(organization, organization)).isEqualTo(0);

    DashboardPolicyWaiverDTO repository =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.REPOSITORY, "repository").getBuiltDTO();
    assertThat(comparator.compare(app1, repository)).isGreaterThan(0);
    assertThat(comparator.compare(organization, repository)).isGreaterThan(0);
    assertThat(comparator.compare(repository, repository)).isEqualTo(0);

    DashboardPolicyWaiverDTO repositoryContainer =
        new DashboardPolicyWaiverDTOBuilder().withOwner(OwnerType.REPOSITORY_CONTAINER, "Repository Managers")
            .getBuiltDTO();
    assertThat(comparator.compare(repositoryContainer, app1)).isLessThan(0);
    assertThat(comparator.compare(repositoryContainer, organization)).isLessThan(0);
    assertThat(comparator.compare(repositoryContainer, repository)).isLessThan(0);
    assertThat(comparator.compare(repositoryContainer, repositoryContainer)).isEqualTo(0);
  }

  @Test
  public void should_sort_policy_waivers_by_expiry_date_when_owner_is_same() {
    DashboardPolicyWaiverDTO policyWaiver1App1 =
        new DashboardPolicyWaiverDTOBuilder()
            .withOwner(OwnerType.APPLICATION, "app1")
            .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
            .getBuiltDTO();

    DashboardPolicyWaiverDTO policyWaiver2App1 =
        new DashboardPolicyWaiverDTOBuilder()
            .withOwner(OwnerType.APPLICATION, "app1")
            .withExpiryTime(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
            .getBuiltDTO();

    DashboardPolicyWaiverDTO policyWaiver1App2 =
        new DashboardPolicyWaiverDTOBuilder()
            .withOwner(OwnerType.APPLICATION, "app2")
            .withExpiryTime(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
            .getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator =
        new DashboardPolicyWaiverDTOComparator(DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE.name());

    assertThat(comparator.compare(policyWaiver1App1, policyWaiver2App1))
        .as("Waiver with the nearest expiration date should come first")
        .isEqualTo(1);

    assertThat(comparator.compare(policyWaiver1App2, policyWaiver2App1))
        .as("Waivers are sorted by OWNER scope first irrespective of expiry dates")
        .isEqualTo(1);

    // test desc ordered sorting
    comparator =
        new DashboardPolicyWaiverDTOComparator(
            ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE.name());

    assertThat(comparator.compare(policyWaiver1App1, policyWaiver2App1))
        .as("It should sort (ASC) by expiry date when owner scope is same")
        .isEqualTo(1);
  }

  @Test
  public void should_sort_waivers_by_expiry_when_owner_is_same_desc_order() {
    DashboardPolicyWaiverDTO policyWaiver1App1 =
        new DashboardPolicyWaiverDTOBuilder()
            .withOwner(OwnerType.APPLICATION, "app1")
            .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
            .getBuiltDTO();

    DashboardPolicyWaiverDTO policyWaiver2App1 =
        new DashboardPolicyWaiverDTOBuilder()
            .withOwner(OwnerType.APPLICATION, "app1")
            .withExpiryTime(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
            .getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator =
        new DashboardPolicyWaiverDTOComparator(
            ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.OWNER_SCOPE.name());

    assertThat(comparator.compare(policyWaiver1App1, policyWaiver2App1))
        .as("It should sort (ASC) by expiry date when owner scope is same")
        .isEqualTo(1);
  }

  @Test
  public void should_sort_policy_waivers_by_expiry_date_when_policy_name_is_same() {
    DashboardPolicyWaiverDTO policyWaiver1App1 =
        new DashboardPolicyWaiverDTOBuilder()
            .withOwner(OwnerType.APPLICATION, "app1")
            .withExpiryTime(Date.from(Instant.now().plus(11, ChronoUnit.DAYS)))
            .withPolicyName("Z Policy Name")
            .getBuiltDTO();

    DashboardPolicyWaiverDTO policyWaiver2App1 =
        new DashboardPolicyWaiverDTOBuilder()
            .withOwner(OwnerType.APPLICATION, "app1")
            .withExpiryTime(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
            .withPolicyName("A Policy Name")
            .getBuiltDTO();

    DashboardPolicyWaiverDTO policyWaiver3App1 =
        new DashboardPolicyWaiverDTOBuilder()
            .withOwner(OwnerType.APPLICATION, "app1")
            .withExpiryTime(Date.from(Instant.now().plus(12, ChronoUnit.DAYS)))
            .withPolicyName("A Policy Name")
            .getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator =
        new DashboardPolicyWaiverDTOComparator(DashboardPolicyWaiverOrderByEnum.POLICY_NAME.name());

    assertThat(comparator.compare(policyWaiver1App1, policyWaiver2App1))
        .as("Waivers are sorted by policy name first. " +
            "When policy name is different, expiry date comparison is not needed.")
        .isPositive();

    assertThat(comparator.compare(policyWaiver2App1, policyWaiver3App1))
        .as("When 2 waivers have same policy name, " +
            "the waiver with nearest expiry date should come first")
        .isNegative();
  }

  @Test
  public void testCompare_POLICY_NAME_ASC() {
    DashboardPolicyWaiverDTO lowestPolicyName =
        new DashboardPolicyWaiverDTOBuilder().withPolicyName("Policy lvl 1").getBuiltDTO();
    DashboardPolicyWaiverDTO highestPolicyName =
        new DashboardPolicyWaiverDTOBuilder().withPolicyName("Policy lvl 9").getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator =
        new DashboardPolicyWaiverDTOComparator(DashboardPolicyWaiverOrderByEnum.POLICY_NAME.toString());
    assertThat(comparator.compare(lowestPolicyName, highestPolicyName)).isLessThan(0);
    assertThat(comparator.compare(lowestPolicyName, lowestPolicyName)).isEqualTo(0);
  }

  @Test
  public void testCompare_POLICY_NAME_DESC() {
    DashboardPolicyWaiverDTO lowestPolicyName =
        new DashboardPolicyWaiverDTOBuilder().withPolicyName("Policy lvl 1").getBuiltDTO();
    DashboardPolicyWaiverDTO highestPolicyName =
        new DashboardPolicyWaiverDTOBuilder().withPolicyName("Policy lvl 9").getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator =
        new DashboardPolicyWaiverDTOComparator(ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.POLICY_NAME);
    assertThat(comparator.compare(lowestPolicyName, highestPolicyName)).isGreaterThan(0);
    assertThat(comparator.compare(highestPolicyName, highestPolicyName)).isEqualTo(0);
  }

  @Test
  public void testCompare_THREAT_LEVEL_ASC_SmallestFirst() {
    DashboardPolicyWaiverDTO lowestThreat = new DashboardPolicyWaiverDTOBuilder().withThreatLevel(1).getBuiltDTO();
    DashboardPolicyWaiverDTO highestThreat = new DashboardPolicyWaiverDTOBuilder().withThreatLevel(9).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator =
        new DashboardPolicyWaiverDTOComparator(DashboardPolicyWaiverOrderByEnum.THREAT_LEVEL.toString());
    assertThat(comparator.compare(lowestThreat, highestThreat)).isLessThan(0);
    assertThat(comparator.compare(lowestThreat, lowestThreat)).isEqualTo(0);
  }

  @Test
  public void testCompare_THREAT_LEVEL_DESC_SmallestLast() {
    DashboardPolicyWaiverDTO lowestThreat = new DashboardPolicyWaiverDTOBuilder().withThreatLevel(1).getBuiltDTO();
    DashboardPolicyWaiverDTO highestThreat = new DashboardPolicyWaiverDTOBuilder().withThreatLevel(9).getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator =
        new DashboardPolicyWaiverDTOComparator(ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.THREAT_LEVEL);
    assertThat(comparator.compare(lowestThreat, highestThreat)).isGreaterThan(0);
    assertThat(comparator.compare(highestThreat, highestThreat)).isEqualTo(0);
  }

  @Test
  public void testCompare_THREAT_LEVEL_SameThreatLevel_SortsByCreationDate() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    DashboardPolicyWaiverDTO olderWaiver = new DashboardPolicyWaiverDTOBuilder()
        .withThreatLevel(9)
        .withCreateTime(Date.from(fiveDaysAgo))
        .getBuiltDTO();

    DashboardPolicyWaiverDTO newerWaiver = new DashboardPolicyWaiverDTOBuilder()
        .withThreatLevel(9)
        .withCreateTime(Date.from(twoDaysAgo))
        .getBuiltDTO();

    DashboardPolicyWaiverDTOComparator comparator =
        new DashboardPolicyWaiverDTOComparator(DashboardPolicyWaiverOrderByEnum.THREAT_LEVEL.toString());

    // When threat levels are equal, newer creation date should come first
    assertThat(comparator.compare(newerWaiver, olderWaiver)).isLessThan(0);
    assertThat(comparator.compare(olderWaiver, newerWaiver)).isGreaterThan(0);
  }

  private List<DashboardPolicyWaiverDTO> getWaiversToSort(List<ComponentIdentifier> componentIdentifiers) {
    ComponentMatcherStrategyForWaiver[] waiverTypes = {EXACT_COMPONENT, ALL_VERSIONS};
    return IntStream.range(0, componentIdentifiers.size()).mapToObj(i -> {
      ComponentMatcherStrategyForWaiver type = waiverTypes[i % waiverTypes.length];
      ComponentIdentifier componentIdentifier = componentIdentifiers.get(i);
      return new DashboardPolicyWaiverDTOBuilder()
          .withComponentIdentifier(componentIdentifier)
          .withComponentMatchStrategy(type).getBuiltDTO();
    }).collect(Collectors.toList());
  }

  private void assertAndCompareWaiverDtos(
      List<DashboardPolicyWaiverDTO> dtos,
      DashboardPolicyWaiverDTOComparator comparator,
      boolean greater
  )
  {
    for (int i = 0; i < dtos.size() - 1; i++) {
      DashboardPolicyWaiverDTO current = dtos.get(i);
      DashboardPolicyWaiverDTO next = dtos.get(i + 1);

      int compareResult = comparator.compare(current, next);
      if (greater) {
        assertThat(compareResult).isGreaterThan(0);
      }
      else {
        assertThat(compareResult).isLessThan(0);
      }
    }
    assertThat(comparator.compare(dtos.get(dtos.size() - 1), dtos.get(dtos.size() - 1))).isEqualTo(0);
  }

  private static class DashboardPolicyWaiverDTOBuilder
  {
    private final DashboardPolicyWaiverDTO dto = new DashboardPolicyWaiverDTO();

    DashboardPolicyWaiverDTO getBuiltDTO() {
      return dto;
    }

    DashboardPolicyWaiverDTOBuilder withCreateTime(Date createTime) {
      dto.createTime = createTime;
      return this;
    }

    DashboardPolicyWaiverDTOBuilder withExpiryTime(Date expiryTime) {
      dto.expiryTime = expiryTime;
      return this;
    }

    DashboardPolicyWaiverDTOBuilder withOwner(OwnerType ownerType, String ownerName) {
      dto.ownerType = ownerType.toString();
      dto.ownerName = ownerName;
      return this;
    }

    DashboardPolicyWaiverDTOBuilder withPolicyName(String policyName) {
      dto.policyName = policyName;
      return this;
    }

    DashboardPolicyWaiverDTOBuilder withThreatLevel(int threatLevel) {
      dto.threatLevel = threatLevel;
      return this;
    }

    DashboardPolicyWaiverDTOBuilder withComponentIdentifier(ComponentIdentifier componentIdentifier) {
      dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
      return this;
    }

    DashboardPolicyWaiverDTOBuilder withComponentMatchStrategy(
        ComponentMatcherStrategyForWaiver componentMatchStrategy
    )
    {
      dto.componentMatchStrategy = componentMatchStrategy;
      return this;
    }
  }

  @Test
  public void testCompare_POLICY_NAME_ASC_WithNullPolicyNames() {
    // Test ascending sort with null policy names
    Instant now = Instant.now();
    List<DashboardPolicyWaiverDTO> waivers = new ArrayList<>();

    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName("Policy B")
        .withExpiryTime(Date.from(now.plus(2, ChronoUnit.DAYS)))
        .getBuiltDTO());
    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName(null)
        .withExpiryTime(Date.from(now.plus(5, ChronoUnit.DAYS)))
        .getBuiltDTO());
    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName("Policy A")
        .withExpiryTime(Date.from(now.plus(1, ChronoUnit.DAYS)))
        .getBuiltDTO());
    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName(null)
        .withExpiryTime(Date.from(now.plus(3, ChronoUnit.DAYS)))
        .getBuiltDTO());

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.POLICY_NAME.toString());
    waivers.sort(comparator);

    // Verify: Non-null policies come first (A, B), then nulls (sorted by expiry)
    assertThat(waivers.get(0).policyName).isEqualTo("Policy A");
    assertThat(waivers.get(1).policyName).isEqualTo("Policy B");
    assertThat(waivers.get(2).policyName).isNull();
    assertThat(waivers.get(3).policyName).isNull();
    // Null policies sorted by expiry date (ascending)
    assertThat(waivers.get(2).expiryTime).isBefore(waivers.get(3).expiryTime);
  }

  @Test
  public void testCompare_POLICY_NAME_DESC_WithNullPolicyNames() {
    // Test descending sort with null policy names
    Instant now = Instant.now();
    List<DashboardPolicyWaiverDTO> waivers = new ArrayList<>();

    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName("Policy A")
        .withExpiryTime(Date.from(now.plus(1, ChronoUnit.DAYS)))
        .getBuiltDTO());
    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName(null)
        .withExpiryTime(Date.from(now.plus(5, ChronoUnit.DAYS)))
        .getBuiltDTO());
    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName("Policy B")
        .withExpiryTime(Date.from(now.plus(2, ChronoUnit.DAYS)))
        .getBuiltDTO());
    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName(null)
        .withExpiryTime(Date.from(now.plus(3, ChronoUnit.DAYS)))
        .getBuiltDTO());

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        ORDER_WAIVER_BY_DESC + DashboardPolicyWaiverOrderByEnum.POLICY_NAME);
    waivers.sort(comparator);

    // Verify: Non-null policies come first (B, A in desc order), then nulls (sorted by expiry desc)
    assertThat(waivers.get(0).policyName).isEqualTo("Policy B");
    assertThat(waivers.get(1).policyName).isEqualTo("Policy A");
    assertThat(waivers.get(2).policyName).isNull();
    assertThat(waivers.get(3).policyName).isNull();
    // Null policies sorted by expiry date descending (later expiry first)
    assertThat(waivers.get(2).expiryTime).isAfter(waivers.get(3).expiryTime);
  }

  @Test
  public void testCompare_POLICY_NAME_WithNullAndEqualPolicies() {
    // Test secondary sort when policy names are equal
    Instant now = Instant.now();
    List<DashboardPolicyWaiverDTO> waivers = new ArrayList<>();

    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName("Policy X")
        .withExpiryTime(Date.from(now.plus(5, ChronoUnit.DAYS)))
        .getBuiltDTO());
    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName("Policy X")
        .withExpiryTime(Date.from(now.plus(1, ChronoUnit.DAYS)))
        .getBuiltDTO());
    waivers.add(new DashboardPolicyWaiverDTOBuilder()
        .withPolicyName("Policy X")
        .withExpiryTime(null)
        .getBuiltDTO());

    DashboardPolicyWaiverDTOComparator comparator = new DashboardPolicyWaiverDTOComparator(
        DashboardPolicyWaiverOrderByEnum.POLICY_NAME.toString());
    waivers.sort(comparator);

    // Verify: All have same policy name, sorted by expiry (earliest first, null last)
    assertThat(waivers.get(0).policyName).isEqualTo("Policy X");
    assertThat(waivers.get(0).expiryTime).isEqualTo(Date.from(now.plus(1, ChronoUnit.DAYS)));
    assertThat(waivers.get(1).expiryTime).isEqualTo(Date.from(now.plus(5, ChronoUnit.DAYS)));
    assertThat(waivers.get(2).expiryTime).isNull();
  }
}
