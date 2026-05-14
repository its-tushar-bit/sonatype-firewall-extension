/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;

/**
 * Dedicated HTTP client with separate connection pool for accessing HDS in context of repository firewall quarantine.
 *
 * <p>
 * The pool size is configurable via the environment variable {@code NXIQ_FIREWALL_QUARANTINE_HDS_POOL_SIZE}
 * or the admin configuration property {@code firewallQuarantineHdsPoolSize}
 * (default: 20, range: 1-50). Increase this value if HDS pool exhaustion is observed under high HA load.
 * Do not exceed 50 without confirming HDS datamart DB pool capacity with the HDS team.
 * Changes to this property take effect only after a server restart.
 * This property is intended for support use only and should not be publicly documented.
 * Customers should not adjust this value without guidance from Sonatype support.
 *
 * @since 1.18
 */
@Named
@Singleton
public class FirewallQuarantineHdsClient
    extends HdsClient
{

  public static final int MAX_POOL_SIZE = 50;

  public static final int DEFAULT_POOL_SIZE = 20;

  @Inject
  public FirewallQuarantineHdsClient(
      InsightProxy proxy,
      ProductLicense productLicense,
      Configuration configuration,
      VersionService versionService,
      TelemetryId telemetryId,
      CurrentUser currentUser)
  {
    super(proxy, productLicense, configuration, versionService, telemetryId, currentUser,
        validatePoolSize(configuration.getFirewallQuarantineHdsPoolSize()));
  }

  // Defensive guard for callers outside the normal injection path (e.g. tests, future direct construction).
  // Under normal startup, ConfigurationUtils.getFirewallQuarantineHdsPoolSize() already clamps out-of-range
  // values to the default before this method is reached, so this throw is not reachable in production.
  @VisibleForTesting
  static int validatePoolSize(int poolSize) {
    if (poolSize <= 0 || poolSize > MAX_POOL_SIZE) {
      throw new IllegalArgumentException(
          "nexus.firewall.hds.quarantine.pool.size must be between 1 and " + MAX_POOL_SIZE + ", got: " + poolSize);
    }

    return poolSize;
  }
}
