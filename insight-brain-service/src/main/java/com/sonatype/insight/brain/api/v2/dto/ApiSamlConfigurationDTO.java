/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * @since 1.72
 */
public class ApiSamlConfigurationDTO
{
  public String entityId;

  public String firstNameAttributeName;

  public String lastNameAttributeName;

  public String emailAttributeName;

  public String usernameAttributeName;

  public String groupsAttributeName;

  public Boolean validateResponseSignature;

  public Boolean validateAssertionSignature;
}
