/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';

export default function Hexagon({ className }) {
  const classes = classnames('hexagon nx-icon', className);

  const path =
    'M91.952,6.246L7.653,55l0.073,97.383l84.373,48.628l84.299-48.754l-0.073-97.382L91.952,6.246z M92.084,183.74';

  return (
    <svg className={classes} viewBox="0 0 185.5 208" preserveAspectRatio="xMidYMid meet" role="presentation">
      <path d={path} />
    </svg>
  );
}
Hexagon.propTypes = {
  className: PropTypes.string,
};
