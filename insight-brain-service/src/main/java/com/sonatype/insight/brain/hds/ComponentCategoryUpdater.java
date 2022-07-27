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
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ComponentCategoryUpdater
    extends AbstractComponentCategoryUpdater
    implements Job
{
  private static final Logger log = LoggerFactory.getLogger(ComponentCategoryUpdater.class);

  public static final String HDS_COMPONENT_CATEGORY_PATH = "rest/componentCategories";

  // Visible for testing
  static final String TASK_NAME = "LoadComponentCategories";

  private final HdsClient client;

  private final TaskScheduler taskScheduler;

  @Inject
  public ComponentCategoryUpdater(HdsClient client, TaskScheduler taskScheduler) {
    this.client = client;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void doUpdate() {
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
    loadComponentCategoriesOnAllOtherClusterNodes();
  }

  private void loadComponentCategoriesOnAllOtherClusterNodes() {
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(getClass(), TASK_NAME);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      doLoadComponentCategories();
    }
    catch (Exception e) {
      log.error("Error when loading component categories: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  // Visible for testing
  void doLoadComponentCategories() {
    loadComponentCategories();
  }
}
