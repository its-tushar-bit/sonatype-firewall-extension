/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.tenant.auth.provisionig;

public class ActionResolver
{
  public static final String PROVISION = "provision";

  private final Auth0ProvisioningService auth0ProvisioningService;

  // visible for testing
  public ActionResolver() {
    this(new Auth0ProvisioningService());
  }

  ActionResolver(final Auth0ProvisioningService auth0ProvisioningService) {
    this.auth0ProvisioningService = auth0ProvisioningService;
  }

  public void perform(TenantParameters parameters) {
    if (parameters.getAction().equalsIgnoreCase(PROVISION)) {
      auth0ProvisioningService.provision(parameters);
      return;
    }
    throw new UnsupportedOperationException(String.format("[%s] action not supported", parameters.getAction()));
  }
}
