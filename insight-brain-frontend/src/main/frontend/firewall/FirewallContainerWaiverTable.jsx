/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React from 'react';
import { useDispatch } from 'react-redux';
import * as PropTypes from 'prop-types';

import {
  NxButton,
  NxButtonBar,
  NxH2,
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';

import { faSync } from '@fortawesome/pro-solid-svg-icons';
import { formatDate, FIREWALL_TIME_DATE_FORMAT, STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';

import './_firewall.scss';

export default function FirewallContainerWaiverTable(props) {
  const { loadContainerWaiverList, setContainerWaiverGridPage, stateGo } = props;
  const {
    loadContainerWaiverGridError,
    loadingContainerWaiverList,
    containerWaiverList,
    containerWaiverPageCount,
    containerWaiverCurrentPage,
    containerWaiverLastUpdated,
  } = props;
  const dispatch = useDispatch();

  const goToWaiverDetails = (waiver) => {
    dispatch(
      stateGo('firewall.waiver.details', {
        waiverId: waiver.policyWaiverId,
        ownerId: waiver.ownerId,
        ownerType: 'application',
        type: 'waiver',
        sidebarReference: 'filter',
        page: containerWaiverCurrentPage + 1,
      })
    );
  };

  const getExpiryTime = (expiryTime) => (expiryTime ? formatDate(expiryTime, STANDARD_DATE_FORMAT) : 'Never');

  return (
    <section id="firewall-container-waiver-tab-content">
      <header className="iq-firewall-table-header nx-page-title">
        <NxH2>Containers Waived</NxH2>
        <div className="iq-firewall-table__time visual-testing-ignore">
          {containerWaiverLastUpdated && 'Updated ' + formatDate(containerWaiverLastUpdated, FIREWALL_TIME_DATE_FORMAT)}
        </div>
        <NxButtonBar>
          <NxButton className="refresh-button" variant="tertiary" onClick={() => loadContainerWaiverList()}>
            <NxFontAwesomeIcon icon={faSync} />
            <span>Refresh</span>
          </NxButton>
        </NxButtonBar>
      </header>
      <div className="nx-table-container firewall-container-waiver-table-container">
        <NxTable className="nx-table--fixed-layout" id="firewall-container-waiver-table">
          <NxTableHead>
            <NxTable.Row>
              <NxTableCell isNumeric className="iq-cell--threat">
                Threat
              </NxTableCell>
              <NxTable.Cell className="iq-cel--date-created">Date Created</NxTable.Cell>
              <NxTable.Cell className="iq-cel--expirations">Expirations</NxTable.Cell>
              <NxTable.Cell className="iq-cel--policy">Policy</NxTable.Cell>
              <NxTable.Cell className="iq-cel--scope">Scope</NxTable.Cell>
              <NxTable.Cell className="iq-cel--components">Components</NxTable.Cell>
              <NxTable.Cell chevron />
            </NxTable.Row>
          </NxTableHead>
          <NxTableBody
            emptyMessage="No data found."
            error={loadContainerWaiverGridError}
            isLoading={loadingContainerWaiverList}
          >
            {containerWaiverList &&
              containerWaiverList.map((waiver, idx) => (
                <NxTableRow
                  key={idx}
                  className="firewall-container-waiver"
                  onClick={() => goToWaiverDetails(waiver)}
                  isClickable
                >
                  <NxTableCell isNumeric className="waiver-threat-cell">
                    <NxThreatIndicator policyThreatLevel={waiver.maxThreatLevel || 0} />
                    <span className="nx-threat-number">{waiver.maxThreatLevel || 0}</span>
                  </NxTableCell>
                  <NxTableCell>{formatDate(waiver.createTime, STANDARD_DATE_FORMAT)}</NxTableCell>
                  <NxTableCell>{getExpiryTime(waiver.expiryTime)}</NxTableCell>
                  <NxTableCell>
                    <NxOverflowTooltip title={`Multiple-Policy-Types(${waiver.uniquePolicyCount || 0})`}>
                      <div className="nx-truncate-ellipsis">
                        {`Multiple-Policy-Types(${waiver.uniquePolicyCount || 0})`}
                      </div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell>
                    <NxOverflowTooltip title={waiver.applicationScope}>
                      <div className="nx-truncate-ellipsis">{waiver.applicationScope}</div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTableCell>
                    <NxOverflowTooltip title={`Multiple Components(${waiver.uniqueComponentCount || 0})`}>
                      <div className="nx-truncate-ellipsis">
                        {`Multiple Components(${waiver.uniqueComponentCount || 0})`}
                      </div>
                    </NxOverflowTooltip>
                  </NxTableCell>
                  <NxTable.Cell chevron />
                </NxTableRow>
              ))}
          </NxTableBody>
        </NxTable>

        <div className="nx-table-container__footer">
          <NxPagination
            className="firewall-container-waiver-table-pagination"
            aria-controls="firewall-container-waiver-table"
            pageCount={containerWaiverPageCount}
            currentPage={containerWaiverCurrentPage}
            onChange={setContainerWaiverGridPage}
          />
        </div>
      </div>
    </section>
  );
}

FirewallContainerWaiverTable.propTypes = {
  loadContainerWaiverList: PropTypes.func.isRequired,
  loadContainerWaiverGridError: PropTypes.string,
  loadingContainerWaiverList: PropTypes.bool.isRequired,
  containerWaiverList: PropTypes.array.isRequired,
  containerWaiverPageCount: PropTypes.number.isRequired,
  containerWaiverCurrentPage: PropTypes.number,
  containerWaiverLastUpdated: PropTypes.object,
  setContainerWaiverGridPage: PropTypes.func.isRequired,
};
