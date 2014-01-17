/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.utils.IdUtils;

/**
 * Marks method parameters which denote the context for the authorization check. If no parameters are annotated, the
 * global context applies for the authorization check. Warning: Some parts of Jersey bail if this annotation isn't
 * properly ordered among other parameter annotations (e.g. FormDataParam), putting it last seems to work.
 * 
 * @since 1.7
 * @see ContextResolver#resolveContextIds(java.util.Map)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface AuthzContext
{
  enum Key
  {
    /**
     * Public ID of an existing org/app, goes together with {@link #TYPE} to denote the context.
     */
    ID,

    /**
     * One of {@link IdUtils#TYPE_APPLICATION} or {@link IdUtils#TYPE_ORGANIZATION}, goes together with {@link #ID} to
     * denote the context.
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
    ORGANIZATION_ID;
  }

  /**
   * Map key to use for parameter value when calling {@link ContextResolver#resolveContextIds(java.util.Map)}.
   */
  Key value();
}
