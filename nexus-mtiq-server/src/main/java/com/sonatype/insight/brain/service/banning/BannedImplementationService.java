/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Module;
import com.google.inject.spi.Elements;
import org.eclipse.sisu.wire.WireModule;

public class BannedImplementationService
{
  private static final List<BannedImplementation> DEFAULT_BANNED = Arrays.asList(
      new DefaultBannedImplementation()
  );

  private final List<BannedImplementation> listOfBannedTypes;

  public BannedImplementationService() {
    this(DEFAULT_BANNED);
  }

  @VisibleForTesting
  protected BannedImplementationService(final List<BannedImplementation> listOfBannedTypes) {
    this.listOfBannedTypes = listOfBannedTypes;
  }

  public boolean isBanned(Class<?> clazz) {
    return listOfBannedTypes.stream().anyMatch(implementation -> implementation.isBanned(clazz));
  }

  public Module getBannedModule(final List<Module> modules) {
    return new WireModule(
        new RequiredExplicitBindingModule(Elements.getElements(modules), this)
    ).with(new IgnoreBannedImplementationStrategy(this));
  }
}
