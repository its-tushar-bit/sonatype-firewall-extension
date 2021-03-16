/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxThreatIndicator
} from '@sonatype/react-shared-components';

import DashboardApplicationsTableRow, { applicationPropTypes } from './DashboardApplicationsTableRow';
import { Messages } from '../../../util/CommonServices';
import { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';
import { sortColumn, getColumnDirection, extractSortFieldName } from '../../../util/sortUtils';
import { isNilOrEmpty } from '../../../util/jsUtil';
import MaxResultsInfoRow from '../MaxResultsInfoRow';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';
import { MAX_RESULTS } from '../../services/dashboard.data.service';

export default function DashboardApplicationsTable(props) {
  const {
        applicationResults: {
          results,
          numResults,
          sortFields,
          error
        },
        colorStyler,
        needsAcknowledgement,
        reload,
        sortApplications
      } = props,
      isLoading = !error && !results && !needsAcknowledgement,
      currentSortedColumnName = sortFields && extractSortFieldName(sortFields[0]),
      isCurrentColumnSortDescending = sortFields && sortFields[0].includes('-'),
      sort = (colName) => sortColumn(sortApplications, currentSortedColumnName, isCurrentColumnSortDescending, colName),
      sortDir = (colName) => !error && results &&
          getColumnDirection(currentSortedColumnName, isCurrentColumnSortDescending, colName),
      emptyTableMessage = 'No data available given the applied filters and permissions.',
      colSpan = 6;

  const generateTableBodyRows = () => {
    if (isNilOrEmpty(results)) {
      return null;
    }

    return (
      <Fragment>
        {results.map(
            (application, rowIndex) => <DashboardApplicationsTableRow application={application}
                                                                      key={application.applicationId}
                                                                      colorStyler={colorStyler}
                                                                      tableRowIndex={rowIndex}/>
        )}
        {numResults > MAX_RESULTS && <MaxResultsInfoRow colSpan={colSpan} maxResults={MAX_RESULTS}/>}
      </Fragment>
    );
  };

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      <NxTable className="nx-table--fixed-layout">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell onClick={() => sort('applicationName')}
                         sortDir={sortDir('applicationName')}
                         isSortable>
              Name
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--total-risk'}
                         onClick={() => sort('-totalApplicationRisk.totalRisk')}
                         sortDir={sortDir('totalApplicationRisk.totalRisk')}
                         isSortable>
              Total Risk
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--critical-risk'}
                         onClick={() => sort('-totalApplicationRisk.criticalRisk')}
                         sortDir={sortDir('totalApplicationRisk.criticalRisk')}
                         isSortable>
              <NxThreatIndicator threatLevelCategory='critical'></NxThreatIndicator>
              <span>Critical</span>
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--severe-risk'}
                         onClick={() => sort('-totalApplicationRisk.severeRisk')}
                         sortDir={sortDir('totalApplicationRisk.severeRisk')}
                         isSortable>
              <NxThreatIndicator threatLevelCategory='severe'></NxThreatIndicator>
              <span>Severe</span>
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--moderate-risk'}
                         onClick={() => sort('-totalApplicationRisk.moderateRisk')}
                         sortDir={sortDir('totalApplicationRisk.moderateRisk')}
                         isSortable>
              <NxThreatIndicator threatLevelCategory='moderate'></NxThreatIndicator>
              <span>Moderate</span>
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--low-risk'}
                         onClick={() => sort('-totalApplicationRisk.lowRisk')}
                         sortDir={sortDir('totalApplicationRisk.lowRisk')}
                         isSortable>
              <NxThreatIndicator threatLevelCategory='low'></NxThreatIndicator>
              <span>Low</span>
            </NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody isLoading={isLoading}
                     error={Messages.getHttpErrorMessage(error)}
                     retryHandler={reload}
                     emptyMessage={emptyTableMessage}>
          {needsAcknowledgement ? <NeedsAcknowledgementInfoRow colSpan={colSpan}/> : generateTableBodyRows()}
        </NxTableBody>
      </NxTable>
    </div>
  );
}

DashboardApplicationsTable.propTypes = {
  applicationResults: PropTypes.shape({
    results: PropTypes.arrayOf(applicationPropTypes),
    numResults: PropTypes.number,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object])
  }),
  colorStyler: heatMapColorStylerPropTypes,
  needsAcknowledgement: PropTypes.bool,
  reload: PropTypes.func.isRequired,
  sortApplications: PropTypes.func.isRequired
};
