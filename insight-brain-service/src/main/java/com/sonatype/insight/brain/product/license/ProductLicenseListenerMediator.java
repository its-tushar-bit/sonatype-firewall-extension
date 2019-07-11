/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import javax.inject.Named;

import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;

/**
 * Automatically un-/registers {@link ProductLicenseListener}s with {@link CLMLicenseManager}.
 */
@Named
class ProductLicenseListenerMediator
    implements Mediator<Named, ProductLicenseListener, CLMLicenseManager>
{
  @Override
  public void add(BeanEntry<Named, ProductLicenseListener> entry, CLMLicenseManager watcher) throws Exception {
    watcher.addListener(entry.getValue());
  }

  @Override
  public void remove(BeanEntry<Named, ProductLicenseListener> entry, CLMLicenseManager watcher) throws Exception {
    watcher.removeListener(entry.getValue());
  }
}
