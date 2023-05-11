/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;

public interface ApiScmOnboardingResource
{
  ImportResults importRepositories(String orgId, ImportScmOrganizationRequest importRequest) throws IOException;
}
