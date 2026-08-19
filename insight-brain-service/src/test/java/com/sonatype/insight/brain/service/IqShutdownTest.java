/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

// TODO: Convert to Spring test configuration
// This test needs to be rewritten to use Spring Boot test configuration.
// The old getBrainModules() hook has been removed as part of the Spring migration.
//
// Original test verified:
// - TaskScheduler is shutdown after TenantManaged beans are deregistered (CLM-24625)
// - This ensures quartz jobs can clean up during de-registration
//
// To migrate: Use @SpringBootTest with @TestConfiguration to register
// the IqShutdownTestTenantManaged bean as a TenantManaged bean.

public class IqShutdownTest
{
  // Test class temporarily disabled pending Spring migration
}
