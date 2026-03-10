/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class ApiVerifyOrCreateApplicationForContainerImageFirewallDTO
{
  private String repositoryManagerInstanceId;

  private String repositoryPublicId;

  private String baseUrl;

  private String containerImageNamespace;

  private String containerImageName;

  private String containerImageVersion;

  private String clientUserAgent;

  private Boolean quarantineEnabled;

  public ApiVerifyOrCreateApplicationForContainerImageFirewallDTO() {
    // Default constructor
  }

  public ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      String baseUrl,
      String containerImageNamespace,
      String containerImageName,
      String containerImageVersion)
  {
    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
    this.repositoryPublicId = repositoryPublicId;
    this.baseUrl = baseUrl;
    this.containerImageNamespace = containerImageNamespace;
    this.containerImageName = containerImageName;
    this.containerImageVersion = containerImageVersion;
  }

  public ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String baseUrl,
      final String containerImageNamespace,
      final String containerImageName,
      final String containerImageVersion,
      final String clientUserAgent)
  {
    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
    this.repositoryPublicId = repositoryPublicId;
    this.baseUrl = baseUrl;
    this.containerImageNamespace = containerImageNamespace;
    this.containerImageName = containerImageName;
    this.containerImageVersion = containerImageVersion;
    this.clientUserAgent = clientUserAgent;
  }

  public String getRepositoryManagerInstanceId() {
    return repositoryManagerInstanceId;
  }

  public String getRepositoryPublicId() {
    return repositoryPublicId;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getContainerImageNamespace() {
    return containerImageNamespace;
  }

  public String getContainerImageName() {
    return containerImageName;
  }

  public String getContainerImageVersion() {
    return containerImageVersion;
  }

  public void setRepositoryManagerInstanceId(final String repositoryManagerInstanceId) {
    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
  }

  public void setRepositoryPublicId(final String repositoryPublicId) {
    this.repositoryPublicId = repositoryPublicId;
  }

  public void setBaseUrl(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public void setContainerImageNamespace(final String containerImageNamespace) {
    this.containerImageNamespace = containerImageNamespace;
  }

  public void setContainerImageName(final String containerImageName) {
    this.containerImageName = containerImageName;
  }

  public void setContainerImageVersion(final String containerImageVersion) {
    this.containerImageVersion = containerImageVersion;
  }

  public String getClientUserAgent() {
    return clientUserAgent;
  }

  public void setClientUserAgent(final String clientUserAgent) {
    this.clientUserAgent = clientUserAgent;
  }

  public Boolean getQuarantineEnabled() {
    return quarantineEnabled;
  }

  public void setQuarantineEnabled(final Boolean quarantineEnabled) {
    this.quarantineEnabled = quarantineEnabled;
  }
}
