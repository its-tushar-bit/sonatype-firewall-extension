/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { propOr } from 'ramda';

import { NxTag } from '@sonatype/react-shared-components';

const formatsThatHaveIcon = {
  maven: require('../../img/tag/maven.svg'),
  pypi: require('../../img/tag/pypi.svg'),
  rpm: require('../../img/tag/rpm.svg'),
  gem: require('../../img/tag/gem.svg'),
  golang: require('../../img/tag/golang.svg'),
  swift: require('../../img/tag/swift.svg'),
};

export default function ComponentFormatTag({ name }) {
  const routeIconOrNull = propOr(null, name);
  const iconRoute = routeIconOrNull(formatsThatHaveIcon);
  const icon = iconRoute ? <img src={iconRoute} alt="" /> : null;

  return (
    <NxTag className="iq-component-format-tag">
      {icon}
      <span>{name}</span>
    </NxTag>
  );
}

ComponentFormatTag.propTypes = {
  name: PropTypes.string.isRequired,
};
