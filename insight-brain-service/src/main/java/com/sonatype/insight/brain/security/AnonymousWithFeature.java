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

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

/**
 * Marks a method as allowing anonymous access only if a given feature flag is enabled.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AnonymousWithFeature
{
  /**
   * The feature flag which, if enabled, allows anonymous access.
   */
  SystemConfigurationPropertyFeature value() default SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES;
}
