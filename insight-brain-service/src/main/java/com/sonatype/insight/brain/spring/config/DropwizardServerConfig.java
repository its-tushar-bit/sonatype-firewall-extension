/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class DropwizardServerConfig
{
  @Deprecated
  @JsonProperty
  String type;

  @JsonProperty
  String applicationContextPath;

  @JsonProperty
  Integer maxThreads;

  @JsonProperty
  Integer minThreads;

  @JsonProperty
  List<Map<String, Object>> applicationConnectors;

  @JsonProperty
  List<Map<String, Object>> adminConnectors;

  @JsonProperty
  DropwizardGzipConfig gzip;

  @JsonProperty
  Map<String, Object> requestLog;

  @JsonProperty
  String shutdownGracePeriod;

  @JsonProperty
  Boolean enableVirtualThreads;

  @JsonProperty
  Boolean enableAdminVirtualThreads;

  @Deprecated
  @JsonProperty
  Integer maxQueuedRequests;

  @Deprecated
  @JsonProperty
  String idleThreadTimeout;

  @Deprecated
  @JsonProperty
  Integer nofileSoftLimit;

  @Deprecated
  @JsonProperty
  Integer nofileHardLimit;

  @Deprecated
  @JsonProperty
  Integer gid;

  @Deprecated
  @JsonProperty
  Integer uid;

  @Deprecated
  @JsonProperty
  String user;

  @Deprecated
  @JsonProperty
  String group;

  @Deprecated
  @JsonProperty
  String umask;

  @Deprecated
  @JsonProperty
  Boolean startsAsRoot;

  @Deprecated
  @JsonProperty
  Object allowedMethods;

  @Deprecated
  @JsonProperty
  String rootPath;

  @Deprecated
  @JsonProperty
  Boolean registerDefaultExceptionMappers;

  @Deprecated
  @JsonProperty
  Boolean enableThreadNameFilter;

  @Deprecated
  @JsonProperty
  Boolean dumpAfterStart;

  @Deprecated
  @JsonProperty
  Boolean dumpBeforeStop;

  @Deprecated
  @JsonProperty
  String responseMeteredLevel;

  @Deprecated
  @JsonProperty
  String metricPrefix;

  @JsonProperty
  String adminContextPath;

  @Deprecated
  @JsonProperty
  Integer adminMinThreads;

  @Deprecated
  @JsonProperty
  Integer adminMaxThreads;

  @Deprecated
  @JsonProperty
  Object connector;

  @Deprecated
  @JsonProperty
  Object serverPush;

  @Deprecated
  @JsonProperty
  Boolean detailedJsonProcessingExceptionMapper;
}
