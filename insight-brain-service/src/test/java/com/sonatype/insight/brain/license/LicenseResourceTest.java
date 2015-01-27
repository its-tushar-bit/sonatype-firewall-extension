/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LicenseResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGet() throws Exception {
    Response response = AuthedRestAccess.get(getRestBaseUrl() + LicenseResource.SERVICE_PATH);
    assertResponseStatus(200, response);

    License[] licenses = fromJson(response, License[].class);
    Assert.assertNotNull(licenses);
    Assert.assertNotEquals(licenses.length, 0);
    assertTrue(isPresent(License.NO_SOURCE_LICENSE_ID, licenses));
    assertTrue(isPresent(License.NOT_DECLARED_ID, licenses));
    assertTrue(isPresent(License.NO_SOURCES_ID, licenses));
  }

  @Test
  public void testGet_FilterSynthetic() throws Exception {
    Response response = AuthedRestAccess.get(getRestBaseUrl() + LicenseResource.SERVICE_PATH + "?filterSynthetic=true");
    assertResponseStatus(200, response);

    License[] licenses = fromJson(response, License[].class);
    Assert.assertNotNull(licenses);
    assertFalse(isPresent(License.NO_SOURCE_LICENSE_ID, licenses));
    assertFalse(isPresent(License.NOT_DECLARED_ID, licenses));
    assertFalse(isPresent(License.NO_SOURCES_ID, licenses));
    Assert.assertNotEquals(licenses.length, 0);
  }

  private static boolean isPresent(String licenseId, License[] licenses) {
    for (License candidate : licenses) {
      if (candidate.getId().equals(licenseId)) {
        return true;
      }
    }
    return false;
  }
}