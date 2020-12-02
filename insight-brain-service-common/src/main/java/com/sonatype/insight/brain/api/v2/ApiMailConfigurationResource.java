/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;

/**
 * Resource for API Mail Configuration
 */
public interface ApiMailConfigurationResource
{
  ApiMailConfigurationDTO getConfiguration();

  void setConfiguration(ApiMailConfigurationDTO configurationDTO);

  void deleteConfiguration();

  void testConfiguration(String recipientEmail, ApiMailConfigurationDTO configurationDTO);
}
