/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.10
 */
class SafeProxySelector
    extends ProxySelector
{
  private final ProxySelector delegate;

  public static void install() {
    ProxySelector delegate = ProxySelector.getDefault();
    if (!(delegate instanceof SafeProxySelector)) {
      ProxySelector.setDefault(new SafeProxySelector(delegate));
    }
  }

  private SafeProxySelector(ProxySelector delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<Proxy> select(URI uri) {
    return fixProxies((delegate != null) ? delegate.select(uri) : null);
  }

  private static List<Proxy> fixProxies(List<Proxy> proxies) {
    List<Proxy> result = new ArrayList<>();
    if (proxies != null) {
      for (Proxy proxy : proxies) {
        if (isValid(proxy.address())) {
          result.add(proxy);
        }
      }
    }
    if (result.isEmpty()) {
      result.add(Proxy.NO_PROXY);
    }
    return result;
  }

  private static boolean isValid(SocketAddress address) {
    if (address == null) {
      return false;
    }
    if (address instanceof InetSocketAddress) {
      InetSocketAddress addr = (InetSocketAddress) address;
      if (addr.getPort() <= 0) {
        return false;
      }
      if (addr.getHostName() == null || addr.getHostName().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    if (delegate != null) {
      delegate.connectFailed(uri, sa, ioe);
    }
  }
}
