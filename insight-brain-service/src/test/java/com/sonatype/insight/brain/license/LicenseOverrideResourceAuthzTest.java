/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Test;

public class LicenseOverrideResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testAddLicenseOverride() throws Exception {
    grantWritePermission(app.getId());
    LicenseOverride override = new LicenseOverride(null, ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
      LicenseOverrideStatus.CONFIRMED, (String)null, "test");

    String url = getRestUrl(LicenseOverrideResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = testAuthzPost(url, toJson(override));
    override = fromJson(response, LicenseOverride.class);
    new LicenseOverrideDAO().delete(override);

    grantWritePermission(org.getId());
    override = new LicenseOverride(null, ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
      LicenseOverrideStatus.CONFIRMED, (String)null, "test");

    url = getRestUrl(LicenseOverrideResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = testAuthzPost(url, toJson(override));
    override = fromJson(response, LicenseOverride.class);
    new LicenseOverrideDAO().delete(override);
  }

  @Test
  public void testDeleteLicenseOverride() throws Exception {
    grantWritePermission(app.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "1");
    LicenseOverride override = tempEntity.newLicenseOverride(app.getId(), componentIdentifier,
      LicenseOverrideStatus.CONFIRMED, (String)null);

    String url = getRestUrl(LicenseOverrideResource.SERVICE_PATH + "/{overrideId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), override.getId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());
    override = tempEntity.newLicenseOverride(org.getId(), componentIdentifier, LicenseOverrideStatus.CONFIRMED, (String)null);

    url = getRestUrl(LicenseOverrideResource.SERVICE_PATH + "/{overrideId}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        override.getId());
    testAuthzDelete(url);
  }

  @Test
  public void testGetAppliedLicenseOverrides() throws Exception {
    grantReadPermission(app.getId());

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "1");

    String url = getServiceURL(IdUtils.TYPE_APPLICATION, app.getPublicId(), componentIdentifier);
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getServiceURL(IdUtils.TYPE_ORGANIZATION, org.getId(), componentIdentifier);
    testAuthzGet(url);
  }

  private String getServiceURL(final String ownerType, final String ownerId, final ComponentIdentifier componentIdentifier) {
    UriBuilder builder = UriBuilder.fromUri(getRestUrl(LicenseOverrideResource.SERVICE_PATH, ownerType, ownerId));
    if (componentIdentifier != null) {
      builder.queryParam("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier));
    }
    return builder.build().toString();
  }
}
