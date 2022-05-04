/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTooltip } from '@sonatype/react-shared-components';

const DEPENDENCY_TYPE_MAP = {
  direct: { label: 'D', toolTipTitle: 'Direct Dependency' },
  transitive: { label: 'T', toolTipTitle: 'Transitive Dependency' },
  'inner-source': { label: 'IS', toolTipTitle: 'InnerSource' },
};

export default function DependencyIndicator({ type, tooltip }) {
  if (!DEPENDENCY_TYPE_MAP.hasOwnProperty(type)) {
    return null;
  }

  const { label, toolTipTitle: defaultTooltip } = DEPENDENCY_TYPE_MAP[type];
  return (
    <NxTooltip title={tooltip || defaultTooltip}>
      <div className={`iq-dependency-indicator ${type}`}>
        <span>{label}</span>
      </div>
    </NxTooltip>
  );
}

DependencyIndicator.propTypes = {
  type: PropTypes.oneOf(['direct', 'transitive', 'inner-source']).isRequired,
  tooltip: PropTypes.node,
};
