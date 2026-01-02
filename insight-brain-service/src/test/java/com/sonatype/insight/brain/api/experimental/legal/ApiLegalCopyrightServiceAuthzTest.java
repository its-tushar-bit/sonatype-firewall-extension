/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@Category(SlowTest.class)
public class ApiLegalCopyrightServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final ComponentIdentifier COMPONENT_IDENTIFIER =
      ComponentIdentifier.createMavenCoordinates("g", "a", "v");

  @Inject
  private ApiLegalCopyrightService apiLegalCopyrightService;

  @Mock
  private ApiLicenseLegalHdsService mockHdsService;

  @Override
  public void configure(final Binder binder) {
    binder.bind(ApiLicenseLegalHdsService.class).toInstance(mockHdsService);
    lenient().when(mockHdsService.getComponentLegalCommentFilePaths(any())).thenReturn(Collections.emptyList());
    super.configure(binder);
  }

  @Test
  public void testGetCopyrightFilePaths_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", 0, 10));
  }

  @Test
  public void testGetCopyrightFilePaths_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", 0, 10));
  }

  @Test
  public void testGetCopyrightFilePaths_Authorized() {
    grantLegalReviewerPermission(org.getPublicId());
    assertThatNoException().isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", 0, 10));
  }

  @Test
  public void testGetCopyrightContextContent_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightContextContent(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", "path/file"));
  }

  @Test
  public void testGetCopyrightContextContent_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightContextContent(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", "path/file"));
  }

  @Test
  public void testGetCopyrightContextContent_Authorized() {
    grantLegalReviewerPermission(org.getPublicId());
    login();
    assertThatNoException().isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightContextContent(
            OwnerType.ORGANIZATION, org.getPublicId(),
            COMPONENT_IDENTIFIER,
            "hash", "copyright hash 2", "path/file"));
  }

  @Test
  public void testGetCopyrightFileCount_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightFileCount(
            OwnerType.ORGANIZATION, org.getPublicId(), COMPONENT_IDENTIFIER, "hash"));
  }

  @Test
  public void testGetCopyrightFileCount_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightFileCount(
            OwnerType.ORGANIZATION, org.getPublicId(), COMPONENT_IDENTIFIER, "hash"));
  }

  @Test
  public void testGetCopyrightFileCount_Authorized() {
    grantLegalReviewerPermission(org.getPublicId());
    assertThatNoException().isThrownBy(() ->
        apiLegalCopyrightService.getCopyrightFileCount(
            OwnerType.ORGANIZATION, org.getPublicId(), COMPONENT_IDENTIFIER, "hash"));
  }
}
