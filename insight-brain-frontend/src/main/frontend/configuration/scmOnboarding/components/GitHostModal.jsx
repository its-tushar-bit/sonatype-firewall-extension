/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxModal, NxStatefulForm, NxTextInput, NxErrorAlert, NxInfoAlert } from '@sonatype/react-shared-components';
import { organizationPropType, textInputPropType } from '../scmPropTypes';
import { hasValidationErrors } from '../../../util/validationUtil';
import { validateHostUrl } from '../utils/validators';
import CredentialsError from './CredentialsError';
/*
 The dialog which prompts users for a base host URL
 */
export default function GitHostModal(props) {
  const {
    scmProvider,
    selectedOrganization,
    currentHostUrlState,
    defaultHostUrl,
    setCurrentHostUrl,
    validateScmHostUrl,
    loadRepositories,
    loadRepositoriesErrorCode,
    isGitHostDialogVisible,
    isGitHostNeeded,
    setShowHostDialog,
    setIsGitHostNeeded,
    errorText,
    $state,
  } = props;

  const onCancelClicked = () => {
    setShowHostDialog(false);
  };

  const onContinueClicked = () => {
    setShowHostDialog(false);
    setIsGitHostNeeded(false);
    loadRepositories(selectedOrganization.organization.id, currentHostUrlState.value);
  };

  function validateAndSetCurrentHostUrl(value) {
    setCurrentHostUrl(value);
    if (value && !hasValidationErrors(validateHostUrl(value))) {
      validateScmHostUrl(scmProvider, value);
    }
  }

  const title = () => {
    if (loadRepositoriesErrorCode === null) {
      return 'SCM Server Needed';
    }
    switch (loadRepositoriesErrorCode) {
      case 'SCM_AUTHN_FAILURE':
        return 'Authentication Error';
      case 'SCM_AUTHZ_FAILURE':
        return 'Authorization Error';
      case 'SCM_UNKNOWN_HOST_FAILURE':
        return 'Unknown Host Error';
      default:
        return 'Connection Error';
    }
  };

  const errorMessage = () => {
    return isGitHostNeeded ? (
      <NxInfoAlert>{errorText}</NxInfoAlert>
    ) : loadRepositoriesErrorCode ? (
      <NxErrorAlert>
        <CredentialsError
          $state={$state}
          errorCode={loadRepositoriesErrorCode}
          selectedOrganization={selectedOrganization}
          scmProvider={scmProvider}
        />
      </NxErrorAlert>
    ) : (
      <NxErrorAlert>{errorText}</NxErrorAlert>
    );
  };

  return (
    <Fragment>
      {isGitHostDialogVisible && (
        <NxModal onClose={onCancelClicked}>
          <header className="nx-modal-header">
            <h2 className="nx-h2">
              <span>{title()}</span>
            </h2>
          </header>
          <div className="nx-modal-content">
            {errorMessage()}
            <NxStatefulForm
              onSubmit={onContinueClicked}
              onCancel={onCancelClicked}
              submitBtnText="Continue"
              validationErrors={currentHostUrlState.validationErrors}
            >
              <label className="nx-label">
                <span className="nx-label__text">Host URL</span>
                <NxTextInput
                  id="iq-scm-default-host-field"
                  {...currentHostUrlState}
                  onChange={validateAndSetCurrentHostUrl}
                  validatable={true}
                  placeholder={defaultHostUrl}
                />
              </label>
            </NxStatefulForm>
          </div>
        </NxModal>
      )}
    </Fragment>
  );
}

GitHostModal.propTypes = {
  loadRepositories: PropTypes.func.isRequired,
  scmProvider: PropTypes.string,
  // textInputPropType is implied required, but this val is optional
  currentHostUrlState: PropTypes.oneOfType([PropTypes.object, PropTypes.shape(textInputPropType)]),
  defaultHostUrl: PropTypes.string,
  setCurrentHostUrl: PropTypes.func.isRequired,
  validateScmHostUrl: PropTypes.func.isRequired,
  selectedOrganization: PropTypes.shape(organizationPropType),
  isGitHostDialogVisible: PropTypes.bool,
  isGitHostNeeded: PropTypes.bool,
  setShowHostDialog: PropTypes.func,
  errorText: PropTypes.object,
  setIsGitHostNeeded: PropTypes.func,
  $state: PropTypes.object.isRequired,
  loadRepositoriesErrorCode: PropTypes.string,
};
