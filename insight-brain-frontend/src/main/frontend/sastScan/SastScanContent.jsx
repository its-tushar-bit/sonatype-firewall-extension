/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTile } from '@sonatype/react-shared-components';
import SastScanFindings from 'MainRoot/sastScan/SastScanFindings';
import * as PropTypes from 'prop-types';

export default function SastScanContent({ sastScan }) {
  return (
    <NxTile>
      <SastScanFindings findings={sastScan.findings} />
    </NxTile>
  );
}

SastScanContent.propTypes = {
  sastScan: PropTypes.object.isRequired,
};
