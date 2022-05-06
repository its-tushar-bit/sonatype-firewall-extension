/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { formatDate } from '../../util/dateUtils';

export default function ApplicationReportVulnerabilitiesHeader({ metadata }) {
  return (
    <div className="nx-tile-header">
      <div id="application-report-vulnerabilities-title" className="nx-tile-header__title">
        <h1 className="nx-h1">
          Vulnerabilities for {metadata.application.name} {metadata.reportTitle}
        </h1>
      </div>
      <div className="nx-tile-header__subtitle visual-testing-ignore">{formatDate(metadata.reportTime)}</div>
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

ApplicationReportVulnerabilitiesHeader.propTypes = {
  metadata: metadataPropType.isRequired,
};
