/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;

public class RoleResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetAllRoles() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl((RoleResource.SERVICE_PATH)));
    assertResponseStatus(200, response);
    Role[] roles = fromJson(response, Role[].class);
    assertThat(roles, not(emptyArray()));
  }
}
