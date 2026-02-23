/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import InnerSourceRepositoryDeleteConfigurationModal from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryDeleteConfigurationModal';
import { getInitialState } from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalTestData';
import * as innerSourceRepositoryConfigurationModalSelectors from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryDeleteConfigurationModalSelectors';
import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';

import 'TestRoot/SpecUtil';

describe('InnerSourceRepositoryDeleteConfigurationModal', function () {
  let renderComponent, spySelectInnerSourceRepositoryDeleteConfigurationModalSlice;

  beforeEach(() => {
    spySelectInnerSourceRepositoryDeleteConfigurationModalSlice = jest.spyOn(
      innerSourceRepositoryConfigurationModalSelectors,
      'selectInnerSourceRepositoryDeleteConfigurationModalSlice'
    );

    spySelectInnerSourceRepositoryDeleteConfigurationModalSlice.mockReturnValue({
      ...getInitialState(),
      showModal: true,
    });

    renderComponent = () => render(<InnerSourceRepositoryDeleteConfigurationModal />);
  });

  it('renders the delete configuration modal', function () {
    spySelectInnerSourceRepositoryDeleteConfigurationModalSlice.mockReturnValue({
      ...getInitialState(),
      showModal: true,
    });
    renderComponent();
    expect(screen.getByText('Delete Repository Configuration?')).toBeInTheDocument();
  });

  it('renders the delete configuration error', function () {
    spySelectInnerSourceRepositoryDeleteConfigurationModalSlice.mockReturnValue({
      ...getInitialState(),
      showModal: true,
      deleteConfigurationError: 'someError',
    });
    renderComponent();
    expect(screen.getByText('Unable to delete the configured repository. someError')).toBeInTheDocument();
  });
});
