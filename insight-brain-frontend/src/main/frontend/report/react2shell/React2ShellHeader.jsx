/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { NxFontAwesomeIcon, NxPageTitle, NxH1 } from '@sonatype/react-shared-components';
import { faDownload } from '@fortawesome/pro-solid-svg-icons';
import { getReact2ShellReportDownloadUrl } from 'MainRoot/util/CLMLocation';
import { sendGainsightCustomEvent } from 'MainRoot/util/gainsightUtils';

const REACT2SHELL_CSV_DOWNLOADED = 'react2shell_csv_downloaded';

export default function React2ShellHeader({ cveIds }) {
  const downloadUrl = getReact2ShellReportDownloadUrl(cveIds);

  const handleDownloadClick = () => {
    sendGainsightCustomEvent(REACT2SHELL_CSV_DOWNLOADED);
  };

  const getCveDisplayText = () => {
    return `Affected Components for ${cveIds.join(', ')}`;
  };

  return (
    <NxPageTitle>
      <NxPageTitle.Headings>
        <NxH1>React2Shell Impact Report</NxH1>
      </NxPageTitle.Headings>
      {getCveDisplayText() && <NxPageTitle.Description>{getCveDisplayText()}</NxPageTitle.Description>}
      <div className="nx-btn-bar">
        <a
          href={downloadUrl}
          download="react2shell-report.csv"
          className="nx-btn nx-btn--tertiary"
          onClick={handleDownloadClick}
        >
          <NxFontAwesomeIcon icon={faDownload} />
          <span>Download CSV</span>
        </a>
      </div>
    </NxPageTitle>
  );
}

React2ShellHeader.propTypes = {
  cveIds: PropTypes.arrayOf(PropTypes.string),
};
