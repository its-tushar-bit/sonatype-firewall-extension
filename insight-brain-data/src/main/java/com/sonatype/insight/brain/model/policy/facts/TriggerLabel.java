/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

/**
 * Holds data about a label that triggered a policy condition.
 * Instances of this class are serialized in JSON format in policy violations in the database and
 * they are compared in policy violation comparison.
 * Any change to this class structure or to its JSON serialization may break policy violation comparison.
 *
 * @since 1.50
 */
public class TriggerLabel
{
  public String id;

  public TriggerLabel() {
  }

  public TriggerLabel(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "TriggerLabel [id=" + id + "]";
  }
}
