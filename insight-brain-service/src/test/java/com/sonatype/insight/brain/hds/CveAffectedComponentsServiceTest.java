/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CveAffectedComponentsServiceTest
{
  @Mock
  private HdsClient hdsClient;

  private CveAffectedComponentsService service;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new CveAffectedComponentsService(hdsClient);
  }

  @Test
  public void testGetAffectedComponents_Success() {
    // Arrange
    String cveId = "CVE-2025-55182";
    AffectedComponentDTO component1 = new AffectedComponentDTO(
        "maven",
        "org.apache.commons",
        "commons-text",
        "1.9"
    );
    AffectedComponentDTO component2 = new AffectedComponentDTO(
        "maven",
        "org.apache.commons",
        "commons-text",
        "1.10"
    );
    AffectedComponentList expectedList = new AffectedComponentList(Arrays.asList(component1, component2));

    when(hdsClient.get(eq(AffectedComponentList.class), eq("/rest/vulnerability/affected/{cveId}"),
        anyMap(), eq(cveId))).thenReturn(expectedList);

    // Act
    List<AffectedComponentDTO> result = service.getAffectedComponents(cveId);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("commons-text");
    assertThat(result.get(1).getVersion()).isEqualTo("1.10");
    verify(hdsClient).get(eq(AffectedComponentList.class), eq("/rest/vulnerability/affected/{cveId}"),
        anyMap(), eq(cveId));
  }

  @Test
  public void testGetAffectedComponents_EmptyResult() {
    // Arrange
    String cveId = "CVE-9999-99999";
    AffectedComponentList emptyList = new AffectedComponentList(Collections.emptyList());

    when(hdsClient.get(eq(AffectedComponentList.class), eq("/rest/vulnerability/affected/{cveId}"),
        anyMap(), eq(cveId))).thenReturn(emptyList);

    // Act
    List<AffectedComponentDTO> result = service.getAffectedComponents(cveId);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAffectedComponents_NotFound() {
    // Arrange
    String cveId = "CVE-2025-55182";

    when(hdsClient.get(eq(AffectedComponentList.class), eq("/rest/vulnerability/affected/{cveId}"),
        anyMap(), eq(cveId))).thenThrow(new NotFoundException("CVE not found"));

    // Act
    List<AffectedComponentDTO> result = service.getAffectedComponents(cveId);

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAffectedComponents_CaseInsensitive() {
    // Arrange
    String lowercaseCveId = "cve-2025-55182";
    String normalizedCveId = "CVE-2025-55182";
    AffectedComponentDTO component = new AffectedComponentDTO(
        "maven",
        "org.apache.commons",
        "commons-text",
        "1.9"
    );
    AffectedComponentList expectedList = new AffectedComponentList(Collections.singletonList(component));

    when(hdsClient.get(eq(AffectedComponentList.class), eq("/rest/vulnerability/affected/{cveId}"),
        anyMap(), eq(normalizedCveId))).thenReturn(expectedList);

    // Act
    List<AffectedComponentDTO> result = service.getAffectedComponents(lowercaseCveId);

    // Assert
    assertThat(result).hasSize(1);
    verify(hdsClient).get(eq(AffectedComponentList.class), eq("/rest/vulnerability/affected/{cveId}"),
        anyMap(), eq(normalizedCveId));
  }

  @Test
  public void testGetAffectedComponents_WithQueryParams() {
    // Arrange
    String cveId = "CVE-2025-55182";
    Map<String, String> queryParams = Map.of("format", "maven");
    AffectedComponentDTO component = new AffectedComponentDTO(
        "maven",
        "org.apache.commons",
        "commons-text",
        "1.9"
    );
    AffectedComponentList expectedList = new AffectedComponentList(Collections.singletonList(component));

    when(hdsClient.get(eq(AffectedComponentList.class), eq("/rest/vulnerability/affected/{cveId}"),
        eq(queryParams), eq(cveId))).thenReturn(expectedList);

    // Act
    List<AffectedComponentDTO> result = service.getAffectedComponents(cveId, queryParams);

    // Assert
    assertThat(result).hasSize(1);
    verify(hdsClient).get(eq(AffectedComponentList.class), eq("/rest/vulnerability/affected/{cveId}"),
        eq(queryParams), eq(cveId));
  }
}
