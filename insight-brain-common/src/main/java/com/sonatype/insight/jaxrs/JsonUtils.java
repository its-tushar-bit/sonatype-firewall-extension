/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-utils
package com.sonatype.insight.jaxrs;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils
{
  private static final ObjectMapper JSON = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  public static <T> T parse(final String json, final TypeReference<T> typeReference) throws IOException {
    return JSON.readValue(json, typeReference);
  }

  public static String toJson(Object pojo) throws IOException {
    return JSON.writeValueAsString(pojo);
  }
}
