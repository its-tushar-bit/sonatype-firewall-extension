/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.auth0.client.mgmt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.client.Addon;
import com.auth0.json.mgmt.client.Addons;
import com.auth0.json.mgmt.client.Client;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

public class Auth0ManagementAPI
    extends ManagementAPI
{
  public static final String AUTH0_APP_TYPE = "regular_web";

  private final String apiToken;

  private final String mtIQSamlEndpoint;

  public Auth0ManagementAPI(final String domain, final String apiToken, final String mtIQTenantUrlTemplate) {
    super(domain, apiToken);
    this.apiToken = apiToken;
    this.mtIQSamlEndpoint = mtIQTenantUrlTemplate + "/saml";
  }

  @Override
  public OkHttpClient getClient() {
    return super.getClient();
  }

  public Client createTenant(String tenantSubdomain, String tenantDescription, String logoUrl) {
    validate(tenantSubdomain, tenantDescription, logoUrl);
    Client client = newClient(tenantSubdomain, tenantDescription, logoUrl);
    try {
      return clients().create(client).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Client newClient(
      final String tenantSubdomain,
      final String tenantDescription,
      final String logoUrl)
  {
    Client client = new Client(tenantSubdomain);
    client.setAppType(AUTH0_APP_TYPE);
    client.setDescription(tenantDescription);
    client.setCallbacks(Collections.singletonList(getTenantSamlEndpoint(tenantSubdomain)));
    client.setLogoUri(logoUrl);
    client.setCrossOriginAuth(false);
    Addon samlpAddOn = new Addon();
    Addons addOns = new Addons(null, null, null, null);
    addOns.setAdditionalAddon("samlp", samlpAddOn);
    client.setAddons(addOns);
    return client;
  }

  private String getTenantSamlEndpoint(final String tenantSubdomain) {
    return StringUtils.replace(mtIQSamlEndpoint, "<tenant>", tenantSubdomain);
  }

  private void validate(final String tenantName, final String tenantDescription, final String logoUrl) {
    if (StringUtils.isBlank(tenantName) || StringUtils.containsAny(tenantName, "<", ">")) {
      throw new IllegalArgumentException("tenant name cannot be blank or invalid characters <,>");
    }

    if (StringUtils.length(tenantDescription) > 140) {
      throw new IllegalArgumentException("tenant description should be less that 140 characters");
    }

    if (StringUtils.isNotBlank(logoUrl)) {
      try {
        new URL(logoUrl);
      }
      catch (MalformedURLException e) {
        throw new IllegalArgumentException("tenant logo url must be a valid url", e);
      }
    }
  }

  public File getSamlMetaData(String auth0ClientId) {
    String url = getBaseUrl()
        .newBuilder()
        .addPathSegments("samlp/metadata/")
        .addPathSegment(auth0ClientId)
        .build()
        .toString();
    String samlMetadata = downloadSamlMetadata(url);
    if (samlMetadata != null) {
      try {
        Path tempFile = Files.createTempFile(auth0ClientId + "-", "-samlmetadata.xml");
        Files.write(tempFile, samlMetadata.getBytes(StandardCharsets.UTF_8));
        return tempFile.toFile();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  String downloadSamlMetadata(String url) {
    Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + apiToken).build();
    try (Response response = getClient().newCall(request).execute()) {
      ResponseBody body = response.body();
      return body != null ? body.string() : null;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
