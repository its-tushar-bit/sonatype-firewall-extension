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
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

/**
 * Marks method parameters which denote the context for the authorization check. If no parameters are annotated, the
 * global context applies for the authorization check. Warning: Some parts of Jersey bail if this annotation isn't
 * properly ordered among other parameter annotations (e.g. FormDataParam), putting it first seems to work.
 *
 * @since 1.7
 * @see ContextResolver#resolveContextIds(java.util.Map)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface AuthzContext
{
  enum Key
  {
    /**
     * Public ID of an existing org/app, goes together with {@link #TYPE} to denote the context.
     */
    ID,

    /**
     * Internal ID of an existing org/app/repository, goes together with {@link #TYPE} to denote the context. Note use
     * this or ID with TYPE.
     */
    INTERNAL_ID,

    /**
     * One of {@link OwnerType#APPLICATION}, {@link OwnerType#ORGANIZATION} or {@link OwnerType#REPOSITORY}, goes
     * together with {@link #ID} to denote the context.
     */
    TYPE,

    /**
     * An existing {@link Application} entity.
     */
    APPLICATION,

    /**
     * The owner of an {@link Application} entity.
     */
    APPLICATION_OWNER,

    /**
     * The internal ID of an existing application.
     */
    APPLICATION_ID,

    /**
     * The public ID of an existing application.
     */
    APPLICATION_PUBLIC_ID,

    /**
     * An existing {@link Organization} entity.
     */
    ORGANIZATION,

    /**
     * The owner of an {@link Organization} entity.
     */
    ORGANIZATION_OWNER,

    /**
     * The ID of an existing organization.
     */
    ORGANIZATION_ID,

    /**
     * A {@link Repository} entity. The entity does not have to already exist.
     */
    REPOSITORY,

    /**
     * The ID of an existing repository.
     */
    REPOSITORY_ID,

    /**
     * A {@link RepositoryManager} entity. The entity does not have to already exist.
     */
    REPOSITORY_MANAGER,

    /**
     * The ID of an existing repository manager.
     */
    REPOSITORY_MANAGER_ID,

    /**
     * An {@link Owner} entity.
     */
    OWNER
  }

  /**
   * Map key to use for parameter value when calling {@link ContextResolver#resolveContextIds(java.util.Map)}.
   */
  Key value();
}
