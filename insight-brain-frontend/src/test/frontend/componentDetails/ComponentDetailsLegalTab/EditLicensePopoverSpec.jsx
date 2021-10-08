/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import IqPopover from 'MainRoot/react/IqPopover/IqPopover';
import EditLicensesPopover from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesPopover';

import * as enzymeUtils from '../../enzymeUtils';
import { IqPopoverHeader } from '../../../../main/frontend/react/IqPopover';
import { NxForm, NxThreatIndicator } from '@sonatype/react-shared-components';

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

  describe('renders license info section', () => {
    const licensesProps = {
      declaredlicenses: [{ license: { licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }, threatLevel: 10 }],
      observedlicenses: [{ license: { licenseId: 'No-Sources', licenseName: 'No Sources' }, threatLevel: 5 }],
      effectiveLicenses: [{ license: { licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }, threatLevel: 9 }],
    };

    it('renders license name', () => {
      const wrapper = getShallowComponent({ ...licensesProps }),
        ddList = wrapper.find('dd'),
        declaredlicenses = ddList.at(0),
        observedlicenses = ddList.at(1),
        effectiveLicenses = ddList.at(2);

      expect(ddList.length).toBe(3);
      expect(declaredlicenses.text()).toContain('Apache-2.0');
      expect(observedlicenses.text()).toContain('No Sources');
      expect(effectiveLicenses.text()).toContain('Apache-2.0');
    });

    it('renders license <NxThreatIndicator/>', () => {
      const wrapper = getShallowComponent({ ...licensesProps }),
        threatIndicators = wrapper.find(NxThreatIndicator);

      expect(threatIndicators.length).toBe(3);
      expect(threatIndicators.at(0)).toExist();
      expect(threatIndicators.at(0)).toHaveProp('policyThreatLevel', 10);
      expect(threatIndicators.at(1)).toExist();
      expect(threatIndicators.at(1)).toHaveProp('policyThreatLevel', 5);
      expect(threatIndicators.at(2)).toExist();
      expect(threatIndicators.at(2)).toHaveProp('policyThreatLevel', 9);
    });
  });
});
