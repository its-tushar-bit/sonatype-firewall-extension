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

import com.sonatype.insight.brain.utils.Retry;

import org.apache.http.HttpEntity;

public interface HdsClient
{
  class RelayResponse<T>
  {
    public T content;

    public String contentType;

    public RelayResponse(T content) {
      this(content, null);
    }

    public RelayResponse(T content, String contentType) {
      this.content = content;
      this.contentType = contentType;
    }
  }

  void start() throws Exception;

  void stop() throws Exception;

  void serverConfigurationChanged();

  <T> T get(Class<T> clazz, String path, Map<String, String> queryParams, String... uriParams);

  <T> T get(Retry retry, Class<T> clazz, String path, Map<String, String> queryParams, String... uriParams);

  <T> T get(
      Class<T> clazz,
      String path,
      String clientUserAgent,
      Map<String, String> queryParams,
      String... uriParams);

  <T> T get(
      Retry retry,
      Class<T> clazz,
      String path,
      String clientUserAgent,
      Map<String, String> queryParams,
      String... uriParams);

  <T> T get(Class<T> clazz, String url);

  <T> T get(Retry retry, Class<T> clazz, String url);

  <T> RelayResponse<T> relay(
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      String... uriParams) throws IOException;

  <T> RelayResponse<T> relay(
      Retry retry,
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      String... uriParams) throws IOException;

  <T> RelayResponse<T> relay(
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException;

  <T> RelayResponse<T> relay(
      Retry retry,
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException;

  <T> RelayResponse<T> relay(
      HttpServletRequest request,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException;

  <T> RelayResponse<T> relay(
      Retry retry,
      HttpServletRequest request,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException;

  /**
   * @since 1.46
   */
  void post(String path, HttpEntity httpEntity, String clientUserAgent);

  void post(Retry retry, String path, HttpEntity httpEntity, String clientUserAgent);

  /**
   * @since 1.13.0
   */
  <T> T post(Class<T> clazz, String path, Object jsonSerializableObject, String... uriParams);

  <T> T post(Retry retry, Class<T> clazz, String path, Object jsonSerializableObject, String... uriParams);

  /**
   * @since 1.43
   */
  <T> T post(
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      String clientUserAgent,
      Object jsonSerializableObject,
      String... uriParams);

  <T> T post(
      Retry retry,
      HdsClientAnalytics analytics,
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

  <T> T put(
      Retry retry,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String clientUserAgent,
      String path,
      File uploadFile,
      Map<String, String> queryParams,
      String... uriParams) throws IOException;
}
