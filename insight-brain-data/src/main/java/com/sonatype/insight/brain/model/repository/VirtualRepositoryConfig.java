/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Per-repository configuration attached to a repository owned by a Virtual Repository Manager
 * (a manager whose {@code manager_type = 'VIRTUAL'}). Holds ecosystem discriminators that only
 * apply to VRM-owned proxy repositories — a protocol/version discriminator (initial writer:
 * NuGet v2/v3), the PyPI package-host URL pair — plus the proxy {@code upstreamUrl}. Kept in a
 * satellite table so the shared {@code repository} table stays free of columns that are NULL
 * for every non-VRM row. Follows the sibling per-repository-type convention already established
 * by {@code proxy_repository_component} and {@code hosted_repository_component}.
 *
 * <p>
 * {@code packageHostUrl} and {@code upstreamUrl} are user-supplied URLs. Both go through the
 * structural checks in {@code VirtualRepositoryConfigDAO.validateUrl} at the persistence
 * boundary (http/https scheme, non-empty host, no embedded credentials, within the varchar
 * column length). Outbound-boundary SSRF enforcement — address classification and DNS-rebinding
 * protection — is scoped to FIRE-664; callers issuing outbound requests must run through that
 * guard before dialling out.
 *
 * <p>
 * The {@code manager_type = 'VIRTUAL'} invariant is enforced at the DAO write path
 * ({@code VirtualRepositoryConfigDAO#requireVirtualOwner}): every {@code insert}/{@code update}
 * — including the batch variants — joins {@code repository} to {@code repository_manager} and
 * rejects rows whose owning manager is not {@code VIRTUAL}. Readers rely on this: a row
 * returned by {@code getByRepositoryId} is guaranteed to belong to a virtual-owned repository.
 */
@Entity
@Table(name = "virtual_repository_config")
public class VirtualRepositoryConfig
    implements HasStringId
{
  @Id
  @Column(name = "virtual_repository_config_id")
  private String id;

  @Column(name = "repository_id")
  private String repositoryId;

  @Column(name = "protocol_version")
  @Enumerated(EnumType.STRING)
  private ProtocolVersion protocolVersion;

  @Column(name = "pypi_package_host_url")
  private String packageHostUrl;

  @Column(name = "upstream_url")
  private String upstreamUrl;

  public VirtualRepositoryConfig() {
  }

  public VirtualRepositoryConfig(final String repositoryId) {
    this.repositoryId = repositoryId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(final String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(final ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public String getPackageHostUrl() {
    return packageHostUrl;
  }

  public void setPackageHostUrl(final String packageHostUrl) {
    this.packageHostUrl = packageHostUrl;
  }

  public String getUpstreamUrl() {
    return upstreamUrl;
  }

  public void setUpstreamUrl(final String upstreamUrl) {
    this.upstreamUrl = upstreamUrl;
  }
}
