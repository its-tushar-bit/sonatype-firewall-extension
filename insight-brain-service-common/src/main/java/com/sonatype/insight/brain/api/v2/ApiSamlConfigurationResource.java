/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationResponseDTO;

/**
 * Resource for API Saml Configuration
 */
public interface ApiSamlConfigurationResource
{
  ApiSamlConfigurationResponseDTO getSamlConfiguration();

  void insertOrUpdateSamlConfiguration(String identityProviderXml, ApiSamlConfigurationDTO samlConfiguration);

  void deleteSamlConfiguration();

  String getMetadata();
}
