/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxStatefulForm,
  NxFormGroup,
  NxTextInput,
  NxRadio,
  NxFieldset,
  NxWarningAlert,
  NxModal,
} from '@sonatype/react-shared-components';
import React, { useEffect } from 'react';
import IqOrgAppPicker from '../../../components/iqOrgAppPicker/IqOrgAppPicker';
import * as PropTypes from 'prop-types';

function AddSuccessMetricsReport({
  dismiss,
  close,
  loading,
  organizations,
  applications,
  loadError,
  loadOrgsAndApps,
  toggleOrgsApps,
  selectedOrgsAndApps,
  reportName,
  setReportName,
  setIncludeLatestData,
  includeLatestData,
  isAllApplications,
  setIsAllApplications,
  submit,
  submitError,
  submitMaskState,
  reports,
  ownersMap,
  topParentOrganizationId,
}) {
  useEffect(() => {
    loadOrgsAndApps();
  }, []);

  function validationError() {
    const { trimmedValue, validationErrors } = reportName;

    if (
      !trimmedValue ||
      (validationErrors || []).length ||
      (!isAllApplications && !selectedOrgsAndApps.applications.size && !selectedOrgsAndApps.organizations.size)
    ) {
      return 'Unable to submit: fields with invalid or missing data.';
    }
    return null;
  }

  return (
    <NxModal onClose={dismiss} variant="narrow">
      <NxStatefulForm
        id="add-success-metrics-report"
        onCancel={dismiss}
        loading={loading}
        loadError={loadError}
        onSubmit={() => submit(close)}
        submitError={submitError}
        submitMaskState={submitMaskState}
        validationErrors={validationError()}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Add Report</h2>
        </header>
        <div className="nx-modal-content">
          <NxFormGroup label="Name" isRequired={true}>
            <NxTextInput
              {...reportName}
              validatable={true}
              onChange={(value) => setReportName({ value, reports })}
              placeholder="Report name"
              id="add-success-metrics-report-name"
            />
          </NxFormGroup>
          <NxFieldset label="Aggregation data" isRequired={true}>
            <NxRadio
              name="includeLatestData"
              value="0"
              isChecked={!includeLatestData}
              onChange={() => setIncludeLatestData(false)}
              id="add-success-metrics-monthly"
            >
              Only full calendar weeks and months
            </NxRadio>
            <NxRadio
              name="includeLatestData"
              value="1"
              isChecked={includeLatestData}
              onChange={() => setIncludeLatestData(true)}
              id="add-success-metrics-latest"
            >
              Including most recent evaluations
            </NxRadio>
            {includeLatestData && (
              <NxWarningAlert id="add-success-metrics-perf-warning">
                Data for incomplete months and weeks will skew averages. May be slow for large data sets.
              </NxWarningAlert>
            )}
          </NxFieldset>
          <NxFieldset label="Scope" isRequired={true}>
            <NxRadio
              name="isAllApplications"
              value="1"
              isChecked={isAllApplications}
              onChange={() => setIsAllApplications(true)}
              id="add-success-metrics-report-all-applications"
            >
              All Applications
            </NxRadio>
            <NxRadio
              name="isAllApplications"
              value="0"
              isChecked={!isAllApplications}
              onChange={() => setIsAllApplications(false)}
              id="add-success-metrics-report-custom"
            >
              Custom
            </NxRadio>
            {!isAllApplications && (
              <IqOrgAppPicker
                organizations={organizations}
                applications={applications}
                onChange={toggleOrgsApps}
                selectedApplications={selectedOrgsAndApps.applications}
                selectedOrganizations={selectedOrgsAndApps.organizations}
                id="add-success-metrics-report-orgs-apps-filter"
                ownersMap={ownersMap}
                topParentOrganizationId={topParentOrganizationId}
              />
            )}
          </NxFieldset>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

AddSuccessMetricsReport.propTypes = {
  dismiss: PropTypes.func.isRequired,
  close: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  organizations: PropTypes.array.isRequired,
  applications: PropTypes.array.isRequired,
  loadError: PropTypes.string,
  loadOrgsAndApps: PropTypes.func.isRequired,
  toggleOrgsApps: PropTypes.func.isRequired,
  selectedOrgsAndApps: PropTypes.object.isRequired,
  reportName: PropTypes.object.isRequired,
  setReportName: PropTypes.func.isRequired,
  setIncludeLatestData: PropTypes.func.isRequired,
  includeLatestData: PropTypes.bool.isRequired,
  isAllApplications: PropTypes.bool.isRequired,
  setIsAllApplications: PropTypes.func.isRequired,
  submit: PropTypes.func.isRequired,
  submitError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  reports: PropTypes.array.isRequired,
  ownersMap: PropTypes.object,
  topParentOrganizationId: PropTypes.string,
};

export default AddSuccessMetricsReport;
