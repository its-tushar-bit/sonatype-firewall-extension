/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxPageMain, NxErrorAlert } from '@sonatype/react-shared-components';
import React2ShellHeader from './React2ShellHeader';
import React2ShellAbout from './React2ShellAbout';
import { actions } from './react2shellSlice';
import { selectDownloadError } from './react2shellSelectors';


export default function React2ShellPage() {
  const dispatch = useDispatch();
  const error = useSelector(selectDownloadError);

  return (
    <NxPageMain className="iq-react2shell-page">
      <React2ShellHeader />
      {error && (
        <NxErrorAlert onClose={() => dispatch(actions.clearDownloadError())}>
          {error}
        </NxErrorAlert>
      )}
      <React2ShellAbout />
    </NxPageMain>
  );
}
