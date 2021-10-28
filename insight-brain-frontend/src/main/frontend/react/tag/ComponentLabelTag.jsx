/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxFontAwesomeIcon, NxTag } from '@sonatype/react-shared-components';
import { faTag } from '@fortawesome/pro-solid-svg-icons';

export const rscColorMap = {
  'light-purple': 'purple',
  'dark-purple': 'indigo',
  'dark-red': 'red',
  'light-red': 'pink',
  'dark-blue': 'blue',
  'light-blue': 'light-blue',
  'dark-green': 'green',
  orange: 'orange',
  yellow: 'yellow',
  'light-green': 'lime',
};

export function TagWithFontAwesomeIcon({ faIcon, color, children }) {
  return (
    <NxTag color={color}>
      <NxFontAwesomeIcon icon={faIcon} />
      <span>{children}</span>
    </NxTag>
  );
}

export default function ComponentLabelTag({ children, color }) {
  const mappedColor = rscColorMap[color];
  return (
    <TagWithFontAwesomeIcon faIcon={faTag} color={mappedColor}>
      {children}
    </TagWithFontAwesomeIcon>
  );
}

TagWithFontAwesomeIcon.propTypes = {
  faIcon: PropTypes.oneOfType([PropTypes.object, PropTypes.array, PropTypes.string]).isRequired,
  color: PropTypes.string,
  children: PropTypes.node.isRequired,
};

ComponentLabelTag.propTypes = {
  children: PropTypes.node.isRequired,
  color: PropTypes.string,
};
