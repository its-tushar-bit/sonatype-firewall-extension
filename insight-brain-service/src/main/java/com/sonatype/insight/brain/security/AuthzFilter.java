/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sonatype.insight.brain.model.security.Permission;

/**
 * Marks a method whose return value is a collection with entities that should be filtered depending on the caller's
 * permissions.
 *
 * @since 1.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AuthzFilter
{
  /**
   * Specifies the owner type for authorization filtering. Used to select the appropriate ancestor view for
   * performance optimization - type-specific views (e.g., {@code application_ancestor}) are faster than the
   * generic {@code owner_ancestor} UNION ALL view.
   */
  enum Context
  {
    APPLICATION,
    ORGANIZATION,
    REPOSITORY,
    REPOSITORY_MANAGER,
    APPLICATION_OR_ORGANIZATION,
  }

  /**
   * The permission to check for.
   */
  Permission permission();

  /**
   * Specifies the owner type for authorization filtering. See {@link Context} for details.
   */
  Context context();
}
