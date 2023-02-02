/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxButton, NxFontAwesomeIcon, useToggle } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';
import LdapTabs from './LdapTabs';
import LdapServerNameForm from './LdapServerNameForm';
import LdapRemoveServerModal, { ldapRemoveServerModalPropTypes } from './LdapRemoveServerModal';
import CheckLogin from './checkLogin/CheckLogin';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const getValidationMessage = ({ isDirty, validationError, mustReenterPassword }) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  if (mustReenterPassword) {
    return 'The password must be given when updating the hostname or port for a connection that uses authentication.';
  }

  return validationError;
};

export default function withLdapHeader(WrappedComponent, { formId }) {
  function WithLdapHeader(props) {
    const {
      stateGo,
      inputFields,
      setServerName,
      saveConnection,
      saveUserAndGroupSettings,
      router,
      loading,
      loadError,
      isDirty,
      validationError,
      maybeLoadEditPage,
      testConnection,
      saveMaskState,
      saveError,
      submitMaskMessage,
      mustReenterPassword,
      resetAllNotifications,
      removeServer,
      removeError,
      removeMaskState,
      resetRemoveServerError,
      resetForm,
      toggleUserMappingModalIsOpen,
      checkLoginProps,
      checkLogin,
      setInputField,
      clearCheckLoginAlerts,
      resetCheckLoginModal,
    } = props;
    const {
      currentParams: { ldapId },
      currentState: { name: currentTab },
    } = router;

    const [showModal, setShowModal] = useState(false);
    const [showCheckLoginModal, toggleShowCheckLoginModal] = useToggle();

    const { serverName } = inputFields;
    const isConnectionTab = currentTab === 'edit-ldap-connection';
    const isUserMappingTab = currentTab === 'edit-ldap-usermapping';

    useEffect(() => {
      doLoad();
      return () => {
        resetAllNotifications();
      };
    }, []);

    function getAdditionalButtons() {
      if (isConnectionTab) {
        return (
          <NxButton
            variant="tertiary"
            type="button"
            id="test-connection"
            onClick={() => testConnection()}
            disabled={validationError}
          >
            Test Connection
          </NxButton>
        );
      }
      if (isUserMappingTab) {
        return (
          <React.Fragment>
            <NxButton
              variant="tertiary"
              type="button"
              id="check-user-mapping"
              onClick={toggleUserMappingModalIsOpen}
              disabled={validationError}
            >
              Check User Mapping
            </NxButton>
            <NxButton
              variant="tertiary"
              type="button"
              onClick={toggleShowCheckLoginModal}
              disabled={validationError}
              id="check-login"
            >
              Check Login
            </NxButton>
          </React.Fragment>
        );
      }
    }

    function doLoad() {
      return maybeLoadEditPage({ ldapId, currentTab });
    }

    function onCloseRemoveModal() {
      setShowModal(false);
      resetRemoveServerError();
    }

    function submit() {
      return isConnectionTab ? saveConnection() : saveUserAndGroupSettings();
    }

    function cancel() {
      stateGo('ldap-list');
      return resetForm();
    }

    return (
      <Fragment>
        <main className="nx-page-main" id="ldap-configuration-editor">
          <MenuBarBackButton stateName="ldap-list" />
          <div className="nx-page-title">
            <h1 className="nx-h1" id="user-title">
              Edit Server
            </h1>
          </div>
          <section className="nx-tile">
            <NxStatefulForm
              id={formId}
              autoComplete="off"
              loadError={loadError}
              loading={loading}
              doLoad={doLoad}
              onSubmit={submit}
              submitBtnText="Save"
              submitError={saveError}
              submitMaskMessage={submitMaskMessage}
              submitMaskState={saveMaskState}
              validationErrors={getValidationMessage({ isDirty, validationError, mustReenterPassword })}
              onCancel={cancel}
              additionalFooterBtns={getAdditionalButtons()}
            >
              <div className="nx-tile-content">
                <NxButton
                  variant="tertiary"
                  type="button"
                  id="remove-server"
                  className="iq-ldap-server-remove"
                  onClick={() => setShowModal(true)}
                >
                  <NxFontAwesomeIcon icon={faTrashAlt} />
                  <span>Remove Server</span>
                </NxButton>
                <LdapServerNameForm serverName={serverName} setServerName={setServerName} />
                <LdapTabs id={ldapId} currentTab={currentTab} stateGo={stateGo} />
                <WrappedComponent {...props} ldapId={ldapId} />
              </div>
            </NxStatefulForm>
          </section>
        </main>
        {showModal && (
          <LdapRemoveServerModal
            ldapId={ldapId}
            closeModal={onCloseRemoveModal}
            removeServer={() => removeServer(ldapId)}
            removeMaskState={removeMaskState}
            removeError={removeError}
          />
        )}
        {showCheckLoginModal && (
          <CheckLogin
            closeModal={toggleShowCheckLoginModal}
            ldapId={ldapId}
            checkLogin={checkLogin}
            setInputField={setInputField}
            clearCheckLoginAlerts={clearCheckLoginAlerts}
            resetCheckLoginModal={resetCheckLoginModal}
            {...checkLoginProps}
          />
        )}
      </Fragment>
    );
  }

  WithLdapHeader.displayName = `withLdapHeader(${getDisplayName(WrappedComponent)})`;

  WithLdapHeader.propTypes = {
    stateGo: PropTypes.func.isRequired,
    resetAllNotifications: PropTypes.func.isRequired,
    validationError: PropTypes.string,
    loading: PropTypes.bool.isRequired,
    mustReenterPassword: PropTypes.bool,
    saveMaskState: PropTypes.bool,
    submitMaskMessage: PropTypes.string,
    loadError: PropTypes.string,
    saveError: PropTypes.string,
    isDirty: PropTypes.bool,
    inputFields: PropTypes.shape({
      serverName: PropTypes.object,
    }),
    setServerName: PropTypes.func.isRequired,
    testConnection: PropTypes.func.isRequired,
    saveConnection: PropTypes.func.isRequired,
    saveUserAndGroupSettings: PropTypes.func.isRequired,
    maybeLoadEditPage: PropTypes.func.isRequired,
    router: PropTypes.shape({
      currentParams: PropTypes.shape({
        ldapId: PropTypes.string,
      }),
      currentState: PropTypes.shape({
        name: PropTypes.string,
      }),
    }),
    ...ldapRemoveServerModalPropTypes,
    checkLoginProps: PropTypes.object,
    setInputField: PropTypes.func,
    checkLogin: PropTypes.func,
    clearCheckLoginAlerts: PropTypes.func,
    resetCheckLoginModal: PropTypes.func,
  };

  return WithLdapHeader;
}

withLdapHeader.propTypes = {
  WrappedComponent: PropTypes.func.isRequired,
  data: PropTypes.shape({
    formId: PropTypes.string.isRequired,
  }),
};

function getDisplayName(WrappedComponent) {
  return WrappedComponent.displayName || WrappedComponent.name || 'AnonymousComponent';
}
