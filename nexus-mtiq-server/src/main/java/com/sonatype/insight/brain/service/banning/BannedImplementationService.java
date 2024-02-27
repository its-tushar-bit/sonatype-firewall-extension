/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.service.DropwizardAwareWireModule;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.banning.rest.PermanentlyBannedRestResources;
import com.sonatype.insight.brain.service.banning.rest.TemporarilyBannedRestResources;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Module;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

public class BannedImplementationService
{
  private List<BannedImplementation> listOfBannedTypes;

  public BannedImplementationService() {
  }

  public void setupBannedClasses(Class<?>... extraToBan) {
    listOfBannedTypes = Arrays.asList(
        new DefaultBannedImplementation(extraToBan),
        new PermanentlyBannedRestResources(),
        new TemporarilyBannedRestResources()
    );
  }

  @VisibleForTesting
  protected BannedImplementationService(final List<BannedImplementation> listOfBannedTypes) {
    this.listOfBannedTypes = listOfBannedTypes;
  }

  public boolean isBanned(Class<?> clazz) {
    return listOfBannedTypes.stream().anyMatch(implementation -> implementation.isBanned(clazz));
  }

  public DropwizardAwareModule getBannedModule(final List<Module> modules) {
    return new DropwizardAwareWireModule<InsightConfig>(
        new RequiredExplicitBindingModule(modules, this)
    ).with(new IgnoreBannedImplementationStrategy(this));
  }
}
