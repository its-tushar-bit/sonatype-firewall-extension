/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;

@Named
@Provider
class InsightJacksonMessageBodyProvider
    extends JacksonMessageBodyProvider
{
  @Inject
  public InsightJacksonMessageBodyProvider(ObjectMapper mapper) {
    super(mapper);
  }

  @Override
  protected boolean hasMatchingMediaType(MediaType mediaType) {
    // As per https://tools.ietf.org/html/rfc7578#section-4.4, the content type of parts in a multipart/form-data
    // entity defaults to text/plain and browser-based form submissions have no way to override that. To still allow
    // easy use and parsing of JSON content, we need to accept text/plain as well.
    if (MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType)) {
      return true;
    }
    return super.hasMatchingMediaType(mediaType);
  }
}
