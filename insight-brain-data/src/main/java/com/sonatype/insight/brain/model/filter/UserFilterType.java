/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.filter;

/**
 * @since 1.105
 */
public enum UserFilterType
{
  ADVANCED_LEGAL_PACK_DASHBOARD
  {
    public String accept(UserFilterVisitor visitor, String json) {
      return visitor.filterAdvancedLegalPack(json);
    }
  };

  UserFilterType() {}

  /**
   * Visitor pattern for the UserFilter.
   *
   * @param userFilterVisitor the visitor
   * @param json              the json of the {@link UserFilter}'s filter property.
   * @return the modified json string.
   */
  public abstract String accept(UserFilterVisitor userFilterVisitor, String json);

  public interface UserFilterVisitor
  {
    String filterAdvancedLegalPack(String json);
  }
}
