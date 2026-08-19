/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTextInput, NxFormGroup } from '@sonatype/react-shared-components';

export default function EditLdapTimeouts({
  connectionTimeout,
  retryDelay,
  setConnection,
  setRetryDelay,
  handleNumberInput,
}) {
  return (
    <section className="nx-tile-subsection">
      <header className="nx-tile-subsection__header">
        <h3 className="nx-h3">Timeouts</h3>
      </header>
      <div className="nx-form-row">
        <NxFormGroup label="Connection">
          <NxTextInput
            {...connectionTimeout}
            onChange={(value) => handleNumberInput(value, setConnection)}
            id="connection"
            validatable={true}
          />
        </NxFormGroup>
        <NxFormGroup label="Retry Delay">
          <NxTextInput
            {...retryDelay}
            onChange={(value) => handleNumberInput(value, setRetryDelay)}
            id="retryDelay"
            validatable={true}
          />
        </NxFormGroup>
      </div>
    </section>
  );
}

const inputTypesProperties = {
  connectionTimeout: PropTypes.object,
  retryDelay: PropTypes.object,
};

export const editLdapTimeoutsActionTypes = {
  setConnection: PropTypes.func.isRequired,
  setRetryDelay: PropTypes.func.isRequired,
};

EditLdapTimeouts.propTypes = {
  ...inputTypesProperties,
  ...editLdapTimeoutsActionTypes,
  handleNumberInput: PropTypes.func.isRequired,
};
