/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faDownload } from '@fortawesome/pro-solid-svg-icons';
import { actions } from './react2shellSlice';
import { selectDownloadLoading } from './react2shellSelectors';

export default function React2ShellHeader() {
  const dispatch = useDispatch();
  const downloadLoading = useSelector(selectDownloadLoading);

  const handleDownloadCSV = () => {
    dispatch(actions.downloadReact2ShellCSV());
  };

  return (
    <div className="nx-page-title">
      <h1 className="nx-h1">React2Shell Impact Report</h1>
      <div className="nx-btn-bar">
        <NxButton variant="tertiary" onClick={handleDownloadCSV} disabled={downloadLoading}>
          <NxFontAwesomeIcon icon={faDownload} />
          <span>{downloadLoading ? 'Downloading...' : 'Download CSV'}</span>
        </NxButton>
      </div>
    </div>
  );
}
