/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

abstract class AbstractRepositoryResource
{
  protected static final String REPOSITORY_PATH = "{repositoryManagerInstanceId}/{repositoryPublicId}/";

  static final String SUMMARY_PATH = REPOSITORY_PATH + "summary";

  static final String QUARANTINE_PATH = REPOSITORY_PATH + "quarantine/{enabled}";

  public static final String ENABLE_PATH = REPOSITORY_PATH + "enable/{enabled}";

  static final String EVALUATE_COMPONENTS_PATH = REPOSITORY_PATH + "evaluate/audit";

  static final String COMPONENTS_PATH = REPOSITORY_PATH + "components/{pathname: .+}";

  static final String EVALUATE_COMPONENTS_WITH_QUARANTINE_PATH = REPOSITORY_PATH + "evaluate/quarantine";

  static final String UNQUARANTINED_COMPONENTS_PATH = REPOSITORY_PATH + "components/unquarantined";

  static final String PROPRIETARY_NAMES_PATH = REPOSITORY_PATH + "proprietary/names";

  static final String QUARANTINED_COMPONENT_REPORT_URL_PATH =
      REPOSITORY_PATH + "components/{pathname: .+}/quarantinedComponentReportUrl";

  static final String EVALUATE_COMPONENT_METADATA = REPOSITORY_PATH + "evaluate/componentMetadata";
}
