/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ComponentIdentifierValidatorTest
{
  @Test
  public void testValidateNull() {
    try {
      ComponentIdentifierValidator.validate(null);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("The component identifier cannot be null."));
    }
  }

  @Test
  public void testValidateInvalid() throws Exception {
    ComponentIdentifier componentIdentifier = JsonUtils.parse("{}", ComponentIdentifier.class);
    try {
      ComponentIdentifierValidator.validate(componentIdentifier);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertThat(expected.getCause(), instanceOf(InvalidComponentIdentifierException.class));
      assertThat(expected.getMessage(), is(expected.getCause().getMessage()));
    }
  }
}
