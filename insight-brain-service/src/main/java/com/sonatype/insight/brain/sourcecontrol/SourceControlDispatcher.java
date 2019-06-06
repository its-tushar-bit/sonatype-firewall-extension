/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;

import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;

@Named
@Singleton
public class SourceControlDispatcher
    implements Managed
{
  private final AsyncEventBus asyncEventBus;

  private ApiSourceControlService sourceControlService;

  @Inject
  public SourceControlDispatcher(final AsyncEventBus asyncEventBus,
      final ApiSourceControlService apiSourceControlService) 
  {
    this.asyncEventBus = asyncEventBus;
    this.sourceControlService = apiSourceControlService;
  }

  @Override
  public void start() throws Exception {
    asyncEventBus.register(this);
  }

  @Override
  public void stop() throws Exception {
    asyncEventBus.unregister(this);
  }

  @Subscribe
  public void on(ApplicationEvaluationEvent event) {
    sourceControlService.maybeRespond(event);
  }
}
