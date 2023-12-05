/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import LoadWrapper from '../../react/LoadWrapper';
import SuccessMetricsReportListItem from './SuccessMetricsReportListItem';
import { useRouterState } from '../../react/RouterStateContext';
import { Fragment } from 'react';
import AddSuccessMetricsReportContainer from './addSuccessMetricsReport/AddSuccessMetricsReportContainer';
import { isNilOrEmpty } from '../../util/jsUtil';

const SuccessMetricsReportList = ({ reports, load, loadError, loading, isAddModalOpen, newReport, toggleAddModal }) => {
  const uiRouterState = useRouterState();

  useEffect(() => {
    load();
  }, []);

  return (
    <Fragment>
      <div id="success-metrics-report-list">
        <div className="nx-page-title">
          <h1 className="nx-h1">Success Metrics</h1>
          <div className="nx-page-title__description">
            <h3 className="nx-h3">
              Success Metrics is an experimental feature providing high-level statistics on the past performance of
              Sonatype Lifecycle. If you want to take action on your current policy violations, start with the{' '}
              <NxTextLink href={uiRouterState.href('dashboard.overview.violations')}>Dashboard.</NxTextLink>
            </h3>
          </div>
        </div>
        <LoadWrapper loading={loading} error={loadError} retryHandler={load}>
          <section className="nx-tile">
            <header className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">Reports</h2>
              </div>
              <div className="nx-tile-header__subtitle">
                Success Metrics data is also accessible via the{' '}
                <NxTextLink
                  external
                  href="http://links.sonatype.com/products/nxiq/doc/success-metrics-data-rest-api/v2"
                >
                  Success Metrics Data API.
                </NxTextLink>
              </div>
              <div className="nx-tile__actions">
                <NxButton variant="tertiary" id="add-success-metrics-report-btn" onClick={toggleAddModal}>
                  <NxFontAwesomeIcon icon={faPlus} />
                  <span>Add a Report</span>
                </NxButton>
              </div>
            </header>
            <div className="nx-tile-content">
              <ul className="nx-list nx-list--clickable">
                {reports.map((report) => (
                  <SuccessMetricsReportListItem key={report.id} reportId={report.id} reportName={report.name} />
                ))}

                {isNilOrEmpty(reports) && (
                  <li className="nx-list__item nx-list__item--empty">
                    <span className="nx-list__text">No reports have been created.</span>
                  </li>
                )}
              </ul>
            </div>
          </section>
        </LoadWrapper>
      </div>
      {isAddModalOpen && (
        <AddSuccessMetricsReportContainer
          close={newReport}
          dismiss={toggleAddModal}
          reports={reports}
        ></AddSuccessMetricsReportContainer>
      )}
    </Fragment>
  );
};

SuccessMetricsReportList.propTypes = {
  reports: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      name: PropTypes.string,
    })
  ),
  load: PropTypes.func.isRequired,
  loadError: PropTypes.string,
  loading: PropTypes.bool.isRequired,
  isAddModalOpen: PropTypes.bool.isRequired,
  newReport: PropTypes.func.isRequired,
  toggleAddModal: PropTypes.func.isRequired,
};

export default SuccessMetricsReportList;
