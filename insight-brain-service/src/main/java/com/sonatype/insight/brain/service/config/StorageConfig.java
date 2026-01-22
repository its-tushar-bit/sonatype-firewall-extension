/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.service.config;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import jakarta.annotation.Nullable;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;

import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

public class StorageConfig
{
  @NotNull
  private StorageConfig.DataStoreType type = DataStoreType.FILE;

  private S3DataStoreConfig s3Config;

  private HybridDataStoreConfig hybridConfig;

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

  public HybridDataStoreConfig getHybridConfig() {
    return hybridConfig;
  }

  public void setHybridConfig(final HybridDataStoreConfig hybridConfig) {
    this.hybridConfig = hybridConfig;
  }

  public void validate() {
    validate(getType());
  }

  public void validate(final DataStoreType dataStoreType) {
    switch (dataStoreType) {
      case S3 -> {
        S3DataStoreConfig s3Config = getS3Config();
        if (s3Config == null) {
          throw new ValidationException("s3Config is required when the data store type is S3.");
        }
        s3Config.validate();
      }
      case HYBRID -> {
        HybridDataStoreConfig hybridDataStoreConfig = getHybridConfig();
        if (hybridDataStoreConfig == null) {
          throw new ValidationException("hybridConfig is required when the data store type is hybrid.");
        }
        hybridDataStoreConfig.validate(this::validate);
      }
      default -> {
        // no-op
      }
    }
  }

  public enum DataStoreType
  {
    FILE,
    S3,
    HYBRID
  }

  public static class HybridDataStoreConfig
  {
    private LinkedHashSet<DataStoreType> types;

    public LinkedHashSet<DataStoreType> getTypes() {
      return types;
    }

    public void setTypes(final LinkedHashSet<DataStoreType> types) {
      this.types = types;
    }
 
    public void validate(final Consumer<DataStoreType> dataStoreTypeValidator) {
      if (types == null || types.size() < 2) {
        throw new ValidationException("Property 'types' must be provided and at least have 2 elements.");
      }
      if (types.contains(DataStoreType.HYBRID)) {
        throw new ValidationException("Property 'types' cannot contain 'HYBRID'.");
      }
      for (DataStoreType dataStoreType : types) {
        dataStoreTypeValidator.accept(dataStoreType);
      }
    }
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

    @Nullable
    private String serverSideEncryption;

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
      this.objectKeyPrefix = objectKeyPrefix;
    }

    public String getServerSideEncryption() {
      return serverSideEncryption;
    }

    public void setServerSideEncryption(@Nullable final String serverSideEncryption) {
      this.serverSideEncryption = serverSideEncryption;
    }

    public void validate() {
      if (bucketName == null || bucketName.isEmpty()) {
        throw new ValidationException("Property 'bucketName' must be provided and non-empty.");
      }
      if (region == null || region.isEmpty()) {
        throw new ValidationException("Property 'region' must be provided and non-empty.");
      }
      if (objectKeyPrefix != null && !objectKeyPrefix.matches(S3_KEY_PREFIX)) {
        throw new ValidationException(
            "Property 'objectKeyPrefix' does not match the expected regex pattern " + S3_KEY_PREFIX);
      }
      if (serverSideEncryption != null &&
          ServerSideEncryption.UNKNOWN_TO_SDK_VERSION == ServerSideEncryption.fromValue(serverSideEncryption)) {
        throw new ValidationException(("Property 'serverSideEncryption' with value '%s' " +
            "does not correspond to a known server side encryption algorithm.").formatted(serverSideEncryption));
      }
    }
  }
}
