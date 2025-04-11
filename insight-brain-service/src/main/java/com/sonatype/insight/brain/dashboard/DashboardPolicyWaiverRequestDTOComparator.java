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

public class DashboardPolicyWaiverRequestDTOComparator
    implements Comparator<DashboardPolicyWaiverRequestDTO>
{
  private final DashboardPolicyWaiverRequestOrderByEnum orderByField;

  private final boolean isAscending;

  public DashboardPolicyWaiverRequestDTOComparator(String orderBy) {
    if (StringUtils.isEmpty(orderBy)) {
      this.orderByField = DashboardPolicyWaiverRequestOrderByEnum.CREATION_DATE;
      this.isAscending = true;
    }
    else {
      boolean isOrderByDesc = orderBy.startsWith("-");
      try {
        this.orderByField =
            DashboardPolicyWaiverRequestOrderByEnum.valueOf(isOrderByDesc ? orderBy.substring(1) : orderBy);
        this.isAscending = !isOrderByDesc;
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid orderBy property.", e);
      }
    }
  }

  @Override
  public int compare(DashboardPolicyWaiverRequestDTO o1, DashboardPolicyWaiverRequestDTO o2) {
    DashboardPolicyWaiverRequestDTO dto1 = isAscending ? o1 : o2;
    DashboardPolicyWaiverRequestDTO dto2 = isAscending ? o2 : o1;

    switch (orderByField) {
      case COMPONENT_SCOPE:
        return compareComponentNames(o1, o2);
      case CREATION_DATE:
        return dto1.requestTime.compareTo(dto2.requestTime);
      case OWNER_SCOPE:
        return compareOwnerScope(dto1, dto2);
      case POLICY_NAME:
        return comparePolicyNames(dto1, dto2);
      case THREAT_LEVEL:
        return dto1.threatLevel - dto2.threatLevel;
      case STATUS:
        return dto1.status.compareTo(dto2.status);
      default:
        throw new BadRequestException("unsupported order by " + orderByField);
    }
  }

  private int compareOwnerScope(DashboardPolicyWaiverRequestDTO dto1, DashboardPolicyWaiverRequestDTO dto2) {
    int sortByOwner =
        String.CASE_INSENSITIVE_ORDER.compare(getOwnerComparisonString(dto1), getOwnerComparisonString(dto2));

    // do secondary sorting by expiration date when owner is same
    return sortByOwner == 0 ? compareExpirationDates(dto1, dto2) : sortByOwner;
  }

  private int comparePolicyNames(DashboardPolicyWaiverRequestDTO dto1, DashboardPolicyWaiverRequestDTO dto2) {
    int sortByPolicyName = String.CASE_INSENSITIVE_ORDER.compare(dto1.policyName, dto2.policyName);

    // do secondary sorting by expiration date when policy is same
    return sortByPolicyName == 0 ? compareExpirationDates(dto1, dto2) : sortByPolicyName;
  }

  private int compareComponentNames(DashboardPolicyWaiverRequestDTO dto1, DashboardPolicyWaiverRequestDTO dto2) {
    if (dto1.componentIdentifier != null && dto2.componentIdentifier != null) {
      return isAscending ? compareNonNullComponentNames(dto1, dto2) : compareNonNullComponentNames(dto2, dto1);
    }

    return compareNullComponentNames(dto1, dto2);
  }

  private int compareNonNullComponentNames(DashboardPolicyWaiverRequestDTO dto1, DashboardPolicyWaiverRequestDTO dto2) {
    int initialSorting = compareComponentIdentifiers(dto1.componentIdentifier.toComponentIdentifier(),
        dto2.componentIdentifier.toComponentIdentifier());

    return initialSorting != 0 ? initialSorting : compareExpirationDates(dto1, dto2);
  }

  private int compareNullComponentNames(DashboardPolicyWaiverRequestDTO dto1, DashboardPolicyWaiverRequestDTO dto2) {
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

  private int compareUnknownComponents(DashboardPolicyWaiverRequestDTO dto1, DashboardPolicyWaiverRequestDTO dto2) {
    if (dto1.componentMatchStrategy == ALL_COMPONENTS) {
      return -1;
    }
    else if (dto2.componentMatchStrategy == ALL_COMPONENTS) {
      return 1;
    }

    return compareExpirationDates(dto1, dto2);
  }

  private int compareComponentIdentifiers(ComponentIdentifier ci1, ComponentIdentifier ci2) {
    return String.CASE_INSENSITIVE_ORDER.compare(ci1.getCoordinates().values().toString(),
        ci2.getCoordinates().values().toString());
  }

  private int compareExpirationDates(
      DashboardPolicyWaiverRequestDTO dashboardPolicyWaiverRequestDTO1,
      DashboardPolicyWaiverRequestDTO dashboardPolicyWaiverRequestDTO2)
  {

    DashboardPolicyWaiverRequestDTO dto1 =
        isAscending ? dashboardPolicyWaiverRequestDTO1 : dashboardPolicyWaiverRequestDTO2;
    DashboardPolicyWaiverRequestDTO dto2 =
        isAscending ? dashboardPolicyWaiverRequestDTO2 : dashboardPolicyWaiverRequestDTO1;

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

  private String getOwnerComparisonString(DashboardPolicyWaiverRequestDTO dto) {
    if (OwnerType.REPOSITORY_CONTAINER.equals(dto.ownerType)) {
      return dto.ownerName;
    }
    return dto.ownerType.toString() + " - " + dto.ownerName;
  }

  // Test visible enumeration
  enum DashboardPolicyWaiverRequestOrderByEnum
  {
    COMPONENT_SCOPE, CREATION_DATE, OWNER_SCOPE, POLICY_NAME, THREAT_LEVEL, STATUS
  }
}
