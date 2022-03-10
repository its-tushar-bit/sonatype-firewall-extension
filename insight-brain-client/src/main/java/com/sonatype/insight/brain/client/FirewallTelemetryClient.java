/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.repository.FirewallTelemetry;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;

/**
 * @since 1.135.0
 */
public class FirewallTelemetryClient
    extends AbstractRequestClient
{
  private static final String RESOURCE_PATH = "rest/integration/repositories/firewall-telemetry";

  public FirewallTelemetryClient(final Configuration config) {
    super(config);
  }

  public void postFirewallTelemetryData(FirewallTelemetry firewallTelemetry) throws IOException {
    final ByteArrayEntity entity = new ByteArrayEntity(JsonUtils.generate(firewallTelemetry),
        ContentType.APPLICATION_JSON);
    verifyStatusCode(path(RESOURCE_PATH).post(entity));
  }
}
