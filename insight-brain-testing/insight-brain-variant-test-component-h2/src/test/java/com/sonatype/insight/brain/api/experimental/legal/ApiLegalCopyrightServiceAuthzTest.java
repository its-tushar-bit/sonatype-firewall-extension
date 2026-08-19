/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.Collections;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApiLegalCopyrightServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final ComponentIdentifier COMPONENT_IDENTIFIER =
      ComponentIdentifier.createMavenCoordinates("g", "a", "v");

  @Inject
  private ApiLegalCopyrightService apiLegalCopyrightService;

  @Mock
  private ApiLicenseLegalHdsService mockHdsService;

  @BeforeEach
  public void setUpMocks() {
    lenient().when(mockHdsService.getComponentLegalCommentFilePaths(any())).thenReturn(Collections.emptyList());
    lenient().when(mockHdsService.getAnameRawComponentLegalComments(any())).thenReturn(Collections.emptySet());
  }

  @Test
  public void testGetCopyrightFilePaths_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", 0, 10));
  }

  @Test
  public void testGetCopyrightFilePaths_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", 0, 10));
  }

  @Test
  public void testGetCopyrightFilePaths_Authorized() {
    grantLegalReviewerPermission(org.getPublicId());
    assertThatNoException().isThrownBy(() -> apiLegalCopyrightService.getCopyrightFilePaths(
        OwnerType.ORGANIZATION, org.getPublicId(),
        COMPONENT_IDENTIFIER,
        "hash", "copyright hash 2", 0, 10));
  }

  @Test
  public void testGetCopyrightContextContent_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightContextContent(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", "path/file"));
  }

  @Test
  public void testGetCopyrightContextContent_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightContextContent(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", "path/file"));
  }

  @Test
  public void testGetCopyrightContextContent_Authorized() {
    grantLegalReviewerPermission(org.getPublicId());
    login();
    assertThatNoException().isThrownBy(() -> apiLegalCopyrightService.getCopyrightContextContent(
        OwnerType.ORGANIZATION, org.getPublicId(),
        COMPONENT_IDENTIFIER,
        "hash", "copyright hash 2", "path/file"));
  }

  @Test
  public void testGetCopyrightFileCount_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFileCount(
            OwnerType.ORGANIZATION, org.getPublicId(), COMPONENT_IDENTIFIER, "hash"));
  }

  @Test
  public void testGetCopyrightFileCount_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFileCount(
            OwnerType.ORGANIZATION, org.getPublicId(), COMPONENT_IDENTIFIER, "hash"));
  }

  @Test
  public void testGetCopyrightFileCount_Authorized() {
    grantLegalReviewerPermission(org.getPublicId());
    assertThatNoException().isThrownBy(() -> apiLegalCopyrightService.getCopyrightFileCount(
        OwnerType.ORGANIZATION, org.getPublicId(), COMPONENT_IDENTIFIER, "hash"));
  }
}
