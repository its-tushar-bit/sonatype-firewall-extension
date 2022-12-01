/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.sonatype.insight.brain.tenancy.Tenant.InvalidTenantSlugException;

import org.junit.Test;

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
  public void shouldOnlyAllowValidTenantSlugs() {
    /**
     * SQL identifiers and keywords must begin with a letter (a-z, but also letters with diacritical marks and
     * non-Latin letters) or an underscore (_). Subsequent characters in an identifier or key word can be letters,
     * underscores or digits (0-9).
     */
    new Tenant("abc");
    new Tenant("_abc");
    new Tenant("_abc0123");
    new Tenant("_0123");

    // While starting with a 0 is not allowed the Tenant class prefixes the schema with t_ which makes this valid
    new Tenant("0abc");

    assertThatThrownBy(() -> new Tenant("_$")).isInstanceOfAny(InvalidTenantSlugException.class);
    assertThatThrownBy(() -> new Tenant("$")).isInstanceOfAny(InvalidTenantSlugException.class);
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
}
