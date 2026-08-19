/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CveAffectedComponentsServiceTest
{
  @Mock
  private HdsClient hdsClient;

  private CveAffectedComponentsService service;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new CveAffectedComponentsService(hdsClient);
  }

  @Test
  public void testFetchMultipleCves_FallbackToIndividual() {
    Set<String> cveIds = Set.of("CVE-2025-55182", "CVE-2025-12345");

    when(hdsClient.getWithMultimap(
        eq(AffectedComponentList.class), eq("/rest/vulnerability/affected"), any()))
            .thenAnswer(invocation -> {
              Multimap<String, String> params = invocation.getArgument(2);

              if (params.get("refId").size() > 1) {
                throw new BadRequestException("Only one RefId is allowed");
              }

              String cveId = params.get("refId").iterator().next();

              if (cveId.equals("CVE-2025-55182")) {
                return new AffectedComponentList(Collections.singletonList(
                    new AffectedComponentDTO(
                        "maven", "org.springframework", "spring-core", "5.3.30", null)),
                    null, null);
              }
              else if (cveId.equals("CVE-2025-12345")) {
                return new AffectedComponentList(Collections.singletonList(
                    new AffectedComponentDTO("npm", "", "lodash", "4.17.19", null)), null, null);
              }

              return new AffectedComponentList(Collections.emptyList(), null, null);
            });

    Map<String, Set<AffectedCoordinates>> result = service.fetchAffectedComponentsForMultipleCves(cveIds);

    assertThat(result).hasSize(2);
    assertThat(result).containsKeys("CVE-2025-55182", "CVE-2025-12345");

    Set<AffectedCoordinates> cve55182Components = result.get("CVE-2025-55182");
    assertThat(cve55182Components).hasSize(1);
    assertThat(cve55182Components).extracting(AffectedCoordinates::name).containsExactly("spring-core");
    assertThat(cve55182Components).extracting(AffectedCoordinates::format).containsExactly("maven");
    assertThat(cve55182Components).extracting(AffectedCoordinates::namespace)
        .containsExactly("org.springframework");
    assertThat(cve55182Components).extracting(AffectedCoordinates::version).containsExactly("5.3.30");

    Set<AffectedCoordinates> cve12345Components = result.get("CVE-2025-12345");
    assertThat(cve12345Components).hasSize(1);
    assertThat(cve12345Components).extracting(AffectedCoordinates::name).containsExactly("lodash");
    assertThat(cve12345Components).extracting(AffectedCoordinates::format).containsExactly("npm");
    assertThat(cve12345Components).extracting(AffectedCoordinates::version).containsExactly("4.17.19");
  }

  @Test
  public void testFetchMultipleCves_WithPagination() {
    Set<String> cveIds = Set.of("CVE-2025-55182", "CVE-2025-12345");

    AffectedComponentDTO comp1 = new AffectedComponentDTO(
        "maven", "org.springframework", "spring-core", "5.3.30", null);
    AffectedComponentDTO comp2 = new AffectedComponentDTO("npm", "", "lodash", "4.17.19", null);
    AffectedComponentDTO comp3 = new AffectedComponentDTO(
        "maven", "com.example", "example-lib", "1.0.0", null);

    when(hdsClient.getWithMultimap(
        eq(AffectedComponentList.class), eq("/rest/vulnerability/affected"), any()))
            .thenAnswer(invocation -> {
              Multimap<String, String> params = invocation.getArgument(2);

              if (params.get("refId").size() > 1) {
                throw new BadRequestException("Only one RefId is allowed");
              }

              String cveId = params.get("refId").iterator().next();

              if (cveId.equals("CVE-2025-55182")) {
                if (params.containsKey("cursor")) {
                  return new AffectedComponentList(Collections.singletonList(comp2), null, false);
                }
                else {
                  return new AffectedComponentList(Collections.singletonList(comp1), "cursor123", true);
                }
              }
              else if (cveId.equals("CVE-2025-12345")) {
                return new AffectedComponentList(Collections.singletonList(comp3), null, false);
              }

              return new AffectedComponentList(Collections.emptyList(), null, false);
            });

    Map<String, Set<AffectedCoordinates>> result = service.fetchAffectedComponentsForMultipleCves(cveIds);

    assertThat(result).hasSize(2);
    assertThat(result).containsKey("CVE-2025-55182");
    assertThat(result).containsKey("CVE-2025-12345");

    Set<AffectedCoordinates> cve55182Components = result.get("CVE-2025-55182");
    assertThat(cve55182Components).hasSize(2);
    assertThat(cve55182Components).extracting(AffectedCoordinates::name)
        .containsExactlyInAnyOrder("spring-core", "lodash");

    Set<AffectedCoordinates> cve12345Components = result.get("CVE-2025-12345");
    assertThat(cve12345Components).hasSize(1);
    assertThat(cve12345Components).extracting(AffectedCoordinates::name).containsExactly("example-lib");
  }

  @Test
  public void testFetchMultipleCves_BatchMode_WithPerComponentRefIds() {
    Set<String> cveIds = Set.of("CVE-2025-55182", "CVE-2025-12345");

    AffectedComponentDTO comp1 = new AffectedComponentDTO(
        "maven", "org.springframework", "spring-core", "5.3.30",
        List.of("CVE-2025-55182", "CVE-2025-12345"));
    AffectedComponentDTO comp2 = new AffectedComponentDTO(
        "npm", "", "lodash", "4.17.19", List.of("CVE-2025-12345"));

    when(hdsClient.getWithMultimap(
        eq(AffectedComponentList.class), eq("/rest/vulnerability/affected"), any()))
            .thenAnswer(invocation -> {
              Multimap<String, String> params = invocation.getArgument(2);

              if (params.get("refId").size() > 1) {
                return new AffectedComponentList(List.of(comp1, comp2), null, false);
              }

              throw new IllegalStateException("Should use batch mode");
            });

    Map<String, Set<AffectedCoordinates>> result = service.fetchAffectedComponentsForMultipleCves(cveIds);

    assertThat(result).hasSize(2);
    assertThat(result).containsKeys("CVE-2025-55182", "CVE-2025-12345");

    Set<AffectedCoordinates> cve55182Components = result.get("CVE-2025-55182");
    assertThat(cve55182Components).hasSize(1);
    assertThat(cve55182Components).extracting(AffectedCoordinates::name).containsExactly("spring-core");
    assertThat(cve55182Components).extracting(AffectedCoordinates::format).containsExactly("maven");
    assertThat(cve55182Components).extracting(AffectedCoordinates::namespace)
        .containsExactly("org.springframework");

    Set<AffectedCoordinates> cve12345Components = result.get("CVE-2025-12345");
    assertThat(cve12345Components).hasSize(2);
    assertThat(cve12345Components).extracting(AffectedCoordinates::name)
        .containsExactlyInAnyOrder("spring-core", "lodash");
    assertThat(cve12345Components).extracting(AffectedCoordinates::format)
        .containsExactlyInAnyOrder("maven", "npm");
  }

  @Test
  public void testFetchMultipleCves_PartialFailure_SkipsInvalidCves() {
    Set<String> cveIds = Set.of("CVE-2025-55182", "CVE-2025-INVALID", "CVE-2025-12345");

    when(hdsClient.getWithMultimap(
        eq(AffectedComponentList.class), eq("/rest/vulnerability/affected"), any()))
            .thenAnswer(invocation -> {
              Multimap<String, String> params = invocation.getArgument(2);

              if (params.get("refId").size() > 1) {
                throw new BadRequestException("Only one RefId is allowed");
              }

              String cveId = params.get("refId").iterator().next();

              if (cveId.equals("CVE-2025-55182")) {
                return new AffectedComponentList(Collections.singletonList(
                    new AffectedComponentDTO(
                        "maven", "org.springframework", "spring-core", "5.3.30", null)),
                    null, null);
              }
              else if (cveId.equals("CVE-2025-INVALID")) {
                throw new BadRequestException(
                    "Invalid CVE ID. Currently only supporting: CVE-2025-55182");
              }
              else if (cveId.equals("CVE-2025-12345")) {
                return new AffectedComponentList(Collections.singletonList(
                    new AffectedComponentDTO("npm", "", "lodash", "4.17.19", null)), null, null);
              }

              return new AffectedComponentList(Collections.emptyList(), null, null);
            });

    Map<String, Set<AffectedCoordinates>> result = service.fetchAffectedComponentsForMultipleCves(cveIds);

    assertThat(result).hasSize(2);
    assertThat(result).containsKeys("CVE-2025-55182", "CVE-2025-12345");
    assertThat(result).doesNotContainKey("CVE-2025-INVALID");

    Set<AffectedCoordinates> cve55182Components = result.get("CVE-2025-55182");
    assertThat(cve55182Components).hasSize(1);
    assertThat(cve55182Components).extracting(AffectedCoordinates::name).containsExactly("spring-core");
    assertThat(cve55182Components).extracting(AffectedCoordinates::format).containsExactly("maven");

    Set<AffectedCoordinates> cve12345Components = result.get("CVE-2025-12345");
    assertThat(cve12345Components).hasSize(1);
    assertThat(cve12345Components).extracting(AffectedCoordinates::name).containsExactly("lodash");
    assertThat(cve12345Components).extracting(AffectedCoordinates::format).containsExactly("npm");
  }
}
