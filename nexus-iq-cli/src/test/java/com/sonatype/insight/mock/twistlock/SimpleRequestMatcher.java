/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.twistlock;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.UrlEncoded;

public class SimpleRequestMatcher
    implements RequestMatcher
{
  private final ParsedUri targetUri;

  public SimpleRequestMatcher(String uri) {
    targetUri = new ParsedUri(uri);
  }

  @Override
  public boolean matches(String uri) {
    ParsedUri requestUri = new ParsedUri(uri);
    if (!targetUri.path.equals(requestUri.path)) {
      return false;
    }
    for (Map.Entry<String, Collection<Object>> entry : targetUri.query.entrySet()) {
      Collection<Object> targetParam = entry.getValue();
      Collection<Object> requestParam = requestUri.query.get(entry.getKey());
      if (!targetParam.equals(requestParam)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SimpleRequestMatcher)) {
      return false;
    }
    SimpleRequestMatcher that = (SimpleRequestMatcher) obj;
    return targetUri.path.equals(that.targetUri.path) && targetUri.query.equals(that.targetUri.query);
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + targetUri.path.hashCode();
    hash = hash * 31 + targetUri.query.hashCode();
    return hash;
  }

  private static class ParsedUri
  {
    final String path;

    final Map<String, Collection<Object>> query;

    public ParsedUri(String uri) {
      URI u = URI.create(uri);
      path = u.getPath();
      query = new LinkedHashMap<>();
      if (u.getRawQuery() != null) {
        UrlEncoded urlEncoded = new UrlEncoded(u.getRawQuery());
        for (String key : (Collection<String>) urlEncoded.keySet()) {
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
}
