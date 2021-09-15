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
  ]),
  version: PropTypes.string,
  linkId: PropTypes.string,
  linkText: PropTypes.string,
});

export const AncestorPropTypes = PropTypes.shape({
  hash: PropTypes.string.isRequired,
  derivedComponentName: PropTypes.string.isRequired,
  componentIdentifier: ComponentIdentifierPropTypes.isRequired,
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
