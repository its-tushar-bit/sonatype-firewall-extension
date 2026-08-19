/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;

import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ProxyRepositoryComponentTelemetry}, specifically focusing on the Builder pattern.
 */
public class ProxyRepositoryComponentTelemetryTest
{
  @Test
  public void testBuilder_MinimalConstruction() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryId("repo-1")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryManagerId()).isEqualTo("repo-manager-1");
    assertThat(telemetry.getRepositoryId()).isEqualTo("repo-1");
    assertThat(telemetry.getEventType()).isEqualTo("quarantine");
  }

  @Test
  public void testBuilder_FullConstruction() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .accountId("account-123")
        .repositoryManagerId("repo-manager-1")
        .repositoryId("repo-1")
        .componentFormat("maven2")
        .componentHash("abc123")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .quarantineTime(1234567890L)
        .releaseQuarantineTime(1234567900L)
        .releaseQuarantineType(ReleaseQuarantineType.MANUAL)
        .releaseReason("waived")
        .componentIdentifier("{\"format\":\"maven2\"}")
        .componentName("example-lib")
        .componentNamespace("com.example")
        .componentVersion("1.0.0")
        .build();

    assertThat(telemetry.getAccountId()).isEqualTo("account-123");
    assertThat(telemetry.getRepositoryManagerId()).isEqualTo("repo-manager-1");
    assertThat(telemetry.getRepositoryId()).isEqualTo("repo-1");
    assertThat(telemetry.getComponentFormat()).isEqualTo("maven2");
    assertThat(telemetry.getComponentHash()).isNotNull();
    assertThat(telemetry.getEventType()).isEqualTo("quarantine");
    assertThat(telemetry.getQuarantineTime()).isEqualTo(1234567890L);
    assertThat(telemetry.getReleaseQuarantineTime()).isEqualTo(1234567900L);
    assertThat(telemetry.getReleaseQuarantineType()).isEqualTo("manual");
    assertThat(telemetry.getReleaseReason()).isEqualTo("waived");
    assertThat(telemetry.getComponentIdentifier()).isEqualTo("{\"format\":\"maven2\"}");
    assertThat(telemetry.getComponentName()).isEqualTo("example-lib");
    assertThat(telemetry.getComponentNamespace()).isEqualTo("com.example");
    assertThat(telemetry.getComponentVersion()).isEqualTo("1.0.0");
  }

  @Test
  public void testBuilder_EventTypeWithEnum() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .eventType(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE)
        .build();

    assertThat(telemetry.getEventType()).isEqualTo("release_quarantine");
  }

  @Test
  public void testBuilder_EventTypeWithString() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .eventType("custom_event")
        .build();

    assertThat(telemetry.getEventType()).isEqualTo("custom_event");
  }

  @Test
  public void testBuilder_ReleaseQuarantineTypeWithEnum() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .releaseQuarantineType(ReleaseQuarantineType.AUTO)
        .build();

    assertThat(telemetry.getReleaseQuarantineType()).isEqualTo("auto");
  }

  @Test
  public void testBuilder_ReleaseQuarantineTypeWithString() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .releaseQuarantineType("custom_type")
        .build();

    assertThat(telemetry.getReleaseQuarantineType()).isEqualTo("custom_type");
  }

  @Test
  public void testBuilder_FromRepositoryComponentWithNull() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .fromRepositoryComponent(null)
        .repositoryManagerId("repo-manager-1")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryManagerId()).isEqualTo("repo-manager-1");
  }

  @Test
  public void testBuilder_WithNullPolicyNotifications() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .policyNotifications(null)
        .build();

    assertThat(telemetry.getNotifications()).isEmpty();
  }

  @Test
  public void testBuilder_ComponentHashObfuscation() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .componentHash("plaintext-hash-12345")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getComponentHash()).isNotNull();
    assertThat(telemetry.getComponentHash()).isNotEqualTo("plaintext-hash-12345");
  }

  @Test
  public void testBuilder_WithNullComponentHash() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .componentHash(null)
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getComponentHash()).isNull();
  }

  @Test
  public void testBuilder_ChainedMethodCalls() {
    ProxyRepositoryComponentTelemetry.Builder builder = ProxyRepositoryComponentTelemetry.builder();

    ProxyRepositoryComponentTelemetry.Builder result = builder
        .accountId("account-1")
        .repositoryManagerId("repo-manager-1")
        .repositoryId("repo-1");

    assertThat(result).isSameAs(builder);
  }

  @Test
  public void testBuilder_MultipleBuildsFromSameBuilder() {
    ProxyRepositoryComponentTelemetry.Builder builder = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE);

    ProxyRepositoryComponentTelemetry telemetry1 = builder.build();
    ProxyRepositoryComponentTelemetry telemetry2 = builder.build();

    assertThat(telemetry1).isNotSameAs(telemetry2);
    assertThat(telemetry1.getRepositoryManagerId()).isEqualTo(telemetry2.getRepositoryManagerId());
  }

  @Test
  public void testEnumValues_RepositoryComponentTelemetryEventType() {
    assertThat(RepositoryComponentTelemetryEventType.AUDIT.getDescription()).isEqualTo("audit");
    assertThat(RepositoryComponentTelemetryEventType.QUARANTINE.getDescription()).isEqualTo("quarantine");
    assertThat(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE.getDescription())
        .isEqualTo("release_quarantine");
    assertThat(RepositoryComponentTelemetryEventType.DELETE.getDescription()).isEqualTo("delete");
  }

  @Test
  public void testEnumValues_ReleaseQuarantineType() {
    assertThat(ReleaseQuarantineType.AUTO.getDescription()).isEqualTo("auto");
    assertThat(ReleaseQuarantineType.MANUAL.getDescription()).isEqualTo("manual");
  }

  @Test
  public void testBuilder_WithNullEnumValues() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .eventType((RepositoryComponentTelemetryEventType) null)
        .releaseQuarantineType((ReleaseQuarantineType) null)
        .build();

    assertThat(telemetry.getEventType()).isNull();
    assertThat(telemetry.getReleaseQuarantineType()).isNull();
  }

  @Test
  public void testBuilder_OverwritingValues() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryManagerId("repo-manager-2")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryManagerId()).isEqualTo("repo-manager-2");
  }

  @Test
  public void testBuilder_EmptyLists() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .policyNotifications(Collections.emptyList())
        .build();

    assertThat(telemetry.getNotifications()).isEmpty();
  }

  @Test
  public void testBuilder_RepositoryNameObfuscation_WhenAdvancedReportingDisabled() {
    Configuration mockConfiguration = mock(Configuration.class);
    when(mockConfiguration.getAdvanceReportingInsightsEnabled()).thenReturn(false);
    TelemetryDataObfuscator obfuscator = new TelemetryDataObfuscator(mockConfiguration);

    String originalRepoName = "my-sensitive-repo-name";
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryName(originalRepoName)
        .telemetryDataObfuscator(obfuscator)
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryName()).isNotNull();
    assertThat(telemetry.getRepositoryName()).isNotEqualTo(originalRepoName);
  }

  @Test
  public void testBuilder_RepositoryNameNotObfuscated_WhenAdvancedReportingEnabled() {
    Configuration mockConfiguration = mock(Configuration.class);
    when(mockConfiguration.getAdvanceReportingInsightsEnabled()).thenReturn(true);
    TelemetryDataObfuscator obfuscator = new TelemetryDataObfuscator(mockConfiguration);

    String originalRepoName = "my-repo-name";
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryName(originalRepoName)
        .telemetryDataObfuscator(obfuscator)
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryName()).isEqualTo(originalRepoName);
  }

  @Test
  public void testBuilder_RepositoryNameNotObfuscated_WhenNoObfuscatorProvided() {
    String originalRepoName = "my-repo-name";
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryName(originalRepoName)
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryName()).isEqualTo(originalRepoName);
  }

  @Test
  public void testBuilder_NullRepositoryName_WithObfuscator() {
    Configuration mockConfiguration = mock(Configuration.class);
    when(mockConfiguration.getAdvanceReportingInsightsEnabled()).thenReturn(false);
    TelemetryDataObfuscator obfuscator = new TelemetryDataObfuscator(mockConfiguration);

    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryName(null)
        .telemetryDataObfuscator(obfuscator)
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryName()).isNull();
  }

  @Test
  public void testBuilder_WithRepositoryType() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryId("repo-1")
        .repositoryType("proxy")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryType()).isEqualTo("proxy");
  }

  @Test
  public void testBuilder_WithNullRepositoryType() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryId("repo-1")
        .repositoryType(null)
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryType()).isNull();
  }

  @Test
  public void testBuilder_WithRepositoryTypeProxy() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryType("proxy")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryType()).isEqualTo("proxy");
  }

  @Test
  public void testBuilder_WithRepositoryTypeHosted() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryType("hosted")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryType()).isEqualTo("hosted");
  }

  @Test
  public void testBuilder_RepositoryTypeNotSetDefaultsToNull() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getRepositoryType()).isNull();
  }

  @Test
  public void testBuilder_WithAllFieldsIncludingRepositoryType() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .accountId("account-123")
        .repositoryManagerId("repo-manager-1")
        .repositoryId("repo-1")
        .repositoryName("my-repo")
        .repositoryType("proxy")
        .componentFormat("maven2")
        .componentHash("abc123")
        .eventType(RepositoryComponentTelemetryEventType.QUARANTINE)
        .build();

    assertThat(telemetry.getAccountId()).isEqualTo("account-123");
    assertThat(telemetry.getRepositoryManagerId()).isEqualTo("repo-manager-1");
    assertThat(telemetry.getRepositoryId()).isEqualTo("repo-1");
    assertThat(telemetry.getRepositoryName()).isEqualTo("my-repo");
    assertThat(telemetry.getRepositoryType()).isEqualTo("proxy");
    assertThat(telemetry.getComponentFormat()).isEqualTo("maven2");
    assertThat(telemetry.getEventType()).isEqualTo("quarantine");
  }

  @Test
  public void testRepositoryComponentConstructor_NullHash_DoesNotThrow() {
    ProxyRepositoryComponent proxyRepositoryComponent = mock(ProxyRepositoryComponent.class);
    when(proxyRepositoryComponent.getRepositoryId()).thenReturn("repo-1");
    when(proxyRepositoryComponent.getHash()).thenReturn(null);

    ProxyRepositoryComponentTelemetry telemetry = new ProxyRepositoryComponentTelemetry(
        "account-1", "repo-manager-1", proxyRepositoryComponent,
        RepositoryComponentTelemetryEventType.QUARANTINE, null, null, Collections.emptyList());

    assertThat(telemetry.getComponentHash()).isNull();
  }

  @Test
  public void testGetRepositoryType_ReturnsCorrectValue() {
    ProxyRepositoryComponentTelemetry telemetry = ProxyRepositoryComponentTelemetry.builder()
        .repositoryManagerId("repo-manager-1")
        .repositoryType("hosted")
        .eventType(RepositoryComponentTelemetryEventType.AUDIT)
        .build();

    String repositoryType = telemetry.getRepositoryType();
    assertThat(repositoryType).isNotNull();
    assertThat(repositoryType).isEqualTo("hosted");
  }
}
