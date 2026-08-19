/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

import { formatDate } from '../../util/dateUtils';
import { getReportDisplayName } from '../reportEntryUtils';

export default function ApplicationReportRawDataHeader({ metadata }) {
  const routerParams = useSelector(selectRouterCurrentParams);
  const titleName = getReportDisplayName(metadata, routerParams);

  return (
    <div className="nx-page-title" id="raw-data-report-title">
      <h1 className="nx-h1">
        Raw Data for {titleName} {metadata?.reportTitle}
      </h1>
      <div className="nx-page-title__description visual-testing-ignore">
        {metadata?.reportTime != null && formatDate(metadata.reportTime)}
      </div>
    </div>
  );
}

// Application-report metadata carries a required `application` object; HRC reports come from
// browseReport where metadata is a plain envelope (name/time/buf) and `application` is null.
// Two shapes let each caller signal the contract it actually satisfies.
export const applicationMetadataPropType = PropTypes.shape({
  reportTitle: PropTypes.string.isRequired,
  reportTime: PropTypes.number.isRequired,
  application: PropTypes.shape({
    name: PropTypes.string.isRequired,
  }).isRequired,
});

export const hrcMetadataPropType = PropTypes.shape({
  reportTitle: PropTypes.string,
  reportTime: PropTypes.number,
});

export const metadataPropType = PropTypes.oneOfType([applicationMetadataPropType, hrcMetadataPropType]);

ApplicationReportRawDataHeader.propTypes = {
  metadata: metadataPropType,
};
