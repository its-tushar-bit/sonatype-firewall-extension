/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from '../test-utils';
import { NotFoundPage } from 'GuideRoot/layout/NotFoundPage';

describe('NotFoundPage', () => {
  it('renders the not-found heading', () => {
    render(<NotFoundPage />);
    expect(screen.getByRole('heading', { name: /we couldn't find that/i })).toBeInTheDocument();
  });

  it('renders a link to home', () => {
    render(<NotFoundPage />);
    expect(screen.getByRole('link', { name: /go to home/i })).toHaveAttribute('href', '/');
  });
});
