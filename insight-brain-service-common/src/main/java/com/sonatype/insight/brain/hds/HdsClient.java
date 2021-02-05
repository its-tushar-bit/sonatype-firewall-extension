/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;

public interface HdsClient
{
  void start() throws Exception;

  void stop() throws Exception;

  void proxyServerConfigurationChanged();

  <T> T get(Class<T> clazz, String path, Map<String, String> queryParams, String... uriParams);

  <T> T get(Class<T> clazz, String url);

  <T> T relay(HttpServletRequest request, Class<T> clazz, String path, String... uriParams) throws IOException;

  <T> T relay(HttpServletRequest request,
              Class<T> clazz,
              String path,
              Map<String, String> queryParams,
              String... uriParams)
      throws IOException;

  <T> T relay(HttpServletRequest request,
              HdsClientAnalytics analytics,
              Class<T> clazz,
              String path,
              Map<String, String> queryParams,
              String... uriParams)
          throws IOException;

  /**
   * @since 1.46
   */
  void post(String path, HttpEntity httpEntity, String clientUserAgent);

  /**
   * @since 1.13.0
   */
  <T> T post(Class<T> clazz, String path, Object jsonSerializableObject, String... uriParams);

  /**
   * @since 1.43
   */
  <T> T post(HdsClientAnalytics analytics,
             Class<T> clazz,
             String path,
             String clientUserAgent,
             Object jsonSerializableObject,
             String... uriParams);

  /**
   * @since 1.8
   */
  <T> T put(
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String clientUserAgent,
      String path,
      File uploadFile,
      Map<String, String> queryParams,
      String... uriParams) throws IOException;
}
