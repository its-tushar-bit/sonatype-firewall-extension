/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.spring.config.NamedBeanRegistrationConfiguration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

public class SpringMultiTenantTestInsightBrainServiceImportAlignmentTest
{
  @Test
  public void shouldUseOnlyTheMtiqNamedBeanRegistrarInTheSpringTestHarness() {
    List<Class<?>> testHarnessImports = Arrays.asList(importedClasses(SpringMultiTenantTestInsightBrainService.class));

    assertThat(testHarnessImports)
        .contains(MultiTenantDataAccessConfiguration.class)
        .doesNotContain(NamedBeanRegistrationConfiguration.class);
  }

  @Test
  public void shouldShareTheMtiqComponentScanExclusionFilterAcrossProductionAndTestBootstraps() {
    assertThat(customFilterClasses(MultiTenantInsightBrainService.class))
        .containsExactly(MtiqComponentScanExclusionFilter.class);
    assertThat(customFilterClasses(SpringMultiTenantTestInsightBrainService.class))
        .contains(MtiqComponentScanExclusionFilter.class);
  }

  private Class<?>[] importedClasses(final Class<?> applicationClass) {
    Import importAnnotation = applicationClass.getAnnotation(Import.class);
    assertThat(importAnnotation).isNotNull();
    return importAnnotation.value();
  }

  private List<Class<?>> customFilterClasses(final Class<?> applicationClass) {
    ComponentScan componentScan = applicationClass.getAnnotation(ComponentScan.class);
    assertThat(componentScan).isNotNull();
    return Arrays.stream(componentScan.excludeFilters())
        .filter(filter -> filter.type() == FilterType.CUSTOM)
        .flatMap(filter -> Arrays.stream(filter.classes()))
        .collect(Collectors.toList());
  }
}
