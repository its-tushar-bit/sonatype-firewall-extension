/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import javax.inject.Named;

import com.sonatype.insight.brain.organization.OrganizationService;

import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;

/**
 * Automatically un-/registers {@link WaivedComponentUpgradeListener}s with {@link OrganizationService}.
 * @since 1.159
 */
@Named
public class WaivedComponentUpgradeListenerMediator
    implements Mediator<Named, WaivedComponentUpgradeListener, OrganizationService>
{
  @Override
  public void add(BeanEntry<Named, WaivedComponentUpgradeListener> entry, OrganizationService watcher)
      throws Exception
  {
    watcher.addListener(entry.getValue());
  }

  @Override
  public void remove(BeanEntry<Named, WaivedComponentUpgradeListener> entry, OrganizationService watcher)
      throws Exception
  {
    watcher.removeListener(entry.getValue());
  }
}
