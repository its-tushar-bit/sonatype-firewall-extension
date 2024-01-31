/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxFontAwesomeIcon, NxTag, NxTooltip } from '@sonatype/react-shared-components';
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

export function TagWithFontAwesomeIcon({ faIcon, color, children, tooltip, ...props }) {
  return (
    <NxTooltip title={tooltip}>
      <NxTag color={color} aria-label={`Label ${children}`} {...props}>
        <NxFontAwesomeIcon icon={faIcon} />
        <span>{children}</span>
      </NxTag>
    </NxTooltip>
  );
}

export default function ComponentLabelTag({ children, color, description, ...props }) {
  const mappedColor = rscColorMap[color];
  return (
    <TagWithFontAwesomeIcon faIcon={faTag} color={mappedColor} tooltip={description} {...props}>
      {children}
    </TagWithFontAwesomeIcon>
  );
}

TagWithFontAwesomeIcon.propTypes = {
  faIcon: PropTypes.oneOfType([PropTypes.object, PropTypes.array, PropTypes.string]).isRequired,
  color: PropTypes.string,
  tooltip: PropTypes.string,
  children: PropTypes.node.isRequired,
};

ComponentLabelTag.propTypes = {
  children: PropTypes.node.isRequired,
  color: PropTypes.string,
  description: PropTypes.string,
};
