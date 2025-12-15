/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.repository.CreateRepositoryPolicyViolationsEventHandler;
import com.sonatype.insight.brain.repository.IgnoredRepositoryComponentCleaner;
import com.sonatype.insight.brain.repository.InactiveRepositoryViolationCleaner;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.repository.ReevaluateCascadeRequestCleaner;
import com.sonatype.insight.brain.repository.RepositoryComponentDeleteService;
import com.sonatype.insight.brain.repository.RepositoryPolicyAlertEmailer;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.RepositoryQueryService;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.repository.RequestSafeComponentsAutoSelectMetricEventHandler;
import com.sonatype.insight.brain.repository.RequestSafeComponentsMetricEventService;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineRelease;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseTask;
import com.sonatype.insight.brain.repository.client.RepositoryClientFactory;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.repository.component.QuarantinedComponentAccessPurger;
import com.sonatype.insight.brain.repository.component.QuarantinedComponentService;

import com.google.inject.AbstractModule;

/**
 * Guice module providing explicit bindings for Repository components. This replaces Sisu's automatic @Named component
 * discovery.
 */
public class RepositoryModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(AutomaticQuarantineRelease.class);
    bind(AutomaticQuarantineReleaseScheduler.class);
    bind(AutomaticQuarantineReleaseTask.class);
    bind(CreateRepositoryPolicyViolationsEventHandler.class);
    bind(DbQuarantinedComponentAccessManager.class);
    bind(IgnoredRepositoryComponentCleaner.class);
    bind(InactiveRepositoryViolationCleaner.class);
    bind(ProprietaryComponentNameDetector.class);
    bind(QuarantinedComponentAccessPurger.class);
    bind(QuarantinedComponentService.class);
    bind(ReevaluateCascadeRequestCleaner.class);
    bind(RepositoryClientFactory.class);
    bind(RepositoryComponentDeleteService.class);
    bind(RepositoryPolicyAlertEmailer.class);
    bind(RepositoryPolicyEvaluator.class);
    bind(RepositoryQueryService.class);
    bind(RepositoryService.class);
    bind(RequestSafeComponentsAutoSelectMetricEventHandler.class);
    bind(RequestSafeComponentsMetricEventService.class);
  }
}
