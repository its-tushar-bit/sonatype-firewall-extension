/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as PropTypes from 'prop-types';

import {
  NEXT_NO_VIOLATIONS,
  NEXT_NO_VIOLATIONS_DEPENDENCIES,
  NEXT_NON_FAILING,
  NEXT_NON_FAILING_DEPENDENCIES,
  RECOMMENDED_NON_BREAKING,
  RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
} from './riskRemediation/recommendedVersionsUtils';

export const CoordinatesPropTypes = PropTypes.shape({
  name: PropTypes.string,
  qualifier: PropTypes.string,
  artifactId: PropTypes.string,
  classifier: PropTypes.string,
  extension: PropTypes.string,
  groupId: PropTypes.string,
  version: PropTypes.string,
});

export const ComponentIdentifierPropTypes = PropTypes.shape({
  format: PropTypes.string.isRequired,
  coordinates: CoordinatesPropTypes.isRequired,
});

export const ComponentPropTypes = PropTypes.shape({
  displayName: PropTypes.string.isRequired,
  hash: PropTypes.any,
  packageUrl: PropTypes.string.isRequired,
  componentIdentifier: ComponentIdentifierPropTypes.isRequired,
});

export const VersionChangePropTypes = PropTypes.shape({
  id: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
  type: PropTypes.oneOf([
    NEXT_NO_VIOLATIONS,
    NEXT_NO_VIOLATIONS_DEPENDENCIES,
    NEXT_NON_FAILING,
    NEXT_NON_FAILING_DEPENDENCIES,
    RECOMMENDED_NON_BREAKING,
    RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
  ]),
  version: PropTypes.string,
  linkId: PropTypes.string,
  linkText: PropTypes.string,
  isGolden: PropTypes.bool,
});

export const AncestorPropTypes = PropTypes.shape({
  hash: PropTypes.string.isRequired,
  derivedComponentName: PropTypes.string.isRequired,
  componentIdentifier: ComponentIdentifierPropTypes.isRequired,
  innerSource: PropTypes.bool.isRequired,
});

export const RemediationPropTypes = PropTypes.shape({
  versionChanges: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.string.isRequired,
      data: PropTypes.shape({
        component: ComponentPropTypes.isRequired,
      }).isRequired,
    }).isRequired
  ),
});

export const componentInformationPropType = PropTypes.shape({
  componentIdentifier: PropTypes.shape({
    format: PropTypes.string,
  }),
  displayName: PropTypes.shape({
    parts: PropTypes.array,
  }).isRequired,
  matchState: PropTypes.string.isRequired,
  identificationSource: PropTypes.string,
  componentCategories: PropTypes.arrayOf(
    PropTypes.shape({
      path: PropTypes.string.isRequired,
    })
  ),
  pathnames: PropTypes.arrayOf(PropTypes.string).isRequired,
  policyThreatLevel: PropTypes.number,
  website: PropTypes.string,
  directDependency: PropTypes.bool,
});
