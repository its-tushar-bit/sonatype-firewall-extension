/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

/**
 * The permissions supporting authorization.
 * 
 * @since 1.7
 */
public enum Permission
{

  /**
   * Administer system or org/app's membership mapping.
   */
  ADMIN,

  /**
   * Manages policies etc. for org/app.
   */
  WRITE,

  /**
   * View policy definition and consume policy evaluation results.
   */
  READ

}
