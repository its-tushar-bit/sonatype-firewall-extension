/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.db.DatabaseConfig;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterIdCalculatorTest
{
  @Test
  public void testCalculateClusterId() {
    String sampleConfigFingerPrint = getSampleDatabaseConfigFingerPrint();

    DatabaseConfig databaseConfig = getSampleDatabaseConfig();
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isEqualTo(sampleConfigFingerPrint);

    // changing hostname changes the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setHostname(databaseConfig.getHostname() + "-changed");
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isNotEqualTo(sampleConfigFingerPrint);

    // changing port changes the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setPort(databaseConfig.getPort() + 1);
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isNotEqualTo(sampleConfigFingerPrint);

    // changing name changes the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setName(databaseConfig.getName() + "-changed");
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isNotEqualTo(sampleConfigFingerPrint);

    // changing username does not modify the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setUsername(databaseConfig.getUsername() + "-changed");
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isEqualTo(sampleConfigFingerPrint);

    // changing password does not modify the fingerprint
    databaseConfig = getSampleDatabaseConfig();
    databaseConfig.setPassword(databaseConfig.getPassword() + "-changed");
    assertThat(TelemetryId.calculateClusterId(databaseConfig)).isEqualTo(sampleConfigFingerPrint);
  }

  private DatabaseConfig getSampleDatabaseConfig() {
    DatabaseConfig database = new DatabaseConfig();
    database.setHostname("aws-postgres");
    database.setPort(1234);
    database.setUsername("username");
    database.setPassword("password");
    return database;
  }

  private String getSampleDatabaseConfigFingerPrint() {
    return "38062673f41e45363a51161d8d0b91dcf6ff81cb0636743161ad2f73796420af5e62dd7f43d9f49f957724a64c6971399aab937" +
        "a0cdb05467720ae053c113a57";
  }
}
