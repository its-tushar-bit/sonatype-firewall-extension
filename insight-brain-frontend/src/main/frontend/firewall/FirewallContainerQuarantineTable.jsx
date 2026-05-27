/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTextLink,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { faSync } from '@fortawesome/pro-solid-svg-icons';
import { formatDate, FIREWALL_TIME_DATE_FORMAT, FIREWALL_DATE_TIME_FORMAT } from 'MainRoot/util/dateUtils';
import {
  FIREWALL_CONTAINER_REPOSITORY_RESULTS,
  FIREWALL_FIREWALLPAGE_CONTAINERS,
} from 'MainRoot/constants/states/firewall';

import './_firewall.scss';

export default function FirewallContainerQuarantineTable(props) {
  // actions
  const { loadContainerQuarantineList, setContainerQuarantineGridPage } = props;

  const uiRouterState = useRouterState();

  // quarantineState.containerQuarantineGridState
  const {
    loadedContainerQuarantineList,
    loadContainerQuarantineGridError,
    containerQuarantinePageCount,
    containerQuarantineList,
    containerCurrentPage,
    containerLastUpdated,
  } = props;

  return (
    <section id="firewall-container-quarantine-table">
      <header className="iq-firewall-table-header nx-page-title">
        <h2 className="nx-h2 iq-firewall-table-label">Containers Actively in Quarantine</h2>
        <div className="iq-firewall-table__time visual-testing-ignore">
          {containerLastUpdated && 'Updated ' + formatDate(containerLastUpdated, FIREWALL_TIME_DATE_FORMAT)}
        </div>
        <div className="nx-btn-bar">
          <NxButton
            id="firewall-container-quarantine-table--refresh-button"
            variant="tertiary"
            onClick={() => loadContainerQuarantineList()}
          >
            <NxFontAwesomeIcon icon={faSync} />
            <span>Refresh</span>
          </NxButton>
        </div>
      </header>

      <div className="nx-table-container iq-firewall-container-quarantine-table">
        <NxTable id="pagination-firewall-container-quarantine-table" className="nx-table--fixed-layout">
          <NxTableHead>
            <NxTableRow>
              <NxTableCell isNumeric className="iq-cell--threat">
                Threat
              </NxTableCell>
              <NxTableCell id="policyName-header" className="iq-cell--policy-type">
                Policy
              </NxTableCell>
              <NxTableCell id="evaluationTime-header" className="iq-cell--quarantine-date">
                Evaluation Time
              </NxTableCell>
              <NxTableCell id="component-header" className="iq-cell--component">
                Container
              </NxTableCell>
              <NxTableCell id="repository-header" className="iq-cell--repository">
                Repository
              </NxTableCell>
            </NxTableRow>
          </NxTableHead>

          <NxTableBody
            id="iq-firewall-container-quarantine-table-body"
            emptyMessage="No data found."
            error={loadContainerQuarantineGridError}
            isLoading={!loadedContainerQuarantineList}
          >
            {containerQuarantineList &&
              containerQuarantineList.map((row, index) => {
                return (
                  <NxTableRow key={index} className="firewall-container-quarantine">
                    <NxTableCell isNumeric className="quarantine-threat-cell">
                      <NxThreatIndicator policyThreatLevel={row.threatLevel === null ? 0 : row.threatLevel} />
                      <span className="nx-threat-number">{row.threatLevel === null ? 0 : row.threatLevel}</span>
                    </NxTableCell>
                    <NxTableCell className="iq-policy-cell">
                      <NxOverflowTooltip title={!row.policyViolationCount ? 'None' : row.policyViolationCount}>
                        <div className="nx-truncate-ellipsis">
                          {`Multiple-Policy-Types(${row.policyViolationCount || 'None'})`}
                        </div>
                      </NxOverflowTooltip>
                    </NxTableCell>
                    <NxTableCell className="visual-testing-ignore">
                      {formatDate(row.openTime, FIREWALL_DATE_TIME_FORMAT)}
                    </NxTableCell>
                    <NxTableCell>
                      <NxOverflowTooltip title={row.applicationName}>
                        <div className="nx-truncate-ellipsis">
                          <NxTextLink
                            id="iq-firewall-container-quarantine-table--container-report-link"
                            href={uiRouterState.href('firewall.containerReport', {
                              origin: FIREWALL_FIREWALLPAGE_CONTAINERS,
                              publicId: row.applicationPublicId,
                              scanId: row.scanId,
                            })}
                            truncate
                          >
                            {row.applicationName}
                          </NxTextLink>
                        </div>
                      </NxOverflowTooltip>
                    </NxTableCell>
                    <NxTableCell>
                      <NxOverflowTooltip title={row.repositoryPublicId}>
                        <div className="nx-truncate-ellipsis">
                          <NxTextLink
                            id="iq-firewall-container-quarantine-table--repo-view-link"
                            href={uiRouterState.href(FIREWALL_CONTAINER_REPOSITORY_RESULTS, {
                              repositoryId: row.repositoryId,
                            })}
                            truncate
                          >
                            {row.repositoryPublicId}
                          </NxTextLink>
                        </div>
                      </NxOverflowTooltip>
                    </NxTableCell>
                  </NxTableRow>
                );
              })}
          </NxTableBody>
        </NxTable>

        <div className="nx-table-container__footer">
          <NxPagination
            className="iq-firewall-table__nav-bar"
            aria-controls="pagination-firewall-container-quarantine-table"
            pageCount={containerQuarantinePageCount}
            currentPage={containerCurrentPage}
            onChange={setContainerQuarantineGridPage}
          />
        </div>
      </div>
    </section>
  );
}

FirewallContainerQuarantineTable.propTypes = {
  loadContainerQuarantineList: PropTypes.func.isRequired,
  loadContainerQuarantineGridError: PropTypes.string,
  setContainerQuarantineGridPage: PropTypes.func.isRequired,
  loadedContainerQuarantineList: PropTypes.bool.isRequired,
  containerQuarantineList: PropTypes.array.isRequired,
  containerQuarantinePageCount: PropTypes.number.isRequired,
  containerCurrentPage: PropTypes.number,
  containerLastUpdated: PropTypes.object,
};
