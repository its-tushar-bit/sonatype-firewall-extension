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

/**
 * Annotation to enforce tier-level entitlement checks on REST resource methods.
 * Works alongside {@link ProductLicenseEnforcementPoint} — both checks run independently.
 *
 * <p>
 * {@code ProductLicenseEnforcementPoint} gates base license features (e.g., POLICY_MANAGEMENT).
 * {@code RequiresEntitlement} gates tier-specific features (e.g., CUSTOM_POLICIES for Enterprise).
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * &#64;ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)  // class-level: base license
 * public class PolicyResource {
 *
 *     &#64;POST
 *     &#64;RequiresEntitlement(LicensedFeature.CUSTOM_POLICIES)  // method-level: tier entitlement
 *     public Policy addPolicy(...) {
 *         // Both POLICY_MANAGEMENT and CUSTOM_POLICIES are validated
 *     }
 * }
 * </pre>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface RequiresEntitlement
{
  LicensedFeature value();
}
