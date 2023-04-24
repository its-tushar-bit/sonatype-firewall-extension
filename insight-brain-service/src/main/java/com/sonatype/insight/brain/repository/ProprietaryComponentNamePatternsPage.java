/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDTO;

/**
 * @since 1.152
 */
public class ProprietaryComponentNamePatternsPage
{
  public List<ProprietaryComponentNamePatternDTO> proprietaryComponentNamePatterns = new ArrayList<>();

  public boolean hasNextPage;
}
