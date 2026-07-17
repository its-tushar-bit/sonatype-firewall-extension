/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.guide.api.error.GuideLicenseUnavailableException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class McpLicenseFilter
    implements Filter
{
  public static final String[] URL_PATTERNS = {"/mcp", "/mcp/*"};

  public static final String ACCESS_DENIED_MSG = "Guide MCP is not available with the current license.";

  private static final String CONTENT_TYPE_TEXT_PLAIN_UTF8 = "text/plain; charset=utf-8";

  private static final Logger log = LoggerFactory.getLogger(McpLicenseFilter.class);

  private final ProductLicense productLicense;

  private final TenantUtil tenantUtil;

  @Inject
  public McpLicenseFilter(ProductLicense productLicense, TenantUtil tenantUtil) {
    this.productLicense = productLicense;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // no op
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    boolean isMultiTenant = tenantUtil.isMultiTenant();
    if (!isLicensed(isMultiTenant)) {
      log.debug("MCP access denied: multi-tenant={}", isMultiTenant);
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      httpResponse.setHeader(GuideLicenseUnavailableException.LICENSE_HEADER,
          GuideLicenseUnavailableException.LICENSE_UNAVAILABLE);
      httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
      httpResponse.setContentType(CONTENT_TYPE_TEXT_PLAIN_UTF8);
      try (PrintWriter writer = httpResponse.getWriter()) {
        writer.write(ACCESS_DENIED_MSG);
      }
      return;
    }

    chain.doFilter(request, response);
  }

  /**
   * MCP is available to a single-tenant Guide license ({@link LicensedFeature#GUIDE_MCP}), or to an
   * AI Developer license ({@link LicensedFeature#AI_DEVELOPER}) whose SKU matches the deployment's
   * tenancy: the self-hosted {@code AiDeveloper} SKU single-tenant, {@code AiDeveloperSaas} multi-tenant.
   */
  private boolean isLicensed(boolean isMultiTenant) {
    boolean hasGuideMcp = !isMultiTenant && productLicense.hasFeature(LicensedFeature.GUIDE_MCP);
    boolean hasAiDeveloper = productLicense.hasFeature(LicensedFeature.AI_DEVELOPER)
        && productLicense.hasProduct(isMultiTenant
            ? ProductLicenseDetails.PRODUCT_AI_DEVELOPER_SAAS
            : ProductLicenseDetails.PRODUCT_AI_DEVELOPER);
    return hasGuideMcp || hasAiDeveloper;
  }

  @Override
  public void destroy() {
    // no op
  }
}
