/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data class to represent a single Tenant within IQ. Note that even 'regular' IQ, clustered or not, is considered to
 * have a single tenant.
 */
public class Tenant
{
  // Regular IQ (even clustered) still has tenancy, but there can only ever be a single tenant
  // Note the invalid database schema name to ensure it is never actually used
  public static final Tenant SINGLE_TENANT = new Tenant("notused", "shouldnotexist!");

  // In multi-tenant IQ there exists a special 'global' schema for data that can exist at that global level
  public static final Tenant GLOBAL_TENANT = new Tenant("global", "global");

  // primary tenant slug that you would see in the vanity URL
  public final String tenantSlug;

  // Database schema version of the slug
  public final String databaseSchema;

  private boolean valid = true;

  public Tenant(String tenantSlug) {
    this.tenantSlug = tenantSlug;
    this.databaseSchema = setDbSchemaSlug(tenantSlug);
  }

  private Tenant(String tenantSlug, String databaseSchema) {
    this.tenantSlug = tenantSlug;
    this.databaseSchema = databaseSchema;
  }

  private String setDbSchemaSlug(final String tenantSlug) {
    String prefix = this.equals(GLOBAL_TENANT) ? "" : "t_";

    return validateSlug(prefix + tenantSlug.replace('-', '_'));
  }

  private String validateSlug(final String slug) {
    Pattern pattern = Pattern.compile("^([[a-zA-Z]_][[a-zA-Z0-9]_]*)$");

    Matcher matcher = pattern.matcher(slug);

    if (!matcher.matches()) {
      throw new InvalidTenantSlugException(slug + " is not a valid tenant slug");
    }

    return slug;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Tenant.class.getSimpleName() + "[", "]")
        .add("tenantSlug='" + tenantSlug + "'")
        .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Tenant tenant = (Tenant) o;
    return tenantSlug.equals(tenant.tenantSlug);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantSlug);
  }

  void invalidate() {
    if (this != GLOBAL_TENANT) {
      valid = false;
    }
  }

  public boolean isInvalid() {
    return !valid;
  }

  public static class InvalidTenantSlugException
      extends RuntimeException
  {
    public InvalidTenantSlugException(String message) {
      super(message);
    }
  }
}
