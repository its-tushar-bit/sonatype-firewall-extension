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

import com.auth0.client.mgmt.filter.ClientFilter;
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

  private final String mtIQTenantUrlTemplate;

  public Auth0ManagementAPI(final String domain, final String apiToken, final String mtIQTenantUrlTemplate) {
    super(domain, apiToken);
    this.apiToken = apiToken;
    this.mtIQTenantUrlTemplate = mtIQTenantUrlTemplate;
    this.mtIQSamlEndpoint = mtIQTenantUrlTemplate + "/saml";
  }

  @Override
  public OkHttpClient getClient() {
    return super.getClient();
  }

  public Client createTenant(String tenantSubdomain, String tenantDescription, String logoUrl) {
    validate(tenantSubdomain, tenantDescription, logoUrl);
    Client client = newClient(tenantSubdomain, tenantSubdomain, tenantDescription, logoUrl);
    return updateOrCreateClient(client);
  }

  public Client createTenant(
      final String name,
      final String tenantSubdomain,
      final String tenantDescription,
      final String logoUrl)
  {
    validate(name, tenantDescription, logoUrl);
    Client client = newClient(name, tenantSubdomain, tenantDescription, logoUrl);
    return updateOrCreateClient(client);
  }

  private Client newClient(
      final String name,
      final String tenantSubdomain,
      final String tenantDescription,
      final String logoUrl)
  {
    Client client = new Client(name.toLowerCase());
    client.setAppType(AUTH0_APP_TYPE);
    client.setDescription(tenantDescription);
    client.setCallbacks(Collections.singletonList(getTenantSamlEndpoint(tenantSubdomain)));
    client.setLogoUri(logoUrl);
    client.setAllowedLogoutUrls(Collections.singletonList(getTenantEndpoint(tenantSubdomain)));
    client.setCrossOriginAuth(false);
    Addon samlpAddOn = new Addon();
    Addons addOns = new Addons(null, null, null, null);
    addOns.setAdditionalAddon("samlp", samlpAddOn);
    client.setAddons(addOns);
    return client;
  }

  private Client updateOrCreateClient(Client client) {
    try {
      Client existing = findClientByName(client.getName());

      if (existing != null) {
        return clients().update(existing.getClientId(), client).execute();
      }

      return clients().create(client).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Client findClientByName(String name) throws Auth0Exception {
    ClientFilter filter = new ClientFilter().withFields("name", true).withFields("client_id", true);
    return clients().list(filter).execute().getItems().stream()
        .filter(client -> name.equalsIgnoreCase(client.getName())).findFirst().orElse(null);
  }

  private String getTenantEndpoint(final String tenantSubdomain) {
    return StringUtils.replace(mtIQTenantUrlTemplate, "<tenant>", tenantSubdomain);
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

  public File getSamlMetaDataFile(String auth0ClientId) {
    String samlMetadata = getSamlMetaData(auth0ClientId);

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

  public String getSamlMetaData(String auth0ClientId) {
    String url = getBaseUrl()
        .newBuilder()
        .addPathSegments("samlp/metadata/")
        .addPathSegment(auth0ClientId)
        .build()
        .toString();

    return downloadSamlMetadata(url);
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
