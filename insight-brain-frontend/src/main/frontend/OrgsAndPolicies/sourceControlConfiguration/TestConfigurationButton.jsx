/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch } from 'react-redux';
import { NxButton } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';

const TestConfigurationButton = ({ isDisabled = false }) => {
  const dispatch = useDispatch();
  const testConfiguration = () => dispatch(actions.validate());

  return (
    <NxButton
      id="test-source-control-config-button"
      type="button"
      variant="secondary"
      disabled={isDisabled}
      onClick={testConfiguration}
    >
      Test Configuration
    </NxButton>
  );
};

export default TestConfigurationButton;
