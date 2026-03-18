/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.tenant.auth.provisionig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.auth0.client.mgmt.Auth0ManagementAPI;
import com.auth0.json.mgmt.client.Client;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Auth0ProvisioningService
{
  private static final Logger log = LoggerFactory.getLogger(Auth0ProvisioningService.class);

  /**
   * TODO: this is the test account auth0 provided for us. Need to be changed in the future.
   */
  static final String DEFAULT_AUTH0_DOMAIN = "https://sonatype-lab.us.auth0.com";

  private static final String DEFAULT_MTIQ_TENANT_URL_TEMPLATE = "https://<tenant>.mtiq.cloudy.sonatype.dev";

  private static final String MTIQ_TENANT_URL_TEMPLATE = "MTIQ_TENANT_URL";

  static final String AUTH0_DOMAIN = "AUTH0_DOMAIN";

  private static final String AUTH0_API_TOKEN = "AUTH0_API_TOKEN";

  public void provision(TenantParameters parameters) {
    Auth0ManagementAPI api = getManagementAPI();
    String subdomain = parameters.getSubdomain();

    log.info(String.format("Creating new auth0 account for tenant=%s in %s", subdomain, resolveAuth0Domain()));
    String tenantUrl = getTenantUrl(subdomain);
    Client tenant = api.createOrUpdateTenant(
        subdomain,
        tenantUrl,
        parameters.getDescription(),
        parameters.getLogoUrl(),
        null);
    log.info(String.format("Created new auth0 account for tenant=%s, clientId=%s", subdomain,
        tenant.getClientId()));

    File samlMetaDataFile = api.getSamlMetaDataFile(tenant.getClientId());
    if (samlMetaDataFile == null) {
      log.error(String.format("Unable to download the saml metadata for tenant=%s, clientId=%s", subdomain,
          tenant.getClientId()));
      return;
    }

    log.info(String.format("Downloaded saml metadata file for tenant=%s, file=%s", subdomain,
        samlMetaDataFile.getAbsolutePath()));
    // TODO remaining provisioning steps
    // - create/associate connections
    // - upload saml metadata file in IQ and configure auth attributes
    // - role-mapping ?
  }

  // visible for testing
  Auth0ManagementAPI getManagementAPI() {
    return new Auth0ManagementAPI(resolveAuth0Domain(), resolveAuth0ApiToken());
  }

  private String getTenantUrl(final String tenantSubdomain) {
    return StringUtils.replace(resolveMTIQSaaSBaseUrl(), "<tenant>", tenantSubdomain);
  }

  private String resolveMTIQSaaSBaseUrl() {
    String mtIQTenantUrl = System.getenv(MTIQ_TENANT_URL_TEMPLATE);
    if (StringUtils.isNotBlank(mtIQTenantUrl) && isURL(mtIQTenantUrl)) {
      return mtIQTenantUrl;
    }
    return DEFAULT_MTIQ_TENANT_URL_TEMPLATE;
  }

  private String resolveAuth0ApiToken() {
    String apiToken = System.getenv(AUTH0_API_TOKEN);
    if (StringUtils.isBlank(apiToken)) {
      throw new IllegalArgumentException("AUTH0_API_TOKEN environment variable not set");
    }
    return apiToken;
  }

  // visible for testing
  String resolveAuth0Domain() {
    String auth0DomainEnv = System.getenv(AUTH0_DOMAIN);
    if (StringUtils.isNotBlank(auth0DomainEnv) && isURL(auth0DomainEnv)) {
      return auth0DomainEnv;
    }
    return DEFAULT_AUTH0_DOMAIN;
  }

  private boolean isURL(final String url) {
    try {
      new URL(url);
    }
    catch (MalformedURLException e) {
      throw new RuntimeException("AUTH0_DOMAIN value must be a URL", e);
    }
    return true;
  }
}
