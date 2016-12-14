/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.util.List;

import com.sonatype.insight.brain.webhook.EventAction;

/**
 * @since 1.25.0
 */
public class PolicyManagementPayload
    extends WebhookPayload
{
  public EventAction action;

  public PolicyManagementType type;

  public String id;

  public OwnerDTO owner;

  public static class OwnerDTO
  {
    public String id;

    public String publicId;

    public String type;

    public String name;

    public String parentOwnerId;

    public List<ApplicationCategoryDTO> applicationCategories;

    public List<LabelDTO> labels;

    public List<LicenseThreatGroupDTO> licenseThreatGroups;

    public List<PolicyDTO> policies;

    public List<RoleDTO> roles;

    public static class ApplicationCategoryDTO
    {
      public String id;

      public String name;

      public String description;

      public String color;
    }

    public static class LabelDTO
    {
      public String id;

      public String name;

      public String description;

      public String color;
    }

    public static class LicenseThreatGroupDTO
    {
      public String id;

      public String name;

      public int threatLevel;
    }

    public static class PolicyDTO
    {
      public String id;

      public String name;

      public int threatLevel;
    }

    public static class RoleDTO
    {
      public String id;

      public String name;

      public List<MemberDTO> members;

      public static class MemberDTO
      {
        public String type;

        public String name;
      }
    }
  }
}
