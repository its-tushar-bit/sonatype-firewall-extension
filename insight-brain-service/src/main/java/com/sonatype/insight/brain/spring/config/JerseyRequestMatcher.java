/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Determines whether an HTTP request should be handled by Jersey (JAX-RS) or passed through
 * to Spring MVC / static resources.
 *
 * <p>
 * In Dropwizard, Jersey handled all requests. In Spring Boot, Jersey runs as a servlet filter
 * alongside Spring MVC, so requests for static assets (CSS, JS, images) and Spring-managed
 * endpoints must bypass Jersey. This matcher scans {@code @Path} annotations on registered
 * JAX-RS resources at startup and matches incoming requests against their top-level path segments.
 * Used by {@link SelectiveJerseyFilter} to short-circuit non-JAX-RS requests.
 */
public final class JerseyRequestMatcher
{
  private final boolean matchesRoot;

  private final Set<String> topLevelSegments;

  private JerseyRequestMatcher(boolean matchesRoot, Set<String> topLevelSegments) {
    this.matchesRoot = matchesRoot;
    this.topLevelSegments = Set.copyOf(topLevelSegments);
  }

  public static JerseyRequestMatcher fromComponents(Collection<Object> components) {
    boolean matchesRoot = false;
    Set<String> topLevelSegments = new LinkedHashSet<>();

    for (Object component : components) {
      Path path = AnnotatedElementUtils.findMergedAnnotation(AopUtils.getTargetClass(component), Path.class);
      if (path == null) {
        continue;
      }

      String normalizedPath = normalizeAnnotationPath(path.value());
      if (normalizedPath.isEmpty()) {
        matchesRoot = true;
        continue;
      }

      String topLevelSegment = normalizedPath.split("/", 2)[0];
      if (!topLevelSegment.startsWith("{")) {
        topLevelSegments.add(topLevelSegment);
      }
    }

    return new JerseyRequestMatcher(matchesRoot, topLevelSegments);
  }

  public boolean matches(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
      requestUri = requestUri.substring(contextPath.length());
    }
    return matches(requestUri);
  }

  public boolean matches(String requestPath) {
    String normalizedPath = normalizeRequestPath(requestPath);
    if (matchesRoot && "/".equals(normalizedPath)) {
      return true;
    }

    for (String topLevelSegment : topLevelSegments) {
      String segmentPath = "/" + topLevelSegment;
      if (normalizedPath.equals(segmentPath) || normalizedPath.startsWith(segmentPath + "/")) {
        return true;
      }
    }

    return false;
  }

  public String describeHandledPaths() {
    return topLevelSegments.stream()
        .map(segment -> "/" + segment + "/*")
        .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), patterns -> {
          if (matchesRoot) {
            patterns.add("/");
          }
          return patterns.toString();
        }));
  }

  private static String normalizeAnnotationPath(String path) {
    String normalized = Objects.requireNonNullElse(path, "").trim();
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    while (normalized.endsWith("/") && !normalized.isEmpty()) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private static String normalizeRequestPath(String path) {
    String normalized = Objects.requireNonNullElse(path, "").trim();
    if (normalized.isEmpty() || "/".equals(normalized)) {
      return "/";
    }
    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }
    while (normalized.endsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
