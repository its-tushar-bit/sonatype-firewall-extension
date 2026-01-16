/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxGlobalFooter2 } from '@sonatype/react-shared-components';
import { getReleaseVersion } from 'MainRoot/util/versionUtil';
import { useSelector } from 'react-redux';

import { selectIsShowVersionEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectProductEdition } from 'MainRoot/productFeatures/productLicenseSelectors';

export default function Footer({ clmServerVersion }) {
  const productEdition = useSelector(selectProductEdition);
  const releaseNumber = getReleaseVersion(clmServerVersion);
  const isShowVersionEnabled = useSelector(selectIsShowVersionEnabled);

  if (!productEdition || !releaseNumber) {
    return null;
  }

  return (
    <NxGlobalFooter2>
      {isShowVersionEnabled && <span>Release {releaseNumber}</span>}
      <span>Powered by Sonatype IQ Server</span>
    </NxGlobalFooter2>
  );
}

Footer.propTypes = {
  clmServerVersion: PropTypes.string,
};
