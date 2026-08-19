/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Data class to represent a single Tenant within IQ. Note that even 'regular' IQ, clustered or not, is considered to
 * have a single tenant.
 */
public class Tenant
    implements Comparable<Tenant>
{
  private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]*[a-z0-9]$");

  // Regular IQ (even clustered) still has tenancy, but there can only ever be a single tenant
  // Note the invalid database schema name to ensure it is never actually used
  public static final Tenant SINGLE_TENANT = new Tenant("notused", "shouldnotexist!");

  // In multi-tenant IQ there exists a special 'global' schema for data that can exist at that global level
  public static final Tenant GLOBAL_TENANT = new Tenant("global", "global");

  // primary tenant slug that you would see in the vanity URL
  public final String tenantSlug;

  // Database schema version of the slug
  public final String databaseSchema;

  private final String createdByThreadName;

  private volatile boolean valid = true;

  Tenant(String tenantSlug) {
    this.tenantSlug = tenantSlug;
    this.databaseSchema = setDbSchemaSlug(tenantSlug);
    this.createdByThreadName = Thread.currentThread().getName();
  }

  Tenant(String tenantSlug, String databaseSchema) {
    this.tenantSlug = tenantSlug;
    this.databaseSchema = databaseSchema;
    this.createdByThreadName = Thread.currentThread().getName();
  }

  private String setDbSchemaSlug(final String tenantSlug) {
    validateSlug(tenantSlug);
    String prefix = this.equals(GLOBAL_TENANT) ? "" : "t_";
    return prefix + tenantSlug.replace('-', '_');
  }

  private void validateSlug(final String slug) {
    if (StringUtils.isBlank(slug) || slug.length() < 3) {
      throw new InvalidTenantSlugException("Slug name must be at least 3 characters");
    }

    if (slug.length() > 61) {
      throw new InvalidTenantSlugException("Slug name must not exceed 61 characters");
    }
    if (!SLUG_PATTERN.matcher(slug).matches()) {
      throw new InvalidTenantSlugException(slug + " is not a valid tenant slug");
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Tenant.class.getSimpleName() + "[", "]")
        .add("tenantSlug='" + tenantSlug + "'")
        .add("createdByThread='" + createdByThreadName + "'")
        .add("valid='" + valid + "'")
        .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Tenant)) {
      return false;
    }
    Tenant tenant = (Tenant) o;
    return tenantSlug.equals(tenant.tenantSlug);
  }

  /**
   * @return the Tenant's hashCode, which is equal to the hashCode of its slug
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(tenantSlug);
  }

  void invalidate() {
    if (this != GLOBAL_TENANT) {
      valid = false;
    }
  }

  public boolean isInvalid() {
    return !valid;
  }

  @Override
  public int compareTo(final Tenant t) {
    return tenantSlug.compareTo(t.tenantSlug);
  }

  static class InvalidTenantSlugException
      extends RuntimeException
  {
    public InvalidTenantSlugException(String message) {
      super(message);
    }
  }
}
