/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentCategory;
import com.sonatype.clm.dto.model.component.ComponentCategoryList;
import com.sonatype.insight.brain.dataaccess.AbstractComponentCategoryUpdater;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ComponentCategoryUpdater
    extends AbstractComponentCategoryUpdater
{
  private static final Logger log = LoggerFactory.getLogger(ComponentCategoryUpdater.class);

  public static final String HDS_COMPONENT_CATEGORY_PATH = "rest/componentCategories";

  private final HdsClient client;

  @Inject
  public ComponentCategoryUpdater(HdsClient client) {
    this.client = client;
  }

  @Override
  public void doUpdate() {
    loadComponentCategories();
  }

  private void loadComponentCategories() {
    log.info("Updating component categories");
    long start = System.currentTimeMillis();

    try {
      ComponentCategoryList componentCategoryList =
          client.get(ComponentCategoryList.class, HDS_COMPONENT_CATEGORY_PATH, null /* params */);

      ComponentCategoryDAO componentCategoryDAO = new ComponentCategoryDAO();
      try (TransactionContext tx = componentCategoryDAO.createTransactionContext()) {
        tx.begin();
        for (ComponentCategory componentCategory : componentCategoryList.getComponentCategories()) {
          if (componentCategoryDAO.getById(tx, String.valueOf(componentCategory.getComponentCategoryId())) == null) {
            componentCategoryDAO
                .insert(tx, new com.sonatype.insight.brain.model.component.ComponentCategory(componentCategory));
          }
          else {
            componentCategoryDAO
                .update(tx, new com.sonatype.insight.brain.model.component.ComponentCategory(componentCategory));
          }
        }
        tx.commit();
      }
    }
    catch (Exception e) {
      throw new RuntimeException("Could not retrieve component category data from Sonatype HDS: " + e.getMessage(), e);
    }
    log.debug("Updated component categories in {} ms.", System.currentTimeMillis() - start);
  }
}
