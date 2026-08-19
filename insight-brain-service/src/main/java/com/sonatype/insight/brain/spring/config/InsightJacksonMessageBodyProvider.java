/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

/**
 * Jackson provider using IQ's configured {@link ObjectMapper}.
 * <p>
 * Browser multipart form fields default to {@code text/plain} when the part does not declare a content type. Legacy
 * Dropwizard/Jersey accepted those parts for JSON DTOs, so this provider keeps that compatibility while still relying
 * on
 * the configured application mapper for serialization behavior.
 */
@Provider
public class InsightJacksonMessageBodyProvider
    extends JacksonJsonProvider
{
  public InsightJacksonMessageBodyProvider(ObjectMapper mapper) {
    super(mapper);
  }

  @Override
  protected boolean hasMatchingMediaType(MediaType mediaType) {
    if (mediaType != null && MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType)) {
      return true;
    }
    return super.hasMatchingMediaType(mediaType);
  }
}
