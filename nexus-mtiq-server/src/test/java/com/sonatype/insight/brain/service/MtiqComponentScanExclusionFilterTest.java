/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

public class MtiqComponentScanExclusionFilterTest
{
  private static final String DEFAULT_INSIGHT_MAIL =
      "com.sonatype.insight.brain.service.InsightMail";

  private static final String MTIQ_INSIGHT_MAIL =
      "com.sonatype.insight.brain.service.MultiTenantInsightMail";

  private static final String DEFAULT_TENANT_MANAGED_INITIALIZER =
      "com.sonatype.insight.brain.service.DefaultTenantManagedInitializer";

  private static final String MTIQ_TENANT_MANAGED_INITIALIZER =
      "com.sonatype.insight.brain.tenancy.MultiTenantTenantManagedInitializer";

  // Single-tenant-only; admitted by component scan via matchIfMissing unless the exclusion filter removes it.
  private static final String SINGLE_TENANT_METRICS_CONFIGURATION =
      "com.sonatype.insight.brain.spring.config.MetricsConfiguration";

  @Test
  public void shouldExcludeSingleTenantDefaultsFromProductionMtiqCandidates() throws ClassNotFoundException {
    Set<String> candidateClassNames = findCandidates(MultiTenantInsightBrainService.class);

    assertThat(candidateClassNames)
        .contains(MTIQ_INSIGHT_MAIL, MTIQ_TENANT_MANAGED_INITIALIZER)
        .doesNotContain(DEFAULT_INSIGHT_MAIL, DEFAULT_TENANT_MANAGED_INITIALIZER,
            SINGLE_TENANT_METRICS_CONFIGURATION);
  }

  @Test
  public void shouldExcludeSingleTenantDefaultsFromSpringMtiqTestHarnessCandidates() throws ClassNotFoundException {
    Set<String> candidateClassNames = findCandidates(SpringMultiTenantTestInsightBrainService.class);

    assertThat(candidateClassNames)
        .contains(MTIQ_INSIGHT_MAIL, MTIQ_TENANT_MANAGED_INITIALIZER)
        .doesNotContain(DEFAULT_INSIGHT_MAIL, DEFAULT_TENANT_MANAGED_INITIALIZER,
            SINGLE_TENANT_METRICS_CONFIGURATION);
  }

  private Set<String> findCandidates(final Class<?> applicationClass) throws ClassNotFoundException {
    ComponentScan componentScan = applicationClass.getAnnotation(ComponentScan.class);

    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
    applyExcludeFilters(scanner, componentScan);

    return Arrays.stream(componentScan.basePackages())
        .flatMap(basePackage -> scanner.findCandidateComponents(basePackage).stream())
        .map(BeanDefinition::getBeanClassName)
        .collect(Collectors.toSet());
  }

  private void applyExcludeFilters(
      final ClassPathScanningCandidateComponentProvider scanner,
      final ComponentScan componentScan) throws ClassNotFoundException
  {
    for (ComponentScan.Filter filter : componentScan.excludeFilters()) {
      if (filter.type() == FilterType.ASSIGNABLE_TYPE) {
        for (Class<?> candidate : filter.classes()) {
          scanner.addExcludeFilter(new AssignableTypeFilter(candidate));
        }
      }
      else if (filter.type() == FilterType.ANNOTATION) {
        for (Class<?> candidate : filter.classes()) {
          @SuppressWarnings("unchecked")
          Class<Annotation> annotationType = (Class<Annotation>) candidate;
          scanner.addExcludeFilter(new AnnotationTypeFilter(annotationType));
        }
      }
      else if (filter.type() == FilterType.REGEX) {
        for (String pattern : filter.pattern()) {
          scanner.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(pattern)));
        }
      }
      else if (filter.type() == FilterType.CUSTOM) {
        for (Class<?> candidate : filter.classes()) {
          try {
            scanner.addExcludeFilter((TypeFilter) candidate.getDeclaredConstructor().newInstance());
          }
          catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not instantiate custom filter " + candidate.getName(), e);
          }
        }
      }
    }
  }
}
