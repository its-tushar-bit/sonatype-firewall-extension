/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxModal, NxErrorAlert, NxSuccessAlert, NxButton } from '@sonatype/react-shared-components';
import { organizationPropType, repositoryPropType } from '../scmPropTypes';
import ReportsCta from './ReportsCta';
/*
 The dialog which provides the user with feedback on the result of their
 repository import request
 */
export default function ImportStatusModal(props) {
  const {
    isImportStatusDialogVisible,
    selectedOrganization,
    newlyImportedRepos,
    failedImportCount,
    failedRepos,
    setIsImportStatusDialogVisible,
  } = props;

  const onCloseClicked = () => {
    setIsImportStatusDialogVisible(false);
  };

  const repositoryPluralized = (count) => (count > 1 ? 'repositories' : 'repository');
  const wasPlural = (count) => (count > 1 ? 'were' : 'was');

  const statusMessage = () => {
    if (failedImportCount > 0) {
      return (
        <NxErrorAlert>
          {failedImportCount} {repositoryPluralized(failedImportCount)} had an error. See details below.
        </NxErrorAlert>
      );
    }
    if (newlyImportedRepos && newlyImportedRepos.length > 0) {
      return (
        <NxSuccessAlert>
          <strong>All repositories</strong> were successfully imported. See details below.
        </NxSuccessAlert>
      );
    }

    return <Fragment />;
  };

  const successDetails = () => {
    if (newlyImportedRepos && newlyImportedRepos.length > 0) {
      return (
        <li className="scm-import-detail-success">
          <strong>
            {newlyImportedRepos.length} {repositoryPluralized(newlyImportedRepos.length)}
          </strong>{' '}
          {wasPlural(newlyImportedRepos.length)} successfully imported to IQ Server as applications under the{' '}
          {selectedOrganization.organization.name} Organization.
        </li>
      );
    }
    return <Fragment />;
  };

  const errorDetails = () => {
    if (failedImportCount === 0) {
      return null;
    }

    return (
      <li className="scm-import-detail-error">
        <strong>
          {failedImportCount} {repositoryPluralized(failedImportCount)}
        </strong>{' '}
        had an error
        <ul className="scm-import-error-detail-list">
          {failedRepos.map(({ repository, errorMessage }) => (
            <li key={repository.httpCloneUrl} className="scm-import-error-detail-item">
              <strong>
                {repository.namespace}/{repository.project}
              </strong>
              {errorMessage && <Fragment> failed with {errorMessage}</Fragment>}
            </li>
          ))}
        </ul>
      </li>
    );
  };

  return (
    <Fragment>
      {isImportStatusDialogVisible && (
        <NxModal onClose={onCloseClicked} id="scm-import-status-modal">
          <header className="nx-modal-header">
            <h2 className="nx-h2">
              <span>Import Status</span>
            </h2>
          </header>
          <div className="nx-modal-content">
            {statusMessage()}
            <strong>Details</strong>
            <ul className="scm-import-details">
              {successDetails()}
              {errorDetails()}
            </ul>
            <p className="nx-p">
              You may continue the importing process or view the applications you just created on the reports page.
            </p>
          </div>
          <footer className="nx-footer">
            <div className="nx-btn-bar">
              <ReportsCta {...props} id="scm-success-gotoreports" />
              <NxButton id="scm-continue-importing" onClick={onCloseClicked} variant="primary">
                Continue Importing
              </NxButton>
            </div>
          </footer>
        </NxModal>
      )}
    </Fragment>
  );
}

ImportStatusModal.propTypes = {
  isImportStatusDialogVisible: PropTypes.bool,
  newlyImportedRepos: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)).isRequired,
  failedImportCount: PropTypes.number,
  failedRepos: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  selectedOrganization: PropTypes.shape(organizationPropType),

  // actions
  setIsImportStatusDialogVisible: PropTypes.func.isRequired,
};
