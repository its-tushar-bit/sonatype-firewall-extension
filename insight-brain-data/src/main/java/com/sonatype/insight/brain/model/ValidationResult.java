/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult
{
  private List<String> errors = new ArrayList<>();

  /**
   * An instance that can be used to indicate that the ValidationResult has no errors, instead of returning null values.
   *
   * Note that errors can be added to this in subsequent calls.
   */
  public static ValidationResult noErrors() {
    return new ValidationResult();
  }

  /**
   * Creates an instance that starts as being valid.
   */
  public ValidationResult() {
  }

  public ValidationResult(String error) {
    errors.add(error);
  }

  public ValidationResult(Exception error) {
    errors.add(error.getMessage());
  }

  public String toMessageString() {
    return toMessageString(errors);
  }

  private static String toMessageString(List<String> errors) {
    if (errors == null || errors.isEmpty()) {
      return null;
    }

    StringBuilder result = new StringBuilder();
    for (String error : errors) {
      result.append(error).append('\n');
    }

    return result.toString().substring(0, result.length() - 1);
  }

  public List<String> getErrors() {
    return errors;
  }

  public boolean isValid() {
    return errors == null || errors.isEmpty();
  }

  public void merge(ValidationResult other) {
    if (other == null || other.isValid()) {
      return;
    }

    if (errors == null) {
      errors = new ArrayList<>();
    }
    errors.addAll(other.getErrors());
  }

  public void addError(String error) {
    errors.add(error);
  }
}
