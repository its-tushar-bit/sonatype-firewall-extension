/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.db.DatabaseConfig;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterIdCalculator
{
  private static final Logger log = LoggerFactory.getLogger(ClusterIdCalculator.class);

  static String calculateClusterId(DatabaseConfig databaseConfig) {
    if (databaseConfig == null) {
      return null;
    }

    String databaseHostname = databaseConfig.getHostname();
    if ("localhost".equalsIgnoreCase(databaseHostname)) {
      try {
        databaseHostname = InetAddress.getLocalHost().getHostName();
      }
      catch (Exception e) {
        log.warn("Cannot get the hostname for the local machine: " + e.getMessage(), e);
      }
    }

    String idBasedOnDatabaseConfig = databaseHostname + databaseConfig.getPort() + databaseConfig.getName();
    Hasher hasher = Hashing.sha512().newHasher();
    hasher.putString(idBasedOnDatabaseConfig, StandardCharsets.UTF_8);
    return hasher.hash().toString();
  }
}
