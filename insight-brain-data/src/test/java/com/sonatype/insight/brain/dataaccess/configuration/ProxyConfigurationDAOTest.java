/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyConfigurationDAOTest
    extends AbstractDbDAOTest
{
  ProxyConfigurationDAO dao = new ProxyConfigurationDAO();

  @Test
  public void testSetAndGetNoProxyHosts() throws Exception {
    // Write
    String proxyExcludeHosts = "example.com";
    dao.setProxyExcludeHosts(proxyExcludeHosts);

    // Read
    String result = dao.getProxyExcludeHosts();
    assertThat(result).isEqualTo(proxyExcludeHosts);
  }
}
