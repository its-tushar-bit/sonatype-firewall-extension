/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Register payload sent to {@code POST /api/register} when registering by JSON. PAT registration
 * may also send the raw license file bytes with {@code Content-Type: application/octet-stream},
 * in which case this DTO is unused. GitHub App registration always uses this DTO with a
 * non-empty {@code installationIds} list.
 */
@JsonInclude(Include.NON_NULL)
public class RelayRegisterRequest
{
  private String license;

  private List<String> installationIds;

  public RelayRegisterRequest() {
  }

  public RelayRegisterRequest(String license, List<String> installationIds) {
    this.license = license;
    this.installationIds = installationIds;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public List<String> getInstallationIds() {
    return installationIds;
  }

  public void setInstallationIds(List<String> installationIds) {
    this.installationIds = installationIds;
  }
}
