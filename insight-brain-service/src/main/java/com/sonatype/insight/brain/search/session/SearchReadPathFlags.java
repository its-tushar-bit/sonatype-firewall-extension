/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.util.Locale;

/**
 * Temporary JVM-wide ops kill switch for selecting old vs new index read paths per surface.
 * <p>
 * Defaults to {@link SearchReadPath#OLD} when the property is absent or any value other than
 * {@code new} (case-insensitive). This is intentionally <strong>not</strong> a
 * {@code SystemConfigurationPropertyFeature} yet: PR-0 scaffolding has no production callers that
 * flip to {@link SearchReadPath#NEW}. Before any surface ships on {@code NEW}, replace this with a
 * DB-backed / Dropwizard-config toggle that is per-tenant and admin-visible. Until then the switch
 * is process-wide ({@code -DnexusOne.search.readPath.<surface>=new}) and requires a restart.
 */
public final class SearchReadPathFlags
{
  private SearchReadPathFlags() {
  }

  public static SearchReadPath forSurface(final SearchReadPathSurface surface) {
    String key = "nexusOne.search.readPath." + surface.name().toLowerCase(Locale.ROOT);
    String raw = System.getProperty(key, "old");
    return "new".equalsIgnoreCase(raw.trim()) ? SearchReadPath.NEW : SearchReadPath.OLD;
  }
}
