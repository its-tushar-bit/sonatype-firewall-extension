/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ProprietaryConfigResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @After
  public void cleanup() throws Exception {
    File configFile = new File(brain.getDataDir(), "proprietary.json");
    assertTrue(configFile.delete() || !configFile.exists());
  }

  @Test
  // TODO: Enable when CLM-541 is fixed.
  @Ignore
  public void testUpdate() throws Exception {
    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    ProprietaryConfig config = new ProprietaryConfig();
    config.setPackages(packages);
    
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.ADMIN_ROLE_ID, authorized.getUsername());

    String url = getRestUrl(ProprietaryConfigResource.SERVICE_PATH);
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), toJson(config));
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), toJson(config));
    assertResponseStatus(204, response);
  }
}
