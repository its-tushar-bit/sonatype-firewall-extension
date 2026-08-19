/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class HashComponentIdentifierServiceAuthzTest
    extends AbstractComponentH2AuthzTest
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

  @BeforeEach
  public void resetMockHdsClient() {
    lenient().when(mockHdsClient.get(eq(ComponentSummary.class), eq("rest/component/summary"), anyMap()))
        .thenReturn(componentSummary);
  }

  @AfterEach
  public void cleanup() {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(HASH);
    if (hashComponentIdentifier != null) {
      hashComponentIdentifierDAO.delete(hashComponentIdentifier);
    }
  }

  @Test
  public void testSet_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class, () -> {
      HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
      hashComponentIdentifierService.set(hashComponentIdentifier);
    });
  }

  @Test
  public void testSet_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class, () -> {
      HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
      hashComponentIdentifierService.set(hashComponentIdentifier);
    });
  }

  @Test
  public void testSet() {
    grantClaimComponentPermission();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);

    hashComponentIdentifierService.set(hashComponentIdentifier);
  }

  @Test
  public void testUpdate_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class, () -> {
      HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
      hashComponentIdentifierService.update(hashComponentIdentifier);
    });
  }

  @Test
  public void testUpdate_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class, () -> {
      HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
      hashComponentIdentifierService.update(hashComponentIdentifier);
    });
  }

  @Test
  public void testUpdate() {
    grantClaimComponentPermission();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);
    hashComponentIdentifier.setComponentIdentifier(COMPONENT_IDENTIFIER.createAlternativeVersion("foo"));

    hashComponentIdentifierService.update(hashComponentIdentifier);
  }

  @Test
  public void testDelete_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class, () -> hashComponentIdentifierService.delete(HASH));
  }

  @Test
  public void testDelete_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class, () -> hashComponentIdentifierService.delete(HASH));
  }

  @Test
  public void testDelete() {
    grantClaimComponentPermission();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    hashComponentIdentifierService.delete(HASH);
  }

  @Test
  public void testGet_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class, () -> hashComponentIdentifierService.get(HASH));
  }

  @Test
  public void testGet_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class, () -> hashComponentIdentifierService.get(HASH));
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
