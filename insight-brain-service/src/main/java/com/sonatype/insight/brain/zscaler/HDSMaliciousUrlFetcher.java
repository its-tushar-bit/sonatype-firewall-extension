/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.MALICIOUS_URLS_PARTNER_ACCESS;

@Named("hds")
public class HDSMaliciousUrlFetcher
    implements ZScalerMaliciousUrlFetcher
{
  private static final String HDS_MALICIOUS_URLS_PATH = "rest/maliciousUrls";

  private static final Logger log = LoggerFactory.getLogger(HDSMaliciousUrlFetcher.class);

  private final HdsClient hdsClient;

  @Inject
  public HDSMaliciousUrlFetcher(final HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  @Override
  public InputStream fetchMaliciousUrls(ZScalerSupportedFormat format) {
    log.debug("Updating zScaler Malicious URLs for format: {}", format);
    try {
      String path = HDS_MALICIOUS_URLS_PATH + "/active/" + format.toString().toLowerCase(Locale.ROOT);
      Map<String, String> queryParams = new HashMap<>();

      if (MALICIOUS_URLS_PARTNER_ACCESS.isEnabled()) {
        queryParams.put("isPartnerAccess", "true");
      }

      return hdsClient.get(InputStream.class, path, queryParams);
    }
    catch (BadGatewayException e) {
      log.warn("Failed to get zScaler malicious URLs: {}", e.getMessage(), e);
      return null;
    }
  }
}
