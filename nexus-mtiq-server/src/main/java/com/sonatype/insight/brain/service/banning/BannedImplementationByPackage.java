/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

public class BannedImplementationByPackage
    implements BannedImplementation
{
  private final String packageName;

  public BannedImplementationByPackage(final String packageName) {
    this.packageName = packageName;
  }

  @Override
  public boolean isBanned(final Class<?> clazz) {
    return clazz.getPackage().getName().startsWith(packageName);
  }
}
