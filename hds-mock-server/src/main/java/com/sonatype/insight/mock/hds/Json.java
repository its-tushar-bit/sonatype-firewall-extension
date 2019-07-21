/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class Json
{
  private static final ObjectMapper JSON = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static <T> T read(String json, Class<T> type) throws IOException {
    return JSON.readValue(json, type);
  }

  public static byte[] write(Object dto) {
    try {
      return JSON.writeValueAsBytes(dto);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
