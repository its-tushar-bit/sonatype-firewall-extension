/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter, configureStore } from 'TestRoot/SpecUtil';
import reducers from 'MainRoot/reduxConfig/reducers';
import { getProductFeaturesUrl, getSuccessMetricsConfigUrl } from 'MainRoot/util/CLMLocation';
import { loadConfigurationIfSupported } from 'MainRoot/configuration/successMetricsConfiguration/successMetricsConfigurationActions';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';

describe('loadConfigurationIfSupported', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  // A fresh store per test, since the product-features map must differ between cases.
  // The handlers are registered per test too: SpecUtil resets the shared adapter after
  // every test, which clears both handlers and request history.
  function storeWithFeatures(features) {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, features);
    axiosMock.onGet(getSuccessMetricsConfigUrl()).reply(200, { enabled: true });
    return configureStore({ reducer: reducers });
  }

  it('does not request the configuration when the feature is unsupported', async () => {
    const store = storeWithFeatures([]);

    await store.dispatch(loadConfigurationIfSupported());

    expect(axiosMock.history.get.map(({ url }) => url)).not.toContain(getSuccessMetricsConfigUrl());
    // The slice is left exactly as initialised. This covers the bootstrap path only —
    // SuccessMetricsConfiguration.jsx dispatches load() from its own useEffect, gated on a
    // different feature flag, so the page is not left loading forever.
    expect(store.getState().successMetricsConfiguration).toEqual({
      formState: { enabled: false },
      viewState: {
        loading: true,
        loadError: null,
        updateError: null,
        submitMaskState: null,
        isDirty: false,
      },
      serverData: null,
    });
  });

  it('loads the configuration when the feature is supported', async () => {
    const store = storeWithFeatures(['success-metrics-configuration']);

    await store.dispatch(loadConfigurationIfSupported());

    expect(axiosMock.history.get.map(({ url }) => url)).toContain(getSuccessMetricsConfigUrl());
    expect(store.getState().successMetricsConfiguration.serverData).toEqual({ enabled: true });
    expect(store.getState().successMetricsConfiguration.viewState.loading).toBe(false);
  });

  it('leaves the product-features slice untouched when the map is already populated', async () => {
    const store = storeWithFeatures(['success-metrics-configuration']);
    // Populate the map the way the bootstrap does, then forget the resulting requests.
    await store.dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());
    axiosMock.resetHistory();

    const featuresLoadingSeen = [];
    const unsubscribe = store.subscribe(() => featuresLoadingSeen.push(store.getState().productFeatures.loading));
    await store.dispatch(loadConfigurationIfSupported());
    unsubscribe();

    // No repeat GET, and `loading` never flips: consumers gated on it blank themselves
    // while it is true (IqSidebarNav renders DefaultEmptyIqSidebar), so a gate that only
    // reads a feature flag must not touch it.
    expect(axiosMock.history.get.map(({ url }) => url)).not.toContain(getProductFeaturesUrl());
    expect(featuresLoadingSeen).not.toContain(true);
  });
});
