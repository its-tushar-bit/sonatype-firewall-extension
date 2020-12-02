/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.tag.ApplicableTagsDTO;
import com.sonatype.insight.brain.tag.AppliedTagsDTO;

/**
 * Resource for API Application Categories
 */
public interface ApiApplicationCategoryResource
{
  List<ApiApplicationCategoryDTO> getTagsUsedByApplications();

  ApplicableTagsDTO getApplicationApplicableTags(String applicationPublicId);

  List<ApiApplicationCategoryDTO> getTags(String organizationId);

  ApplicableTagsDTO getApplicableTags(String organizationId);

  List<ApiApplicationCategoryDTO> getApplicableTagsByApplicationPublicId(String applicationPublicId);

  AppliedTagsDTO getAppliedTags(String organizationId);

  List<PolicyTag> getAppliedPolicyTags(String organizationId);

  ApiApplicationCategoryDTO addTag(String organizationId, ApiApplicationCategoryDTO tag);

  ApiApplicationCategoryDTO updateTag(String organizationId, ApiApplicationCategoryDTO tag);

  void deleteTag(String organizationId, String tagId);
}
