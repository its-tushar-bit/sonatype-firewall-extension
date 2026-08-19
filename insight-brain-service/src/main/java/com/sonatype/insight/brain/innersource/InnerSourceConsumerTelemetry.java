/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.util.Set;

public class InnerSourceConsumerTelemetry
{
  public static final String ATTRIBUTE_NAME = "inner_source";

  private final String consumerAppId;

  private String realConsumerAppId;

  private final Set<InnerSourceProducerComponentTelemetry> producers;

  public InnerSourceConsumerTelemetry(
      final String consumerAppId,
      final String realConsumerAppId,
      final Set<InnerSourceProducerComponentTelemetry> producers)
  {
    this.consumerAppId = consumerAppId;
    this.realConsumerAppId = realConsumerAppId;
    this.producers = producers;
  }

  public Set<InnerSourceProducerComponentTelemetry> getProducers() {
    return producers;
  }

  public String getConsumerAppId() {
    return consumerAppId;
  }

  public String getRealConsumerAppId() {
    return realConsumerAppId;
  }
}
