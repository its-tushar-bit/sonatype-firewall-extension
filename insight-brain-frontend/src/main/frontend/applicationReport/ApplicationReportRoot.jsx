/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { UIView } from '@uirouter/react';
import { useDispatch } from 'react-redux';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { setReportParameters } from 'MainRoot/applicationReport/applicationReportActions';

export default function ApplicationReportRoot() {
  const dispatch = useDispatch();
  const routerState = useRouterState();
  const params = routerState.params;

  useEffect(() => {
    dispatch(
      setReportParameters(
        params.publicId,
        params.scanId,
        !!params.unknownjs,
        !!params.embeddable,
        params.policyViolationId,
        params.componentHash,
        params.tabId,
        true
      )
    );
  }, [
    dispatch,
    params.publicId,
    params.scanId,
    params.unknownjs,
    params.embeddable,
    params.policyViolationId,
    params.componentHash,
    params.tabId,
  ]);

  return <UIView />;
}
