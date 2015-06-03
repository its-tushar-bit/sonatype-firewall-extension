/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.eclipse.jetty.util.UrlEncoded;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractComponentInfoResourceTest
    extends AbstractResourceTest
{
  protected abstract String getResourcePath();

  @Before
  public void clearEnforcementPointsFromLicense() throws Exception {
    /*
     * License restrictions on enforcement points are checked when uploading scan data, report data retrieval is
     * permitted with any valid license, so these tests should not require any enforcement point in the license.
     */
    setEnforcementPoints();
  }

  @Test
  public void testGetLicenses_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getLicensesServiceURL("unlicensedappid", "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetailsList_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getComponentDetailsListUrl("unlicensedappid", "ulg", "ula", "ulv"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentDetails_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getComponentDetailsUrl("unlicensedappId", "ulg", "ula", "ulv", "ulh",
        "unknown"));
    assertResponseStatus(402, response);
  }

  private String getComponentDetailsUrl(String applicationPublicId, String groupId, String artifactId, String version,
      String hash, String matchState)
  {
    return getComponentDetailsUrl(applicationPublicId, groupId, artifactId, version, hash, matchState, null);
  }

  private String getComponentDetailsUrl(String applicationPublicId, String groupId, String artifactId, String version,
      String hash, String matchState, String proprietary)
  {
    return getComponentDetailsUrl(applicationPublicId, getComponentIdentifierParam(groupId, artifactId, version), hash,
        matchState, proprietary);
  }

  private String getComponentDetailsUrl(String applicationPublicId, String identifier, String hash,
      String matchState, String proprietary)
  {
    UriBuilder builder = UriBuilder.fromUri(getServiceURL());
    builder.path("{appId}");
    if (identifier != null) {
      builder.queryParam("componentIdentifier", identifier);
    }
    if (hash != null) {
      builder.queryParam("hash", hash);
    }
    if (matchState != null) {
      builder.queryParam("matchState", matchState);
    }
    if (proprietary != null) {
      builder.queryParam("proprietary", proprietary);
    }
    return builder.build(applicationPublicId).toString();
  }

  private String getComponentDetailsListUrl(String applicationPublicId, String g, String a, String v) {
    return getServiceURL() + "/" + applicationPublicId + "/list?componentIdentifier="
        + getComponentIdentifierParam(g, a, v);
  }

  private String getLicensesServiceURL(String applicationPublicId, String g, String a, String v) {
    return getLicensesServiceURL(applicationPublicId) + "?componentIdentifier=" +
        getComponentIdentifierParam(g, a, v);
  }

  private String getLicensesServiceURL(String applicationPublicId) {
    return getServiceURL() + "/licenses/" + applicationPublicId;
  }

  private String getComponentIdentifierParam(String g, String a, String v) {
    return UrlEncoded.encodeString(toJson(ComponentIdentifier.createMavenCoordinates(g, a, v)));
  }

  private String getServiceURL() {
    return getRestBaseUrl() + getResourcePath();
  }

  protected abstract String getToolName();
}
