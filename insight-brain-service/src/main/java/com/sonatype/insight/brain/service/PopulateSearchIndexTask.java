/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.QuartzConcurrencyListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.index.IndexCreationScheduler;

import io.dropwizard.servlets.tasks.Task;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.quartz.DisallowConcurrentExecution;

@Named
@Singleton
@DisallowConcurrentExecution
public class PopulateSearchIndexTask
    extends Task
{
  private static final String PATH = "populateSearchIndex";

  private final TaskScheduler taskScheduler;

  private final IndexCreationScheduler indexCreationScheduler;

  private final Configuration configuration;

  private final ProductLicense productLicense;

  @Inject
  public PopulateSearchIndexTask(
      final TaskScheduler taskScheduler,
      final IndexCreationScheduler indexCreationScheduler,
      final Configuration configuration,
      final ProductLicense productLicense)
  {
    super(PATH);
    this.taskScheduler = taskScheduler;
    this.indexCreationScheduler = indexCreationScheduler;
    this.configuration = configuration;
    this.productLicense = productLicense;
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    productLicense.validate();
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    taskScheduler.scheduleOneTimeTask(indexCreationScheduler, Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, String.valueOf(configuration.getMaxConcurrentTenantIndexCreation()))
    );
  }
}
