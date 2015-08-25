/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FirewallClientTest
    extends AbstractBrainServiceTest
{

  private static final String REPOSITORY_PUBLIC_ID = "central";

  private String rmInstanceId;

  @Before
  public void start() {
    rmInstanceId = tempEntity.newRepositoryManager().getInstanceId();
  }

  @Test
  public void testEnableRepository() throws Exception {
    FirewallClient client = new FirewallClient(getConfiguration(), rmInstanceId, REPOSITORY_PUBLIC_ID);

    client.enableRepository();

    Repository repo = new RepositoryDAO()
        .getByRepositoryManagerInstanceIdAndPublicId(rmInstanceId, REPOSITORY_PUBLIC_ID);
    assertEquals(REPOSITORY_PUBLIC_ID, repo.getPublicId());
    assertTrue(repo.isEnabled());
  }

  @Test
  public void testEnableRepository_Error() throws Exception {
    FirewallClient client = new FirewallClient(getCLMServer().getClientConfiguration(), rmInstanceId,
        REPOSITORY_PUBLIC_ID);

    try {
      client.enableRepository();
      fail("Did not throw the expected exception");
    }
    catch (HttpResponseException e) {
      assertEquals(401, e.getStatusCode());
    }
  }

  private Configuration getConfiguration() {
    Configuration config = getCLMServer().getClientConfiguration();
    SimpleAuthentication auth = new SimpleAuthentication();
    auth.setPassword("admin123");
    auth.setUsername("admin");
    config.setServerAuth(auth);
    return config;
  }
}
