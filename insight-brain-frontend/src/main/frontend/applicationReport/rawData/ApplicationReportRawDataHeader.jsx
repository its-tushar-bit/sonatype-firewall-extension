/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { formatDate } from '../../util/dateUtils';

export default function ApplicationReportRawDataHeader({ metadata }) {
  return (
    <div className="nx-page-title" id="raw-data-report-title">
      <h1 className="nx-h1">
        Raw Data for {metadata.application.name} {metadata.reportTitle}
      </h1>
      <div className="nx-page-title__description visual-testing-ignore">{formatDate(metadata.reportTime)}</div>
    </div>
  );
}

export const metadataPropType = PropTypes.shape({
  reportTitle: PropTypes.string.isRequired,
  reportTime: PropTypes.number.isRequired,
  application: PropTypes.shape({
    name: PropTypes.string.isRequired,
  }).isRequired,
});

ApplicationReportRawDataHeader.propTypes = {
  metadata: metadataPropType.isRequired,
};
