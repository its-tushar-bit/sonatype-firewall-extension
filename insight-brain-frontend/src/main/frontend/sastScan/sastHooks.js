/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useDispatch, useSelector } from 'react-redux';
import { useEffect } from 'react';
import { actions } from 'MainRoot/sastScan/sastScanSlice';
import { selectSastScanSlice } from 'MainRoot/sastScan/sastSelectors';

export default function useLoadSastScan({ applicationPublicId, sastScanId }) {
  const dispatch = useDispatch();

  const doLoad = () => dispatch(actions.loadSastScan({ applicationPublicId, sastScanId }));

  useEffect(() => {
    doLoad();
  }, []);

  const selectorVal = useSelector(selectSastScanSlice);

  return { ...selectorVal, reload: doLoad };
}
