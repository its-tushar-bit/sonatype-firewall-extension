/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClient.RelayResponse;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.50
 */
@Named
@Singleton
public class PendoService
{
  public static final String HDS_TELEMETRY_PATH = "user-telemetry/v1";

  private static final Logger log = LoggerFactory.getLogger(PendoService.class);

  private static final String ID = "id";

  private static final String VERSION = "iq-server-version";

  private static final String BUILD = "iq-server-build";

  private final TelemetryId telemetryId;

  private final PendoCache pendoCache;

  private final HdsClient hdsClient;

  private final VersionService versionService;

  private final CurrentUser currentUser;

  private final ProductLicense productLicense;

  @Inject
  public PendoService(
      HdsClient hdsClient,
      PendoCache pendoCache,
      TelemetryId telemetryId,
      VersionService versionService,
      CurrentUser currentUser,
      ProductLicense productLicense)
  {
    this.hdsClient = hdsClient;
    this.pendoCache = pendoCache;
    this.telemetryId = telemetryId;
    this.versionService = versionService;
    this.currentUser = currentUser;
    this.productLicense = productLicense;
  }

  public PendoConfig getConfig() {
    PendoConfig pendoConfig = new PendoConfig();
    CustomerTelemetryProperties segmentInfo = pendoCache.getCustomerTelemetryProperties();

    if (segmentInfo.disabled == null || !segmentInfo.disabled) {
      pendoConfig.account.putAll(segmentInfo.segmentAttributes);
      pendoConfig.account.put(ID, getTelemetryId(segmentInfo));
      pendoConfig.account.put(VERSION, versionService.getVersion());
      pendoConfig.account.put(BUILD, versionService.getBuild());

      // add user info only if user is logged in
      UserPrincipal userPrincipal = currentUser.getUserPrincipal();
      if (userPrincipal != null) {
        pendoConfig.visitor.put(ID,
            Hashing.sha256().hashUnencodedChars(getTelemetryId(segmentInfo) + userPrincipal.getUsername()).toString());
      }
    }

    return pendoConfig;
  }

  public RelayResponse<InputStream> proxy(HttpServletRequest request, String pendoPath) {
    try {
      return hdsClient.relay(request, InputStream.class,
          UriBuilder.fromPath(HDS_TELEMETRY_PATH).path(pendoPath).build().toString());
    }
    catch (Exception e) {
      log.debug("An error occurred while proxying user telemetry", e);
      return new RelayResponse<>(new ByteArrayInputStream(new byte[0]));
    }
  }

  public byte[] getJavascript() {
    return pendoCache.getJs();
  }

  public static class PendoConfig
  {
    @JsonInclude(Include.NON_EMPTY)
    public Map<String, String> visitor = new HashMap<>();

    public Map<String, Object> account = new HashMap<>();
  }

  private String getTelemetryId(final CustomerTelemetryProperties segmentInfo) {
    Object iqAccountId = segmentInfo.segmentAttributes.get("iq_accountId");
    if (iqAccountId != null && !iqAccountId.toString().startsWith("UNKNOWN-")) {
      return iqAccountId.toString();
    }
    else if (productLicense.getContactCompany() != null) {
      return Hashing.sha256().hashString(productLicense.getContactCompany(), StandardCharsets.UTF_8).toString();
    }
    else {
      return telemetryId.getId();
    }
  }
}
