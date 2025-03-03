/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ApiPage from 'MainRoot/api/ApiPage';
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import { getEndpointsUrl } from 'MainRoot/util/CLMLocation';
import userEvent from '@testing-library/user-event';

describe('ApiPage', function () {
  let axiosMock;
  const renderComponent = () => render(<ApiPage />);
  const expectSwaggerUi = (expectedTitle) => {
    const swaggerUi = screen.getByText(expectedTitle);
    expect(swaggerUi).toBeVisible();
  };
  const expectHiddenSchemas = () => {
    const schemas = screen.queryByText('Schemas');
    expect(schemas).toBeNull();
  };
  const expectError = (errorMessage) => () => {
    const error = screen.getByText('An error occurred loading data. ' + errorMessage);
    expect(error).toBeVisible();
  };
  const getMinimalOpenApi = (title) => ({
    openapi: '3.0.0',
    info: {
      title,
    },
    paths: {
      '/api/thing/{thingId}': {
        get: {
          operationId: 'getThing',
          parameters: [
            {
              name: 'thingId',
              in: 'path',
              required: true,
              schema: {
                type: 'string',
              },
            },
          ],
          responses: {
            default: {
              content: {
                'application/json': {
                  schema: {
                    $ref: '#/components/schemas/ApiThingDTO',
                  },
                },
              },
            },
          },
        },
      },
    },
    components: {
      schemas: {
        ApiThingDTO: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
            },
          },
        },
      },
    },
  });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  describe('when there is a load error', function () {
    beforeEach(async function () {
      axiosMock.onGet(getEndpointsUrl('public')).reply(500, 'someError');
      renderComponent();
      await waitFor(expectError('someError'));
    });

    it('has a header of API', function () {
      const header = screen.getByRole('heading', { name: 'API' });
      expect(header).toBeVisible();
    });

    it('clears the error if the retry works', async function () {
      const user = userEvent.setup();
      axiosMock.onGet(getEndpointsUrl('public')).reply(200, getMinimalOpenApi('Public API'));

      const retryButton = screen.getByRole('button', { name: 'Retry' });
      await user.click(retryButton);
      await waitFor(() => expectSwaggerUi('Public API'));
    });
  });

  describe('when there is no load error', function () {
    beforeEach(async function () {
      axiosMock.onGet(getEndpointsUrl('public')).reply(200, getMinimalOpenApi('Public API'));
      axiosMock.onGet(getEndpointsUrl('experimental')).reply(200, getMinimalOpenApi('Experimental API'));
      renderComponent();
      await waitFor(() => expectSwaggerUi('Public API'));
    });

    it('has a header of API', async function () {
      const header = screen.getByRole('heading', { name: 'API' });
      expect(header).toBeVisible();
    });

    it('has 2 tabs for public and experimental', function () {
      const publicTab = screen.getByRole('tab', { name: /Public/i });
      expect(publicTab).toBeVisible();
      const experimentalTab = screen.getByRole('tab', { name: /Experimental/i });
      expect(experimentalTab).toBeVisible();
    });

    it('selects the public tab by default', function () {
      const publicTab = screen.getByRole('tab', { name: /Public/i });
      expect(publicTab).toHaveAttribute('aria-selected', 'true');
    });

    it('has a warning on the experimental tab', async function () {
      const experimentalTab = screen.getByRole('tab', { name: /Experimental/i });
      experimentalTab.click();
      await waitFor(() => expectSwaggerUi('Experimental API'));
      const warning = screen.getByText('These REST APIs are liable to change.');
      expect(warning).toBeVisible();
    });

    it('has a swagger ui on each tab', async function () {
      expectHiddenSchemas();
      const experimentalTab = screen.getByRole('tab', { name: /Experimental/i });
      experimentalTab.click();
      await waitFor(() => expectSwaggerUi('Experimental API'));
      expectHiddenSchemas();
    });
  });
});
