/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { actions } from './solutionSwitcherSlice';
import { useDispatch, useSelector } from 'react-redux';
import { SolutionSwitcher } from '@sonatype/solution-switcher-react-component';
import { prop } from 'ramda';
import { NxTooltip } from '@sonatype/react-shared-components';

const SolutionSwitcherContainer = () => {
  const dispatch = useDispatch();
  const { licensedSolutions, loading, loadError } = useSelector(prop('solutionSwitcher'));

  useEffect(() => {
    dispatch(actions.fetchLicensedSolutions());
  }, []);

  return (
    <NxTooltip title="Sonatype Solutions">
      <span id="iq-solution-switcher">
        <SolutionSwitcher size="small" licensedSolutions={licensedSolutions} loading={loading} error={loadError} />
      </span>
    </NxTooltip>
  );
};

export default SolutionSwitcherContainer;
