/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

class ParsedUri
{
  final String path;

  final Map<String, Collection<Object>> query;

  public ParsedUri(String uri) {
    URI u = URI.create(uri);
    path = u.getPath();
    query = new LinkedHashMap<>();
    if (u.getRawQuery() != null) {
      MultiMap<String> urlEncoded = new MultiMap<>();
      UrlEncoded.decodeTo(u.getRawQuery(), urlEncoded, UrlEncoded.ENCODING);
      for (String key : urlEncoded.keySet()) {
        Collection<Object> values = new LinkedHashSet<>();
        query.put(key, values);
        Collection<String> decodedValues = urlEncoded.getValues(key);
        for (String decodedValue : decodedValues) {
          Object parsedValue = decodedValue;
          try {
            if (decodedValue.startsWith("{") && decodedValue.endsWith("}")) {
              parsedValue = Json.read(decodedValue, Map.class);
            }
            else if (decodedValue.startsWith("[") && decodedValue.endsWith("]")) {
              parsedValue = Json.read(decodedValue, List.class);
            }
          }
          catch (IOException e) {
            throw new IllegalArgumentException(e);
          }
          values.add(parsedValue);
        }
      }
    }
  }
}
