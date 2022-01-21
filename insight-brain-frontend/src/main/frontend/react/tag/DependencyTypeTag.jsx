/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxTag } from '@sonatype/react-shared-components';

const dependencyMap = new Map([
  ['direct', 'Direct Dependency'],
  ['transitive', 'Transitive Dependency'],
  ['innerSource', 'InnerSource'],
]);

export default function DependencyTypeTag({ type }) {
  return (
    <NxTag className={type} aria-label={`Dependency type is ${type}`}>
      {dependencyMap.get(type)}
    </NxTag>
  );
}

DependencyTypeTag.propTypes = {
  type: PropTypes.oneOf(['direct', 'transitive', 'innerSource']).isRequired,
};
