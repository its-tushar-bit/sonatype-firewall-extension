/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { selectSourceControlConfigurationSlice } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import { NxButton } from '@sonatype/react-shared-components';

const TestConfigurationButton = () => {
  const dispatch = useDispatch();

  const {
    scmConfigValidation: { loading },
  } = useSelector(selectSourceControlConfigurationSlice);

  const testConfiguration = () => dispatch(actions.validate());

  return (
    <NxButton id="test-source-control-config-button" variant="secondary" disabled={loading} onClick={testConfiguration}>
      Test Configuration
    </NxButton>
  );
};

export default TestConfigurationButton;
