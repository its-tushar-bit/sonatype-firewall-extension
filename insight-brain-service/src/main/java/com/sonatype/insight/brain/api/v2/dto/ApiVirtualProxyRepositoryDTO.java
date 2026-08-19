/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.model.repository.ProtocolVersion;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.VirtualRepositoryConfig;

/**
 * Lean DTO for a proxy repository owned by a Virtual Repository Manager. Carries only the fields
 * an owner-admin may set — policy flags (audit / quarantine / namespace-confusion) are server-owned
 * and never leave through this shape.
 *
 * <p>
 * {@code upstreamUrl}, {@code protocolVersion} and {@code packageHostUrl} live on the
 * {@code virtual_repository_config} satellite table (see {@link VirtualRepositoryConfig}).
 * {@link #fromRepository} joins them onto the DTO and {@link #toVirtualRepositoryConfig} builds
 * the satellite entity for persistence. {@code protocolVersion} is a lowercase wire form
 * ({@code "v2"} / {@code "v3"}) that maps to {@link ProtocolVersion} on the satellite.
 *
 * @since 1.194.0
 */
public class ApiVirtualProxyRepositoryDTO
{
  public String repositoryId;

  public String publicId;

  public String format;

  public String upstreamUrl;

  public Boolean pccsEnabled;

  public String protocolVersion;

  public String packageHostUrl;

  public String proxyUrl;

  public static ApiVirtualProxyRepositoryDTO fromRepository(
      Repository repository,
      VirtualRepositoryConfig config,
      String proxyUrl)
  {
    ApiVirtualProxyRepositoryDTO dto = new ApiVirtualProxyRepositoryDTO();
    dto.repositoryId = repository.getId();
    dto.publicId = repository.getPublicId();
    dto.format = repository.getFormat();
    dto.pccsEnabled = repository.isPolicyCompliantComponentSelectionEnabled();
    if (config != null) {
      dto.upstreamUrl = config.getUpstreamUrl();
      dto.protocolVersion = config.getProtocolVersion() != null
          ? config.getProtocolVersion().name().toLowerCase()
          : null;
      dto.packageHostUrl = config.getPackageHostUrl();
    }
    dto.proxyUrl = proxyUrl;
    return dto;
  }

  public static Repository toRepository(ApiVirtualProxyRepositoryDTO dto) {
    Repository repository = new Repository();
    repository.setId(dto.repositoryId);
    repository.setPublicId(dto.publicId);
    repository.setFormat(dto.format);
    repository.setRepositoryType(RepositoryType.proxy);
    if (dto.pccsEnabled != null) {
      repository.setPolicyCompliantComponentSelectionEnabled(dto.pccsEnabled);
    }
    return repository;
  }

  /**
   * Builds the satellite config row for the given repository ID from this DTO. Every VRM-owned
   * proxy repository gets a satellite row — {@code upstreamUrl} is always present, and format-
   * specific fields ({@code protocolVersion} for NuGet, {@code packageHostUrl} for PyPI) are
   * carried on the same row.
   */
  public static VirtualRepositoryConfig toVirtualRepositoryConfig(
      ApiVirtualProxyRepositoryDTO dto,
      String repositoryId)
  {
    VirtualRepositoryConfig config = new VirtualRepositoryConfig(repositoryId);
    config.setUpstreamUrl(dto.upstreamUrl);
    if (dto.protocolVersion != null) {
      config.setProtocolVersion(ProtocolVersion.valueOf(dto.protocolVersion.toUpperCase()));
    }
    config.setPackageHostUrl(dto.packageHostUrl);
    return config;
  }
}
