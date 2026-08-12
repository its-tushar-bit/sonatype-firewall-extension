/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Ecosystem-neutral protocol/version discriminator for a VRM-owned repository. The initial
 * writer is NuGet v2 vs v3; the column stays generic so other formats can reuse it, with
 * per-format validation constraining the legal values at the write boundary. Persisted as
 * {@code varchar(50)} on {@code virtual_repository_config.protocol_version} via
 * {@code @Enumerated(EnumType.STRING)}, serialized on the wire as lowercase (mirrors
 * {@link ManagerType}).
 */
public enum ProtocolVersion
{
  V2,
  V3;

  @Override
  @JsonValue
  public String toString() {
    return name().toLowerCase(Locale.ENGLISH);
  }

  public static ProtocolVersion fromString(String name) {
    if (name == null) {
      return null;
    }
    return valueOf(name.toUpperCase(Locale.ENGLISH));
  }
}
