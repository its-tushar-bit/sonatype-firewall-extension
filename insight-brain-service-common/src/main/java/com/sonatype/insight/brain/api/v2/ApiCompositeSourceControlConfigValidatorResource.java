/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.git.ConfigurationValidationResult;

/**
 * Resource for API Composite Source Control Config Validator
 */
public interface ApiCompositeSourceControlConfigValidatorResource
{
  ConfigurationValidationResult validateSourceControlConfig(String applicationId);
}
