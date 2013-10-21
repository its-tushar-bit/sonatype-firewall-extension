/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a boolean method parameter that indicates whether authorization failures should be handled by returning a plain
 * text error message from the protected method instead of throwing an exception. If applied on a method,
 * unconditionally reports authorization failures via string messages. This is useful for those special REST resources
 * that must not return error codes due to compatibility with certain browsers.
 * 
 * @since 1.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface AuthzErrorMsg
{
}
