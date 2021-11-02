/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

/**
 * @since 1.20.0
 */
public class OwnerListDTO
{
  public List<SidebarOrganizationDTO> organizations;

  public static class SidebarOrganizationDTO
  {
    public String id;

    public String name;

    public boolean synthetic;

    public List<SidebarApplicationDTO> applications;
  }

  public static class SidebarApplicationDTO
  {
    public String id;

    public String publicId;

    public String organizationId;

    public String name;

    public String provider;

    public String repositoryUrl;
  }
}
