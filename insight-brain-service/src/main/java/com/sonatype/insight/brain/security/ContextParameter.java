/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.security.AuthzContext.Key;

public class ContextParameter
{

  public final AuthzContext.Key key;

  public final Object object;

  public final boolean multiple;

  public ContextParameter(final Key key, final Object object, final boolean multiple) {
    this.key = key;
    this.object = object;
    this.multiple = multiple;
  }
}