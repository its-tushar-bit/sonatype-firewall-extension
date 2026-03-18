/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentCategory;
import com.sonatype.clm.dto.model.component.ComponentCategoryList;
import com.sonatype.insight.brain.dataaccess.AbstractComponentCategoryUpdater;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ComponentCategoryUpdater
    extends AbstractComponentCategoryUpdater
    implements InsightJob, GlobalTenantJob
{
  private static final Logger log = LoggerFactory.getLogger(ComponentCategoryUpdater.class);

  public static final String HDS_COMPONENT_CATEGORY_PATH = "rest/componentCategories";

  // Visible for testing
  static final String TASK_NAME = "LoadComponentCategories";

  private static final String CATEGORY_LOAD_ERROR = "Error when loading component categories";

  private final HdsClient client;

  private final TaskScheduler taskScheduler;

  @Inject
  public ComponentCategoryUpdater(
      HdsClient client,
      TaskScheduler taskScheduler,
      ComponentCategoryDAO componentCategoryDAO)
  {
    super(componentCategoryDAO);
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
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    execute(this::doLoadComponentCategories, log, CATEGORY_LOAD_ERROR);
  }

  // Visible for testing
  void doLoadComponentCategories() {
    loadComponentCategories();
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
