/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.AffectedComponentList;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeveloperPrioritiesPayloadExtractorTest
{
  @Test
  public void extract_returnsEmpty_forNullPayload() {
    assertThat(DeveloperPrioritiesPayloadExtractor.extract(null)).isEmpty();
  }

  @Test
  public void extract_returnsEmpty_forPayloadWithNoComponents() {
    AffectedComponentList payload = new AffectedComponentList(new ArrayList<>(), null, null);
    assertThat(DeveloperPrioritiesPayloadExtractor.extract(payload)).isEmpty();
  }

  @Test
  public void extract_returnsOneEntityPer_cveAndCoordinatePair() {
    AffectedComponentDTO component1 = new AffectedComponentDTO(
        "maven", "com.foo", "bar", "1.0", List.of("CVE-2024-1"));
    AffectedComponentDTO component2 = new AffectedComponentDTO(
        "maven", "com.foo", "baz", "2.0", List.of("CVE-2024-1"));
    AffectedComponentList payload = new AffectedComponentList(
        List.of(component1, component2), null, null);

    List<String> entities = DeveloperPrioritiesPayloadExtractor.extract(payload);

    assertThat(entities).containsExactlyInAnyOrder(
        "CVE-2024-1|maven:com.foo:bar:1.0",
        "CVE-2024-1|maven:com.foo:baz:2.0");
  }

  @Test
  public void extract_emitsOneEntityPerRefId_whenComponentHasMultipleRefIds() {
    AffectedComponentDTO component = new AffectedComponentDTO(
        "maven", "com.foo", "bar", "1.0",
        List.of("CVE-2024-1", "CVE-2024-2"));
    AffectedComponentList payload = new AffectedComponentList(
        List.of(component), null, null);

    List<String> entities = DeveloperPrioritiesPayloadExtractor.extract(payload);

    assertThat(entities).containsExactlyInAnyOrder(
        "CVE-2024-1|maven:com.foo:bar:1.0",
        "CVE-2024-2|maven:com.foo:bar:1.0");
  }

  @Test
  public void extract_dedupsRepeatedPairsWithinSamePayload() {
    AffectedComponentDTO component = new AffectedComponentDTO(
        "maven", "com.foo", "bar", "1.0", List.of("CVE-2024-1"));
    // Same component appears twice in the response (degenerate but possible)
    AffectedComponentList payload = new AffectedComponentList(
        List.of(component, component), null, null);

    assertThat(DeveloperPrioritiesPayloadExtractor.extract(payload))
        .containsExactly("CVE-2024-1|maven:com.foo:bar:1.0");
  }

  @Test
  public void extract_skipsComponentsWithNullRefIds() {
    AffectedComponentDTO componentWithRefs = new AffectedComponentDTO(
        "maven", "com.foo", "bar", "1.0", List.of("CVE-2024-1"));
    AffectedComponentDTO componentNoRefs = new AffectedComponentDTO(
        "maven", "com.foo", "baz", "2.0", null);
    AffectedComponentList payload = new AffectedComponentList(
        List.of(componentWithRefs, componentNoRefs), null, null);

    assertThat(DeveloperPrioritiesPayloadExtractor.extract(payload))
        .containsExactly("CVE-2024-1|maven:com.foo:bar:1.0");
  }

  @Test
  public void extract_skipsNullComponentsInList() {
    AffectedComponentDTO valid = new AffectedComponentDTO(
        "maven", "com.foo", "bar", "1.0", List.of("CVE-2024-1"));
    // Arrays.asList allows null elements; List.of does not.
    AffectedComponentList payload = new AffectedComponentList(
        Arrays.asList(valid, null), null, null);

    assertThat(DeveloperPrioritiesPayloadExtractor.extract(payload))
        .containsExactly("CVE-2024-1|maven:com.foo:bar:1.0");
  }

  @Test
  public void extract_handlesNullNamespace() {
    AffectedComponentDTO component = new AffectedComponentDTO(
        "npm", null, "express", "4.18.0", List.of("CVE-2024-1"));
    AffectedComponentList payload = new AffectedComponentList(
        List.of(component), null, null);

    // npm packages have no namespace; AffectedComponentDTO normalizes empty/null
    // to null, and formatCoordinates renders null as an empty middle segment:
    // "npm::express:4.18.0".
    assertThat(DeveloperPrioritiesPayloadExtractor.extract(payload))
        .containsExactly("CVE-2024-1|npm::express:4.18.0");
  }
}
