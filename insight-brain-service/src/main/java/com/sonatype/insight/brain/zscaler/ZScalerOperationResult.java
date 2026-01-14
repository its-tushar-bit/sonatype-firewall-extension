/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.Optional;

/**
 * Result of a ZScaler API operation, containing success status, HTTP status code, and any error details.
 * This allows callers to decide whether to throw exceptions (strict validation) or log and continue (lenient updates).
 */
public class ZScalerOperationResult<T>
{
  private final boolean success;

  private final int statusCode;

  private final String message;

  private final T data;

  private ZScalerOperationResult(boolean success, int statusCode, String message, T data) {
    this.success = success;
    this.statusCode = statusCode;
    this.message = message;
    this.data = data;
  }

  public static <T> ZScalerOperationResult<T> success(int statusCode, T data) {
    return new ZScalerOperationResult<>(true, statusCode, null, data);
  }

  public static <T> ZScalerOperationResult<T> success(int statusCode) {
    return new ZScalerOperationResult<>(true, statusCode, null, null);
  }

  public static <T> ZScalerOperationResult<T> failure(int statusCode, String message) {
    return new ZScalerOperationResult<>(false, statusCode, message, null);
  }

  public static <T> ZScalerOperationResult<T> failure(String message) {
    return new ZScalerOperationResult<>(false, 0, message, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getMessage() {
    return message;
  }

  public Optional<T> getData() {
    return Optional.ofNullable(data);
  }

  public boolean isForbidden() {
    return statusCode == 403;
  }

  public boolean isBadRequest() {
    return statusCode == 400;
  }

  public boolean isQuotaError() {
    if (message == null) {
      return false;
    }
    String lowerMessage = message.toLowerCase();
    return lowerMessage.contains("quota") || lowerMessage.contains("limit exceeded");
  }
}
