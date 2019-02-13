/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

import com.sonatype.insight.error.exception.BadRequestException;

/**
 * Sorts the component risk DTOs based on the orderBy property.
 */
class ApplicationRiskScoreDTOComparator
    implements Comparator<ApplicationRiskScoreDTO>
{
  private ApplicationRiskScoreOrderBy applicationRiskScoreOrderBy;

  public ApplicationRiskScoreDTOComparator(String orderBy) {
    this.applicationRiskScoreOrderBy = ApplicationRiskScoreOrderBy.getOrderBy(orderBy);
  }

  @Override
  public int compare(ApplicationRiskScoreDTO o1, ApplicationRiskScoreDTO o2) {
    if (applicationRiskScoreOrderBy != null) {
      ApplicationRiskScoreDTO ob1 = applicationRiskScoreOrderBy.isOrderByAsc() ? o1 : o2;
      ApplicationRiskScoreDTO ob2 = applicationRiskScoreOrderBy.isOrderByAsc() ? o2 : o1;

      switch (applicationRiskScoreOrderBy.getApplicationRiskOrderByEnum()) {
        case CRITICAL_RISK:
          return ob1.totalApplicationRisk.criticalRisk - ob2.totalApplicationRisk.criticalRisk;
        case MODERATE_RISK:
          return ob1.totalApplicationRisk.moderateRisk - ob2.totalApplicationRisk.moderateRisk;
        case LOW_RISK:
          return ob1.totalApplicationRisk.lowRisk - ob2.totalApplicationRisk.lowRisk;
        case NAME:
          return String.CASE_INSENSITIVE_ORDER.compare(ob1.applicationName, ob2.applicationName);
        case SEVERE_RISK:
          return ob1.totalApplicationRisk.severeRisk - ob2.totalApplicationRisk.severeRisk;
        case TOTAL_RISK:
          return ob1.totalApplicationRisk.totalRisk - ob2.totalApplicationRisk.totalRisk;
        default:
          throw new IllegalArgumentException(
              "unsupported order by " + applicationRiskScoreOrderBy.applicationRiskOrderByEnum);
      }
    }

    return 0;
  }

  private static class ApplicationRiskScoreOrderBy
  {
    private ApplicationRiskOrderByEnum applicationRiskOrderByEnum;

    private boolean orderByAsc = true;

    public ApplicationRiskScoreOrderBy(ApplicationRiskOrderByEnum applicationRiskOrderByEnum, boolean orderByAsc) {
      this.applicationRiskOrderByEnum = applicationRiskOrderByEnum;
      this.setOrderByAsc(orderByAsc);
    }

    public static ApplicationRiskScoreOrderBy getOrderBy(String orderByText) {
      ApplicationRiskScoreOrderBy applicationRiskScoreOrderBy = null;
      try {
        if (orderByText != null) {
          boolean isOrderByDesc = orderByText.startsWith("-");

          ApplicationRiskOrderByEnum orderByEnum = ApplicationRiskOrderByEnum
              .valueOf(isOrderByDesc ? orderByText.substring(1) : orderByText);
          applicationRiskScoreOrderBy = new ApplicationRiskScoreOrderBy(orderByEnum, !isOrderByDesc);
        }
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid orderBy property.", e);
      }
      return applicationRiskScoreOrderBy;
    }

    public ApplicationRiskOrderByEnum getApplicationRiskOrderByEnum() {
      return applicationRiskOrderByEnum;
    }

    public boolean isOrderByAsc() {
      return orderByAsc;
    }

    public void setOrderByAsc(boolean orderByAsc) {
      this.orderByAsc = orderByAsc;
    }
  }
}
