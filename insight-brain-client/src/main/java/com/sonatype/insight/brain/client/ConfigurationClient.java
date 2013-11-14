/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.client.HttpResponseException;

public class ConfigurationClient
    extends AbstractClient
{
  public ConfigurationClient(final Configuration config) {
    super(config);
  }

  private Result get(RequestBuilder builder) throws IOException {
    final Result result;
    try {
      result = builder.get();
    }
    catch (UnknownHostException e) {
      // improve error msg
      throw (IOException) new UnknownHostException("Unknown host: " + e.getMessage()).initCause(e);
    }
    final int status = result.status();
    if (status >= 300) {
      String msg = result.text();
      if (status == 401) {
        /*
         * Until the client uses authentication, a misconfigured base URL will make the client encounter authentication
         * errors from already protected resources, so tweak the user facing error message to better highlight the
         * proper remediation.
         */
        msg = "Resource not found, please check your request URL.";
      }
      throw new HttpResponseException(status, msg);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getApplicationIdNameMap() throws IOException {
    Result result = get(path("rest/application/services/names"));
    Map<String, String> applicationsById = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    applicationsById.putAll(JsonUtils.parse(result.text(), Map.class));
    return applicationsById;
  }

  public void validateConfiguration() throws IOException {
    final Result result = get(path("rest/version"));
    final String text = result.text();
    // at this point, the network connection appears fine, now let's just check we actually talked to a CLM server
    try {
      final Map<?, ?> versionInfo = JsonUtils.parse(text, Map.class);
      if (versionInfo.get("version") == null && versionInfo.get("name") == null) {
        throw new Exception("No CLM version information present");
      }
    }
    catch (Exception e) {
      throw new IOException("Server is not compatible with this Sonatype CLM integration", e);
    }
  }

  public void validateApplicationId(final String appId) throws IOException {
    final Result result = get(path("rest/application/validate", UrlUtils.encodeUrlComponent(appId)));
    final String text = result.text();
    if (!"OK".equals(text)) {
      throw new IOException(text);
    }
  }

  public ProprietaryConfig getProprietaryConfiguration() throws IOException {
    Result result = get(path("rest/config/proprietary"));
    return JsonUtils.parse(result.text(), ProprietaryConfig.class);
  }
}
