/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';

/***
 * Decorates a deprecated component with a console warning to discourage further usage.
 * @param WrappedComponent the deprecated component
 * @param message optional details on what alternative should be used. Eg. "Please use NxTextLink from RSC instead"
 */
export default function withDeprecated(WrappedComponent, message = '') {
  const DeprecatedContainer = (props) => {
    useEffect(() => {
      if (process.env.NODE_ENV === 'development') {
        console.warn(`${WrappedComponent.name} is deprecated.`, message);
      }
    }, []);

    return <WrappedComponent {...props} />;
  };

  DeprecatedContainer.displayName = `Deprecated${WrappedComponent.name}Container`;
  return DeprecatedContainer;
}
