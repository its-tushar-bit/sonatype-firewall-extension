/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

class DropwizardAppenderConfig
{
  @JsonProperty
  String type;

  @JsonProperty
  String threshold;

  @JsonProperty
  String logFormat;

  @JsonProperty
  Integer queueSize;

  @JsonProperty
  Integer discardingThreshold;

  @JsonProperty
  Boolean neverBlock;

  @Deprecated
  @JsonProperty
  String timeZone;

  @Deprecated
  @JsonProperty
  List<Object> filterFactories;

  @JsonProperty
  Boolean includeCallerData;

  @Deprecated
  @JsonProperty
  Object messageRate;

  @JsonProperty
  Object layout;

  static class File
      extends DropwizardAppenderConfig
  {
    @JsonProperty
    String currentLogFilename;

    @JsonProperty
    String archivedLogFilenamePattern;

    @JsonProperty
    Integer archivedFileCount;

    @JsonProperty
    Boolean archive;

    @Deprecated
    @JsonProperty
    Object bufferSize;

    @Deprecated
    @JsonProperty
    Boolean immediateFlush;

    @Deprecated
    @JsonProperty
    Object maxFileSize;

    @Deprecated
    @JsonProperty
    Object totalSizeCap;
  }

  static class Console
      extends DropwizardAppenderConfig
  {
    @JsonProperty
    String target;
  }

  static class Syslog
      extends DropwizardAppenderConfig
  {
    @JsonProperty
    String host;

    @JsonProperty
    Integer port;

    @JsonProperty
    String facility;

    @JsonProperty
    String stackTracePrefix;

    @Deprecated
    @JsonProperty
    Boolean includeStackTrace;
  }

  static class Tcp
      extends DropwizardAppenderConfig
  {
    @JsonProperty
    String host;

    @JsonProperty
    Integer port;

    @JsonProperty
    String connectionTimeout;

    @Deprecated
    @JsonProperty
    Object sendBufferSize;

    @Deprecated
    @JsonProperty
    Boolean immediateFlush;
  }

  static class Tls
      extends DropwizardAppenderConfig
  {
    @JsonProperty
    String host;

    @JsonProperty
    Integer port;
  }

  static class Udp
      extends DropwizardAppenderConfig
  {
    @Deprecated
    @JsonProperty
    String host;

    @Deprecated
    @JsonProperty
    Integer port;
  }
}
