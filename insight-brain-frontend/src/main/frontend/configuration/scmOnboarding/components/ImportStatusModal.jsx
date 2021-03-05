/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {Fragment} from 'react';
import * as PropTypes from 'prop-types';
import {NxModal, NxErrorAlert, NxSuccessAlert, NxButton} from '@sonatype/react-shared-components';
import {organizationPropType, repositoryPropType} from '../ScmOnboarding';
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
    setIsImportStatusDialogVisible
  } = props;

  const onCloseClicked = () => {
    setIsImportStatusDialogVisible(false);
  };

  const errorMessage = () => {
    return (
      <Fragment>
        {newlyImportedRepos && newlyImportedRepos.length > 0 &&
        <NxSuccessAlert>
          <strong>{newlyImportedRepos.length} Repositories</strong> were successfully imported to IQ Server
          as applications under the {selectedOrganization.organization.name} Organization.
        </NxSuccessAlert>
        }
        {failedImportCount > 0 &&
        <NxErrorAlert>
          {failedImportCount} Repositories failed to import.
        </NxErrorAlert>
        }
      </Fragment>
    );
  };

  return (
    <Fragment>
      {isImportStatusDialogVisible &&
      <NxModal onClose={onCloseClicked} id="scm-import-status-modal">
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <span>Import Status</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          {errorMessage()}
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
      }
    </Fragment>
  );
}

ImportStatusModal.propTypes = {
  isImportStatusDialogVisible: PropTypes.bool,
  newlyImportedRepos: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)).isRequired,
  failedImportCount: PropTypes.number,
  selectedOrganization: PropTypes.shape(organizationPropType),

  // actions
  setIsImportStatusDialogVisible: PropTypes.func.isRequired
};
