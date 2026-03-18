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
class ComponentRiskDTOComparator
    implements Comparator<ComponentRiskDTO>
{
  private ComponentRiskOrderBy componentRiskOrderBy;

  public ComponentRiskDTOComparator(String orderBy) {
    this.componentRiskOrderBy = ComponentRiskOrderBy.getOrderBy(orderBy);
  }

  @Override
  public int compare(ComponentRiskDTO o1, ComponentRiskDTO o2) {
    int result = 0;
    if (componentRiskOrderBy != null) {
      ComponentRiskDTO ob1 = componentRiskOrderBy.isOrderByAsc() ? o1 : o2;
      ComponentRiskDTO ob2 = componentRiskOrderBy.isOrderByAsc() ? o2 : o1;

      result = switch (componentRiskOrderBy.getComponentRiskOrderByEnum()) {
        case CRITICAL_RISK -> ob1.scoreCritical - ob2.scoreCritical;
        case MODERATE_RISK -> ob1.scoreModerate - ob2.scoreModerate;
        case LOW_RISK -> ob1.scoreLow - ob2.scoreLow;
        case NAME -> String.CASE_INSENSITIVE_ORDER.compare(ob1.derivedComponentName, ob2.derivedComponentName);
        case NUMBER_OF_AFFECTED_APPS -> Integer.compare(ob1.affectedApplications, ob2.affectedApplications);
        case SEVERE_RISK -> ob1.scoreSevere - ob2.scoreSevere;
        case TOTAL_RISK -> ob1.score - ob2.score;
        default -> throw new IllegalArgumentException(
            "unsupported order by " + componentRiskOrderBy.componentRiskOrderByEnum);
      };
    }

    if (result != 0) {
      return result;
    }

    // If the objects are equal, use the hash to establish a complete order.
    // This matches the Postgres implementation.
    String hash1 = o1.hash;
    String hash2 = o2.hash;
    if (hash1 == null) {
      if (hash2 == null) {
        return 0;
      }
      return 1; // nulls last
    }
    else {
      if (hash2 == null) {
        return -1; // nulls last
      }
      return hash1.compareTo(hash2);
    }
  }

  private static class ComponentRiskOrderBy
  {
    private ComponentRiskOrderByEnum componentRiskOrderByEnum;

    private boolean orderByAsc = true;

    public ComponentRiskOrderBy(ComponentRiskOrderByEnum componentRiskOrderByEnum, boolean orderByAsc) {
      this.componentRiskOrderByEnum = componentRiskOrderByEnum;
      this.setOrderByAsc(orderByAsc);
    }

    public static ComponentRiskOrderBy getOrderBy(String orderByText) {
      ComponentRiskOrderBy componentRiskOrderBy = null;
      try {
        if (orderByText != null) {
          boolean isOrderByDesc = orderByText.startsWith("-");

          ComponentRiskOrderByEnum orderByEnum = ComponentRiskOrderByEnum
              .valueOf(isOrderByDesc ? orderByText.substring(1) : orderByText);
          if (orderByEnum != null) {
            componentRiskOrderBy = new ComponentRiskOrderBy(orderByEnum, !isOrderByDesc);
          }
        }
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid orderBy property.", e);
      }
      return componentRiskOrderBy;
    }

    public ComponentRiskOrderByEnum getComponentRiskOrderByEnum() {
      return componentRiskOrderByEnum;
    }

    public boolean isOrderByAsc() {
      return orderByAsc;
    }

    public void setOrderByAsc(boolean orderByAsc) {
      this.orderByAsc = orderByAsc;
    }
  }
}
