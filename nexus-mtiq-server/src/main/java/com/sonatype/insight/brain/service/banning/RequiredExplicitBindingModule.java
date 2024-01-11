/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.List;

import com.sonatype.insight.brain.service.DropwizardAwareAggregatingModule;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.google.inject.Module;
import com.google.inject.Key;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.UntargettedBinding;

public class RequiredExplicitBindingModule
    extends DropwizardAwareAggregatingModule<MultiTenantInsightConfig>
{
  private final BannedImplementationService banned;

  public RequiredExplicitBindingModule(final List<Module> modules, final BannedImplementationService banned) {
    super(modules);
    this.banned = banned;
  }

  @Override
  protected void configure() {
    for (Element element : Elements.getElements(modules)) {
      if (isUntargettedBinding(element) || isDefinedAsInterface(element)) {
        continue;
      }
      element.applyTo(binder());
    }
    binder().requireExplicitBindings();
  }

  private boolean isDefinedAsInterface(final Element element) {
    if (element instanceof LinkedKeyBinding) {
      return banned.isBanned(((LinkedKeyBinding) element).getLinkedKey().getTypeLiteral().getRawType());
    }
    return false;
  }

  private boolean isUntargettedBinding(final Element element) {
    if (element instanceof UntargettedBinding) {
      UntargettedBinding untargettedBinding = (UntargettedBinding) element;
      Key key = untargettedBinding.getKey();
      return banned.isBanned(key.getTypeLiteral().getRawType());
    }
    return false;
  }
}
