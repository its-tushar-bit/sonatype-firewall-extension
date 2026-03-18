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

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
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
  enum Context
  {
    /**
     * An existing {@link Application} entity.
     */
    APPLICATION,

    /**
     * An existing {@link Organization} entity.
     */
    ORGANIZATION,

    /**
     * An existing {@link Repository} entity.
     */
    REPOSITORY,

    /**
     * An existing {@link RepositoryManager} entity.
     */
    REPOSITORY_MANAGER,

    /**
     * An existing {@link Owner} entity.
     */
    APPLICATION_OR_ORGANIZATION,
  }

  /**
   * The permission to check for.
   */
  Permission permission();

  /**
   * The context for the permission check.
   */
  Context context();
}
