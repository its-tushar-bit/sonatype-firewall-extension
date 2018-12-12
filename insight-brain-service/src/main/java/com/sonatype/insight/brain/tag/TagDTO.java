/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.model.tag.Tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import static java.util.stream.Collectors.toList;

@JsonInclude(Include.NON_NULL)
public class TagDTO
{
  public String applicationCategoryId;

  public String applicationCategoryName;

  public TagDTO() {
    //for jackson
  }

  public TagDTO(Tag tag) {
    this.applicationCategoryId = tag.getId();
    this.applicationCategoryName = tag.getName();
  }

  public TagDTO(String id, String name) {
    applicationCategoryId = id;
    applicationCategoryName = name;
  }

  public static List<TagDTO> transcribe(List<Tag> tags) {
    return tags.stream().map(TagDTO::new).collect(toList());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TagDTO that = (TagDTO) o;
    return Objects.equals(applicationCategoryId, that.applicationCategoryId)
        && Objects.equals(applicationCategoryName, that.applicationCategoryName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(applicationCategoryId, applicationCategoryName);
  }

  @Override
  public String toString() {
    return "TagDTO [applicationCategoryId=" + applicationCategoryId + ", applicationCategoryName="
        + applicationCategoryName + "]";
  }
}
