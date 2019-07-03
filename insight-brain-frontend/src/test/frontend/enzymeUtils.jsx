import React from 'react';
import { shallow } from 'enzyme';

export const getShallowComponent = (Component, minimalProps) => function getShallowComponent(additionalProps) {
  return shallow(<Component { ...minimalProps } { ...additionalProps } />);
};
