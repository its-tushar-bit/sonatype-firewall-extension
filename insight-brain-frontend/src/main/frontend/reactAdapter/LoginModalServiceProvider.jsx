/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

/*
  High-order component to translate angular LoginModalService's necessary methods to props for the LoginModal.
  Decouples the modal from angular dependency for when future migrations obviate this HOC.
*/
export default function withLoginModalService(WrappedComponent) {
  function WithLoginModalService({ LoginModalService, ...props }) {
    return (
      <WrappedComponent
        {...props}
        onDismiss={LoginModalService.dismiss}
        onSubmit={LoginModalService.onSubmit}
        onClickSSO={LoginModalService.onClickSSO}
      />
    );
  }
  WithLoginModalService.displayName = `withLoginModalService(${getDisplayName(WrappedComponent)})`;
  return WithLoginModalService;
}

withLoginModalService.propTypes = {
  WrappedComponent: PropTypes.func.isRequired,
};

function getDisplayName(WrappedComponent) {
  return WrappedComponent.displayName || WrappedComponent.name || 'AnonymousComponent';
}
