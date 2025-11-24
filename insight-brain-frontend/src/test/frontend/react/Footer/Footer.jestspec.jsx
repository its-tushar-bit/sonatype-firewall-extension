/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import Footer from 'MainRoot/react/Footer/Footer';

describe('Footer', () => {
  let renderComponent;
  const defaultPreloadedState = {
    productFeatures: {
      productFeatures: {
        'single-tenant': true,
        'multi-tenant': false,
      },
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    renderComponent = (preloadedState) =>
      render(<Footer productEdition="Lifecycle" clmServerVersion="1.185.0-01" />, {
        preloadedState: preloadedState || defaultPreloadedState,
      });
  });

  describe('when productEdition and releaseNumber are provided', () => {
    it('renders footer with version when isShowVersionEnabled is true', () => {
      renderComponent();

      expect(screen.getByText('Powered by Sonatype IQ Server')).toBeInTheDocument();
      expect(screen.getByText('Release 185')).toBeInTheDocument();
    });

    it('renders footer without version when isShowVersionEnabled is false (multi-tenant)', () => {
      const preloadedState = {
        productFeatures: {
          productFeatures: {
            'single-tenant': false,
            'multi-tenant': true,
          },
        },
      };

      renderComponent(preloadedState);

      expect(screen.getByText('Powered by Sonatype IQ Server')).toBeInTheDocument();
      expect(screen.queryByText('Release 185')).not.toBeInTheDocument();
    });

    it('renders footer without version when productFeatures are not available', () => {
      const preloadedState = {
        productFeatures: {
          productFeatures: {},
        },
      };

      renderComponent(preloadedState);

      expect(screen.getByText('Powered by Sonatype IQ Server')).toBeInTheDocument();
      expect(screen.queryByText('Release 185')).not.toBeInTheDocument();
    });
  });

  describe('when productEdition or releaseNumber are missing', () => {
    it('renders nothing when productEdition is not provided', () => {
      render(<Footer clmServerVersion="1.185.0-01" />, { defaultPreloadedState });

      expect(screen.queryByText('Powered by Sonatype IQ Server')).not.toBeInTheDocument();
    });

    it('throws TypeError when clmServerVersion is undefined', () => {
      renderComponent = (preloadedState) =>
        render(<Footer productEdition="Lifecycle" />, {
          preloadedState: preloadedState || defaultPreloadedState,
        });

      expect(() => renderComponent()).toThrow(TypeError);
      expect(() => renderComponent()).toThrow(`Cannot determine release version from '${global.clmServerVersion}'.`);
    });
  });
});
