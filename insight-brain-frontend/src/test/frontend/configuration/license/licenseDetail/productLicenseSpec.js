/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxErrorAlert, NxLoadWrapper, NxSubmitMask } from '@sonatype/react-shared-components';
import NoProductLicense from '../../../../../main/frontend/configuration/license/contents/NoProductLicense';
import ProductLicenseInfo from '../../../../../main/frontend/configuration/license/contents/ProductLicenseInfo';
import WithLicenseFooter from '../../../../../main/frontend/configuration/license/footers/WithLicenseFooter';
import WithNoLicenseFooter from '../../../../../main/frontend/configuration/license/footers/WithNoLicenseFooter';
import ProductLicenseDetail from '../../../../../main/frontend/configuration/license/ProductLicense';
import * as enzymeUtils from '../../../enzymeUtils';

describe('ProductLicenseDetail', () => {
  let mockLoad, minimalProps, getShallowComponent, mockClearUpdateLicenseError;

  beforeEach(() => {
    mockLoad = jasmine.createSpy('load');
    mockClearUpdateLicenseError = jasmine.createSpy('clearUpdateLicenseError');
    minimalProps = {
      load: mockLoad,
      loading: true,
      license: {},
      clearUpdateLicenseError: mockClearUpdateLicenseError,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(ProductLicenseDetail, minimalProps);
  });

  it('renders a component with nx-page-main class', () => {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  describe('on initial load', () => {
    it('calls load', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(ProductLicenseDetail, minimalProps);
      const mountedComponent = getMountedComponent();

      expect(mockLoad).toHaveBeenCalledTimes(1);
      mountedComponent.unmount();
    });

    it('shows error if some error occurs', () => {
      const loadError = 'some error happened';
      const shallowComponent = getShallowComponent({ loadError });
      const loadWrapper = shallowComponent.find(NxLoadWrapper);
      expect(loadWrapper).toHaveProp('error', loadError);
    });

    it('has true in loading prop', () => {
      const shallowComponent = getShallowComponent();
      const loadWrapper = shallowComponent.find(NxLoadWrapper);
      expect(loadWrapper).toHaveProp('loading', true);
    });
  });

  it('shows the ProductLicenseInfo component', () => {
    const shallowComponent = getShallowComponent();
    const productLicenseInfoComp = shallowComponent.find(ProductLicenseInfo);
    expect(productLicenseInfoComp).toExist();
  });

  it('does not show the ProductLicenseInfo component', () => {
    const shallowComponent = getShallowComponent({ license: null });
    const productLicenseInfoComp = shallowComponent.find(ProductLicenseInfo);
    expect(productLicenseInfoComp).not.toExist();
  });

  it('shows the NoProductLicense component', () => {
    const shallowComponent = getShallowComponent({ license: null });
    const noProductLicense = shallowComponent.find(NoProductLicense);
    expect(noProductLicense).toExist();
  });

  describe('error on update', () => {
    let shallowComponent, nxErrorAlert;
    beforeEach(() => {
      shallowComponent = getShallowComponent({ updateLicenseError: 'some error happened' });
      nxErrorAlert = shallowComponent.find(NxErrorAlert);
    });

    it('shows the NxErrorAlert component when updateLicenseError exists', () => {
      expect(nxErrorAlert).toExist();
    });

    it('clears the updateError on close nxErrorAlert', () => {
      expect(nxErrorAlert).toHaveProp('onClose', mockClearUpdateLicenseError);
    });
  });

  describe('footers', () => {
    it('shows WithLicenseFooter component', () => {
      const shallowComponent = getShallowComponent();
      const withLicenseFooter = shallowComponent.find(WithLicenseFooter);
      expect(withLicenseFooter).toExist();
    });

    it('shows WithNoLicenseFooter component', () => {
      const shallowComponent = getShallowComponent({ license: null });
      const withNoLicenseFooter = shallowComponent.find(WithNoLicenseFooter);
      expect(withNoLicenseFooter).toExist();
    });
  });

  describe('submit mask state', () => {
    it('has success prop with the value true', () => {
      const shallowComponent = getShallowComponent({ submitMaskState: true });
      const submitMask = shallowComponent.find(NxSubmitMask);
      expect(submitMask).toHaveProp('success', true);
    });

    it('has success prop with the value false', () => {
      const shallowComponent = getShallowComponent({ submitMaskState: false });
      const submitMask = shallowComponent.find(NxSubmitMask);
      expect(submitMask).toHaveProp('success', false);
    });

    it('does not show the submitMask component', () => {
      const shallowComponent = getShallowComponent({ submitMaskState: null });
      const submitMask = shallowComponent.find(NxSubmitMask);
      expect(submitMask).not.toExist();
    });
  });
});
