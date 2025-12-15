/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.google.inject.AbstractModule;

import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ApplicationCleaner;
import com.sonatype.insight.brain.organization.ApplicationCloneService;
import com.sonatype.insight.brain.organization.ApplicationCountHistoryKeeper;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.organization.ApplicationMoveService;
import com.sonatype.insight.brain.organization.ApplicationNameConverter;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.ApplicationSourceControlService;
import com.sonatype.insight.brain.organization.ApplicationTelemetryCollector;
import com.sonatype.insight.brain.organization.MoveOrganizationService;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.organization.OrganizationTelemetryCollector;
import com.sonatype.insight.brain.organization.RobotImageService;
import com.sonatype.insight.brain.organization.SampleDataCreator;

/**
 * Guice module providing explicit bindings for Organization components.
 * This replaces Sisu's automatic @Named component discovery.
 */
public class OrganizationModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(ApplicationAdapter.class);
    bind(ApplicationCleaner.class);
    bind(ApplicationCloneService.class);
    bind(ApplicationCountHistoryKeeper.class);
    bind(ApplicationHelper.class);
    bind(ApplicationMoveService.class);
    bind(ApplicationNameConverter.class);
    bind(ApplicationService.class);
    bind(ApplicationSourceControlService.class);
    bind(ApplicationTelemetryCollector.class);
    bind(MoveOrganizationService.class);
    bind(OrganizationService.class);
    bind(OrganizationTelemetryCollector.class);
    bind(RobotImageService.class);
    bind(SampleDataCreator.class);
  }
}
