/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import IqPopover from 'MainRoot/react/IqPopover/IqPopover';
import EditLicensesPopover from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesPopover';

import * as enzymeUtils from '../../enzymeUtils';
import { IqPopoverHeader } from '../../../../main/frontend/react/IqPopover';
import { NxForm } from '@sonatype/react-shared-components';

describe('EditLicensesPopover', () => {
  let minimalProps, getShallowComponent;

  beforeEach(function () {
    minimalProps = {
      onClose: () => {},
      showEditLicensesPopover: true,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(EditLicensesPopover, minimalProps);
  });

  it('should not render an IqPopover if `showEditLicensesPopover` is false', () => {
    const component = getShallowComponent({ showEditLicensesPopover: false });

    expect(component).toBeEmptyRender();
  });

  it('renders an IqPopover if `showEditLicensesPopover` is true', () => {
    const component = getShallowComponent(),
      iqPopover = component.find(IqPopover);

    expect(iqPopover).toExist();
  });

  it('renders an IqPopover header', () => {
    const component = getShallowComponent(),
      header = component.find(IqPopoverHeader);

    expect(header).toExist();
    expect(header).toHaveProp('headerTitle', 'Edit Licenses');
    expect(header).toHaveProp('onClose', minimalProps.onClose);
  });

  it('renders a NxForm', () => {
    const component = getShallowComponent(),
      form = component.find(NxForm);

    expect(form).toExist();
  });
});
