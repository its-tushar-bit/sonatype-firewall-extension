/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

/**
 * @since 1.39
 */
public enum ApplicationRiskOrderByEnum
{
  NAME,
  TOTAL_RISK,
  CRITICAL_RISK,
  SEVERE_RISK,
  MODERATE_RISK,
  LOW_RISK,
  /** Martha list default — {@code lastEvaluationTime} API token (CLM-42229). */
  LAST_EVALUATION_TIME;

  public static ApplicationRiskOrderByEnum fromOrderByToken(final String token) {
    return switch (token) {
      case "NAME" -> NAME;
      case "TOTAL_RISK" -> TOTAL_RISK;
      case "CRITICAL_RISK" -> CRITICAL_RISK;
      case "SEVERE_RISK" -> SEVERE_RISK;
      case "MODERATE_RISK" -> MODERATE_RISK;
      case "LOW_RISK" -> LOW_RISK;
      case "lastEvaluationTime" -> LAST_EVALUATION_TIME;
      default -> throw new IllegalArgumentException("Unknown orderBy token: " + token);
    };
  }
}
