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

const nameMapping = {
  'a-name': 'A-Name',
  alpine: 'Alpine',
  cargo: 'Cargo',
  cocoapods: 'CocoaPods',
  composer: 'Composer',
  conan: 'Conan',
  conda: 'Conda',
  cran: 'CRAN',
  deb: 'Debian',
  drupal: 'Drupal',
  golang: 'Go',
  maven: 'Maven',
  nuget: 'NuGet',
  pypi: 'PyPI',
  rpm: 'RPM',
  gem: 'RubyGems',
  swift: 'Swift',
};

export default function ComponentFormatTag({ name, ...props }) {
  const routeIconOrNull = propOr(null, name);
  const iconRoute = routeIconOrNull(formatsThatHaveIcon);
  const icon = iconRoute ? <img src={iconRoute} alt="" /> : null;
  const formatName = nameMapping[name] || name;

  return (
    <NxTag className="iq-component-format-tag" aria-label={`Format ${formatName}`} {...props}>
      {icon}
      <span>{formatName}</span>
    </NxTag>
  );
}

ComponentFormatTag.propTypes = {
  name: PropTypes.string.isRequired,
};
