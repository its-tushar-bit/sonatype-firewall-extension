/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.47
 */
@Named
public class HdsPingService
{
  private static final Logger log = LoggerFactory.getLogger(HdsPingService.class);

  private final PingHdsClient pingHdsClient;

  @Inject
  public HdsPingService(final PingHdsClient pingHdsClient) {
    this.pingHdsClient = pingHdsClient;
  }

  /**
   * Perform a GET request of the HDS ping endpoint.
   */
  public PingResponseDTO pingHds() {
    PingResponseDTO response =  new PingResponseDTO();

    try {
      pingHdsClient.get(String.class, "ping");

      response.alive = true;
    }
    catch (Exception e) {
      response.incidentId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      response.errorMessage = e.getMessage();
      log.error("{} (ID {})", e.getMessage(), response.incidentId, e);
    }

    return response;
  }
}
