/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.annotation.Nullable;

/**
 * DTO for a top-consuming application entry.
 *
 * @since 1.204
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsumptionTopAppDTO
{
  private String appId;

  private String publicId;

  private String name;

  private long consumed;

  public ConsumptionTopAppDTO() {
  }

  public ConsumptionTopAppDTO(String appId, @Nullable String publicId, @Nullable String name, long consumed) {
    this.appId = appId;
    this.publicId = publicId;
    this.name = name;
    this.consumed = consumed;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  @Nullable
  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(@Nullable String publicId) {
    this.publicId = publicId;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public long getConsumed() {
    return consumed;
  }

  public void setConsumed(long consumed) {
    this.consumed = consumed;
  }
}
