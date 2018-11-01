/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import com.sonatype.insight.brain.model.tag.Tag;

import static java.util.stream.Collectors.toList;

public class TagDTO
{
  public String applicationCategoryId;

  public String applicationCategoryName;

  public TagDTO(Tag tag) {
    this.applicationCategoryId = tag.getId();
    this.applicationCategoryName = tag.getName();
  }

  public static List<TagDTO> transcribe(List<Tag> tags) {
    return tags.stream().map(TagDTO::new).collect(toList());
  }
}
