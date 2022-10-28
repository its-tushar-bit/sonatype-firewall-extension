/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxToastContainer,
  NxToast,
  NxSuccessAlert,
  NxErrorAlert,
  NxInfoAlert,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { actions } from './toastSlice';
import { selectToastSlice } from './toastSelectors';
import { isEmpty } from 'ramda';

const ToastContainer = () => {
  const dispatch = useDispatch();

  const removeToast = (id) => {
    dispatch(actions.removeToast(id));
  };

  useEffect(() => {
    return () => {
      dispatch(actions.removeAllToasts());
    };
  }, []);

  const { toasts } = useSelector(selectToastSlice);

  if (isEmpty(toasts)) {
    return null;
  }

  function renderToast({ id, type, message }) {
    let ToastAlert = NxErrorAlert;

    switch (type) {
      case 'success':
        ToastAlert = NxSuccessAlert;
        break;
      case 'info':
        ToastAlert = NxInfoAlert;
        break;
      case 'warning':
        ToastAlert = NxWarningAlert;
        break;
    }
    return (
      <NxToast key={id} onClose={() => removeToast(id)}>
        <ToastAlert>{message}</ToastAlert>
      </NxToast>
    );
  }

  return <NxToastContainer>{toasts.map(renderToast)}</NxToastContainer>;
};
export default ToastContainer;
