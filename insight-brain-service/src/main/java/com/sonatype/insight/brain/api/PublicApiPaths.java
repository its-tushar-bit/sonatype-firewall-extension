/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

/**
 * @since 1.11.0
 */
public class PublicApiPaths
{
  private static final String API_VERSION_PATH = "api/v1/";

  public static final String APP_SERVICE_PATH = API_VERSION_PATH + "applications";

  public static final String ORG_SERVICE_PATH = API_VERSION_PATH + "organizations";

  public static final String ROLE_MEMBER_SERVICE_PATH =
      API_VERSION_PATH + "roleMembers/{ownerType: global|application|organization}/{ownerId}";

}
