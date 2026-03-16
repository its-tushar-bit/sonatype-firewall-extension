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

import jakarta.inject.Named;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.tenancy.TenantReference;
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
  private static final Pattern URL_PATTERN =
      Pattern.compile("^(" + PublicApiPaths.BASE_PATH + "|" + UserInterfaceLinksHelper.RESOURCE_PATH + ")/.*$");

  @VisibleForTesting
  public static final TenantReference<Map<String, LongAdder>> REST_ENDPOINT_INVOCATIONS =
      new TenantReference<>(ConcurrentHashMap::new);

  public static final String REST_ENDPOINT_TELEMETRY = "rest_endpoint_telemetry";

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    ExtendedUriInfo extendedUriInfo = (ExtendedUriInfo) containerRequestContext.getUriInfo();
    String path = extendedUriInfo.getPath();
    if (URL_PATTERN.matcher(path).matches()) {
      String method = containerRequestContext.getMethod();
      // jersey 2.41 changed the way they calculate normalized templates.
      // In order to have consistent data for telemetry, we need to parse the new normalized templates and change them
      // to match the values from jersey <= 2.40.
      String anonymisedPath = extendedUriInfo.getMatchedTemplates()
          .stream()
          .map(t -> normalizePathForTelemetry(new UriTemplateParser(t.getTemplate()).getNormalizedTemplate()))
          .reduce((t1, t2) -> t2.concat(t1))
          .orElse(null);
      String methodAndAnonymisedPath = method + " " + anonymisedPath;
      REST_ENDPOINT_INVOCATIONS.get().computeIfAbsent(methodAndAnonymisedPath, key -> new LongAdder()).increment();
    }
  }

  private String normalizePathForTelemetry(String path) {
    // Example 1:
    // Path: /api/v2/users/{username}
    // Normalized path jersey <= 2.40: /api/v2/users/{username}
    // Normalized path jersey > 2.40: /api/v2/users/{{username}}
    //
    // Example 2:
    // Path: /api/v2/users/{username}
    // Normalized path jersey <= 2.40:
    // /api/v2/roleMemberships/{ownerType}/{internalOwnerId}/role/{roleId}/{memberType}/{memberName}
    // Normalized path jersey > 2.40:
    // /api/v2/roleMemberships/{{ownerType:applicationorganization}}/{{internalOwnerId}}/role/{{roleId}}/
    // {{memberType:usergroup}}/{{memberName}}

    // Replace all {{ with { and }} with }.
    path = path.replace("{{", "{").replace("}}", "}");

    // Remove path param options
    StringBuilder result = new StringBuilder();
    boolean skipChar = false;
    for (char c : path.toCharArray()) {
      switch (c) {
        case ':':
          skipChar = true;
          continue;
        case '}':
          skipChar = false;
          //$FALL-THROUGH$
        default:
          if (!skipChar) {
            result.append(c);
          }
      }
    }

    return result.toString();
  }

  @Override
  public List<TelemetryData> collectAllData() {
    List<TelemetryData> telemetryData =
        REST_ENDPOINT_INVOCATIONS.get()
            .entrySet()
            .stream()
            .map(this::createTelemetryData)
            .collect(Collectors.toList());
    REST_ENDPOINT_INVOCATIONS.get().clear();
    return telemetryData;
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
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
