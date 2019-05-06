/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.65
 */
public class ApiProxyConfigurationDTOV2
{
  private List<String> proxyExcludeHosts;

  public ApiProxyConfigurationDTOV2() {
  }

  public ApiProxyConfigurationDTOV2(List<String> proxyExcludeHosts) {
    this.proxyExcludeHosts = unmodifiableList(proxyExcludeHosts);
  }

  public ApiProxyConfigurationDTOV2(String proxyExcludeHosts) {
    this(Stream.of(proxyExcludeHosts.split("[,\\s]"))
        .filter(proxy -> !proxy.isEmpty())
        .collect(toList()));
  }

  public List<String> getProxyExcludeHosts() {
    return proxyExcludeHosts == null ? emptyList() : proxyExcludeHosts;
  }
}
