package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

/**
 * @since 1.7
 */
public interface PolicyImporter
{
  /**
   * Import policy into an Application, removing any existing policy data first.
   * Application Labels will be merged if they match(case-insensitive by name) existing data.
   *
   * @param application app to import policy to
   * @param exportDTO update existing IDs from this export to match newly persisted data(as a side effect)
   * @return result embedding the url of the application
   */
  public PolicyImportResult importApplication(Application application, PolicyExportResult exportDTO);

  /**
   * Import policy into an Organization, removing any existing policy data first.
   * This includes deletion of data from child Applications.
   * Organization Labels will be merged if they match(case-insensitive by name) existing data.
   *
   * @param organization org to import policy to
   * @param exportDTO update existing IDs from this export to match newly persisted data(as a side effect)
   * @return result embedding the url of the organization
   */
  public PolicyImportResult importOrganization(Organization organization, PolicyExportResult exportDTO);

}
