/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTextInput, NxFormGroup } from '@sonatype/react-shared-components';

export default function LdapServerNameForm({ setServerName, serverName, autoFocus }) {
  return (
    <NxFormGroup label="Server Name" isRequired>
      <NxTextInput
        {...serverName}
        onChange={setServerName}
        validatable={true}
        id="serverName"
        aria-required={true}
        autoFocus={autoFocus}
      />
    </NxFormGroup>
  );
}

LdapServerNameForm.propTypes = {
  serverName: PropTypes.object,
  setServerName: PropTypes.func.isRequired,
  autoFocus: PropTypes.bool,
};
