import React from 'react';
import * as PropTypes from 'prop-types';
import moment from 'moment';

const formatDate = date => moment(date).format('YYYY-MM-DD HH:mm:ss [UTC]Z');

export default function ApplicationReportVulnerabilitiesHeader({ metadata }) {
  return (
    <div className="nx-tile-header">
      <div id="application-report-vulnerabilities-title" className="nx-tile-header__title">
        <h1>
          Vulnerabilities for {metadata.application.name} {metadata.reportTitle} -{' '}
          <span className="visual-testing-ignore">{formatDate(metadata.reportTime)}</span>
        </h1>
      </div>
    </div>
  );
}

export const metadataPropType = PropTypes.shape({
  reportTitle: PropTypes.string.isRequired,
  reportTime: PropTypes.number.isRequired,
  application: PropTypes.shape({
    name: PropTypes.string.isRequired
  }).isRequired
});

ApplicationReportVulnerabilitiesHeader.propTypes = {
  metadata: metadataPropType.isRequired
};
