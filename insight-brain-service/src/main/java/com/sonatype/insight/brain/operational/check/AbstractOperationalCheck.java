/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

public abstract class AbstractOperationalCheck
    extends NamedHealthCheck
{
  private final String name;

  protected AbstractOperationalCheck(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
