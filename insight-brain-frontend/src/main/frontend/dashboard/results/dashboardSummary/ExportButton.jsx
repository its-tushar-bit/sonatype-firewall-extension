/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faFile } from '@fortawesome/pro-regular-svg-icons';

const ExportButton = ({ exportTitle, exportRequestData, exportUrl }) => {
  const exportRequestJson = JSON.stringify(exportRequestData);

  return (
    <form
      id="export-results-form"
      className="iq-form iq-form--no-border iq-form--no-padding"
      name="export-results"
      action={exportUrl}
      method="post"
      encType="multipart/form-data"
    >
      <input type="hidden" name="filter" value={exportRequestJson} />
      <NxButton id="export-results" variant="tertiary" className="export-dashboard-results-btn" disabled={!exportUrl}>
        <NxFontAwesomeIcon icon={faFile} />
        Export {exportTitle} Data
      </NxButton>
    </form>
  );
};

export const exportButtonPropTypes = {
  exportTitle: PropTypes.string,
  exportRequestData: PropTypes.any,
  exportUrl: PropTypes.string,
};

ExportButton.propTypes = {
  ...exportButtonPropTypes,
};

export default ExportButton;
