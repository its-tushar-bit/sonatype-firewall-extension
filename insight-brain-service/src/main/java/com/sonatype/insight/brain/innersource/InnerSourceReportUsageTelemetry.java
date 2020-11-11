/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;

public class InnerSourceReportUsageTelemetry
{
  public static final String ATTRIBUTE_NAME = "inner_source";

  private final String consumerAppId;

  private final Set<String> producerAppIds;

  public InnerSourceReportUsageTelemetry(final String consumerAppId, final Set<String> producerAppIds) {
    this.consumerAppId = HdsClientAnalytics.obfuscate(consumerAppId);
    this.producerAppIds = obfuscateProducersId(producerAppIds);
  }

  public String getConsumerAppId() {
    return consumerAppId;
  }

  public Set<String> getProducerAppIds() {
    return producerAppIds;
  }

  private Set<String> obfuscateProducersId(final Set<String> producerAppIds) {
    return producerAppIds.stream().map(HdsClientAnalytics::obfuscate).collect(Collectors.toSet());
  }
}
