/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxH1, NxPageTitle } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function SastScanTitle({ title, description }) {
  return (
    <NxPageTitle>
      <NxH1>{title}</NxH1>
      <div className="nx-page-title__description">{description}</div>
    </NxPageTitle>
  );
}
SastScanTitle.propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.node.isRequired,
};
