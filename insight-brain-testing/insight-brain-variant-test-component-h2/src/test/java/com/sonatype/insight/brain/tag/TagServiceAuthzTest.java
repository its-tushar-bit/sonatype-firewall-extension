/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class TagServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private TagService tagService;

  String policyId;

  @BeforeEach
  public void init() {
    policyId = tempEntity.newPolicy(org).getId();
  }

  @Test
  public void testGetApplicableTags_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> tagService.getApplicableTags(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicableTags_Authorized() {
    grantReadPermission(org.getId());
    tagService.getApplicableTags(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testAddTag_Unauthorized() {
    grantReadPermission(org.getId());
    Tag tag = new Tag(org.getId(), "name", "description", Color.yellow);
    assertThrows(UnauthorizedException.class, () -> tagService.addTag(org.getId(), TagService.toDTO(tag)));
  }

  @Test
  public void testAddTag_Authorized() {
    grantWritePermission(org.getId());
    Tag tag = new Tag(org.getId(), "name", "description", Color.yellow);
    tagService.addTag(org.getId(), TagService.toDTO(tag));
  }

  @Test
  public void testUpdateTag_Unauthorized() {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    assertThrows(UnauthorizedException.class, () -> tagService.updateTag(org.getId(), TagService.toDTO(tag)));
  }

  @Test
  public void testUpdateTag_Authorized() {
    grantWritePermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.updateTag(org.getId(), TagService.toDTO(tag));
  }

  @Test
  public void testDeleteTag_Unauthorized() {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    assertThrows(UnauthorizedException.class, () -> tagService.deleteTag(org.getId(), tag.getId()));
  }

  @Test
  public void testDeleteTag_Authorized() {
    grantWritePermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.deleteTag(org.getId(), tag.getId());
  }

  @Test
  public void testGetAppliedApplicationTags_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> tagService.getAppliedApplicationTags(app.getPublicId()));
  }

  @Test
  public void testGetAppliedApplicationTags_Authorized() {
    grantReadPermission(app.getId());
    tagService.getAppliedApplicationTags(app.getPublicId());
  }

  @Test
  public void testGetPolicyTags_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> tagService.getPolicyTags(OwnerType.ORGANIZATION, org.getId(), policyId));
  }

  @Test
  public void testGetPolicyTags_Authorized() {
    grantReadPermission(org.getId());
    tagService.getPolicyTags(OwnerType.ORGANIZATION, org.getId(), policyId);
  }

  @Test
  public void testUpdatePolicyTags_Authorized() {
    grantWritePermission(org.getId());
    tagService.updatePolicyTags(OwnerType.ORGANIZATION, org.getId(), policyId, new ArrayList<>());
  }

  @Test
  public void testUpdatePolicyTags_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> tagService.updatePolicyTags(OwnerType.ORGANIZATION, org.getId(), policyId, new ArrayList<>()));
  }

  @Test
  public void testUpdatePolicyTags_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> tagService.updatePolicyTags(OwnerType.ORGANIZATION, org.getId(), policyId, new ArrayList<>()));
  }

  @Test
  public void testGetAppliedTags_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> tagService.getAppliedTags(org.getId()));
  }

  @Test
  public void testGetAppliedTags_Authorized() {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newApplicationTag(app.getId(), tag.getId());
    tagService.getAppliedTags(org.getId());
  }

  @Test
  public void testGetAppliedPolicyTags_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> tagService.getAppliedPolicyTags(org.getId()));
  }

  @Test
  public void testGetAppliedPolicyTags_Authorize() {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newPolicyTag(policyId, tag.getId());
    tagService.getAppliedPolicyTags(org.getId());
  }

  @Test
  public void testGetApplicableTagsByApplicationPublicId_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> tagService.getApplicableTagsByApplicationPublicId(app.getPublicId()));
  }

  @Test
  public void testGetApplicableTagsByApplicationPublicId_Authorized() {
    grantReadPermission(app.getId());
    tagService.getApplicableTagsByApplicationPublicId(app.getPublicId());
  }

  @Test
  public void testGetAllTagsWithReadPermission() {
    Organization organization1 = tempEntity.newOrganization("testGetAllTagsOrg1");
    Application application1 = tempEntity.newApplication(organization1.getId());
    Tag tag1 = tempEntity.newTag(organization1.getId());
    tempEntity.newApplicationTag(application1.getId(), tag1.getId());
    grantReadPermission(application1.getId());

    Organization organization2 = tempEntity.newOrganization("testGetAllTagsOrg2");
    Application application2 = tempEntity.newApplication(organization2.getId());
    Tag tag2 = tempEntity.newTag(organization2.getId());
    tempEntity.newApplicationTag(application2.getId(), tag2.getId());

    List<Tag> allTags = tagService.getTagsUsedByApplications()
        .stream()
        .map(dto -> TagService.fromDTO(dto, dto.organizationId))
        .collect(Collectors.toList());
    assertThat(allTags).extracting(Tag::getId).containsExactly(tag1.getId());
  }

  @Test
  public void testUpdateApplicationUpdateTags_Authorized() {
    grantWritePermission(app.getId());
    tagService.updateApplicationTags(app.getPublicId(), Collections.emptyList());
  }

  @Test
  public void testUpdateApplicationUpdateTags_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> tagService.updateApplicationTags(app.getPublicId(), Collections.emptyList()));
  }

  @Test
  public void testUpdateApplicationUpdateTags_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> tagService.updateApplicationTags(app.getPublicId(), Collections.emptyList()));
  }

  @Test
  public void testGetTags_Authorized() {
    grantReadPermission(org.getId());
    tagService.getTags(org.getId());
  }

  @Test
  public void testGetTags_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> tagService.getTags(org.getId()));
  }

  @Test
  public void testGetTags_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> tagService.getTags(org.getId()));
  }
}
