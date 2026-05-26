/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import jakarta.inject.Named;
import org.springframework.context.annotation.Primary;

@Named
@Primary
public class MultiTenantVersionService
    extends DefaultVersionService
{

  @Override
  public String getShortVersion() {
    return getBuild();
  }

  @Override
  public String getFullVersion() {
    return getBuild();
  }
}
