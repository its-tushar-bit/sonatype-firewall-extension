/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.hash.Hashing;
import org.apache.shiro.SecurityUtils;
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

  private final TelemetryId telemetryId;

  private final PendoCache pendoCache;

  private final HdsClient hdsClient;

  private final VersionService versionService;

  @Inject
  public PendoService(HdsClient hdsClient,
                      PendoCache pendoCache,
                      TelemetryId telemetryId,
                      VersionService versionService)
  {
    this.hdsClient = hdsClient;
    this.pendoCache = pendoCache;
    this.telemetryId = telemetryId;
    this.versionService = versionService;
  }

  public PendoConfig getConfig() {
    PendoConfig pendoConfig = new PendoConfig();
    CustomerTelemetryProperties segmentInfo = pendoCache.getCustomerTelemetryProperties();

    if (segmentInfo.disabled == null || !segmentInfo.disabled) {
      pendoConfig.account.putAll(segmentInfo.segmentAttributes);
      pendoConfig.account.put(ID, telemetryId.getId());
      pendoConfig.account.put(VERSION, versionService.getVersion());

      // add user info only if user is logged in
      Object principal = SecurityUtils.getSubject().getPrincipal();
      if (principal != null) {
        pendoConfig.visitor.put(ID, Hashing.sha256().hashUnencodedChars(telemetryId.getId() + principal).toString());
      }
    }

    return pendoConfig;
  }

  public InputStream proxy(HttpServletRequest request, String pendoPath) {
    try {

      return hdsClient.relay(request, InputStream.class,
          UriBuilder.fromPath(HDS_TELEMETRY_PATH).path(pendoPath).build().toString());
    }
    catch (Exception e) {
      log.debug("An error occurred while proxying user telemetry", e);
      return new ByteArrayInputStream(new byte[0]);
    }
  }

  public File getJavascript() {
    return pendoCache.getJs();
  }

  public static class PendoConfig
  {
    @JsonInclude(Include.NON_EMPTY)
    public Map<String, String> visitor = new HashMap<>();

    public Map<String, Object> account = new HashMap<>();
  }
}
