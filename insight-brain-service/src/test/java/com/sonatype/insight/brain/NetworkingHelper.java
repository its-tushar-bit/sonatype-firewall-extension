/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.AssumptionViolatedException;

public class NetworkingHelper
{
  public static void assumeDnsResolutionIsNormal() {
    try {
      // if an unknown host name resolves to an IP (e.g. to a site selling domains), some tests can't pass
      InetAddress.getByName("{unknown}");
      throw new AssumptionViolatedException("DNS resolution is atypical");
    }
    catch (final UnknownHostException e) {
      // that's what we expect to see
    }
  }
}
