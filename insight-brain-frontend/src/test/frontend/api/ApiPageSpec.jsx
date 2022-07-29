/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ApiPage from 'MainRoot/api/ApiPage';
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import axios from 'axios';
import { getEndpointsUrl } from 'MainRoot/util/CLMLocation';

describe('ApiPage', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const renderComponent = () => render(<ApiPage />);
  const expectSwaggerUi = () => {
    const swaggerUi = screen.getByText('Minimal OpenAPI');
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
  const minimalOpenApi = {
    openapi: '3.0.0',
    info: {
      title: 'Minimal OpenAPI',
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
  };

  describe('when there is a load error', function () {
    beforeEach(async function () {
      mockAxiosCalls({
        get: {
          [getEndpointsUrl('public')]: Promise.reject('someError'),
        },
      });
      renderComponent();
      await waitFor(expectError('someError'));
    });

    it('has a header of API', function () {
      const header = screen.getByText('API');
      expect(header).toBeVisible();
    });
  });

  describe('when there is no load error', function () {
    beforeEach(async function () {
      mockAxiosCalls({
        get: {
          [getEndpointsUrl('public')]: Promise.resolve({
            data: { ...minimalOpenApi },
          }),
          [getEndpointsUrl('experimental')]: Promise.resolve({
            data: { ...minimalOpenApi },
          }),
        },
      });
      renderComponent();
      await waitFor(expectSwaggerUi);
    });

    it('has a header of API', async function () {
      const header = screen.getByText('API');
      expect(header).toBeVisible();
    });

    it('has 2 tabs for public and experimental', async function () {
      const publicTab = screen.getByRole('tab', { name: /Public/i });
      expect(publicTab).toBeVisible();
      const experimentalTab = screen.getByRole('tab', { name: /Experimental/i });
      expect(experimentalTab).toBeVisible();
    });

    it('selects the public tab by default', async function () {
      const publicTab = screen.getByRole('tab', { name: /Public/i });
      expect(publicTab).toHaveAttribute('aria-selected', 'true');
    });

    it('has a warning on the experimental tab', async function () {
      const experimentalTab = screen.getByRole('tab', { name: /Experimental/i });
      experimentalTab.click();
      await waitFor(expectSwaggerUi);
      const warning = screen.getByText('These REST APIs are liable to change.');
      expect(warning).toBeVisible();
    });

    it('has a swagger ui on each tab', async function () {
      await waitFor(expectSwaggerUi);
      expectHiddenSchemas();
      const experimentalTab = screen.getByRole('tab', { name: /Experimental/i });
      experimentalTab.click();
      await waitFor(expectSwaggerUi);
      expectHiddenSchemas();
    });
  });
});
