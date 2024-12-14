/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.OidcToken;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcTokenDAOTest
    extends AbstractDbDAOTest
{
  private OidcTokenDAO oidcTokenDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    oidcTokenDAO = daoFactory.createOidcTokenDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    OidcToken oidcToken = new OidcToken("id-token");
    oidcTokenDAO.insert(oidcToken);
    assertThat(oidcToken.getId()).isNotNull();

    // Read
    OidcToken storedOidcToken = oidcTokenDAO.getById(oidcToken.getId());
    assertThat(storedOidcToken).isNotNull();
    assertThat(storedOidcToken.getToken()).isEqualTo(oidcToken.getToken());
    assertThat(storedOidcToken.getRegistrationTime()).isEqualTo(oidcToken.getRegistrationTime());

    // Update
    oidcToken.setToken(oidcToken.getToken() + "2");
    oidcTokenDAO.update(oidcToken);
    storedOidcToken = oidcTokenDAO.getById(oidcToken.getId());
    assertThat(storedOidcToken.getToken()).isEqualTo(oidcToken.getToken());
    assertThat(storedOidcToken.getRegistrationTime()).isEqualTo(oidcToken.getRegistrationTime());

    // Delete
    oidcTokenDAO.delete(oidcToken);
    assertThat(oidcTokenDAO.getById(oidcToken.getId())).isNull();
  }

  @Test
  public void testCleanUpOidcTokens() {
    // Insert Tokens
    OidcToken oidcToken1 = new OidcToken("id-token-1");
    OidcToken oidcToken2 = new OidcToken("id-token-2", Date.from(Instant.now().minus(Duration.ofMinutes(10))));
    oidcTokenDAO.insert(oidcToken1);
    oidcTokenDAO.insert(oidcToken2);

    // Run the cleanup
    oidcTokenDAO.cleanUpOidcTokens();

    // Check result
    assertThat(oidcTokenDAO.getById(oidcToken1.getId())).isNotNull();
    assertThat(oidcTokenDAO.getById(oidcToken2.getId())).isNull();
  }

  @Test
  public void testDeleteById() {
    // Insert Tokens
    OidcToken oidcToken = new OidcToken("id-token");
    oidcTokenDAO.insert(oidcToken);

    // Run the cleanup
    oidcTokenDAO.deleteById(oidcToken.getId());

    // Check result
    assertThat(oidcTokenDAO.getById(oidcToken.getId())).isNull();
  }
}
