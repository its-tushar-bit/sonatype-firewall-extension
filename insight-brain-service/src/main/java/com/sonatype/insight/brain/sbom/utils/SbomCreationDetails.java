/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.util.Arrays;
import java.util.List;
import jakarta.ws.rs.BadRequestException;

public class SbomCreationDetails
{
  public String type;

  public String created;

  public List<Creator> creators;

  public List<Tool> tools;

  public enum CreatorType
  {
    Author,
    Manufacturer,
    Supplier,
    Person,
    Organization;

    public static CreatorType parseCreatorType(String type) {
      return Arrays.stream(CreatorType.values())
          .filter(creator -> creator.name().equalsIgnoreCase(type))
          .findFirst()
          .orElseThrow(() -> new BadRequestException("Invalid creator type: " + type));
    }
  }

  public static class Creator
  {
    public String type;

    public String name;

    public String email;

    public String phone;

    public String url;
  }

  public static class Tool
  {
    public String type;

    public String name;

    public String version;
  }
}
