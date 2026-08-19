/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.Comparator;

import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusDTO;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

public class IntegrationStatusDTOComparator
    implements Comparator<IntegrationStatusDTO>
{
  private final IntegrationStatusOrderBy integrationStatusOrderBy;

  public IntegrationStatusDTOComparator(final String orderBy) {
    this.integrationStatusOrderBy = IntegrationStatusOrderBy.getOrderBy(orderBy);
  }

  @Override
  public int compare(IntegrationStatusDTO o1, IntegrationStatusDTO o2) {
    if (integrationStatusOrderBy != null) {
      final IntegrationStatusDTO ob1 = integrationStatusOrderBy.isOrderByAsc() ? o1 : o2;
      final IntegrationStatusDTO ob2 = integrationStatusOrderBy.isOrderByAsc() ? o2 : o1;

      switch (integrationStatusOrderBy.getIntegrationSummaryOrderByEnum()) {
        case NAME:
          return String.CASE_INSENSITIVE_ORDER.compare(ob1.getApplicationName(), ob2.getApplicationName());
        case COMMIT:
          if (ob1.getLastCommitTimestamp() == ob2.getLastCommitTimestamp()) {
            return String.CASE_INSENSITIVE_ORDER.compare(ob1.getApplicationName(), ob2.getApplicationName());
          }
          return getTimeDiff(ob1.getLastCommitTimestamp(), ob2.getLastCommitTimestamp());
        case EVALUATION:
          if (ob1.getLastEvaluationTimestamp() == ob2.getLastEvaluationTimestamp()) {
            return String.CASE_INSENSITIVE_ORDER.compare(ob1.getApplicationName(), ob2.getApplicationName());
          }
          return getTimeDiff(ob1.getLastEvaluationTimestamp(), ob2.getLastEvaluationTimestamp());
        case TOTAL_RISK:
          if (ob1.getTotalRiskScore() == ob2.getTotalRiskScore()) {
            return String.CASE_INSENSITIVE_ORDER.compare(ob1.getApplicationName(), ob2.getApplicationName());
          }
          return ob1.getTotalRiskScore() - ob2.getTotalRiskScore();
        default:
          throw new IllegalArgumentException(
              "Unsupported order by " + integrationStatusOrderBy.getIntegrationSummaryOrderByEnum());
      }
    }

    return 0;
  }

  private static int getTimeDiff(final long ob1Timestamp, final long ob2Timestamp) {
    // Account for potential integer over/underflow
    final long timeDiff = ob1Timestamp - ob2Timestamp;
    if (timeDiff < 0) {
      return -1;
    }
    else if (timeDiff > 0) {
      return 1;
    }
    else {
      return (int) timeDiff;
    }
  }

  private static class IntegrationStatusOrderBy
  {
    private final IntegrationStatusOrderByEnum integrationStatusOrderByEnum;

    private final boolean asc;

    public IntegrationStatusOrderBy(final IntegrationStatusOrderByEnum integrationStatusOrderByEnum, boolean asc) {
      this.integrationStatusOrderByEnum = integrationStatusOrderByEnum;
      this.asc = asc;
    }

    public static IntegrationStatusOrderBy getOrderBy(final String orderByText) {
      try {
        if (StringUtils.isNotEmpty(orderByText)) {
          final boolean isOrderByDesc = orderByText.startsWith("-");

          final IntegrationStatusOrderByEnum orderByEnum = IntegrationStatusOrderByEnum
              .valueOf(isOrderByDesc ? orderByText.substring(1) : orderByText);
          return new IntegrationStatusOrderBy(orderByEnum, !isOrderByDesc);
        }
        return null;
      }
      catch (final IllegalArgumentException e) {
        throw new BadRequestException("Invalid orderBy property " + orderByText, e);
      }
    }

    public IntegrationStatusOrderByEnum getIntegrationSummaryOrderByEnum() {
      return integrationStatusOrderByEnum;
    }

    public boolean isOrderByAsc() {
      return asc;
    }
  }
}
