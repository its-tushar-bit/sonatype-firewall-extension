/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.error.exception.BadRequestException;

class DashboardViolationRiskDTOComparator
    implements Comparator<DashboardViolationRiskDTO>
{
  private List<DashboardViolationRiskOrderBy> dashboardViolationRiskOrderByList;

  public DashboardViolationRiskDTOComparator(String orderBy) {
    this.dashboardViolationRiskOrderByList = DashboardViolationRiskOrderBy.getOrderBys(orderBy);
  }

  @Override
  public int compare(DashboardViolationRiskDTO o1, DashboardViolationRiskDTO o2) {
    int rel = 0;

    for (DashboardViolationRiskOrderBy dashboardViolationRiskOrderBy : dashboardViolationRiskOrderByList) {
      DashboardViolationRiskDTO ob1 = dashboardViolationRiskOrderBy.isOrderByAsc() ? o1 : o2;
      DashboardViolationRiskDTO ob2 = dashboardViolationRiskOrderBy.isOrderByAsc() ? o2 : o1;

      switch (dashboardViolationRiskOrderBy.getDashboardViolationRiskOrderByEnumRiskOrderByEnum()) {
        case AGE:
          rel = Long.compare(ob1.firstOccurrenceTime, ob2.firstOccurrenceTime);
          if (rel != 0) {
            return rel;
          }
          break;
        case APPLICATION_NAME:
          rel = String.CASE_INSENSITIVE_ORDER.compare(ob1.applicationName, ob2.applicationName);
          if (rel != 0) {
            return rel;
          }
          break;
        case COMPONENT_NAME:
          rel = String.CASE_INSENSITIVE_ORDER.compare(ob1.derivedComponentName, ob2.derivedComponentName);
          if (rel != 0) {
            return rel;
          }
          break;
        case POLICY_NAME:
          rel = String.CASE_INSENSITIVE_ORDER.compare(ob1.policyName, ob2.policyName);
          if (rel != 0) {
            return rel;
          }
          break;
        case THREAT_LEVEL:
          rel = Integer.compare(ob1.threatLevel, ob2.threatLevel);
          if (rel != 0) {
            return rel;
          }
          break;
        case POLICY_VIOLATION_ID:
          rel = ob1.policyViolationId != null ? ob1.policyViolationId.compareTo(ob2.policyViolationId) : 1;
          if (rel != 0) {
            return rel;
          }
          break;
        default:
          throw new IllegalArgumentException(
              "unsupported order by " + dashboardViolationRiskOrderBy.dashboardViolationRiskOrderByEnum);
      }
    }

    return rel;
  }

  private static class DashboardViolationRiskOrderBy
  {
    private DashboardViolationRiskOrderByEnum dashboardViolationRiskOrderByEnum;

    private boolean orderByAsc = true;

    public DashboardViolationRiskOrderBy(
        DashboardViolationRiskOrderByEnum dashboardViolationRiskOrderByEnum,
        boolean orderByAsc)
    {
      this.dashboardViolationRiskOrderByEnum = dashboardViolationRiskOrderByEnum;
      this.setOrderByAsc(orderByAsc);
    }

    public static List<DashboardViolationRiskOrderBy> getOrderBys(String orderByText) {
      List<DashboardViolationRiskOrderBy> dashboardViolationRiskOrderByList = new ArrayList<>();
      try {
        if (orderByText != null) {
          for (String orderBy : orderByText.split(",")) {
            boolean isOrderByDesc = orderBy.startsWith("-");

            DashboardViolationRiskOrderByEnum orderByEnum = DashboardViolationRiskOrderByEnum
                .valueOf(isOrderByDesc ? orderBy.substring(1) : orderBy);
            if (orderByEnum != null) {
              dashboardViolationRiskOrderByList.add(new DashboardViolationRiskOrderBy(orderByEnum, !isOrderByDesc));
            }
          }
        }
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid orderBy property.", e);
      }
      return dashboardViolationRiskOrderByList;
    }

    public DashboardViolationRiskOrderByEnum getDashboardViolationRiskOrderByEnumRiskOrderByEnum() {
      return dashboardViolationRiskOrderByEnum;
    }

    public boolean isOrderByAsc() {
      return orderByAsc;
    }

    public void setOrderByAsc(boolean orderByAsc) {
      this.orderByAsc = orderByAsc;
    }
  }
}
