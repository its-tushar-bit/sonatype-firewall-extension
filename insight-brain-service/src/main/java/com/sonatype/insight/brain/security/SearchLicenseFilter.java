/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.error.GuideLicenseUnavailableException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS request filter that gates Guide API access (paths under {@code /api/v2/guide/}). Access is
 * granted to a single-tenant Guide license ({@link LicensedFeature#GUIDE_SEARCH}), or to an AI
 * Developer license ({@link LicensedFeature#AI_DEVELOPER}) whose SKU matches the deployment's tenancy
 * — self-hosted {@code AiDeveloper} single-tenant, {@code AiDeveloperSaas} multi-tenant. Guide itself
 * remains single-tenant only.
 *
 * <p>
 * Implemented as a {@link ContainerRequestFilter} (rather than a servlet filter) so denials
 * throw {@link GuideApiException} and flow through {@link
 * com.sonatype.insight.brain.guide.api.error.GuideExceptionMapper} — guaranteeing the same
 * {@code {"success":false,"message":"..."}} envelope every other Guide error returns.
 * Compare with {@link McpLicenseFilter}, which is a servlet filter because the {@code /mcp/*}
 * paths are not JAX-RS managed.
 */
@Named
@Singleton
@Provider
@Priority(Priorities.AUTHORIZATION)
public class SearchLicenseFilter
    implements ContainerRequestFilter
{
  /**
   * Path prefix this filter applies to. Comparisons use {@link
   * jakarta.ws.rs.core.UriInfo#getPath()} which strips the leading slash, so the constant has
   * no leading slash either.
   */
  private static final String GUIDE_API_PATH_PREFIX = "api/v2/guide/";

  public static final String LICENSE_DENIED_MSG = "Guide API is not available with the current license.";

  public static final String MULTI_TENANT_DENIED_MSG = "Guide API is not available in multi-tenant deployments.";

  private static final Logger log = LoggerFactory.getLogger(SearchLicenseFilter.class);

  private final ProductLicense productLicense;

  private final TenantUtil tenantUtil;

  @Inject
  public SearchLicenseFilter(ProductLicense productLicense, TenantUtil tenantUtil) {
    this.productLicense = productLicense;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (!requestContext.getUriInfo().getPath().startsWith(GUIDE_API_PATH_PREFIX)) {
      return;
    }

    boolean isMultiTenant = tenantUtil.isMultiTenant();
    if (!isLicensed(isMultiTenant)) {
      // Guide search is single-tenant only, so a multi-tenant Guide license is told exactly that;
      // every other denial (no entitlement, or an AI Developer SKU that doesn't match the tenancy)
      // gets the generic license message.
      boolean multiTenantGuide = isMultiTenant && productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH);
      String message = multiTenantGuide ? MULTI_TENANT_DENIED_MSG : LICENSE_DENIED_MSG;
      log.debug("Guide API access denied: multi-tenant={}", isMultiTenant);
      throw new GuideLicenseUnavailableException(Response.Status.FORBIDDEN, message);
    }
  }

  /**
   * Guide search is available to a single-tenant Guide license ({@link LicensedFeature#GUIDE_SEARCH}),
   * or to an AI Developer license ({@link LicensedFeature#AI_DEVELOPER}) whose SKU matches the
   * deployment's tenancy: the self-hosted {@code AiDeveloper} SKU single-tenant, {@code
   * AiDeveloperSaas} multi-tenant.
   */
  private boolean isLicensed(boolean isMultiTenant) {
    boolean hasGuideSearch = !isMultiTenant && productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH);
    boolean hasAiDeveloper = productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)
        && productLicense.hasProduct(isMultiTenant
            ? ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS
            : ProductLicenseDetails.PRODUCT_AI_DEVELOPER);
    return hasGuideSearch || hasAiDeveloper;
  }
}
