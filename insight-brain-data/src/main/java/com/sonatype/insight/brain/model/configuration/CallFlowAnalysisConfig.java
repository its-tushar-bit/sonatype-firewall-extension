/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.sonatype.clm.dto.model.callflowanalysis.CallFlowAlgorithm;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.172
 */
@Entity
@Table(name = "call_flow_analysis_config")
public class CallFlowAnalysisConfig
    implements HasStringId
{
  @Id
  @Column(name = "call_flow_analysis_config_id")
  private String id;

  @Column(name = "enabled")
  private boolean enabled;

  @Column(name = "namespaces_json")
  private String namespacesJson;

  @Transient
  private List<String> namespaces;

  @Column(name = "algorithm")
  @Enumerated(EnumType.STRING)
  private CallFlowAlgorithm algorithm;

  @Column(name = "thread_count")
  private Integer threadCount;

  @Column(name = "owner_id")
  private String ownerId;

  public CallFlowAnalysisConfig() {
  }

  public CallFlowAnalysisConfig(
      boolean enabled,
      List<String> namespaces,
      CallFlowAlgorithm algorithm,
      Integer threadCount)
  {
    this.enabled = enabled;
    setNamespaces(namespaces);
    this.algorithm = algorithm;
    this.threadCount = threadCount;
  }

  public CallFlowAnalysisConfig(
      boolean enabled,
      List<String> namespaces,
      CallFlowAlgorithm algorithm,
      Integer threadCount,
      String ownerId)
  {
    this.enabled = enabled;
    setNamespaces(namespaces);
    this.algorithm = algorithm;
    this.threadCount = threadCount;
    this.ownerId = ownerId;
  }

  @Override
  public String getId() {
    return id;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getNamespacesJson() {
    return namespacesJson;
  }

  @SuppressWarnings("unchecked")
  public List<String> getNamespaces() {
    if (namespaces == null && namespacesJson != null) {
      try {
        namespaces = JsonUtils.parse(namespacesJson, List.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read call flow analysis namespaces configuration ", e);
      }
    }
    if (namespaces == null) {
      namespaces = new ArrayList<>();
    }
    return namespaces;
  }

  public void setNamespaces(List<String> namespaces) {
    if (namespaces == null || namespaces.isEmpty()) {
      this.namespaces = new ArrayList<>();
      namespacesJson = null;
      return;
    }

    this.namespaces = namespaces;
    namespacesJson = JsonUtils.writeUnformatted(namespaces);
  }

  public CallFlowAlgorithm getAlgorithm() {
    return algorithm;
  }

  public Integer getThreadCount() {
    return threadCount;
  }

  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public void setNamespaces(final String namespacesJson) {
    this.namespacesJson = namespacesJson;
  }

  public void setAlgorithm(final CallFlowAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  public void setThreadCount(final Integer threadCount) {
    this.threadCount = threadCount;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }
}
