/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

public class LicenseOverrideResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LicenseOverrideResource.SERVICE_PATH);
  }

  @Test
  public void testAddLicenseOverride() throws Exception {
    grantWritePermission(app.getId());
    LicenseOverride override = new LicenseOverride(null, ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
      LicenseOverrideStatus.CONFIRMED, (String)null, "test");

    HttpRequest request = restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()).body(override);
    HttpResponse response = testAuthzPost(request);
    override = response.getBody(LicenseOverride.class);
    new LicenseOverrideDAO().delete(override);

    grantWritePermission(org.getId());
    override = new LicenseOverride(null, ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
      LicenseOverrideStatus.CONFIRMED, (String)null, "test");

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()).body(override);
    response = testAuthzPost(request);
    override = response.getBody(LicenseOverride.class);
    new LicenseOverrideDAO().delete(override);
  }

  @Test
  public void testDeleteLicenseOverride() throws Exception {
    grantWritePermission(app.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "1");
    LicenseOverride override = tempEntity.newLicenseOverride(app.getId(), componentIdentifier,
      LicenseOverrideStatus.CONFIRMED, (String)null);

    HttpRequest request = restRequest().path("{overrideId}").parameter(IdUtils.TYPE_APPLICATION, app.getPublicId(),
        override.getId());
    testAuthzDelete(request);

    grantWritePermission(org.getId());
    override = tempEntity.newLicenseOverride(org.getId(), componentIdentifier, LicenseOverrideStatus.CONFIRMED, (String)null);

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId(), override.getId());
    testAuthzDelete(request);
  }

  @Test
  public void testGetAppliedLicenseOverrides() throws Exception {
    grantReadPermission(app.getId());

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "1");

    HttpRequest request = restRequest().query("componentIdentifier", "{compId}").parameter(IdUtils.TYPE_APPLICATION,
        app.getPublicId(), ComponentIdentifierAdapter.toJson(componentIdentifier));
    testAuthzGet(request);

    grantReadPermission(org.getId());

    request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId(), ComponentIdentifierAdapter.toJson(componentIdentifier));
    testAuthzGet(request);
  }
}
