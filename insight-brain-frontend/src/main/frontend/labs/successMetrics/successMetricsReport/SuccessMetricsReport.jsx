/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import ViolationsByCategoryChart from './violationsByCategoryChart/ViolationsByCategoryChart';
import ViolationTrendsChart from './violationTrendsChart/ViolationTrendsChart';
import {
  applicationCountsShape,
  averagesShape,
  componentCountsShape,
  mttrShape,
  violationCountsShape,
  violationsByCategoryShape,
} from './SuccessMetricsPropTypes';
import {
  NxButton,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxLoadWrapper,
  NxModal,
  NxWarningAlert,
  useToggle,
} from '@sonatype/react-shared-components';
import { formatDate } from '../../../util/dateUtils.js';
import ViolationAveragesChart from './violationAveragesChart/ViolationAveragesChart';
import MttrChart from './mttrChart/MttrChart';
import ApplicationCountsChart from './applicationCountsChart/ApplicationCountsChart';
import ComponentCountsChart from './componentCountsChart/ComponentCountsChart';
import MenuBarBackButton from '../../../mainHeader/MenuBar/MenuBarBackButton';

const DATE_FORMAT_WITH_TIME = 'MMM DD, YYYY hh:mm:ss A';
const DATE_FORMAT = 'MMM DD, YYYY';

const SuccessMetricsReport = ({
  load,
  loading,
  loadError,
  reportName,
  monthCount,
  lastUpdated,
  isSingleApplicationReport,
  singleApplicationName,
  includeLatestData,
  applicationCounts,
  averages,
  mttrs,
  violationCounts,
  violationsByCategoryWeeks,
  componentCounts,
  router,
  deleteMaskState,
  deleteError,
  deleteReport,
  successMetricsStageId,
}) => {
  const {
    currentParams: { successMetricsReportId },
  } = router;

  const activeApplicationCount = applicationCounts.activeApplications;
  const hasData = activeApplicationCount > 0;

  const [showModal, toggleShowModal] = useToggle(false);

  useEffect(() => {
    load(successMetricsReportId);
  }, []);

  return (
    <Fragment>
      <div id="success-metrics-report">
        <NxLoadWrapper
          loading={loading}
          error={loadError}
          retryHandler={() => {
            load(successMetricsReportId);
          }}
        >
          <MenuBarBackButton stateName="labs.successMetrics" />
          <div className="nx-page-title" id="success-metrics-header">
            <h1 className="nx-h1">{reportName}</h1>
            <NxButtonBar>
              <NxButton type="button" variant="tertiary" onClick={toggleShowModal} id="delete-report-button">
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete Report</span>
              </NxButton>
            </NxButtonBar>
            <div className="nx-page-title__description">
              <h3 className="nx-h3">
                {hasData ? (
                  `
              This report contains data for
              ${applicationCounts.activeApplications}
              application${isSingleApplicationReport ? '' : 's'}, evaluated over the past
              ${monthCount} ${monthCount === 1 ? 'month' : 'months'},
              ${getStageInfoText()}. Last
              updated
              ${formatDate(lastUpdated, includeLatestData ? DATE_FORMAT_WITH_TIME : DATE_FORMAT)}.
            `
                ) : (
                  <span id="no-data-warning">
                    <i className="fa fa-info-circle"></i>
                    {` There's not enough data to generate Success Metrics. Run some evaluations and check again${
                      includeLatestData ? '' : ' next month'
                    }.${
                      includeLatestData
                        ? ''
                        : " Create a Success Metrics report using the 'include most recent evaluations' option to see the latest data."
                    }`}
                  </span>
                )}
              </h3>
            </div>
          </div>
          {hasData && (
            <Fragment>
              <ViolationTrendsChart violationCounts={violationCounts} />
              <ViolationsByCategoryChart violationsByCategoryData={violationsByCategoryWeeks} />
              <ViolationAveragesChart
                {...{
                  averages,
                  isSingleApplicationReport,
                  activeApplicationCount,
                  monthCount,
                }}
              />
              <MttrChart
                {...{
                  activeApplicationCount,
                  monthCount,
                  mttrs,
                }}
              />
              {!isSingleApplicationReport && (
                <ApplicationCountsChart
                  {...{
                    monthCount,
                    applicationCounts,
                  }}
                />
              )}
              <ComponentCountsChart
                {...{
                  activeApplicationCount,
                  isSingleApplicationReport,
                  singleApplicationName,
                  componentCounts,
                }}
              />
            </Fragment>
          )}
        </NxLoadWrapper>
      </div>
      {showModal && (
        <NxModal onClose={toggleShowModal} variant="narrow" id="delete-modal">
          <NxStatefulForm
            className="nx-form"
            onSubmit={() => deleteReport(successMetricsReportId)}
            submitMaskState={deleteMaskState}
            onCancel={toggleShowModal}
            submitBtnText="Delete"
            submitError={deleteError}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2">
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete Report</span>
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>You are about to delete {reportName}. This action cannot be undone.</NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </Fragment>
  );

  function getStageInfoText() {
    if (successMetricsStageId) {
      return `for evaluations of the ${successMetricsStageId} stage`;
    } else {
      return 'aggregated and deduplicated over the source, build, stage release, release, and operate stages';
    }
  }
};

SuccessMetricsReport.propTypes = {
  load: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  loadError: PropTypes.string,
  deleteMaskState: PropTypes.bool,
  deleteError: PropTypes.string,
  deleteReport: PropTypes.func.isRequired,
  reportName: PropTypes.string,
  singleApplicationName: PropTypes.string,
  monthCount: PropTypes.number,
  lastUpdated: PropTypes.number,
  includeLatestData: PropTypes.bool,
  isSingleApplicationReport: PropTypes.bool,
  averages: averagesShape,
  mttrs: PropTypes.arrayOf(mttrShape),
  componentCounts: componentCountsShape,
  applicationCounts: applicationCountsShape,
  violationCounts: PropTypes.arrayOf(violationCountsShape),
  violationsByCategoryWeeks: PropTypes.arrayOf(violationsByCategoryShape),
  successMetricsStageId: PropTypes.string,
  router: PropTypes.shape({
    currentParams: PropTypes.oneOfType([
      PropTypes.shape({
        successMetricsReportId: PropTypes.string,
      }),
      PropTypes.object,
    ]),
  }),
};

export default SuccessMetricsReport;
