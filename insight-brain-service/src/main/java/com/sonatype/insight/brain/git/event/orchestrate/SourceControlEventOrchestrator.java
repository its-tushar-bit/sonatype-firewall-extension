/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import io.dropwizard.lifecycle.Managed;

@Named
@Singleton
public class SourceControlEventOrchestrator
    implements Managed, SourceControlEventCreationListener
{
  private final Map<String, UserEventManager> userEventManagerMap = new HashMap<>();

  private final InsightConfig insightConfig;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlEventProcessor sourceControlEventProcessor;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public SourceControlEventOrchestrator(
      InsightConfig insightConfig,
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlEventProcessor sourceControlEventProcessor,
      SourceControlEventPublisher sourceControlEventPublisher,
      SourceControlUtils sourceControlUtils)
  {
    this.insightConfig = insightConfig;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlEventProcessor = sourceControlEventProcessor;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.sourceControlUtils = sourceControlUtils;
  }

  @Override
  public void onNewEvent(SourceControlEvent event) {
    synchronized (userEventManagerMap) {
      UserEventManager userEventManager = userEventManagerMap.computeIfAbsent(event.getScmUsername(),
          k -> new UserEventManager(sourceControlEventDAO, sourceControlEventProcessor, sourceControlUtils));
      userEventManager.addEvent(event);
    }
  }

  @Override
  public void start() {
    if (insightConfig.isExperimentalFeatureEnabled(Feature.ORCHESTRATED_EVENT_PROCESSING)) {
      sourceControlEventPublisher.setSourceControlEventListener(this);
    }
  }

  @Override
  public void stop() {
    synchronized (userEventManagerMap) {
      userEventManagerMap.forEach((user, userEventManager) -> userEventManager.stop());
      sourceControlEventProcessor.shutdown();
    }
  }
}
