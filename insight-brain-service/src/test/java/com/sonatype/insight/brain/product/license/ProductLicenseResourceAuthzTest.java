/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import com.ning.http.client.Response;
import org.junit.Test;

public class ProductLicenseResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testInstallLicense() throws Exception {
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    Response response = uploadLicense(null /* queryParams */, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = uploadLicense(null /* queryParams */, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(200, response);
  }

  @Test
  public void testUninstallLicense() throws Exception {
    installLicense();

    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    String url = getRestUrl(ProductLicenseResource.SERVICE_PATH);
    Response response = AuthedRestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = AuthedRestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);
  }
}
