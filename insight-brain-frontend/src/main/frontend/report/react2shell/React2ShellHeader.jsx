/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faDownload } from '@fortawesome/pro-solid-svg-icons';
import { getReact2ShellReportDownloadUrl } from 'MainRoot/util/CLMLocation';

export default function React2ShellHeader() {
  const downloadUrl = getReact2ShellReportDownloadUrl();

  return (
    <div className="nx-page-title">
      <h1 className="nx-h1">React2Shell Impact Report</h1>
      <div className="nx-btn-bar">
        <a href={downloadUrl} download="react2shell-report.csv" className="nx-btn nx-btn--tertiary">
          <NxFontAwesomeIcon icon={faDownload} />
          <span>Download CSV</span>
        </a>
      </div>
    </div>
  );
}
