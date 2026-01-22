/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;

import jakarta.ws.rs.core.UriBuilder;

import com.codeborne.selenide.Configuration;

public class BaseUrl
{
  public static String resolvePageUrl(String path, Object... parameters) {
    return undoUnnecessaryUrlEscapes(pageUriBuilder().fragment(path).build(parameters));
  }

  public static String resolveRestUrl(String path, Object... parameters) {
    return undoUnnecessaryUrlEscapes(restUriBuilder().path(path).build(parameters));
  }

  public static String resolveApiV2Url(String path, Object... parameters) {
    return undoUnnecessaryUrlEscapes(apiV2UriBuilder().path(path).build(parameters));
  }

  public static String resolveUiLinksUrl(String path, Object... parameters) {
    return undoUnnecessaryUrlEscapes(uiLinksUriBuilder().path(path).build(parameters));
  }

  private static String undoUnnecessaryUrlEscapes(URI uri) {
    return uri.toString().replaceAll("%2F", "/").replaceAll("%3F", "?").replaceAll("%2C", ",").replaceAll("%3A", ":");
  }

  private static UriBuilder pageUriBuilder() {
    return rootUriBuilder().path("assets/index.html");
  }

  private static UriBuilder restUriBuilder() {
    return rootUriBuilder().path("rest");
  }

  private static UriBuilder apiV2UriBuilder() {
    return rootUriBuilder().path("api/v2");
  }

  private static UriBuilder uiLinksUriBuilder() {
    return rootUriBuilder().path("ui/links");
  }

  public static UriBuilder rootUriBuilder() {
    return UriBuilder.fromUri(Configuration.baseUrl);
  }

  public static String resolveBaseUrl(String baseUrl) throws Exception {
    if (Configuration.remote != null && baseUrl.contains("localhost")) {
      // On some docker hosts the containers cannot use the loopback address of the host, so we need to lookup an
      // address that they can use
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        try {
          if (iface.isUp() && !iface.isLoopback()) {
            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
              InetAddress address = addresses.nextElement();
              String addressedUrl = baseUrl.replace("localhost", address.getHostAddress());
              try {
                if (!address.isLoopbackAddress() && address.isReachable(2000) && isReachable(addressedUrl)) {
                  return addressedUrl;
                }
              }
              catch (Exception ignored) {
                // try the next address
              }
            }
          }
        }
        catch (Exception ignored) {
          // try the next interface
        }
      }
    }
    return baseUrl;
  }

  /**
   * @param containerUrl a URL suitable for reaching the IQ server from within the selenium docker container
   * @return a URL suitable for reaching the IQ server from the host machine
   */
  public static String convertContainerUrlToHostUrl(String containerUrl) {
    return UriBuilder.fromUri(containerUrl).host("localhost").build().toString();
  }

  private static boolean isReachable(String url) {
    HttpURLConnection connection = null;

    try {
      connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setConnectTimeout(2000);
      connection.setReadTimeout(2000);
      connection.setRequestMethod("GET");
      int responseCode = connection.getResponseCode();
      return 200 <= responseCode && responseCode <= 399;
    }
    catch (IOException exception) {
      return false;
    }
    finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
