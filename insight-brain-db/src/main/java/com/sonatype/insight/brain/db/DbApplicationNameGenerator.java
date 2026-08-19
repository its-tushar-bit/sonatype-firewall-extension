/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbApplicationNameGenerator
{
  private static final Logger log = LoggerFactory.getLogger(DbApplicationNameGenerator.class);

  /**
   * Generate a value for the 'application_name' property for Postgres (visible in pg_stat_activity). We include the
   * 'host name' of where this instance is running to help identify connections from multiple IQ nodes.
   *
   * @param applicationNamePrefix A prefix to add for the application name, so different {@link DataSource}s can be
   *          identified.
   * @return An application name in the form "<prefix>-hostname-<randomness>". If the hostname cannot be determined the
   *         value 'unknown' will be used. The 'randomness' is 5 random hex digits which can be used to differentiate
   *         connections when multiple IQ servers are executed on the same host.
   */
  public String generateApplicationNameWithHost(final String applicationNamePrefix) {
    String hostname = "unknown";
    try {
      hostname = getHostName();
    }
    catch (UnknownHostException e) {
      // no-op
      log.debug("Unable to determine host name for the database 'application_name'. Will default to 'unknown'", e);
    }
    // add some randomness for the case of running multiple instances on a single machine
    String randomness = UUID.randomUUID().toString().substring(0, 5);
    return applicationNamePrefix + "-" + hostname + "-" + randomness;
  }

  // visible for testing
  String getHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostName();
  }
}
