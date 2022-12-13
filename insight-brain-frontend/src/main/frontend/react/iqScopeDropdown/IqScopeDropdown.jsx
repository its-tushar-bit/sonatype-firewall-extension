/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxFormSelect } from '@sonatype/react-shared-components';

function IqScopeDropdown({
  id,
  availableScopes,
  onChangeHandler,
  getOptionText,
  currentValue,
  withHiddenOption = false,
}) {
  const onScopeChange = (event) => {
    onChangeHandler(event.currentTarget.value);
  };

  return (
    <NxFormSelect id={id} onChange={onScopeChange} value={currentValue}>
      {withHiddenOption && <option hidden>Scope Level</option>}
      {availableScopes &&
        availableScopes.map((scope) => (
          <option key={scope.id || scope.ownerId} value={scope.id || scope.ownerId}>
            {getOptionText(scope)}
          </option>
        ))}
    </NxFormSelect>
  );
}

IqScopeDropdown.propTypes = {
  id: PropTypes.string,
  availableScopes: PropTypes.array,
  onChangeHandler: PropTypes.func,
  getOptionText: PropTypes.func,
  currentValue: PropTypes.string,
  withHiddenOption: PropTypes.bool,
};

export default IqScopeDropdown;
