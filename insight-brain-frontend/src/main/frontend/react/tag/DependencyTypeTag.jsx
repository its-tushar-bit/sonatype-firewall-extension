/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxTag } from '@sonatype/react-shared-components';

const VALUES_FOR_TYPE = {
  direct: {
    text: 'Direct Dependency',
    color: 'blue',
  },
  transitive: {
    text: 'Transitive Dependency',
    color: 'purple',
  },
  innerSource: {
    text: 'InnerSource',
    color: 'green',
  },
};
export default function DependencyTypeTag({ type }) {
  const { color, text } = VALUES_FOR_TYPE[type];
  return <NxTag color={color}>{text}</NxTag>;
}

DependencyTypeTag.propTypes = {
  type: PropTypes.oneOf(['direct', 'transitive', 'innerSource']).isRequired,
};
