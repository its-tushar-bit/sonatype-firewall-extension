/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * User token configuration response.
 * <p>
 * When adding properties: use same name as SystemConfigurationProperty constant
 */
public record ApiUserTokenConfigurationDTO(Integer userTokenDefaultExpirationDays) {}
