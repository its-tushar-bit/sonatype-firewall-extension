/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.api.admin.service.TenantService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiTenantRelayPollingStartDelayCalculatorTest
{
  @Mock
  private TenantService tenantService;

  private MultiTenantRelayPollingStartDelayCalculator calculator;

  @BeforeEach
  public void before() {
    calculator = new MultiTenantRelayPollingStartDelayCalculator(tenantService);
  }

  @Test
  public void singleTenant_offsetIsZeroPlusDefault() {
    int delay = calculator.computeOffset("only", Collections.singletonList("only"), 60, 5);
    assertThat(delay).isEqualTo(5);
  }

  @Test
  public void fourTenants_evenlySpacedAcrossInterval() {
    List<String> tenants = Arrays.asList("slug1", "slug2", "slug3", "slug4");
    int interval = 60;
    int defaultDelay = 0;

    assertThat(calculator.computeOffset("slug1", tenants, interval, defaultDelay)).isEqualTo(0);
    assertThat(calculator.computeOffset("slug2", tenants, interval, defaultDelay)).isEqualTo(15);
    assertThat(calculator.computeOffset("slug3", tenants, interval, defaultDelay)).isEqualTo(30);
    assertThat(calculator.computeOffset("slug4", tenants, interval, defaultDelay)).isEqualTo(45);
  }

  @Test
  public void offsetIsAddedToDefaultInitialDelay() {
    List<String> tenants = Arrays.asList("a", "b");
    assertThat(calculator.computeOffset("a", tenants, 60, 10)).isEqualTo(10);
    assertThat(calculator.computeOffset("b", tenants, 60, 10)).isEqualTo(40);
  }

  @Test
  public void unknownTenant_fallsBackToDefault() {
    List<String> tenants = Arrays.asList("a", "b");
    assertThat(calculator.computeOffset("c", tenants, 60, 30)).isEqualTo(30);
  }

  @Test
  public void emptyTenantList_fallsBackToDefault() {
    assertThat(calculator.computeOffset("a", Collections.emptyList(), 60, 30)).isEqualTo(30);
  }

  @Test
  public void manyTenants_doNotCollapseToSameOffsetFromIntegerTruncation() {
    // 100 tenants on a 60s interval: naive (interval / count) * index would round to 0
    // and all tenants would start at the default delay simultaneously. Verify the
    // multiply-first ordering preserves a meaningful spread.
    List<String> tenants = new java.util.ArrayList<>();
    for (int i = 0; i < 100; i++) {
      tenants.add(String.format("slug%03d", i));
    }
    java.util.Set<Integer> distinctOffsets = new java.util.HashSet<>();
    for (int i = 0; i < tenants.size(); i++) {
      distinctOffsets.add(calculator.computeOffset(tenants.get(i), tenants, 60, 0));
    }
    // Not all 100 can be unique with a 60-second budget but we must spread across the
    // window rather than collapsing every tenant onto the same instant.
    assertThat(distinctOffsets).hasSizeGreaterThan(50);
    assertThat(distinctOffsets).allMatch(o -> o >= 0 && o < 60);
  }

  @Test
  public void productionEntryPoint_resolvesViaTenantService() {
    when(tenantService.getTenantSlug()).thenReturn("slug2");
    // Intentionally unsorted to exercise the Collections.sort step in getCachedSortedTenants
    // — if a future refactor accidentally drops the sort, this test would fail (slug2 would
    // resolve to a different index in the unsorted list and produce a non-15 delay).
    when(tenantService.getAllTenantsNames()).thenReturn(Arrays.asList("slug4", "slug2", "slug1", "slug3"));

    int delay = calculator.computeInitialDelaySeconds(60, 0);

    assertThat(delay).isEqualTo(15);
  }

  @Test
  public void productionEntryPoint_currentTenantMissingFromCache_isMergedIn() {
    // Reproduces the chicken-and-egg case observed in MTIQ: register() runs per-tenant
    // but TenantService.getAllTenantsNames() doesn't yet include the tenant currently
    // registering. Without the defensive merge, every tenant whose slug is missing from
    // the cached snapshot falls through to defaultInitialDelaySeconds and stampedes
    // the relay at the same instant on cold boot.
    when(tenantService.getTenantSlug()).thenReturn("acme");
    when(tenantService.getAllTenantsNames()).thenReturn(Arrays.asList("peas"));

    int delay = calculator.computeInitialDelaySeconds(60, 30);

    // After merge: sorted ["acme", "peas"], acme is index 0 of 2 → 30 + (60*0/2) = 30
    assertThat(delay).isEqualTo(30);

    // And the second tenant lands at +60 once it's known via the service.
    int delayPeas = calculator.computeOffset("peas", Arrays.asList("acme", "peas"), 60, 30);
    assertThat(delayPeas).isEqualTo(60);
  }
}
