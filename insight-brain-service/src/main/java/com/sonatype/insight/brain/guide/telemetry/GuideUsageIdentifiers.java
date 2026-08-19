/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import java.lang.reflect.Method;

/** Best-effort extraction of the component/vuln identifier from a SearchApiClient method's arguments. */
public final class GuideUsageIdentifiers
{
  private GuideUsageIdentifiers() {
  }

  /**
   * Best-effort extraction of a component PURL or vulnerability id from a method's arguments.
   *
   * <p>
   * <b>Privacy contract:</b> callers must only invoke this on lookups whose direct {@code String}
   * argument is a component PURL or a vulnerability id &mdash; never a free-text search query. Search
   * or query operations must pass their criteria via a request object; this extractor only follows
   * {@code purl()}/{@code id()} accessors and deliberately ignores any {@code query()} or other
   * free-text accessor, so searches remain count-only (no query text captured).
   */
  public static String extract(final Object[] args) {
    if (args == null) {
      return null;
    }
    for (Object arg : args) {
      if (arg instanceof String s && !s.isBlank()) {
        return s;
      }
    }
    for (Object arg : args) {
      String viaAccessor = accessor(arg, "purl");
      if (viaAccessor == null) {
        viaAccessor = accessor(arg, "id");
      }
      if (viaAccessor != null && !viaAccessor.isBlank()) {
        return viaAccessor;
      }
    }
    return null;
  }

  private static String accessor(final Object arg, final String name) {
    if (arg == null) {
      return null;
    }
    try {
      // Per-call reflection is intentionally un-cached: cost is negligible next to the HDS lookup it accompanies.
      Method m = arg.getClass().getMethod(name);
      if (m.getReturnType() == String.class) {
        return (String) m.invoke(arg);
      }
    }
    catch (ReflectiveOperationException ignored) {
      // no such accessor on this arg type
    }
    return null;
  }
}
