/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.InputStream;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("hds")
public class HDSMaliciousUrlFetcher
    implements ZScalerMaliciousUrlFetcher
{
  private static final String HDS_MALICIOUS_URLS_PATH = "rest/component/details/firewall/maliciousUrls";

  private static final Logger log = LoggerFactory.getLogger(HDSMaliciousUrlFetcher.class);

  private final HdsClient hdsClient;

  @Inject
  public HDSMaliciousUrlFetcher(final HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  @Override
  public InputStream fetchMaliciousUrls(ZScalerFormat format) {
    log.debug("Updating zScaler Malicious URLs for format: {}", format);
    try {
      return hdsClient.get(InputStream.class, HDS_MALICIOUS_URLS_PATH + "/" +
          format.toString().toLowerCase(Locale.ROOT));
    }
    catch (BadGatewayException e) {
      throw new RuntimeException("Failed to get zScaler malicious URLs: " + e.getMessage(), e);
    }
  }
}
