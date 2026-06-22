/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.URI;

import com.sonatype.insight.brain.guide.api.error.GuideLicenseUnavailableException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchLicenseFilterTest
{
  private final TenantUtil singleTenantUtil = createTenantUtil(false);

  private final TenantUtil multiTenantUtil = createTenantUtil(true);

  private static TenantUtil createTenantUtil(boolean multiTenant) {
    TenantUtil tenantUtil = mock(TenantUtil.class);
    when(tenantUtil.isMultiTenant()).thenReturn(multiTenant);
    return tenantUtil;
  }

  /** {@link UriInfo#getPath()} returns the path without leading slash. */
  private static ContainerRequestContext requestForPath(String pathWithoutLeadingSlash) {
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getPath()).thenReturn(pathWithoutLeadingSlash);
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/" + pathWithoutLeadingSlash));
    ContainerRequestContext ctx = mock(ContainerRequestContext.class);
    when(ctx.getUriInfo()).thenReturn(uriInfo);
    return ctx;
  }

  @Test
  public void accessAllowed_whenLicensedAndSingleTenant() {
    ProductLicense productLicense = mock(ProductLicense.class);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(true);

    SearchLicenseFilter filter = new SearchLicenseFilter(productLicense, singleTenantUtil);

    assertThatCode(() -> filter.filter(requestForPath("api/v2/guide/components/search")))
        .doesNotThrowAnyException();
  }

  @Test
  public void accessDenied_whenNotLicensed() {
    ProductLicense productLicense = mock(ProductLicense.class);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(false);

    SearchLicenseFilter filter = new SearchLicenseFilter(productLicense, singleTenantUtil);

    assertThatThrownBy(() -> filter.filter(requestForPath("api/v2/guide/components/search")))
        .isInstanceOfSatisfying(GuideLicenseUnavailableException.class, e -> {
          assertThat(e.getResponse().getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
          assertThat(e.getMessage()).isEqualTo(SearchLicenseFilter.LICENSE_DENIED_MSG);
        });
  }

  @Test
  public void accessDenied_whenMultiTenantEvenIfLicensed() {
    ProductLicense productLicense = mock(ProductLicense.class);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(true);

    SearchLicenseFilter filter = new SearchLicenseFilter(productLicense, multiTenantUtil);

    assertThatThrownBy(() -> filter.filter(requestForPath("api/v2/guide/components/search")))
        .isInstanceOfSatisfying(GuideLicenseUnavailableException.class, e -> {
          assertThat(e.getResponse().getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
          assertThat(e.getMessage()).isEqualTo(SearchLicenseFilter.MULTI_TENANT_DENIED_MSG);
        });
  }

  @Test
  public void filter_skips_nonGuidePaths() {
    // Even with no license and multi-tenant, requests outside /api/v2/guide/ pass through
    // untouched — JAX-RS picks this filter up via @Provider, so it sees every request and
    // must short-circuit on path before applying license rules.
    ProductLicense productLicense = mock(ProductLicense.class);
    when(productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH)).thenReturn(false);

    SearchLicenseFilter filter = new SearchLicenseFilter(productLicense, multiTenantUtil);

    assertThatCode(() -> filter.filter(requestForPath("api/v2/applications"))).doesNotThrowAnyException();
    assertThatCode(() -> filter.filter(requestForPath(""))).doesNotThrowAnyException();
    assertThatCode(() -> filter.filter(requestForPath("api/v2/guideguide/x"))).doesNotThrowAnyException();
  }
}
