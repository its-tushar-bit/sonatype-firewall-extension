/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.internal.UriTemplateParser;

/**
 * A container request filter that captures endpoint telemetry.
 *
 * @since 1.76
 */
@Named
@Provider
public class TelemetryContainerRequestFilter
    implements ContainerRequestFilter, TelemetryCollector
{
  private static final Pattern URL_PATTERN = Pattern.compile("^" + PublicApiPaths.BASE_PATH + "/.*$");

  @VisibleForTesting
  public static final Map<String, LongAdder> REST_ENDPOINT_INVOCATIONS = new ConcurrentHashMap<>();

  public static final String REST_ENDPOINT_TELEMETRY = "rest_endpoint_telemetry";

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    ExtendedUriInfo extendedUriInfo = (ExtendedUriInfo) containerRequestContext.getUriInfo();
    String path = extendedUriInfo.getPath();
    if (URL_PATTERN.matcher(path).matches()) {
      String method = containerRequestContext.getMethod();
      String anonymisedPath = extendedUriInfo.getMatchedTemplates().stream()
          .map(t -> new UriTemplateParser(t.getTemplate()).getNormalizedTemplate())
          .reduce((t1, t2) -> t2.concat(t1))
          .orElse(null);
      String methodAndAnonymisedPath = method + " " + anonymisedPath;
      REST_ENDPOINT_INVOCATIONS.computeIfAbsent(methodAndAnonymisedPath, key -> new LongAdder()).increment();
    }
  }

  @Override
  public List<TelemetryData> collectAllData() {
    List<TelemetryData> telemetryData =
        REST_ENDPOINT_INVOCATIONS.entrySet().stream().map(this::createTelemetryData)
            .collect(Collectors.toList());
    REST_ENDPOINT_INVOCATIONS.clear();
    return telemetryData;
  }

  private TelemetryData createTelemetryData(Entry<String, LongAdder> entry) {
    String[] methodAndPath = entry.getKey().split(" ");
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REST_ENDPOINT_USAGE);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(REST_ENDPOINT_TELEMETRY,
        new RestEndpointTelemetry(methodAndPath[0], methodAndPath[1], entry.getValue().intValue()));
    telemetryData.setAttributes(attributes);
    return telemetryData;
  }
}
