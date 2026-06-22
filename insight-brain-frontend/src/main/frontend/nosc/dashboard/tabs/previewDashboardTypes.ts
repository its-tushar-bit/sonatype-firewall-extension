/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/** Shared display-name DTO from dashboard policy risk endpoints. */
export interface DisplayNameDTO {
  parts?: ReadonlyArray<{ value?: string; field?: string }>;
  name?: string;
}

export interface PreviewDashboardComponentIdentifier {
  coordinates?: {
    artifactId?: string;
    packageId?: string;
    version?: string;
    name?: string;
  };
  format?: string;
}

/** Fields consumed by {@link getComponentName} for dashboard component rows. */
export interface ComponentNameSource {
  derivedComponentName?: string;
  displayName?: DisplayNameDTO;
  filename?: string;
  componentIdentifier?: PreviewDashboardComponentIdentifier;
}
