/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.service.config;

import java.net.URI;
import javax.annotation.Nullable;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

public class StorageConfig
{
  @NotNull
  private StorageConfig.DataStoreType type = DataStoreType.FILE;

  private S3DataStoreConfig s3Config;

  public DataStoreType getType() {
    return type;
  }

  public void setType(final DataStoreType type) {
    this.type = type;
  }

  public S3DataStoreConfig getS3Config() {
    return s3Config;
  }

  public void setS3Config(final S3DataStoreConfig s3Config) {
    this.s3Config = s3Config;
  }

  public enum DataStoreType
  {
    FILE, S3
  }

  public static class S3DataStoreConfig
  {
    public static final String S3_KEY_PREFIX = "^(|[a-zA-Z0-9!_.*'()/-]+)$";

    private String bucketName;

    private String region;

    @Nullable
    private URI endpoint;

    @Nullable
    private String objectKeyPrefix;

    public String getBucketName() {
      return bucketName;
    }

    public void setBucketName(final String bucketName) {
      this.bucketName = bucketName;
    }

    public String getRegion() {
      return region;
    }

    public void setRegion(final String region) {
      this.region = region;
    }

    @Nullable
    public URI getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(@Nullable final URI endpoint) {
      this.endpoint = endpoint;
    }

    @NotNull
    public String getObjectKeyPrefix() {
      if (objectKeyPrefix == null || objectKeyPrefix.isEmpty()) {
        return "";
      }
      if (objectKeyPrefix.endsWith("/")) {
        return objectKeyPrefix;
      }
      return objectKeyPrefix + "/";
    }

    public void setObjectKeyPrefix(@Nullable final String objectKeyPrefix) {
      if (objectKeyPrefix != null && !objectKeyPrefix.matches(S3_KEY_PREFIX)) {
        throw new ValidationException(
            "Property 'objectKeyPrefix' does not match the expected regex pattern " + S3_KEY_PREFIX);
      }
      this.objectKeyPrefix = objectKeyPrefix;
    }
  }
}
