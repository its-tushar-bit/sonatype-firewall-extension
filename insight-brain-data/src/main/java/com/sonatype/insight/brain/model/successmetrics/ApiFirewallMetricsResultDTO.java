/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.util.Date;

import com.sonatype.insight.json.store.ApiDateFormat;
import com.sonatype.insight.json.store.ISODateDeserializer;
import com.sonatype.insight.json.store.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ApiFirewallMetricsResultDTO
{
  private int firewallMetricsValue;

  @JsonSerialize(using = ISODateSerializer.class)
  @JsonDeserialize(using = ISODateDeserializer.class)
  private Date latestUpdatedTime;

  public ApiFirewallMetricsResultDTO(int firewallMetricsValue, Date latestUpdatedTime) {
    this.firewallMetricsValue = firewallMetricsValue;
    this.latestUpdatedTime = latestUpdatedTime;
  }

  public ApiFirewallMetricsResultDTO() {
  }

  public int getFirewallMetricsValue() {
    return firewallMetricsValue;
  }

  @ApiDateFormat
  public Date getLatestUpdatedTime() {
    return latestUpdatedTime;
  }
}
