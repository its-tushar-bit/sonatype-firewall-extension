/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.DatabaseConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.telemetry.ClusterIdentificationService;
import com.sonatype.insight.brain.tenancy.TenantReference;

@Named
@Singleton
/**
 * The telemetry ID is an ID unique to the IQ server instance and it is used to identify and link telemetry data.
 * It has two parts (separated by a dash):
 * - The first part is randomly generated and it has 5 hex digits;
 * - The second part is the SHA1 of the hostname + IQ server HTTP port + all network interface hardware addresses,
 * truncated to the first 5 hex digits.
 *
 * The telemetry ID cannot be used to identify a customer or customer installation and it should not be linkable to a
 * customer or customer installation.
 * This means we cannot log the ID (or any parts of the ID) anywhere.
 *
 * Note: I chose 5 as length for the two parts of the ID because it gives a collision risk of 1 in 1,048,576,
 * which is well below what we need.
 */
public class TelemetryId
{
  @Deprecated
  public static final String TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME = "TELEMETRY_GENERATED_INSTANCE_ID";

  private final ClusterIdentificationService clusterIdentificationService;

  private final InsightConfig insightConfig;

  private final SystemConfigurationPropertyDAO dao;

  private final TenantReference<ClusterIdentity> tenantClusterIdentity = new TenantReference<>();

  private final String globalTelemetryId;

  @Inject
  public TelemetryId(
      InsightConfig insightConfig,
      final SystemConfigurationPropertyDAO dao,
      ClusterIdentificationService clusterIdentificationService)
  {
    this.insightConfig = insightConfig;
    this.dao = dao;
    this.clusterIdentificationService = clusterIdentificationService;
    this.globalTelemetryId = generateId();
  }

  protected String generateId() {
    return TelemetryIdGenerator.generateId(insightConfig, dao);
  }

  static String calculateClusterId(DatabaseConfig databaseConfig) {
    return ClusterIdCalculator.calculateClusterId(databaseConfig);
  }

  public String getId() {
    return getClusterIdentity().telemetryId();
  }

  public String getClusterId() {
    return getClusterIdentity().clusterId();
  }

  // lazily initialize the cluster identity per tenant
  private ClusterIdentity getClusterIdentity() {
    var clusterIdentity = tenantClusterIdentity.get();
    if (null == clusterIdentity) {
      // currently, the system effectively (and inadvertently) defaults the telemetry ID to what is in the global
      // configuration for multi-tenant;  so, we'll preserve that behavior for existing instances when seeding
      // cluster identification with an initial value;  at that point the telemetry ID is maintained per tenant
      final var computedTelemetryId = globalTelemetryId;
      final var computedClusterId = ClusterIdCalculator.calculateClusterId(insightConfig.getDatabase());

      var resolvedIds = clusterIdentificationService.resolveClusterIdentity(computedClusterId, computedTelemetryId);

      clusterIdentity = new ClusterIdentity(resolvedIds.assignedClusterId(), resolvedIds.assignedTelemetryId());
      tenantClusterIdentity.set(clusterIdentity);

      // the clusterIdentificationService can queue up telemetry to be sent once the identity is fully resolved and
      // set, which just happened above, so we can send that telemetry now
      clusterIdentificationService.sendTelemetry();
    }
    return clusterIdentity;
  }

  private record ClusterIdentity(String clusterId, String telemetryId) { }
}
