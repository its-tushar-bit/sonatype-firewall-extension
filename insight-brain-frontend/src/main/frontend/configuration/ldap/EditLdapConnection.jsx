/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxSuccessAlert, NxErrorAlert } from '@sonatype/react-shared-components';
import EditLdapTimeouts, { editLdapTimeoutsActionTypes } from './connection/EditLdapTimeouts';
import EditLdapConnectionDetails, {
  editLdapConnectionDetailsActionTypes,
} from './connection/EditLdapConnectionDetails';
import EditLdapAuth, { editLdapAuthActionTypes } from './connection/EditLdapAuth';

export default function EditLdapConnection(props) {
  const {
    inputFields,
    successMessage,
    testConnectionErrorMessage,
    mustReenterPassword,
    setProtocol,
    setHostname,
    setPort,
    setSearchBase,
    setReferralIgnored,
    setConnection,
    setRetryDelay,
    setMethod,
    setSaslRealm,
    setUsername,
    setPassword,
    resetAlertMessages,
  } = props;
  const {
    authenticationMethod,
    saslRealm,
    systemUsername,
    systemPassword,
    connectionTimeout,
    retryDelay,
    protocol,
    hostname,
    port,
    searchBase,
    referralIgnored,
  } = inputFields;

  function handleNumberInput(value, cb) {
    const regExp = /^[0-9]*$/;
    if (!value || regExp.test(value)) {
      cb(value);
    }
  }

  return (
    <Fragment>
      <EditLdapConnectionDetails
        protocol={protocol}
        hostname={hostname}
        port={port}
        searchBase={searchBase}
        referralIgnored={referralIgnored}
        setProtocol={setProtocol}
        setHostname={setHostname}
        setPort={setPort}
        setSearchBase={setSearchBase}
        setReferralIgnored={setReferralIgnored}
        handleNumberInput={handleNumberInput}
      />
      <EditLdapAuth
        authenticationMethod={authenticationMethod}
        saslRealm={saslRealm}
        systemUsername={systemUsername}
        systemPassword={systemPassword}
        setMethod={setMethod}
        setSaslRealm={setSaslRealm}
        setUsername={setUsername}
        setPassword={setPassword}
        mustReenterPassword={mustReenterPassword}
      />
      <EditLdapTimeouts
        connectionTimeout={connectionTimeout}
        retryDelay={retryDelay}
        setConnection={setConnection}
        setRetryDelay={setRetryDelay}
        handleNumberInput={handleNumberInput}
      />
      {successMessage && <NxSuccessAlert onClose={resetAlertMessages}>{successMessage}</NxSuccessAlert>}
      {testConnectionErrorMessage && (
        <NxErrorAlert onClose={resetAlertMessages}>{testConnectionErrorMessage}</NxErrorAlert>
      )}
    </Fragment>
  );
}

EditLdapConnection.propTypes = {
  inputFields: PropTypes.object,
  successMessage: PropTypes.string,
  mustReenterPassword: PropTypes.bool.isRequired,
  testConnectionErrorMessage: PropTypes.string,
  resetAlertMessages: PropTypes.func.isRequired,
  ...editLdapConnectionDetailsActionTypes,
  ...editLdapAuthActionTypes,
  ...editLdapTimeoutsActionTypes,
};
