/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Normalizes a componentIdentifier so that maven coordinates include the
 * classifier field. The licenseOverrides API requires an exact match on
 * the serialized componentIdentifier, and the ALP legal pages always
 * include classifier (even if empty) while the SBOM API omits it.
 *
 * Note: the Java-side ComponentLoader.normalizeComponentIdentifier strips
 * empty coordinate values instead of adding them. The backend's multi-candidate
 * lookup in LicenseOverrideInternalDAO.getCandidateCoordinateJsons reconciles
 * both forms, so these two functions are intentionally asymmetric.
 */
export const normalizeComponentIdentifier = (componentIdentifier) => {
  if (!componentIdentifier || componentIdentifier.format !== 'maven') {
    return componentIdentifier;
  }
  const { coordinates } = componentIdentifier;
  if (!coordinates || 'classifier' in coordinates) {
    return componentIdentifier;
  }
  return {
    ...componentIdentifier,
    coordinates: {
      ...coordinates,
      classifier: '',
    },
  };
};

