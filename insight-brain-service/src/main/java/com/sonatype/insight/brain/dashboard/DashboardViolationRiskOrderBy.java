/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.error.exception.BadRequestException;

class DashboardViolationRiskOrderBy
{
  private DashboardViolationRiskOrderByEnum dashboardViolationRiskOrderByEnum;

  private boolean orderByAsc = true;

  DashboardViolationRiskOrderBy(
      DashboardViolationRiskOrderByEnum dashboardViolationRiskOrderByEnum,
      boolean orderByAsc)
  {
    this.dashboardViolationRiskOrderByEnum = dashboardViolationRiskOrderByEnum;
    this.setOrderByAsc(orderByAsc);
  }

  static List<DashboardViolationRiskOrderBy> getOrderBys(String orderByText) {
    List<DashboardViolationRiskOrderBy> dashboardViolationRiskOrderByList = new ArrayList<>();
    try {
      if (orderByText != null) {
        for (String orderBy : orderByText.split(",")) {
          boolean isOrderByDesc = orderBy.startsWith("-");

          DashboardViolationRiskOrderByEnum orderByEnum =
              DashboardViolationRiskOrderByEnum.valueOf(isOrderByDesc ? orderBy.substring(1) : orderBy);
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

  DashboardViolationRiskOrderByEnum getDashboardViolationRiskOrderByEnumRiskOrderByEnum() {
    return dashboardViolationRiskOrderByEnum;
  }

  boolean isOrderByAsc() {
    return orderByAsc;
  }

  void setOrderByAsc(boolean orderByAsc) {
    this.orderByAsc = orderByAsc;
  }
}
