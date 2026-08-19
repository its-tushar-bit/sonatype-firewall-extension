/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

/**
 * Holds data about the AI model content type that triggered the condition.
 * Instances of this class are serialized in JSON format in policy violations in the database and
 * they are compared in policy violation comparison.
 * Any change to this class structure or to its JSON serialization may break policy violation comparison.
 */
public class TriggerAiModelContentType
{
  public String contentType;

  public TriggerAiModelContentType() {
  }

  public TriggerAiModelContentType(String contentType) {
    this.contentType = contentType;
  }

  @Override
  public String toString() {
    return "TriggerAiModelContentType [contentType=" + contentType + "]";
  }
}
