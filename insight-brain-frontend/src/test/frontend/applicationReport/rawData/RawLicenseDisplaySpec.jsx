/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as enzymeUtils from '../../enzymeUtils';

import RawLicenseDisplay from '../../../../main/frontend/applicationReport/rawData/RawLicenseDisplay';

describe('RawLicenseDisplay', () => {
  const minimalProps = {
    license: {
      declaredLicenses: ['Public Domain'],
      observedLicenses: ['Apache-1.1', 'Apache-2.0'],
    },
  };

  const getShallowComponent = enzymeUtils.getShallowComponent(RawLicenseDisplay, minimalProps);

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  describe('declared licenses', () => {
    it('shows declared license', () => {
      const component = getShallowComponent();
      const message = component.find('strong');

      expect(message.text()).toBe('Public Domain');
    });

    it('shows default license message if declaredLicense is not provided', () => {
      const component = getShallowComponent({ license: { declaredLicenses: [] } });
      const message = component.find('strong');

      expect(message.text()).toBe('Not Declared');
    });
  });

  describe('observed licenses', () => {
    it('shows observed license', () => {
      const component = getShallowComponent();
      const observedLicenses = component.find('div').find('span');

      expect(observedLicenses.text()).toBe('Apache-1.1, Apache-2.0');
    });

    it("doesn't show observed license if not provided", () => {
      const component = getShallowComponent({ license: { observedLicenses: [] } });
      const observedLicenses = component.find('div').find('span');

      expect(observedLicenses.text()).toBe('');
    });
  });

  describe('title prop', () => {
    it('shows licenses if provided', () => {
      const tooltip = getShallowComponent();

      expect(tooltip).toHaveProp(
        'title',
        <Fragment>
          <div>
            <strong>Declared: </strong>Public Domain
          </div>
          <div>
            <strong>Observed: </strong>Apache-1.1, Apache-2.0
          </div>
        </Fragment>
      );
    });
  });
});
