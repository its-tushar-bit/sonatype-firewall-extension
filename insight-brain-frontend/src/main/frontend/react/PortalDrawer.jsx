/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useRef } from 'react';
import ReactDOM from 'react-dom';
import { NxDrawer } from '@sonatype/react-shared-components';

const NX_PAGE_SELECTOR = '.nx-page';

/**
 * PortalDrawer component renders a NxDrawer inside a portal targeting the '.nx-page' element.
 * This allows the drawer to be rendered outside of the normal DOM hierarchy, which is required
 * due to the placement of the new footer.
 * @param {Object} props - The properties passed to the component.
 * @param {React.ReactNode} props.children - The content to be displayed inside the drawer.
 * @returns {React.ReactPortal|null} - Returns a React portal if the target element exists,
 * otherwise returns null.
 */

export default function PortalDrawer({ children, ...drawerProps }) {
  const portalTargetRef = useRef(null);
  const portalTarget = document.querySelector(NX_PAGE_SELECTOR);
  portalTargetRef.current = portalTargetRef.current || portalTarget;

  if (!portalTarget || !portalTargetRef.current) {
    return null;
  }

  return ReactDOM.createPortal(<NxDrawer {...drawerProps}>{children}</NxDrawer>, portalTargetRef.current);
}
