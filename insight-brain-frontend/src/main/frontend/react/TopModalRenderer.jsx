/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ReactDOM from 'react-dom';
import { useEffect, useRef } from 'react';

const MODAL_CONTAINER_NODE_ID = 'modal-view';
/*
 * Component used to render modals on top of all the content of the main application, in the special cases where normal
 * positioning inside the parent component does not work.
 * It should NOT be used to render modals over other modals.
 */
export default function TopModalRenderer({ children }) {
  const modalContainer = document.getElementById(MODAL_CONTAINER_NODE_ID);
  const modalParentRef = useRef(null);
  modalParentRef.current = modalParentRef.current || document.createElement('div');

  const mountModal = () => {
    modalContainer.appendChild(modalParentRef.current);
    // returns unmounting function
    return () => modalContainer.removeChild(modalParentRef.current);
  };

  useEffect(mountModal, []);

  return ReactDOM.createPortal(children, modalParentRef.current);
}
