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

/**
 * Sorts the NewestRiskDTOs based on the orderBy property.
 */
class NewestRiskDTOComparator
    implements Comparator<NewestRiskDTO>
{
  private List<NewestRiskOrderBy> newestRiskOrderByList;

  public NewestRiskDTOComparator(String orderBy) {
    this.newestRiskOrderByList = NewestRiskOrderBy.getOrderBys(orderBy);
  }

  @Override
  public int compare(NewestRiskDTO o1, NewestRiskDTO o2) {
    int rel = 0;

    for (NewestRiskOrderBy newestRiskOrderBy : newestRiskOrderByList) {
      NewestRiskDTO ob1 = newestRiskOrderBy.isOrderByAsc() ? o1 : o2;
      NewestRiskDTO ob2 = newestRiskOrderBy.isOrderByAsc() ? o2 : o1;

      switch (newestRiskOrderBy.getNewestRiskOrderByEnum()) {
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
          throw new IllegalArgumentException("unsupported order by " + newestRiskOrderBy.newestRiskOrderByEnum);
      }
    }

    return rel;
  }

  private static class NewestRiskOrderBy
  {
    private NewestRiskOrderByEnum newestRiskOrderByEnum;

    private boolean orderByAsc = true;

    public NewestRiskOrderBy(NewestRiskOrderByEnum newestRiskOrderByEnum, boolean orderByAsc) {
      this.newestRiskOrderByEnum = newestRiskOrderByEnum;
      this.setOrderByAsc(orderByAsc);
    }

    public static List<NewestRiskOrderBy> getOrderBys(String orderByText) {
      List<NewestRiskOrderBy> newestRiskOrderByList = new ArrayList<>();
      try {
        if (orderByText != null) {
          for (String orderBy : orderByText.split(",")) {
            boolean isOrderByDesc = orderBy.startsWith("-");

            NewestRiskOrderByEnum orderByEnum = NewestRiskOrderByEnum
                .valueOf(isOrderByDesc ? orderBy.substring(1) : orderBy);
            if (orderByEnum != null) {
              newestRiskOrderByList.add(new NewestRiskOrderBy(orderByEnum, !isOrderByDesc));
            }
          }
        }
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid orderBy property.", e);
      }
      return newestRiskOrderByList;
    }

    public NewestRiskOrderByEnum getNewestRiskOrderByEnum() {
      return newestRiskOrderByEnum;
    }

    public boolean isOrderByAsc() {
      return orderByAsc;
    }

    public void setOrderByAsc(boolean orderByAsc) {
      this.orderByAsc = orderByAsc;
    }
  }
}
