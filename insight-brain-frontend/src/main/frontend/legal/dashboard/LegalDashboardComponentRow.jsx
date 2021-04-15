/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxBinaryDonutChart, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function LegalDashboardComponentRow({ row }) {
  return (
    <NxTableRow key={row.applicationName}>
      <NxTableCell className="legal-dashboard-components-component-name nx-truncate-ellipsis">
        {row.componentName}
      </NxTableCell>
      <NxTableCell className="legal-dashboard-components-licenses nx-truncate-ellipsis">{row.licenses}</NxTableCell>
      <NxTableCell className="legal-dashboard-components-occurrences isNumeric">{row.occurrences}</NxTableCell>
      <NxTableCell className="legal-dashboard-components-review-progress">
        <NxBinaryDonutChart percent={Math.min(100, (row.obligationsCompleted * 100) / row.obligationsTotal)} />
        <span>
          {row.obligationsCompleted} / {row.obligationsTotal}
        </span>
      </NxTableCell>
    </NxTableRow>
  );
}

LegalDashboardComponentRow.propTypes = {
  row: PropTypes.any,
};
