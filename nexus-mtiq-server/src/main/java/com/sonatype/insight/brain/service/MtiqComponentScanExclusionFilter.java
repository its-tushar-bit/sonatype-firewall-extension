/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.api.admin.authorization.JwtHttpAuthorizationFilter;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.spring.InsightBrainSpringApplication;
import com.sonatype.insight.brain.spring.config.AdminCompatibilityConfiguration;
import com.sonatype.insight.brain.spring.config.DatabaseConfiguration;
import com.sonatype.insight.brain.spring.config.DropwizardManagementConnectorConfiguration;
import com.sonatype.insight.brain.spring.config.InsightBrainConfiguration;
import com.sonatype.insight.brain.spring.config.JerseyConfiguration;
import com.sonatype.insight.brain.spring.config.MetricsConfiguration;
import com.sonatype.insight.brain.spring.config.ScheduledConfiguration;
import com.sonatype.insight.brain.spring.config.SingleTenantAdminFilterConfiguration;
import com.sonatype.insight.brain.spring.config.SingleTenantMainFilterConfiguration;
import com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter;
import com.sonatype.insight.brain.tenancy.AdminTenantFilter;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Shared MTIQ component-scan exclusions used by both the production bootstrap and the Spring test harness.
 */
public final class MtiqComponentScanExclusionFilter
    implements TypeFilter
{
  private static final Set<String> EXCLUDED_CLASS_NAMES = Set.of(
      FeaturesService.class.getName(),
      JerseyConfiguration.class.getName(),
      MetricsConfiguration.class.getName(),
      AdminCompatibilityConfiguration.class.getName(),
      InsightBrainSpringApplication.class.getName(),
      InsightBrainConfiguration.class.getName(),
      DatabaseConfiguration.class.getName(),
      ScheduledConfiguration.class.getName(),
      TaskScheduler.class.getName(),
      QuartzJobStoreTX.class.getName(),
      InsightMail.class.getName(),
      SingleTenantAdminFilterConfiguration.class.getName(),
      SingleTenantMainFilterConfiguration.class.getName(),
      DropwizardManagementConnectorConfiguration.class.getName(),
      DefaultTenantManagedInitializer.class.getName(),
      MtiqAdminJerseyConfiguration.class.getName(),
      MtiqAdminFilterConfiguration.class.getName(),
      AdminTenantFilter.class.getName(),
      AdminTasksTenantFilter.class.getName(),
      JwtHttpAuthorizationFilter.class.getName());

  private static final String IQ_ONLY_ENDPOINT_ANNOTATION_NAME = IqOnlyEndpoint.class.getName();

  @Override
  public boolean match(
      final MetadataReader metadataReader,
      final MetadataReaderFactory metadataReaderFactory) throws IOException
  {
    String className = metadataReader.getClassMetadata().getClassName();
    AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();

    return EXCLUDED_CLASS_NAMES.contains(className)
        || annotationMetadata.hasAnnotation(IQ_ONLY_ENDPOINT_ANNOTATION_NAME)
        || annotationMetadata.hasMetaAnnotation(IQ_ONLY_ENDPOINT_ANNOTATION_NAME);
  }
}
