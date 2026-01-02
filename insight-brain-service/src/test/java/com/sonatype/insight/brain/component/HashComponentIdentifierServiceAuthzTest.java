/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@Category(SlowTest.class)
public class HashComponentIdentifierServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String HASH = "test-abcdef";

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("gid",
      "aid", "1.0", "jdk15", "jar");

  private final ComponentSummary componentSummary = new ComponentSummary();

  @Inject
  private HashComponentIdentifierService hashComponentIdentifierService;

  @Inject
  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Mock
  private HdsClient mockHdsClient;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
  }

  @Before
  public void resetMockHdsClient() {
    lenient().when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(componentSummary);
  }

  @After
  public void cleanup() {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(HASH);
    if (hashComponentIdentifier != null) {
      hashComponentIdentifierDAO.delete(hashComponentIdentifier);
    }
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSet_Unauthenticated() {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierService.set(hashComponentIdentifier);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSet_Unauthorized() {
    login();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierService.set(hashComponentIdentifier);
  }

  @Test
  public void testSet() {
    grantClaimComponentPermission();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);

    hashComponentIdentifierService.set(hashComponentIdentifier);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdate_Unauthenticated() {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierService.update(hashComponentIdentifier);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdate_Unauthorized() {
    login();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierService.update(hashComponentIdentifier);
  }

  @Test
  public void testUpdate() {
    grantClaimComponentPermission();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);
    hashComponentIdentifier.setComponentIdentifier(COMPONENT_IDENTIFIER.createAlternativeVersion("foo"));

    hashComponentIdentifierService.update(hashComponentIdentifier);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDelete_Unauthenticated() {
    hashComponentIdentifierService.delete(HASH);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDelete_Unauthorized() {
    login();
    hashComponentIdentifierService.delete(HASH);
  }

  @Test
  public void testDelete() {
    grantClaimComponentPermission();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    hashComponentIdentifierService.delete(HASH);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGet_Unauthenticated() {
    hashComponentIdentifierService.get(HASH);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGet_Unauthorized() {
    login();
    hashComponentIdentifierService.get(HASH);
  }

  @Test
  public void testGet() {
    grantClaimComponentPermission();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);
    hashComponentIdentifier.setComponentIdentifier(COMPONENT_IDENTIFIER.createAlternativeVersion("foo"));

    hashComponentIdentifierService.get(HASH);
  }
}
