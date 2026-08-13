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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentIdentifierValidatorTest
{
  @Test
  public void testValidateNull() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> ComponentIdentifierValidator.validate(null))
        .withMessage("The component identifier cannot be null.");
  }

  @Test
  public void testValidateInvalid() throws Exception {
    ComponentIdentifier componentIdentifier = JsonUtils.parse("{}", ComponentIdentifier.class);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> ComponentIdentifierValidator.validate(componentIdentifier))
        .withCauseInstanceOf(InvalidComponentIdentifierException.class)
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(e.getCause().getMessage()));
  }
}
