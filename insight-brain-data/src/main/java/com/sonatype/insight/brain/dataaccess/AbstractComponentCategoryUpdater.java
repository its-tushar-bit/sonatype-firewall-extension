/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractComponentCategoryUpdater
{
  private static final Logger log = LoggerFactory.getLogger(AbstractComponentCategoryUpdater.class);

  private static AbstractComponentCategoryUpdater updater;

  public static final synchronized void update() {
    if (updater == null) {
      log.warn("Cannot update component category data because there is no component category updater.");
      return;
    }

    updater.doUpdate();
    new ComponentCategoryDAO().load();
  }

  // for testing only
  static AbstractComponentCategoryUpdater getUpdater() {
    return updater;
  }

  public static void setUpdater(final AbstractComponentCategoryUpdater componentCategoryUpdater) {
    updater = componentCategoryUpdater;
  }

  public abstract void doUpdate();
}
