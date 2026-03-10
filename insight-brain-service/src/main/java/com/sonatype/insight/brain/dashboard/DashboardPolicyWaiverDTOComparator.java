/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;

public class DashboardPolicyWaiverDTOComparator
    implements Comparator<DashboardPolicyWaiverDTO>
{
  private final DashboardPolicyWaiverOrderByEnum orderByField;

  private final boolean isAscending;

  public DashboardPolicyWaiverDTOComparator(final String orderBy) {
    if (StringUtils.isEmpty(orderBy)) {
      this.orderByField = DashboardPolicyWaiverOrderByEnum.EXPIRATION_DATE;
      this.isAscending = true;
    }
    else {
      boolean isOrderByDesc = orderBy.startsWith("-");
      try {
        this.orderByField = DashboardPolicyWaiverOrderByEnum
            .valueOf(isOrderByDesc ? orderBy.substring(1) : orderBy);
        this.isAscending = !isOrderByDesc;
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid orderBy property.", e);
      }
    }
  }

  @Override
  public int compare(final DashboardPolicyWaiverDTO o1, final DashboardPolicyWaiverDTO o2) {
    DashboardPolicyWaiverDTO dto1 = isAscending ? o1 : o2;
    DashboardPolicyWaiverDTO dto2 = isAscending ? o2 : o1;

    switch (orderByField) {
      case COMPONENT_SCOPE:
        return compareComponentNames(o1, o2);
      case EXPIRATION_DATE:
        return compareExpirationDates(o1, o2);
      case CREATION_DATE:
        return dto1.createTime.compareTo(dto2.createTime);
      case OWNER_SCOPE:
        return compareOwnerScope(dto1, dto2);
      case POLICY_NAME:
        return comparePolicyNames(o1, o2);
      case THREAT_LEVEL:
        return compareThreatLevel(o1, o2);
      default:
        throw new IllegalArgumentException(
            "unsupported order by " + orderByField);
    }
  }

  private int compareOwnerScope(final DashboardPolicyWaiverDTO dto1, final DashboardPolicyWaiverDTO dto2) {

    int sortByOwner =
        String.CASE_INSENSITIVE_ORDER.compare(getOwnerComparisonString(dto1), getOwnerComparisonString(dto2));

    // do secondary sorting by expiration date when owner is same
    return sortByOwner == 0 ? compareExpirationDates(dto1, dto2) : sortByOwner;
  }

  private int comparePolicyNames(final DashboardPolicyWaiverDTO dto1, final DashboardPolicyWaiverDTO dto2) {
    if (dto1.policyName != null && dto2.policyName != null) {
      return isAscending ? compareNonNullPolicyNames(dto1, dto2) : compareNonNullPolicyNames(dto2, dto1);
    }

    return compareNullPolicyNames(dto1, dto2);
  }

  private int compareNonNullPolicyNames(final DashboardPolicyWaiverDTO dto1, final DashboardPolicyWaiverDTO dto2) {
    int sortByPolicyName = String.CASE_INSENSITIVE_ORDER.compare(dto1.policyName, dto2.policyName);

    // do secondary sorting by expiration date when policy is same
    return sortByPolicyName == 0 ? compareExpirationDates(dto1, dto2) : sortByPolicyName;
  }

  private int compareNullPolicyNames(final DashboardPolicyWaiverDTO dto1, final DashboardPolicyWaiverDTO dto2) {
    if (dto1.policyName != null) {
      return -1;
    }
    else if (dto2.policyName != null) {
      return 1;
    }

    return compareExpirationDates(dto1, dto2);
  }

  private int compareThreatLevel(final DashboardPolicyWaiverDTO o1, final DashboardPolicyWaiverDTO o2) {
    int sortByThreatLevel = o2.threatLevel - o1.threatLevel;
    if (sortByThreatLevel == 0) { // When threat is same, sort by creation date DESCENDING
      if (o1.createTime == null && o2.createTime == null) {
        return 0; // Both null → equal
      }
      if (o1.createTime == null) {
        return 1;  // o1 is null → o1 comes AFTER o2
      }
      if (o2.createTime == null) {
        return -1;  //o2 is null → o1 comes BEFORE o2
      }
      return o2.createTime.compareTo(o1.createTime); // DESCENDING: newer dates come first
    }
    return isAscending ? -sortByThreatLevel : sortByThreatLevel;
  }

  private int compareComponentNames(final DashboardPolicyWaiverDTO dto1, final DashboardPolicyWaiverDTO dto2) {
    if (dto1.componentIdentifier != null && dto2.componentIdentifier != null) {
      return isAscending ? compareNonNullComponentNames(dto1, dto2) : compareNonNullComponentNames(dto2, dto1);
    }

    return compareNullComponentNames(dto1, dto2);
  }

  private int compareNonNullComponentNames(final DashboardPolicyWaiverDTO dto1, final DashboardPolicyWaiverDTO dto2) {
    int initialSorting = compareComponentIdentifiers(
        dto1.componentIdentifier.toComponentIdentifier(),
        dto2.componentIdentifier.toComponentIdentifier());

    return initialSorting != 0 ? initialSorting : compareExpirationDates(dto1, dto2);
  }

  private int compareNullComponentNames(final DashboardPolicyWaiverDTO dto1, final DashboardPolicyWaiverDTO dto2) {
    if (dto1.componentIdentifier != null) {
      return -1;
    }
    else if (dto2.componentIdentifier != null) {
      return 1;
    }

    if (dto1.componentMatchStrategy == ALL_COMPONENTS && dto2.componentMatchStrategy == ALL_COMPONENTS) {
      return compareExpirationDates(dto1, dto2);
    }
    else {
      return compareUnknownComponents(dto1, dto2);
    }
  }

  private int compareUnknownComponents(final DashboardPolicyWaiverDTO dto1, final DashboardPolicyWaiverDTO dto2) {
    if (dto1.componentMatchStrategy == ALL_COMPONENTS) {
      return -1;
    }
    else if (dto2.componentMatchStrategy == ALL_COMPONENTS) {
      return 1;
    }

    return compareExpirationDates(dto1, dto2);
  }

  private int compareComponentIdentifiers(final ComponentIdentifier ci1, final ComponentIdentifier ci2) {
    return String.CASE_INSENSITIVE_ORDER.compare(
        ci1.getCoordinates().values().toString(),
        ci2.getCoordinates().values().toString());
  }

  private int compareExpirationDates(
      final DashboardPolicyWaiverDTO dashboardPolicyWaiverDTO1,
      final DashboardPolicyWaiverDTO dashboardPolicyWaiverDTO2)
  {

    DashboardPolicyWaiverDTO dto1 = isAscending ? dashboardPolicyWaiverDTO1 : dashboardPolicyWaiverDTO2;
    DashboardPolicyWaiverDTO dto2 = isAscending ? dashboardPolicyWaiverDTO2 : dashboardPolicyWaiverDTO1;

    if (dto1.expiryTime != null && dto2.expiryTime != null) {
      return dto1.expiryTime.compareTo(dto2.expiryTime);
    }

    if (dto1.expiryTime != null) {
      return -1;
    }
    else if (dto2.expiryTime != null) {
      return 1;
    }

    return 0;
  }

  private String getOwnerComparisonString(DashboardPolicyWaiverDTO dto) {
    if (OwnerType.REPOSITORY_CONTAINER.equals(dto.ownerType)) {
      return dto.ownerName;
    }
    return dto.ownerType.toString() + " - " + dto.ownerName;
  }

  // Test visible enumeration
  enum DashboardPolicyWaiverOrderByEnum
  {
    COMPONENT_SCOPE, CREATION_DATE, EXPIRATION_DATE, OWNER_SCOPE, POLICY_NAME, THREAT_LEVEL
  }
}
