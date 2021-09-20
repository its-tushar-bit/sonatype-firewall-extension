/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTextInput, NxFormGroup, NxToggle } from '@sonatype/react-shared-components';

export const protocols = ['LDAP', 'LDAPS'];

export default function EditLdapConnectionDetails({
  protocol,
  hostname,
  port,
  searchBase,
  referralIgnored,
  setProtocol,
  setHostname,
  setPort,
  setSearchBase,
  setReferralIgnored,
  handleNumberInput,
}) {
  const handleProtocol = (e) => {
    setProtocol(e.target.value);
  };
  return (
    <section className="nx-tile-subsection">
      <header className="nx-tile-subsection__header">
        <h3 className="nx-h3">Connection Details</h3>
      </header>
      <div className="nx-form-row">
        <NxFormGroup label="Protocol" isRequired>
          <select className="nx-form-select" id="protocol-selector" value={protocol} onChange={handleProtocol}>
            {protocols.map((protocol) => (
              <option key={protocol} value={protocol}>
                {protocol}
              </option>
            ))}
          </select>
        </NxFormGroup>
        <NxFormGroup label="Hostname" isRequired>
          <NxTextInput {...hostname} onChange={setHostname} validatable={true} id="hostname" aria-required={true} />
        </NxFormGroup>
      </div>
      <div className="nx-form-row">
        <NxFormGroup label="Port" isRequired>
          <NxTextInput
            {...port}
            onChange={(value) => handleNumberInput(value, setPort)}
            validatable={true}
            id="port"
            aria-required={true}
          />
        </NxFormGroup>
        <NxFormGroup label="Search Base" isRequired>
          <NxTextInput
            {...searchBase}
            onChange={setSearchBase}
            validatable={true}
            id="searchBase"
            aria-required={true}
          />
        </NxFormGroup>
      </div>
      <NxToggle
        id="ignore-referrals-toggle"
        className="nx-toggle--no-gap"
        onChange={setReferralIgnored}
        isChecked={referralIgnored}
      >
        Ignore Referrals
      </NxToggle>
    </section>
  );
}

const inputTypesProperties = {
  protocol: PropTypes.string.isRequired,
  hostname: PropTypes.object,
  port: PropTypes.object,
  searchBase: PropTypes.object,
  referralIgnored: PropTypes.bool,
};

export const editLdapConnectionDetailsActionTypes = {
  setProtocol: PropTypes.func.isRequired,
  setHostname: PropTypes.func.isRequired,
  setPort: PropTypes.func.isRequired,
  setSearchBase: PropTypes.func.isRequired,
  setReferralIgnored: PropTypes.func.isRequired,
};

EditLdapConnectionDetails.propTypes = {
  ...inputTypesProperties,
  ...editLdapConnectionDetailsActionTypes,
  handleNumberInput: PropTypes.func.isRequired,
};
