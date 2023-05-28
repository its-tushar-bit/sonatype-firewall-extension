/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationStatus;

public interface ApiScmOnboardingResource
{
  Response importRepositories(String orgId, ImportScmOrganizationRequest importRequest);

  ImportScmOrganizationStatus getImportRepositoriesStatus(String organizationId, String importEventId);
}
