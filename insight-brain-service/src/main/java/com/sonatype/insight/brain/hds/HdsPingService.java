/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

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
    boolean alive = false;
    String errorMessage = null;

    try {
      pingHdsClient.get(String.class, "ping");

      alive = true;
    }
    catch (Exception e) {
      String incidentId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      errorMessage = e.getMessage() + " (ID " + incidentId + ").";
      log.error("{} (ID {})", e.getMessage(), incidentId, e);
    }

    return new PingResponseDTO(errorMessage, alive);
  }
}
