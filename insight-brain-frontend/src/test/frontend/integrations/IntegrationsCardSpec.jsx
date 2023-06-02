/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import IntegrationsCard from 'MainRoot/integrations/IntegrationsCard';
import { faker } from '@faker-js/faker';

describe('IntegrationsCard', () => {
  let givenProps;

  beforeEach(() => {
    givenProps = generateProps();
    renderComponent(givenProps);
  });

  it('renders a screen reader friendly card', function () {
    const card = screen.getByRole('region', { name: givenProps.title });
    expect(card).toBeInTheDocument();
  });

  it('renders the heading', function () {
    const card = screen.getByRole('heading', { name: givenProps.title });
    expect(card).toBeInTheDocument();
  });

  it('renders an image with an accessible name', function () {
    const image = screen.getByRole('img');
    expect(image).toBeInTheDocument();
    expect(image).toHaveAttribute('src', givenProps.imgUrl);
  });

  it('renders a description', function () {
    const description = screen.getByText(givenProps.description);
    expect(description).toBeInTheDocument();
  });

  it('renders a link', function () {
    const link = screen.getByRole('link', { name: givenProps.linkText });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', givenProps.linkUrl);
  });

  function renderComponent(props) {
    return render(<IntegrationsCard {...props} />);
  }

  function generateProps() {
    return {
      title: faker.lorem.sentence(),
      description: faker.lorem.paragraph(),
      imgUrl: faker.internet.url(),
      linkText: faker.lorem.sentence(),
      linkUrl: faker.internet.url(),
    };
  }
});
