/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mount, shallow } from 'enzyme';
import LoadWrapper from '../../main/frontend/react/LoadWrapper';

export const getShallowComponent = (Component, minimalProps) =>
  function getShallowComponent(additionalProps) {
    return shallow(<Component {...minimalProps} {...additionalProps} />);
  };

export const getMountedComponent = (Component, minimalProps, mountOpts) =>
  function getMounted(additionalProps) {
    return mount(<Component {...minimalProps} {...additionalProps} />, mountOpts);
  };

export const getLoadWrapperChildren = function getLoadWrapperChildren(pageShallowWrapper) {
  return shallow(React.createElement(pageShallowWrapper.find(LoadWrapper).prop('children')));
};
