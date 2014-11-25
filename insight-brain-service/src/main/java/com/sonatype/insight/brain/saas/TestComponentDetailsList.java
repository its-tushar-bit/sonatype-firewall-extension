/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class TestComponentDetailsList
    extends ComponentDetailsList
{
  @Override
  @JsonDeserialize(contentAs = TestComponentDetails.class)
  public void setList(List<ComponentDetails> list) {
    super.setList(list);
  }
}
