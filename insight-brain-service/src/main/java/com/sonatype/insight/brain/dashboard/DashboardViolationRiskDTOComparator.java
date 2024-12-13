/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;
import java.util.List;

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
              "unsupported order by "
                  + dashboardViolationRiskOrderBy.getDashboardViolationRiskOrderByEnumRiskOrderByEnum());
      }
    }

    return rel;
  }
}
