/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.NameBinding;

/**
 * Marker annotation for REST resources that should ONLY be available in single-tenant IQ Server
 * and should NOT be loaded in Multi-Tenant IQ (MTIQ). Marking a class with this annotation stops the resource class
 * being loaded at all. This is different to BlockIfMultiTenant which allows the class to be loaded but uses a filter
 * to prevent access.
 * <p>
 * Example usage:
 * <pre>
 * &#64;Named
 * &#64;IqOnlyEndpoint
 * &#64;Path("/api/v2/config/features")
 * public class ApiConfigFeaturesResource {
 *   // This resource will only be available in IQ, not MTIQ
 * }
 * </pre>
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IqOnlyEndpoint
{
}
