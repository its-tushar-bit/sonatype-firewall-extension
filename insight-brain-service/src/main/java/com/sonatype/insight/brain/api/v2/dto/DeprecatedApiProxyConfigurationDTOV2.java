/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * @since 1.65
 *
 * @deprecated Replaced by {@link ApiProxyServerConfigurationDTO} in 1.84.
 */
@Deprecated
public class DeprecatedApiProxyConfigurationDTOV2
{
  private List<String> proxyExcludeHosts;

  public DeprecatedApiProxyConfigurationDTOV2() {
  }

  public DeprecatedApiProxyConfigurationDTOV2(List<String> proxyExcludeHosts) {
    this.proxyExcludeHosts = unmodifiableList(proxyExcludeHosts);
  }

  public List<String> getProxyExcludeHosts() {
    return proxyExcludeHosts == null ? emptyList() : proxyExcludeHosts;
  }
}
