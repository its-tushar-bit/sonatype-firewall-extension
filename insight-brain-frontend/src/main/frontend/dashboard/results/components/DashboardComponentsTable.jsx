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
  NxThreatIndicator,
} from '@sonatype/react-shared-components';

import DashboardComponentsTableRow, { componentPropTypes } from './DashboardComponentsTableRow';
import { Messages } from '../../../utilAngular/CommonServices';
import { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';
import { extractSortFieldName, getColumnDirection, sortColumn } from '../../../util/sortUtils';
import MaxResultsInfoRow from '../MaxResultsInfoRow';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';
import { isNilOrEmpty } from '../../../util/jsUtil';
import { MAX_RESULTS } from '../../services/dashboard.data.service';

export default function DashboardComponentsTable(props) {
  const {
      componentResults: { results, numResults, sortFields, error },
      colorStyler,
      needsAcknowledgement,
      reload,
      sortComponents,
      stateGo,
    } = props,
    isLoading = !error && !results && !needsAcknowledgement,
    currentSortedColumnName = sortFields && extractSortFieldName(sortFields[0]),
    isCurrentColumnSortDescending = sortFields && sortFields[0].includes('-'),
    sort = (colName) => sortColumn(sortComponents, currentSortedColumnName, isCurrentColumnSortDescending, colName),
    sortDir = (colName) =>
      !error && results && getColumnDirection(currentSortedColumnName, isCurrentColumnSortDescending, colName),
    emptyTableMessage = 'No data available given the applied filters and permissions.',
    colSpan = 8;

  const generateTableBodyRows = () => {
    if (isNilOrEmpty(results)) {
      return null;
    }

    return (
      <Fragment>
        {results.map((component) => (
          <DashboardComponentsTableRow
            component={component}
            key={component.hash}
            stateGo={stateGo}
            colorStyler={colorStyler}
          />
        ))}
        {numResults > MAX_RESULTS && <MaxResultsInfoRow colSpan={colSpan} maxResults={MAX_RESULTS} />}
      </Fragment>
    );
  };

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      <NxTable className="nx-table--fixed-layout">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell
              onClick={() => sort('derivedComponentName')}
              sortDir={sortDir('derivedComponentName')}
              isSortable
            >
              Name
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--affected-app'}
              onClick={() => sort('-affectedApplications')}
              sortDir={sortDir('affectedApplications')}
              isSortable
            >
              Apps
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--total-risk'}
              onClick={() => sort('-score')}
              sortDir={sortDir('score')}
              isSortable
            >
              Total Risk
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--critical-risk'}
              onClick={() => sort('-scoreCritical')}
              sortDir={sortDir('scoreCritical')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="critical" />
              <span>Critical</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--severe-risk'}
              onClick={() => sort('-scoreSevere')}
              sortDir={sortDir('scoreSevere')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="severe" />
              <span>Severe</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--moderate-risk'}
              onClick={() => sort('-scoreModerate')}
              sortDir={sortDir('scoreModerate')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="moderate" />
              <span>Moderate</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--low-risk'}
              onClick={() => sort('-scoreLow')}
              sortDir={sortDir('scoreLow')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="low" />
              <span>Low</span>
            </NxTableCell>
            <NxTableCell chevron />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          isLoading={isLoading}
          error={Messages.getHttpErrorMessage(error)}
          retryHandler={reload}
          emptyMessage={emptyTableMessage}
        >
          {needsAcknowledgement ? <NeedsAcknowledgementInfoRow colSpan={colSpan} /> : generateTableBodyRows()}
        </NxTableBody>
      </NxTable>
    </div>
  );
}

DashboardComponentsTable.propTypes = {
  componentResults: PropTypes.shape({
    results: PropTypes.arrayOf(componentPropTypes),
    numResults: PropTypes.number,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
  }),
  colorStyler: heatMapColorStylerPropTypes,
  needsAcknowledgement: PropTypes.bool,
  reload: PropTypes.func.isRequired,
  sortComponents: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
};
