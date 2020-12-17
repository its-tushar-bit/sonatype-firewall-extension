/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;

/**
 * Dedicated HTTP client with separate connection pool for accessing HDS in context of repository firewall quarantine.
 * 
 * @since 1.18
 */
@Named
@Singleton
public class FirewallQuarantineHdsClient
    extends DefaultHdsClient
{
  @Inject
  public FirewallQuarantineHdsClient(final InsightProxy proxy,
                                     ProductLicense productLicense,
                                     InsightConfig insightConfig,
                                     VersionService versionService,
                                     TelemetryId telemetryId)
  {
    super(proxy, productLicense, insightConfig, versionService, telemetryId, 20);
  }
}
