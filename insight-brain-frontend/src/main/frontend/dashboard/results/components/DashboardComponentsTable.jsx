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
import { take } from 'ramda';

import { MAX_RESULTS } from '../../services/dashboard.data.service';
import DashboardComponentsTableRow, { componentPropTypes } from './DashboardComponentsTableRow';
import { NxInfoAlert } from '@sonatype/react-shared-components/components/NxAlert/NxAlert';
import { Messages } from '../../../util/CommonServices';
import { heatMapColorStylerPropTypes } from './DashboardComponentsHeatMapCell';
import { extractSortFieldName } from '../../../util/sortUtils';

export default function DashboardComponentsTable(props) {
  const {
        componentResults: {
          results,
          numResults,
          sortFields,
          error
        },
        colorStyler,
        needsAcknowledgement,
        reload,
        sortComponents,
        stateGo
      } = props,
      isLoading = !error && !results && !needsAcknowledgement,
      componentsToDisplay = results && take(MAX_RESULTS, results),
      currentSortedColumnName = extractSortFieldName(sortFields[0]),
      isCurrentColumnSortDescending = sortFields[0].includes('-'),
      emptyTableMessage = 'No data available given the applied filters and permissions.';

  const generateNeedsAcknowledgementInfoRow = () => (
    <NxTableRow>
      <NxTableCell colSpan={8} metaInfo>
        <NxInfoAlert id="needs-acknowledgement">
          {'Select your filter criteria on the left, and click \'apply\' to see results.'}
        </NxInfoAlert>
      </NxTableCell>
    </NxTableRow>
  );

  const generateTableBodyRows = () => {
    const thereAreComponentsToDisplay = componentsToDisplay && componentsToDisplay.length > 0;
    if (!thereAreComponentsToDisplay) {
      return null;
    }

    return (
      <Fragment>
        {componentsToDisplay.map(
            component => <DashboardComponentsTableRow component={component}
                                                      key={component.hash}
                                                      stateGo={stateGo}
                                                      colorStyler={colorStyler}/>
        )}
        {numResults > MAX_RESULTS && generateMaxResultsInfoRow()}
      </Fragment>
    );
  };

  const generateMaxResultsInfoRow = () => (
    <NxTableRow>
      <NxTableCell colSpan={8} metaInfo>
        <span id="max-results-shown">First { MAX_RESULTS } results shown</span>
      </NxTableCell>
    </NxTableRow>
  );

  const sortColumn = (columnNameWithDefaultSortDirection) => {
    const columnNameAscending = extractSortFieldName(columnNameWithDefaultSortDirection);
    if (currentSortedColumnName === columnNameAscending) {
      sortComponents(isCurrentColumnSortDescending ? [columnNameAscending] : [`-${columnNameAscending}`]);
    }
    else {
      sortComponents([columnNameWithDefaultSortDirection]);
    }
  };

  const getColumnDirection = (columnName) => {
    if (!componentsToDisplay || error) {
      return null;
    }

    const isThisColumnSorted = currentSortedColumnName === columnName,
        isAscending = isThisColumnSorted && !isCurrentColumnSortDescending,
        isDescending = isThisColumnSorted && isCurrentColumnSortDescending;

    return isAscending ? 'asc' : isDescending ? 'desc' : null;
  };

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      <NxTable className="nx-table--fixed-layout">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell onClick={()=>sortColumn('derivedComponentName')}
                         sortDir={getColumnDirection('derivedComponentName')}
                         isSortable>
              Name
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--affected-app'}
                         onClick={()=>sortColumn('-affectedApplications')}
                         sortDir={getColumnDirection('affectedApplications')}
                         isSortable>
              Apps
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--total-risk'}
                         onClick={()=>sortColumn('-score')}
                         sortDir={getColumnDirection('score')}
                         isSortable>
              Total Risk
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--critical-risk'}
                         onClick={()=>sortColumn('-scoreCritical')}
                         sortDir={getColumnDirection('scoreCritical')}
                         isSortable>
              <NxThreatIndicator threatLevelCategory='critical'/>
              <span>Critical</span>
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--severe-risk'}
                         onClick={()=>sortColumn('-scoreSevere')}
                         sortDir={getColumnDirection('scoreSevere')}
                         isSortable>
              <NxThreatIndicator threatLevelCategory='severe'/>
              <span>Severe</span>
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--moderate-risk'}
                         onClick={()=>sortColumn('-scoreModerate')}
                         sortDir={getColumnDirection('scoreModerate')}
                         isSortable>
              <NxThreatIndicator threatLevelCategory='moderate'/>
              <span>Moderate</span>
            </NxTableCell>
            <NxTableCell className={'nx-cell--num iq-cell--low-risk'}
                         onClick={()=>sortColumn('-scoreLow')}
                         sortDir={getColumnDirection('scoreLow')}
                         isSortable>
              <NxThreatIndicator threatLevelCategory='low'/>
              <span>Low</span>
            </NxTableCell>
            <NxTableCell chevron/>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody isLoading={isLoading}
                     error={Messages.getHttpErrorMessage(error)}
                     retryHandler={reload}
                     emptyMessage={emptyTableMessage}>
          {needsAcknowledgement ? generateNeedsAcknowledgementInfoRow() : generateTableBodyRows()}
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
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object])
  }),
  colorStyler: heatMapColorStylerPropTypes,
  needsAcknowledgement: PropTypes.bool,
  reload: PropTypes.func.isRequired,
  sortComponents: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired
};
