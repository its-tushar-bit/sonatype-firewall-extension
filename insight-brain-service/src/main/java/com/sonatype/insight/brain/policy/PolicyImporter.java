/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

/**
 * @since 1.7
 */
public interface PolicyImporter
{
  /**
   * Import policy into an Application.
   * Application Labels will be merged if they match(case-insensitive by name) existing data; this preserves any
   * related ComponentLabels.
   * License Threat Groups and associated Licenses are all deleted as part of the import.
   *
   * @param application app to import policy to
   * @param exportDTO data to import
   * @return result embedding the url of the application
   */
  public PolicyImportResult importApplication(Application application, PolicyExportResult exportDTO);

  /**
   * Import policy into an Organization.
   * This includes deletion of data from child Applications(Labels, License Threat Groups and associated Licenses).
   * Organization Labels will be merged if they match(case-insensitive by name) existing data; this preserves any
   * related ComponentLabels.
   * License Threat Groups and associated Licenses are all deleted as part of the import.
   *
   * @param organization org to import policy to
   * @param exportDTO data to import
   * @return result embedding the url of the organization
   */
  public PolicyImportResult importOrganization(Organization organization, PolicyExportResult exportDTO);

}
