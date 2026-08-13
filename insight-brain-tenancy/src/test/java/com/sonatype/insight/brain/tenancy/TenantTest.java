/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.sonatype.insight.brain.tenancy.Tenant.InvalidTenantSlugException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TenantTest
{
  @Test
  public void databaseSchemaShouldBePrefixed() {
    String urlSlug = "name";
    Tenant tenant = new Tenant(urlSlug);

    assertThat(tenant.databaseSchema).isEqualTo("t_" + urlSlug);
  }

  @Test
  public void allowsTenantCreationForValidSlugs() {
    /**
     * Valid tenant slugs should conform to both of the following:
     * i. must only contain valid characters/length supported for URL subdomains.
     * ii. must comply with naming of SQL identifiers/keywords.
     * iii. must not exceed 61 chars, as we append "t_" to slug for use as schema name which exceeds the 63 char limit
     */
    new Tenant("abc");
    new Tenant("a-b-c");
    new Tenant("a00");
    new Tenant("abc-0123");
    new Tenant("1-slug-with-number-start");
    new Tenant(StringUtils.repeat("a", 61));
  }

  @Test
  public void disallowsTenantCreationForInvalidSlugs() {
    assertThatThrownBy(() -> new Tenant(null)).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("Slug name must be at least 3 characters");
    assertThatThrownBy(() -> new Tenant("")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("Slug name must be at least 3 characters");
    assertThatThrownBy(() -> new Tenant("   ")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("Slug name must be at least 3 characters");
    assertThatThrownBy(() -> new Tenant("a- ")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("a-  is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("ab$")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("ab$ is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("slug-with space")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("slug-with space is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("slug-with_underscore")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("slug-with_underscore is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("slug-with.dot")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("slug-with.dot is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("-slug-with-hyphen-start")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("-slug-with-hyphen-start is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("slug-with-hyphen-end-")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("slug-with-hyphen-end- is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("slug-with-Capital-Letters")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("slug-with-Capital-Letters is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("X-invalid-first-letter")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("X-invalid-first-letter is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant("invalid-last-letter-X")).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("invalid-last-letter-X is not a valid tenant slug");
    assertThatThrownBy(() -> new Tenant(StringUtils.repeat("a", 62))).isInstanceOfAny(InvalidTenantSlugException.class)
        .hasMessage("Slug name must not exceed 61 characters");
  }

  @Test
  public void shouldNormalizeHyphensForDbSchema() {
    Tenant tenant = new Tenant("name-2");

    assertThat(tenant.databaseSchema).isEqualTo("t_name_2");
    assertThat(tenant.tenantSlug).isEqualTo("name-2");
  }

  @Test
  public void validateGlobalTenant() {
    Tenant globalTenant = Tenant.GLOBAL_TENANT;

    // only customer specific tenants are prefixed
    assertThat(globalTenant.databaseSchema).isEqualTo("global");
    assertThat(globalTenant.tenantSlug).isEqualTo("global");
    assertThat(new Tenant("global")).isEqualTo(globalTenant);
  }

  @Test
  public void validateSingleTenant() {
    Tenant singleTenant = Tenant.SINGLE_TENANT;

    assertThat(singleTenant.databaseSchema).isEqualTo("shouldnotexist!");
    assertThat(singleTenant.tenantSlug).isEqualTo("notused");
  }

  @Test
  public void invalidateTenantShouldSetFlag() {
    Tenant tenant = new Tenant("name");

    assertThat(tenant.isInvalid()).isFalse();
    tenant.invalidate();
    assertThat(tenant.isInvalid()).isTrue();
  }

  @Test
  public void invalidatingGlobalTenantDoesNothing() {
    Tenant.GLOBAL_TENANT.invalidate();

    assertThat(Tenant.GLOBAL_TENANT.isInvalid()).isFalse();
  }

  @Test
  public void testHashCode() {
    assertThat(new Tenant("name").hashCode()).isEqualTo("name".hashCode());
  }
}
