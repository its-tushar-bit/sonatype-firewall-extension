/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.annotation.JsonProperty;

class DropwizardConnectorConfig
{
  @JsonProperty
  String type;

  @JsonProperty
  Object port;

  @JsonProperty
  String bindHost;

  @JsonProperty
  Object idleTimeout;

  @JsonProperty
  String keyStorePath;

  @JsonProperty
  String keyStorePassword;

  @JsonProperty
  String keyStoreType;

  @JsonProperty
  String trustStorePath;

  @JsonProperty
  String trustStorePassword;

  @JsonProperty
  String trustStoreType;

  @JsonProperty
  String certAlias;

  @JsonProperty
  String keyManagerPassword;

  @JsonProperty
  Boolean needClientAuth;

  @JsonProperty
  Boolean wantClientAuth;

  @JsonProperty
  String protocol;

  @Deprecated
  @JsonProperty
  Boolean inheritChannel;

  @Deprecated
  @JsonProperty
  Object headerCacheSize;

  @Deprecated
  @JsonProperty
  Object outputBufferSize;

  @Deprecated
  @JsonProperty
  Object maxRequestHeaderSize;

  @Deprecated
  @JsonProperty
  Object maxResponseHeaderSize;

  @Deprecated
  @JsonProperty
  Object inputBufferSize;

  @Deprecated
  @JsonProperty
  Object minResponseDataPerSecond;

  @Deprecated
  @JsonProperty
  Object minRequestDataPerSecond;

  @Deprecated
  @JsonProperty
  Object minBufferPoolSize;

  @Deprecated
  @JsonProperty
  Object bufferPoolIncrement;

  @Deprecated
  @JsonProperty
  Object maxBufferPoolSize;

  @Deprecated
  @JsonProperty
  Integer acceptorThreads;

  @Deprecated
  @JsonProperty
  Integer selectorThreads;

  @Deprecated
  @JsonProperty
  Integer acceptQueueSize;

  @Deprecated
  @JsonProperty
  Boolean reuseAddress;

  @Deprecated
  @JsonProperty
  Boolean useServerHeader;

  @Deprecated
  @JsonProperty
  Boolean useDateHeader;

  @Deprecated
  @JsonProperty
  Boolean useForwardedHeaders;

  @Deprecated
  @JsonProperty
  Boolean useProxyProtocol;

  @Deprecated
  @JsonProperty
  String httpCompliance;

  @Deprecated
  @JsonProperty
  String uriCompliance;

  @Deprecated
  @JsonProperty
  String requestCookieCompliance;

  @Deprecated
  @JsonProperty
  String responseCookieCompliance;

  @Deprecated
  @JsonProperty
  Integer maxConcurrentStreams;

  @Deprecated
  @JsonProperty
  Integer initialStreamRecvWindow;

  @Deprecated
  @JsonProperty
  String path;

  @Deprecated
  @JsonProperty
  Boolean deleteSocketFileOnStartup;

  @Deprecated
  @JsonProperty
  String keyStoreProvider;

  @Deprecated
  @JsonProperty
  String trustStoreProvider;

  @Deprecated
  @JsonProperty
  String crlPath;

  @Deprecated
  @JsonProperty
  Boolean enableCRLDP;

  @Deprecated
  @JsonProperty
  Boolean enableOCSP;

  @Deprecated
  @JsonProperty
  Integer maxCertPathLength;

  @Deprecated
  @JsonProperty
  String ocspResponderUrl;

  @Deprecated
  @JsonProperty
  String jceProvider;

  @Deprecated
  @JsonProperty
  Boolean validateCerts;

  @Deprecated
  @JsonProperty
  Boolean validatePeers;

  @Deprecated
  @JsonProperty
  Object supportedProtocols;

  @Deprecated
  @JsonProperty
  Object excludedProtocols;

  @Deprecated
  @JsonProperty
  Object supportedCipherSuites;

  @Deprecated
  @JsonProperty
  Object excludedCipherSuites;

  @Deprecated
  @JsonProperty
  Boolean allowRenegotiation;

  @Deprecated
  @JsonProperty
  String endpointIdentificationAlgorithm;

  @Deprecated
  @JsonProperty
  Boolean disableSniHostCheck;
}
