/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../../enzymeUtils';

import { PaginationLink } from '../../../../main/frontend/componentDetails/ComponentDetailsFooter/PaginationLink';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faChevronRight, faChevronLeft } from '@fortawesome/free-solid-svg-icons';

describe('PaginationLink', () => {
  let minimalProps;
  let getShallowComponent;

  beforeEach(() => {
    minimalProps = {};

    getShallowComponent = enzymeUtils.getShallowComponent(PaginationLink, minimalProps);
  });

  it('renders a right chevron icon by default', () => {
    const component = getShallowComponent();
    expect(component).toContainReact(<NxFontAwesomeIcon icon={faChevronRight} />);
  });

  it('renders a left chevron icon when direction="prev" props is passed', () => {
    const component = getShallowComponent({ direction: 'prev' });
    expect(component).toContainReact(<NxFontAwesomeIcon icon={faChevronLeft} />);
  });

  it('adds a disabled class when no href or empty string href is passed', () => {
    const componentWithoutHref = getShallowComponent();
    expect(componentWithoutHref.find('.iq-pagination-link--disabled')).toExist();

    const component = getShallowComponent({ href: '' });
    expect(component.find('.iq-pagination-link--disabled')).toExist();
  });

  it('renders the text prop passed as content', () => {
    const component = getShallowComponent({ text: 'Mock Text' });
    expect(component.find('span')).toHaveText('Mock Text');
  });
});
