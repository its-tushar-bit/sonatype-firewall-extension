/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as PropTypes from 'prop-types';
import { waiverType } from '../util/waiverUtils';

export const scopePropType = PropTypes.shape({
  id: PropTypes.string.isRequired,
  publicId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
});

export const availableScopesPropType = PropTypes.shape({
  loading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  data: PropTypes.arrayOf(scopePropType.isRequired),
});

export const reportMetadataPropType = PropTypes.shape({
  loading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  data: PropTypes.shape({
    reportTime: PropTypes.number.isRequired,
    reportTitle: PropTypes.string.isRequired,
    stageId: PropTypes.string.isRequired,
  }),
});

export const componentIdentifierPropType = PropTypes.shape({
  format: PropTypes.string.isRequired,
  coordinates: PropTypes.object.isRequired,
});

export const policyViolationPropType = PropTypes.shape({
  policyId: PropTypes.string.isRequired,
  policyName: PropTypes.string.isRequired,
  threatLevel: PropTypes.number.isRequired,
  threatCategory: PropTypes.string.isRequired,
  policyViolationId: PropTypes.string.isRequired,
  action: PropTypes.string,
  componentIdentifier: componentIdentifierPropType,
  packageUrl: PropTypes.string,
  hash: PropTypes.string.isRequired,
  displayName: PropTypes.string.isRequired,
});

export const sortConfigurationPropType = PropTypes.shape({
  key: PropTypes.string.isRequired,
  dir: PropTypes.string.isRequired,
});

export const filterConfigurationPropType = PropTypes.shape({
  policyName: PropTypes.string.isRequired,
  displayName: PropTypes.string.isRequired,
});

export const threatCountsPropType = PropTypes.shape({
  critical: PropTypes.number.isRequired,
  severe: PropTypes.number.isRequired,
  moderate: PropTypes.number.isRequired,
  low: PropTypes.number.isRequired,
  none: PropTypes.number.isRequired,
});

export const componentTransitivePolicyViolationsPropType = PropTypes.shape({
  loading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  sortConfiguration: sortConfigurationPropType.isRequired,
  filterConfiguration: filterConfigurationPropType.isRequired,
  data: PropTypes.shape({
    componentIdentifier: componentIdentifierPropType,
    packageUrl: PropTypes.string,
    hash: PropTypes.string.isRequired,
    displayName: PropTypes.string,
    isInnerSource: PropTypes.bool.isRequired,
    violations: PropTypes.arrayOf(policyViolationPropType.isRequired).isRequired,
    displayedViolations: PropTypes.arrayOf(policyViolationPropType.isRequired).isRequired,
  }),
  threatCounts: threatCountsPropType,
  threatCountsTotal: PropTypes.number,
  componentCount: PropTypes.number,
});

export const transitiveViolationWaiversPropType = PropTypes.shape({
  loading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  data: PropTypes.shape({
    componentPolicyWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType).isRequired).isRequired,
  }).isRequired,
});
