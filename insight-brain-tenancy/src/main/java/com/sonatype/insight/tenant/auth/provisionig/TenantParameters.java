/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.tenant.auth.provisionig;

import com.beust.jcommander.Parameter;

public class TenantParameters
{
  @Parameter(
      names = {"-t", "--tenantSubdomain"},
      description = "tenant specific subdomain part in the SaaS public URL",
      required = true)
  private String subdomain;

  @Parameter(
      names = {"-a", "--action"},
      required = true,
      description = "valid actions => [provision]")
  private String action;

  @Parameter(
      names = {"-d", "--description"},
      description = "text describing tenant (must be less than 140 characters)")
  private String description;

  @Parameter(
      names = {"-l", "--logo-url"},
      description = "url to the tenant specific logo.  auth0 logo will be used if not provided")
  private String logoUrl;

  public String getSubdomain() {
    return subdomain;
  }

  public String getAction() {
    return action;
  }

  public String getDescription() {
    return description;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setSubdomain(final String subdomain) {
    this.subdomain = subdomain;
  }

  public void setAction(final String action) {
    this.action = action;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public void setLogoUrl(final String logoUrl) {
    this.logoUrl = logoUrl;
  }
}
