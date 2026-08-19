/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.validation;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MtiqSourceControlSshValidatorTest
{
  private final MtiqSourceControlSshValidator mtiqSourceControlSshValidator = new MtiqSourceControlSshValidator();

  @Test
  public void testMultiTenantHasSshEnabledFails() {
    SourceControl sourceControl = new SourceControl.Builder().setSshEnabled(true).build();
    assertThatThrownBy(() -> mtiqSourceControlSshValidator.validate(sourceControl)).isInstanceOf(
        BadRequestException.class).hasMessageContaining("SSH not supported");
  }

  @Test
  public void testMultiTenantHasNullSshAllowed() {
    SourceControl sourceControl = new SourceControl.Builder().setSshEnabled(null).build();
    mtiqSourceControlSshValidator.validate(sourceControl); // no error is success
  }

  @Test
  public void testMultiTenantHasSshDisabledAllowed() {
    SourceControl sourceControl = new SourceControl.Builder().setSshEnabled(false).build();
    mtiqSourceControlSshValidator.validate(sourceControl); // no error is success
  }
}
