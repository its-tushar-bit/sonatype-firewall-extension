/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxModal, NxSubmitMask, NxWarningAlert, NxTextLink } from '@sonatype/react-shared-components';

import UserTokenDisplay, { userTokenType } from './UserTokenDisplay';
import LoadWrapper from '../../../../react/LoadWrapper';
import LoadError from '../../../../react/LoadError';

export default function UserTokenModal(props) {
  const {
    // action creators
    checkUserTokenExistence,
    generateUserToken,
    deleteUserToken,
    hideUserTokenModal,
    // loading flags
    checkUserTokenLoading,
    generateUserTokenLoading,
    deleteUserTokenLoading,
    // errors
    checkUserTokenError,
    generateUserTokenError,
    deleteUserTokenError,
    // data
    userToken,
  } = props;

  useEffect(() => {
    if (userToken === null) {
      checkUserTokenExistence();
    }
  }, [userToken]);

  const primaryAction = userToken === false ? generateUserToken : deleteUserToken;
  const primaryActionLabel = userToken === false ? 'Generate User Token' : 'Delete User Token';
  const primaryActionId = userToken === false ? 'generate-user-token' : 'delete-user-token';
  // Show primary action button only if page is not a newly-generated token & is not loading & has no errors
  const showPrimaryAction =
    typeof userToken === 'boolean' &&
    !checkUserTokenLoading &&
    !generateUserTokenLoading &&
    !deleteUserTokenLoading &&
    !checkUserTokenError &&
    !generateUserTokenError &&
    !deleteUserTokenError;

  return (
    <NxModal variant="narrow" id="user-token-modal" className="iq-user-token-modal" onClose={hideUserTokenModal}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">
          <span>Manage User Token</span>
        </h2>
      </header>
      <div className="nx-modal-content">
        {deleteUserTokenLoading != null && <NxSubmitMask message="Deleting…" success={deleteUserTokenLoading} />}
        {generateUserTokenLoading != null && <NxSubmitMask message="Generating…" success={generateUserTokenLoading} />}
        <LoadWrapper loading={checkUserTokenLoading} error={checkUserTokenError} retryHandler={checkUserTokenExistence}>
          <p className="iq-user-token-modal__highlight-paragraph">
            Your user token credentials are only available upon creation. You can not recover them later.
          </p>
          <p className="iq-user-token-modal__info-paragraph">
            Should you forget or lose your user token credentials, you should delete your user token and create a new
            one. To learn more about User Tokens please see the{' '}
            <NxTextLink external href="http://links.sonatype.com/products/nxiq/doc/user-tokens">
              help documentation.
            </NxTextLink>
          </p>
          {userToken === true && (
            <NxWarningAlert id="user-token-modal-token-exists-alert">
              A user token already exists for this user.
              <br />
              To create a new token please delete the existing one.
            </NxWarningAlert>
          )}
          {userToken && userToken !== true && <UserTokenDisplay userToken={userToken} />}
        </LoadWrapper>
      </div>
      <footer className="nx-footer">
        {deleteUserTokenError && (
          <LoadError
            error={deleteUserTokenError}
            retryHandler={deleteUserToken}
            titleMessage="An error occurred deleting the token."
          />
        )}
        {generateUserTokenError && (
          <LoadError
            error={generateUserTokenError}
            retryHandler={generateUserToken}
            titleMessage="An error occurred generating the token."
          />
        )}
        <div className="nx-btn-bar">
          {showPrimaryAction && (
            <NxButton id={primaryActionId} variant="primary" onClick={primaryAction}>
              {primaryActionLabel}
            </NxButton>
          )}
          <NxButton id="user-token-modal-cancel" variant="tertiary" onClick={hideUserTokenModal}>
            Close
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

UserTokenModal.propTypes = {
  userToken: PropTypes.oneOfType([PropTypes.shape(userTokenType), PropTypes.bool]),
  hideUserTokenModal: PropTypes.func.isRequired,
  checkUserTokenExistence: PropTypes.func.isRequired,
  checkUserTokenLoading: PropTypes.bool,
  checkUserTokenError: PropTypes.object,
  generateUserToken: PropTypes.func.isRequired,
  generateUserTokenLoading: PropTypes.bool,
  generateUserTokenError: PropTypes.object,
  deleteUserToken: PropTypes.func.isRequired,
  deleteUserTokenLoading: PropTypes.bool,
  deleteUserTokenError: PropTypes.object,
};
