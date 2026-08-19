/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.util.Objects;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;

public class InnerSourceProducerComponentTelemetry
{
  private String producerAppId;

  private String format;

  private String scanType;

  private String scanClient;

  private String manifestContentType;

  public InnerSourceProducerComponentTelemetry() {

  }

  public InnerSourceProducerComponentTelemetry(
      final String producerAppId,
      final String format,
      final String scanType,
      final String scanClient,
      final String manifestContentType)
  {
    this.producerAppId = HdsClientAnalytics.obfuscate(producerAppId);
    this.format = format;
    this.scanType = scanType;
    this.scanClient = scanClient;
    this.manifestContentType = manifestContentType;
  }

  public void setProducerAppId(final String producerAppId) {
    this.producerAppId = HdsClientAnalytics.obfuscate(producerAppId);
  }

  public String getProducerAppId() {
    return producerAppId;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public String getScanType() {
    return scanType;
  }

  public void setScanType(final String scanType) {
    this.scanType = scanType;
  }

  public String getScanClient() {
    return scanClient;
  }

  public void setScanClient(final String scanClient) {
    this.scanClient = scanClient;
  }

  public String getManifestContentType() {
    return manifestContentType;
  }

  public void setManifestContentType(final String manifestContentType) {
    this.manifestContentType = manifestContentType;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InnerSourceProducerComponentTelemetry)) {
      return false;
    }
    InnerSourceProducerComponentTelemetry that = (InnerSourceProducerComponentTelemetry) o;
    return Objects.equals(getProducerAppId(), that.getProducerAppId()) &&
        Objects.equals(getFormat(), that.getFormat()) &&
        Objects.equals(getScanType(), that.getScanType()) &&
        Objects.equals(getScanClient(), that.getScanClient()) &&
        Objects.equals(getManifestContentType(), that.getManifestContentType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getProducerAppId(), getFormat(), getScanType(), getScanClient(), getManifestContentType());
  }

  @Override
  public String toString() {
    return "InnerSourceProducerComponentTelemetry{" +
        "producerAppId='" + producerAppId + '\'' +
        ", format='" + format + '\'' +
        ", scanType='" + scanType + '\'' +
        ", scanClient='" + scanClient + '\'' +
        ", manifestContentType='" + manifestContentType + '\'' +
        '}';
  }
}
