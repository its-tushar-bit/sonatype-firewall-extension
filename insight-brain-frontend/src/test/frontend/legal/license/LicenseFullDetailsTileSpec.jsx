/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../../enzymeUtils';
import LicenseFullDetailsTile from '../../../../main/frontend/legal/license/LicenseFullDetailsTile';
import { licenseState } from './licenseCommonState';
import { render, screen } from 'TestRoot/SpecUtil';
import { within } from '@testing-library/react';

describe('LicenseFullDetailsTile component', function () {
  let getShallowComponent;

  const minimalProps = licenseState;

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseFullDetailsTile, minimalProps);
  });

  it('renders a header with label `License Obligations`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('GPL-2 License Obligations');
  });

  it('renders the given license obligations', function () {
    const wrapper = getShallowComponent();
    let obligationTitles = wrapper.find('dt');
    let obligationBodies = wrapper.find('dd');
    expect(obligationTitles.length).toBe(2);
    expect(obligationBodies.length).toBe(2);
    expect(obligationTitles.at(0)).toHaveText('Inclusion of License');
    expect(obligationBodies.at(0)).toHaveText('distribute a copy of this License along with the Library');
    expect(obligationTitles.at(1)).toHaveText('Inclusion of Copyright');
    expect(obligationBodies.at(1)).toHaveText('copyright this');
  });

  it('creates anchors for the given license obligations', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('#InclusionOfLicense')).toHaveText('distribute a copy of this License along with the Library');
    expect(wrapper.find('#InclusionOfCopyright')).toHaveText('copyright this');
  });

  it('renders obligations without unnecessary tabs in texts', function () {
    minimalProps.licenseLegalMetadata[1].obligations[0].obligationTexts[0] = 'Obligation\t\t\ttext\t\twith tabs\t\t';
    render(<LicenseFullDetailsTile {...minimalProps} />);
    expect(screen.getByText('Obligation text with tabs')).toBeInTheDocument();
  });

  it('renders different obligations with same text just once in the license text', function () {
    minimalProps.licenseLegalMetadata[1].obligations[0].obligationTexts[0] = 'common text for obligations';
    minimalProps.licenseLegalMetadata[1].obligations[1].obligationTexts[0] = 'common text for obligations';
    minimalProps.licenseLegalMetadata[1].licenseText = 'GPL 2.0 long text here including common text for obligations';
    render(<LicenseFullDetailsTile {...minimalProps} />);
    const licenseText = screen.getByTestId('licenseText');
    const obligationsTexts = screen.getByTestId('obligationsTexts');
    expect(within(obligationsTexts).getAllByText('common text for obligations').length).toBe(2);
    expect(within(licenseText).getAllByText('common text for obligations').length).toBe(1);
  });
});
