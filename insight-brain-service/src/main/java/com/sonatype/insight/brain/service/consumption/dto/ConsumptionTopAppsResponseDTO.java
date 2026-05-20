/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption.dto;

import java.util.List;

/**
 * Response DTO wrapping top consuming applications with summary totals.
 *
 * @since 1.204
 */
public class ConsumptionTopAppsResponseDTO
{
  private List<ConsumptionTopAppDTO> apps;

  private int totalApps;

  private long totalConsumed;

  public ConsumptionTopAppsResponseDTO() {
  }

  public ConsumptionTopAppsResponseDTO(List<ConsumptionTopAppDTO> apps, int totalApps, long totalConsumed) {
    this.apps = apps;
    this.totalApps = totalApps;
    this.totalConsumed = totalConsumed;
  }

  public List<ConsumptionTopAppDTO> getApps() {
    return apps;
  }

  public void setApps(List<ConsumptionTopAppDTO> apps) {
    this.apps = apps;
  }

  public int getTotalApps() {
    return totalApps;
  }

  public void setTotalApps(int totalApps) {
    this.totalApps = totalApps;
  }

  public long getTotalConsumed() {
    return totalConsumed;
  }

  public void setTotalConsumed(long totalConsumed) {
    this.totalConsumed = totalConsumed;
  }
}
