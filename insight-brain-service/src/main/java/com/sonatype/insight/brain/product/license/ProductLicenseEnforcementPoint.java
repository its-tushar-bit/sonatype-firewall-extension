/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sonatype.insight.license.model.LicensedFeature;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface ProductLicenseEnforcementPoint
{
  LicensedFeature value();

  /**
   * Additional features that alternatively satisfy this enforcement point: it is met when the license
   * has {@link #value()} OR any feature listed here. Defaults to none, so every existing
   * single-feature usage is unchanged.
   */
  LicensedFeature[] anyOf() default {};
}
