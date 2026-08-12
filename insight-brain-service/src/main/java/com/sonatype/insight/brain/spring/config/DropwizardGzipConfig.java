/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

class DropwizardGzipConfig
{
  @JsonProperty
  Boolean enabled;

  @JsonProperty
  Object minimumEntitySize;

  @JsonProperty
  List<String> compressedMimeTypes;

  @Deprecated
  @JsonProperty
  Object bufferSize;

  @Deprecated
  @JsonProperty
  List<String> includedMethods;

  @Deprecated
  @JsonProperty
  Integer deflateCompressionLevel;

  @Deprecated
  @JsonProperty
  Boolean gzipCompatibleInflation;

  @Deprecated
  @JsonProperty
  Boolean syncFlush;

  @JsonProperty
  List<String> excludedMimeTypes;

  @Deprecated
  @JsonProperty
  List<String> excludedPaths;

  @Deprecated
  @JsonProperty
  List<String> includedPaths;

  @Deprecated
  @JsonProperty
  List<String> excludedUserAgentPatterns;
}
