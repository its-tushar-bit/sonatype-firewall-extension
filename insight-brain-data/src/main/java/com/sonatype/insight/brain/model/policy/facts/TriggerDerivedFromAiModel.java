/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import com.sonatype.clm.dto.model.DerivedFromAiModel;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * Holds data about the AI model this component (AI model) was derived from.
 * Instances of this class are serialized in JSON format in policy violations in the database and
 * they are compared in policy violation comparison.
 * Any change to this class structure or to its JSON serialization may break policy violation comparison.
 */
public class TriggerDerivedFromAiModel
{
  public ComponentIdentifier componentIdentifier;

  public double similarityScore;

  public TriggerDerivedFromAiModel() {
  }

  public TriggerDerivedFromAiModel(DerivedFromAiModel derivedFromAiModel) {
    if (derivedFromAiModel != null) {
      componentIdentifier = derivedFromAiModel.getComponentIdentifier();
      similarityScore = derivedFromAiModel.getSimilarityScore();
    }
  }

  @Override
  public String toString() {
    return "TriggerDerivedFromAiModel [componentIdentifier=" + componentIdentifier + ", similarityScore="
        + similarityScore + "]";
  }
}
