/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../main/frontend/firewall/firewallReducer';

describe('firewallReducer', function () {
  const defaultState = Object.freeze({
    selectedPolicyId: null,
    showWelcomeModal: false,
    showLimitedFirewallAccessAlert: false,
    cip: Object.freeze({
      selectedComponent: null,
      selectedComponentIndex: null,
      displayedEntries: [],
    }),
    componentDetailsPage: Object.freeze({
      isLoadingComponentDetails: false,
      firewallViolationDetailsError: null,
      componentDetails: null,
      componentDetailsError: null,
      policyViolations: [],
      isLoadingPolicyViolations: false,
      policyViolationsError: null,
      firewallPolicyName: null,
      firewallThreatLevel: null,
      firewallViolationDetailsLoading: false,
      componentLicenses: {
        declaredLicenses: [],
        observedLicenses: [],
        effectiveLicenses: [],
        selectableLicenses: [],
        licenseOverride: [],
        allLicenses: [],
      },
      isLoadingComponentLicenses: false,
      componentLicensesError: null,
      policyExistingWaivers: null,
      isLoadExistingWaivers: false,
      existingWaiversError: null,
      showManageWaiverPage: false,
      violationDetails: [],
      hasWaivePermission: false,
    }),
    viewState: Object.freeze({
      isShowConfigurationModal: false,
      loadError: null,
    }),
    tileMetricsState: Object.freeze({
      loadedTileMetrics: false,
      loadTileMetricsError: null,
      componentsAutoReleased: 0,
      componentsQuarantined: 0,
      namespaceAttacksBlocked: 0,
      safeVersionsSelected: 0,
      supplyChainAttacksBlocked: 0,
      waivedComponents: 0,
    }),
    autoUnquarantineState: Object.freeze({
      viewState: Object.freeze({
        loadedConfiguration: false,
        loadConfigurationError: null,
        loadedReleaseQuarantineSummary: false,
        loadReleaseQuarantineSummaryError: null,
        autoReleaseQuarantineCountMTD: '-',
        autoReleaseQuarantineCountYTD: '-',
        enabledPolicyConditionTypesCount: 0,
        totalPolicyConditionTypesCount: 0,
      }),
      autoUnquarantineGridState: Object.freeze({
        loadedReleaseQuarantineList: false,
        loadAutoUnquarantineGridError: null,
        releaseQuarantineList: [],
        releaseQuarantinePageCount: 0,
        pageSize: 12,
        currentPage: null,
        sortDir: null,
        sortField: null,
      }),
    }),
    policiesState: Object.freeze({
      loadedPolicies: false,
      policies: [],
    }),
    configurationState: Object.freeze({
      autoUnquarantineEnabled: false,
    }),
    quarantineSummaryState: Object.freeze({
      viewState: Object.freeze({
        loadedQuarantineSummary: false,
        loadQuarantineSummaryError: null,
        quarantineEnabled: false,
        quarantineEnabledRepositoryCount: 0,
        repositoryCount: 0,
        totalComponentCount: 0,
        quarantinedComponentCount: 0,
      }),
    }),
    quarantineGridState: Object.freeze({
      loadQuarantineGridError: null,
      loadedQuarantineList: false,
      quarantineList: [],
      quarantinePageCount: 0,
      pageSize: 12,
      currentPage: null,
      sortDir: 'desc',
      sortField: 'quarantineTime',
      filterPolicies: [],
      filterComponentName: '',
      filterRepositoryPublicId: '',
      filterQuarantineTime: null,
      lastUpdated: null,
    }),
    containerQuarantineGridState: Object.freeze({
      loadContainerQuarantineGridError: null,
      loadedContainerQuarantineList: false,
      containerQuarantineList: [],
      containerQuarantinePageCount: 0,
      containerPageSize: 12,
      containerCurrentPage: null,
      containerLastUpdated: null,
    }),
    containerWaiverGridState: Object.freeze({
      loadContainerWaiverGridError: null,
      loadingContainerWaiverList: false,
      containerWaiverList: [],
      containerWaiverPageCount: 0,
      containerWaiverPageSize: 10,
      containerWaiverCurrentPage: null,
      containerWaiverLastUpdated: null,
    }),
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' },
        newState = reduce(undefined, action);

      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' },
        newState = reduce(undefined, action);

      expect(newState).toEqual(defaultState);
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' }),
        newState = reduce(state, { type: 'UNKNOWN' });

      expect(newState).toBe(state);
    });
  });

  describe('FIREWALL_SET_SHOW_WELCOME_MODAL action', function () {
    let minimumState = {};

    it('updates the state and sets the showWelcomeModal to the payload', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_SET_SHOW_WELCOME_MODAL', payload: true })).toEqual({
        ...minimumState,
        showWelcomeModal: true,
      });
    });
  });

  describe('FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT action', function () {
    let minimumState = {};

    it('updates the state and sets the showLimitedFirewallAccessAlert to the payload', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT', payload: true })).toEqual({
        ...minimumState,
        showLimitedFirewallAccessAlert: true,
      });
    });

    it('updates the state and sets the showLimitedFirewallAccessAlert to false', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_SET_SHOW_LIMITED_FIREWALL_ACCESS_ALERT', payload: false })).toEqual(
        {
          ...minimumState,
          showLimitedFirewallAccessAlert: false,
        }
      );
    });
  });

  describe('FIREWALL_SET_SHOW_CONFIGURATION_MODAL action', function () {
    let minimumState = {};

    it('updates the state and sets the isShowConfigurationModal to the payload', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_SET_SHOW_CONFIGURATION_MODAL', payload: true })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          isShowConfigurationModal: true,
        },
      });
    });
  });

  describe('FIREWALL_SAVE_CONFIGURATION_FULFILLED action', function () {
    let minimumState = {
      autoUnquarantineState: {
        viewState: {},
      },
    };

    it('updates the state with the enabled and total policy condition counts', function () {
      const payload = [{ id: 'IntegrityRating', autoReleaseQuarantineEnabled: true }];

      expect(reduce(minimumState, { type: 'FIREWALL_SAVE_CONFIGURATION_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          viewState: {
            ...minimumState.autoUnquarantineState.viewState,
            enabledPolicyConditionTypesCount: 1,
            totalPolicyConditionTypesCount: 1,
          },
        },
        configurationState: {
          ...minimumState.configurationState,
          autoUnquarantineEnabled: true,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_REQUESTED action', function () {
    let minimumState = {
      autoUnquarantineState: {
        viewState: {},
      },
    };

    it('resets the state used for firewall configuration', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_CONFIGURATION_REQUESTED' })).toEqual({
        ...minimumState,
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          viewState: {
            ...minimumState.autoUnquarantineState.viewState,
            loadedConfiguration: false,
            loadConfigurationError: null,
          },
        },
      });
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_FULFILLED action', function () {
    let minimumState = {
      autoUnquarantineState: {
        viewState: {},
      },
    };

    it('updates the state', function () {
      let payload = [{ autoReleaseQuarantineEnabled: true }, { autoReleaseQuarantineEnabled: false }];

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_CONFIGURATION_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          viewState: {
            ...minimumState.autoUnquarantineState.viewState,
            loadedConfiguration: true,
            enabledPolicyConditionTypesCount: 1,
            totalPolicyConditionTypesCount: 2,
          },
        },
        configurationState: {
          ...minimumState.configurationState,
          autoUnquarantineEnabled: true,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_CONFIGURATION_FAILED action', function () {
    let minimumState = {
      viewState: {},
      autoUnquarantineState: {
        viewState: {},
      },
    };

    it('updates the state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_CONFIGURATION_FAILED', payload: 'error!' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'error!',
        },
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          viewState: {
            ...minimumState.autoUnquarantineState.viewState,
            loadedConfiguration: true,
            loadConfigurationError: 'error!',
          },
        },
      });
    });

    it('does not update loadError if it exists', function () {
      let newState = reduce(minimumState, { type: 'FIREWALL_LOAD_CONFIGURATION_FAILED', payload: 'old error!' });

      expect(reduce(newState, { type: 'FIREWALL_LOAD_CONFIGURATION_FAILED', payload: 'error!' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'old error!',
        },
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          viewState: {
            ...minimumState.autoUnquarantineState.viewState,
            loadedConfiguration: true,
            loadConfigurationError: 'error!',
          },
        },
      });
    });
  });

  describe('FIREWALL_LOAD_TILE_METRICS_REQUESTED action', function () {
    let minimumState = {
      tileMetricsState: {
        loadedTileMetrics: false,
        loadTileMetricsError: null,
      },
    };

    it('resets the state used for firewall tile metrics', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_TILE_METRICS_REQUESTED_REQUESTED' })).toEqual({
        ...minimumState,
        tileMetricsState: {
          loadedTileMetrics: false,
          loadTileMetricsError: null,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_TILE_METRICS_FULFILLED action', function () {
    let minimumState = {
      tileMetricsState: {
        loadedTileMetrics: false,
        componentsAutoReleased: 0,
        componentsQuarantined: 0,
        namespaceAttacksBlocked: 0,
        safeVersionsSelected: 0,
        supplyChainAttacksBlocked: 0,
        waivedComponents: 0,
      },
    };

    it('updates the state from payload and sets tileMetricsState to true', function () {
      const payload = {
        COMPONENTS_AUTO_RELEASED: {
          firewallMetricsValue: 1,
          latestUpdatedTime: '2020-01-01T01:00:00.000-00:00',
        },
        COMPONENTS_QUARANTINED: {
          firewallMetricsValue: 2,
          latestUpdatedTime: '2020-01-01T00:00:00.000-00:00',
        },
        NAMESPACE_ATTACKS_BLOCKED: {
          firewallMetricsValue: 3,
          latestUpdatedTime: '2020-01-01T01:00:00.000-00:00',
        },
        SAFE_VERSIONS_SELECTED_AUTOMATICALLY: {
          firewallMetricsValue: 4,
          latestUpdatedTime: '2020-01-01T00:00:00.000-00:00',
        },
        SUPPLY_CHAIN_ATTACKS_BLOCKED: {
          firewallMetricsValue: 5,
          latestUpdatedTime: '2020-01-01T00:00:00.000-00:00',
        },
        WAIVED_COMPONENTS: {
          firewallMetricsValue: 6,
          latestUpdatedTime: '2020-01-01T01:00:00.000-00:00',
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_TILE_METRICS_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        tileMetricsState: {
          loadedTileMetrics: true,
          componentsAutoReleased: 1,
          componentsQuarantined: 2,
          namespaceAttacksBlocked: 3,
          safeVersionsSelected: 4,
          supplyChainAttacksBlocked: 5,
          waivedComponents: 6,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_TILE_METRICS_FAILED action', function () {
    let minimumState = {
      tileMetricsState: {
        loadedTileMetrics: false,
        loadTileMetricsError: null,
      },
    };

    it('sets loadedTileMetrics to true and loadStatusError to the payload', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_TILE_METRICS_FAILED', payload: 'error!' })).toEqual({
        ...minimumState,
        tileMetricsState: {
          ...minimumState.tileMetricsState,
          loadedTileMetrics: true,
          loadTileMetricsError: 'error!',
        },
      });
    });
  });

  describe('FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED action', function () {
    let minimumState = {
      viewState: {
        loadError: 'Error!',
      },
    };

    it('updates the state and sets the loadError to null', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_POLICIES_WITH_CONDITIONS_REQUESTED' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: null,
        },
      });
    });
  });

  describe('FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED action', function () {
    let minimumState = {
      viewState: {
        loadError: 'Error!',
      },
    };

    it('updates the state and sets the loadError to null', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_POLICIES_WITH_CONDITIONS_FULFILLED' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: null,
        },
      });
    });
  });

  describe('FIREWALL_POLICIES_WITH_CONDITIONS_FAILED action', function () {
    let minimumState = {
      viewState: {},
    };

    it('updates the state and sets the loadError to the payload', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_POLICIES_WITH_CONDITIONS_FAILED', payload: 'Error!' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'Error!',
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_SUMMARY_REQUESTED action', function () {
    let minimumState = {
      quarantineSummaryState: {
        viewState: {},
      },
    };

    it('resets the state used by firewall quarantine summary', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_SUMMARY_REQUESTED' })).toEqual({
        ...minimumState,
        quarantineSummaryState: {
          ...minimumState.quarantineSummaryState,
          viewState: {
            ...minimumState.quarantineSummaryState.viewState,
            loadedQuarantineSummary: false,
            loadQuarantineSummaryError: null,
          },
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_SUMMARY_FULFILLED action', function () {
    let minimumState = {
      quarantineSummaryState: {
        viewState: {},
      },
    };

    it('updates the state, sets the load error to null and sets enabled flag from payload', function () {
      const payload = {
        quarantineEnabled: true,
        repositoryCount: 5,
        quarantineEnabledRepositoryCount: 2,
        totalComponentCount: 10,
        quarantinedComponentCount: 3,
      };

      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_SUMMARY_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        quarantineSummaryState: {
          ...minimumState.quarantineSummaryState,
          viewState: {
            ...minimumState.quarantineSummaryState.viewState,
            loadedQuarantineSummary: true,
            quarantineEnabled: true,
            repositoryCount: 5,
            quarantineEnabledRepositoryCount: 2,
            totalComponentCount: 10,
            quarantinedComponentCount: 3,
          },
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_SUMMARY_FAILED action', function () {
    let minimumState = {
      viewState: {},
      quarantineSummaryState: {
        viewState: {},
      },
    };

    it('updates the state and sets the loadStatusError to the payload', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_SUMMARY_FAILED', payload: 'error!' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'error!',
        },
        quarantineSummaryState: {
          ...minimumState.quarantineSummaryState,
          viewState: {
            ...minimumState.quarantineSummaryState.viewState,
            loadedQuarantineSummary: true,
            loadQuarantineSummaryError: 'error!',
          },
        },
      });
    });

    it('does not update loadError if it exists', function () {
      let newState = reduce(minimumState, {
        type: 'FIREWALL_QUARANTINE_SUMMARY_FAILED',
        payload: 'old error!',
      });

      expect(reduce(newState, { type: 'FIREWALL_QUARANTINE_SUMMARY_FAILED', payload: 'error!' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'old error!',
        },
        quarantineSummaryState: {
          ...minimumState.quarantineSummaryState,
          viewState: {
            ...minimumState.quarantineSummaryState.viewState,
            loadedQuarantineSummary: true,
            loadQuarantineSummaryError: 'error!',
          },
        },
      });
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED action', function () {
    let minimumState = {
      autoUnquarantineState: {
        viewState: {},
      },
    };

    it('resets the state used for release quarantine summary', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_REQUESTED' })).toEqual({
        ...minimumState,
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          viewState: {
            ...minimumState.autoUnquarantineState.viewState,
            loadedReleaseQuarantineSummary: false,
            loadReleaseQuarantineSummaryError: null,
          },
        },
      });
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED action', function () {
    let minimumState = {
      autoUnquarantineState: {
        viewState: {},
      },
    };

    it('updates the state', function () {
      let payload = { autoReleaseQuarantineCountMTD: 0, autoReleaseQuarantineCountYTD: 1 };

      expect(reduce(minimumState, { type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FULFILLED', payload: payload })).toEqual(
        {
          ...minimumState,
          autoUnquarantineState: {
            ...minimumState.autoUnquarantineState,
            viewState: {
              ...minimumState.autoUnquarantineState.viewState,
              loadedReleaseQuarantineSummary: true,
              autoReleaseQuarantineCountMTD: '0',
              autoReleaseQuarantineCountYTD: '1',
            },
          },
        }
      );
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED action', function () {
    let minimumState = {
      viewState: {},
      autoUnquarantineState: {
        viewState: {},
      },
    };

    it('updates the state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED', payload: 'error!' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'error!',
        },
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          viewState: {
            ...minimumState.autoUnquarantineState.viewState,
            loadedReleaseQuarantineSummary: true,
            loadReleaseQuarantineSummaryError: 'error!',
          },
        },
      });
    });

    it('it does not update loadError if it exists', function () {
      let newState = reduce(minimumState, {
        type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED',
        payload: 'old error!',
      });

      expect(reduce(newState, { type: 'FIREWALL_RELEASE_QUARANTINE_SUMMARY_FAILED', payload: 'error!' })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'old error!',
        },
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          viewState: {
            ...minimumState.autoUnquarantineState.viewState,
            loadedReleaseQuarantineSummary: true,
            loadReleaseQuarantineSummaryError: 'error!',
          },
        },
      });
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED action', function () {
    let minimumState = {
      autoUnquarantineState: {
        autoUnquarantineGridState: {},
      },
    };

    it('updates the state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_RELEASE_QUARANTINE_LIST_REQUESTED' })).toEqual({
        ...minimumState,
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          autoUnquarantineGridState: {
            ...minimumState.autoUnquarantineState.autoUnquarantineGridState,
            loadedReleaseQuarantineList: false,
            loadAutoUnquarantineGridError: null,
            releaseQuarantineList: [],
          },
        },
      });
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED action', function () {
    let minimumState = {
      autoUnquarantineState: {
        autoUnquarantineGridState: {},
      },
    };

    it('updates the state', function () {
      let payload = {
        pageCount: 1,
        page: 1,
        results: [
          { displayName: 'testVal', other: 'other' },
          { displayName: 'testVal', other: 'other' },
        ],
      };

      expect(reduce(minimumState, { type: 'FIREWALL_RELEASE_QUARANTINE_LIST_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          autoUnquarantineGridState: {
            ...minimumState.autoUnquarantineState.autoUnquarantineGridState,
            loadedReleaseQuarantineList: true,
            releaseQuarantineList: [
              { componentDisplayText: 'testVal', other: 'other' },
              { componentDisplayText: 'testVal', other: 'other' },
            ],
            releaseQuarantinePageCount: payload.pageCount,
            currentPage: payload.pageCount - 1,
          },
        },
      });
    });
  });

  describe('FIREWALL_RELEASE_QUARANTINE_LIST_FAILED action', function () {
    let minimumState = {
      viewState: {},
      autoUnquarantineState: {
        autoUnquarantineGridState: {},
      },
    };

    it('updates the state', function () {
      let payload = 'error!';

      expect(reduce(minimumState, { type: 'FIREWALL_RELEASE_QUARANTINE_LIST_FAILED', payload: payload })).toEqual({
        ...minimumState,
        viewState: {
          ...minimumState.viewState,
          loadError: 'error!',
        },
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          autoUnquarantineGridState: {
            ...minimumState.autoUnquarantineState.autoUnquarantineGridState,
            loadAutoUnquarantineGridError: payload,
            loadedReleaseQuarantineList: true,
            releaseQuarantineList: [],
          },
        },
      });
    });
  });

  describe('FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE action', function () {
    let minimumState = {
      autoUnquarantineState: {
        autoUnquarantineGridState: {},
      },
    };

    it('updates the state', function () {
      let payload = { currentPage: 123 };

      expect(reduce(minimumState, { type: 'FIREWALL_AUTO_UNQUARANTINE_GRID_SET_PAGE', payload })).toEqual({
        ...minimumState,
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          autoUnquarantineGridState: {
            ...minimumState.autoUnquarantineState.autoUnquarantineGridState,
            currentPage: payload.currentPage,
          },
        },
      });
    });
  });

  describe('FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING action', function () {
    let minimumState = {
      autoUnquarantineState: {
        autoUnquarantineGridState: {},
      },
    };

    it('updates the state', function () {
      let payload = { sortField: 'testSort', sortDir: 'asc' };

      expect(reduce(minimumState, { type: 'FIREWALL_AUTO_UNQUARANTINE_GRID_SET_SORTING', payload: payload })).toEqual({
        ...minimumState,
        autoUnquarantineState: {
          ...minimumState.autoUnquarantineState,
          autoUnquarantineGridState: {
            ...minimumState.autoUnquarantineState.autoUnquarantineGridState,
            sortField: 'testSort',
            sortDir: 'asc',
            currentPage: null,
            loadedReleaseQuarantineList: false,
            releaseQuarantineList: [],
            releaseQuarantinePageCount: 0,
          },
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_LIST_REQUESTED action', function () {
    let minimumState = {};

    it('updates the state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_LIST_REQUESTED' })).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          loadedQuarantineList: false,
          loadQuarantineGridError: null,
          currentPage: null,
          quarantineList: [],
          quarantinePageCount: 0,
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_LIST_FULFILLED action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let payload = {
        pageCount: 1,
        page: 1,
        results: [
          { displayName: 'testVal', other: 'other' },
          { displayName: 'testVal', other: 'other' },
        ],
      };

      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_LIST_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          loadedQuarantineList: true,
          quarantineList: [
            { componentDisplayText: 'testVal', other: 'other' },
            { componentDisplayText: 'testVal', other: 'other' },
          ],
          quarantinePageCount: payload.pageCount,
          currentPage: 0,
        },
      });

      payload = { pageCount: 0, page: 0, results: [] };

      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_LIST_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          loadedQuarantineList: true,
          quarantineList: payload.results,
          quarantinePageCount: payload.pageCount,
          currentPage: null,
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_LIST_FAILED action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let payload = 'error!';

      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_LIST_FAILED', payload: payload })).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          loadQuarantineGridError: payload,
          loadedQuarantineList: true,
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_GRID_SET_PAGE action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let payload = { currentPage: 123 };

      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_GRID_SET_PAGE', payload })).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          currentPage: payload.currentPage,
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_GRID_SET_SORTING action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let payload = { sortField: 'testSort', sortDir: 'asc' };

      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_GRID_SET_SORTING', payload: payload })).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          sortField: 'testSort',
          sortDir: 'asc',
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let payload = { policies: [{ id: '456', name: 'test-name' }] };

      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_GRID_SET_POLICY_FILTER', payload: payload })).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          filterPolicies: payload.policies,
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let payload = { componentName: { componentName: 'name' } };

      expect(
        reduce(minimumState, { type: 'FIREWALL_QUARANTINE_GRID_SET_COMPONENT_NAME_FILTER', payload: payload })
      ).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          filterComponentName: payload.componentName,
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let payload = { repositoryPublicId: { repositoryPublicId: 'publicId' } };

      expect(
        reduce(minimumState, { type: 'FIREWALL_QUARANTINE_GRID_SET_REPOSITORY_PUBLIC_ID_FILTER', payload: payload })
      ).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          filterRepositoryPublicId: payload.repositoryPublicId,
        },
      });
    });
  });

  describe('FIREWALL_POLICIES_REQUESTED action', function () {
    let minimumState = { policiesState: {} };

    it('updates the state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_POLICIES_REQUESTED' })).toEqual({
        ...minimumState,
        policiesState: {
          ...minimumState.policiesState,
          loadedPolicies: false,
          policies: [],
        },
      });
    });
  });

  describe('FIREWALL_POLICIES_FULFILLED action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let payload = {
        policies: [
          {
            name: 'testName',
            ownerId: 'ROOT_ORGANIZATION_ID',
          },
          {
            name: 'testName2',
            ownerId: 'invalid-owner',
          },
        ],
      };

      expect(reduce(minimumState, { type: 'FIREWALL_POLICIES_FULFILLED', payload: payload })).toEqual({
        ...minimumState,
        policiesState: {
          ...minimumState.policiesState,
          loadedPolicies: true,
          policies: [{ name: 'testName', ownerId: 'ROOT_ORGANIZATION_ID' }],
        },
      });
    });
  });

  describe('FIREWALL_POLICIES_FAILED action', function () {
    let minimumState = {};

    it('updates the state', function () {
      expect(reduce(minimumState, { type: 'FIREWALL_POLICIES_FAILED' })).toEqual({
        ...minimumState,
        policiesState: {
          ...minimumState.policiesState,
          loadedPolicies: true,
          policies: [],
        },
      });
    });
  });

  describe('FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED action', function () {
    let minimumState = {};

    it('updates the state', function () {
      let lastUpdated = new Date(),
        payload = { lastUpdated: lastUpdated };

      expect(reduce(minimumState, { type: 'FIREWALL_QUARANTINE_GRID_SET_LAST_UPDATED', payload: payload })).toEqual({
        ...minimumState,
        quarantineGridState: {
          ...minimumState.quarantineGridState,
          lastUpdated: lastUpdated,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_DATA_REQUESTED action', function () {
    it('updates to the initial state', function () {
      const customMinimumState = {
        quarantineGridState: {
          sortDir: 'asc',
          sortField: 'componentName',
          filterPolicies: ['123'],
          filterComponentName: '',
          filterRepositoryPublicId: '',
          filterQuarantineTime: null,
        },
      };
      expect(reduce(customMinimumState, { type: 'FIREWALL_LOAD_DATA_REQUESTED' })).toEqual({
        ...defaultState,
        quarantineGridState: {
          ...defaultState.quarantineGridState,
          ...customMinimumState.quarantineGridState,
        },
        containerQuarantineGridState: {
          ...defaultState.containerQuarantineGridState,
        },
      });
    });
  });

  describe('FIREWALL_SELECT_COMPONENT action', function () {
    let initialState = {
      cip: {
        selectedComponent: null,
        selectedComponentIndex: null,
        displayedEntries: [],
      },
    };

    it('updates the state with the selected component, index and all entries', function () {
      let component = { componentDisplayText: 'text' },
        components = [component],
        componentIndex = 0,
        payload = { component: component, components: components, componentIndex: componentIndex };

      expect(reduce(initialState, { type: 'FIREWALL_SELECT_COMPONENT', payload: payload })).toEqual({
        ...initialState,
        cip: {
          ...initialState.cip,
          selectedComponent: component,
          selectedComponentIndex: componentIndex,
          displayedEntries: components,
        },
      });
    });
  });

  describe('FIREWALL_COMPONENT_DETAILS_REQUESTED action', function () {
    it('updates the state and sets isLoadingComponentDetails to true', function () {
      let minimumState = {
        componentDetailsPage: { isLoadingComponentDetails: false, componentDetails: null, componentDetailsError: null },
      };
      expect(reduce(minimumState, { type: 'FIREWALL_COMPONENT_DETAILS_REQUESTED' })).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          isLoadingComponentDetails: true,
          componentDetails: null,
          componentDetailsError: null,
        },
      });
    });
  });

  describe('FIREWALL_COMPONENT_DETAILS_FULFILLED action', function () {
    it('updates the state and sets componentDetails with the response results', function () {
      let minimumState = {
        componentDetailsPage: { isLoadingComponentDetails: true, componentDetails: null, componentDetailsError: null },
      };
      expect(
        reduce(minimumState, { type: 'FIREWALL_COMPONENT_DETAILS_FULFILLED', payload: { data: 'payload' } })
      ).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          isLoadingComponentDetails: false,
          componentDetails: { data: 'payload' },
          componentDetailsError: null,
        },
      });
    });
  });

  describe('FIREWALL_COMPONENT_DETAILS_FAILED action', function () {
    it('updates the state and sets componentDetailsError with the request error message', function () {
      let minimumState = {
        componentDetailsPage: { isLoadingComponentDetails: true, componentDetails: null, componentDetailsError: null },
      };
      expect(reduce(minimumState, { type: 'FIREWALL_COMPONENT_DETAILS_FAILED', payload: 'Error' })).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          isLoadingComponentDetails: false,
          componentDetails: null,
          componentDetailsError: 'Error',
        },
      });
    });
  });

  describe('FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED action', function () {
    it('updates the state and sets isLoadingPolicyViolations to true', () => {
      let minimumState = {
        componentDetailsPage: {
          policyViolations: [],
          isLoadingPolicyViolations: false,
          policyViolationsError: null,
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_REQUESTED' })).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          policyViolations: [],
          isLoadingPolicyViolations: true,
          policyViolationsError: null,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED action', function () {
    it('updates the state and sets componentDetails with the response results', () => {
      let minimumState = {
        componentDetailsPage: {
          policyViolations: [],
          isLoadingPolicyViolations: false,
          policyViolationsError: null,
        },
      };

      expect(
        reduce(minimumState, {
          type: 'FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FULFILLED',
          payload: [{ policyViolationId: 'policyViolationId' }],
        })
      ).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          policyViolations: [{ policyViolationId: 'policyViolationId' }],
          isLoadingPolicyViolations: false,
          policyViolationsError: null,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED action', function () {
    it('updates the state and sets componentDetailsError with the request error message', () => {
      let minimumState = {
        componentDetailsPage: {
          policyViolations: [],
          isLoadingPolicyViolations: true,
          policyViolationsError: null,
        },
      };

      expect(
        reduce(minimumState, { type: 'FIREWALL_LOAD_COMPONENT_POLICY_VIOLATIONS_FAILED', payload: 'Error' })
      ).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          policyViolations: [],
          isLoadingPolicyViolations: false,
          policyViolationsError: 'Error',
        },
      });
    });
  });

  describe('FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED', function () {
    it('updates the state and sets isLoadingComponentLicenses to true', () => {
      let minimumState = {
        componentDetailsPage: {
          isLoadingComponentLicenses: false,
          componentLicensesError: null,
          componentLicenses: {
            declaredLicenses: [],
            observedLicenses: [],
            effectiveLicenses: [],
            selectableLicenses: [],
            licenseOverride: [],
            allLicenses: [],
          },
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_COMPONENT_LICENSES_REQUESTED', payload: 'Error' })).toEqual({
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          isLoadingComponentLicenses: true,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED action', function () {
    it('updates the state and sets loadExistingWaiversData to true', () => {
      let minimumState = {
        componentDetailsPage: {
          policyExistingWaivers: null,
          isLoadExistingWaivers: false,
          existingWaiversError: null,
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_EXISTING_WAIVERS_DATA_REQUESTED' })).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          policyExistingWaivers: null,
          isLoadExistingWaivers: true,
          existingWaiversError: null,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED action', function () {
    it('updates the state and sets componentLicenses with the response results', () => {
      let minimumState = {
        componentDetailsPage: {
          isLoadingComponentLicenses: true,
          componentLicensesError: null,
          componentLicenses: {
            declaredLicenses: [],
            observedLicenses: [],
            effectiveLicenses: [],
            selectableLicenses: [],
            licenseOverride: [],
            allLicenses: [],
          },
        },
      };

      const payload = {
        declaredLicenses: [{ licenseId: 'licenseId' }],
        observedLicenses: [{ licenseId: 'licenseId' }],
        effectiveLicenses: [{ licenseId: 'licenseId' }],
        selectableLicenses: [{ licenseId: 'licenseId' }],
        licenseOverride: [{ licenseId: 'licenseId' }],
        allLicenses: [{ licenseId: 'licenseId' }],
      };

      expect(
        reduce(minimumState, {
          type: 'FIREWALL_LOAD_COMPONENT_LICENSES_FULFILLED',
          payload,
        })
      ).toEqual({
        componentDetailsPage: {
          isLoadingComponentLicenses: false,
          componentLicensesError: null,
          componentLicenses: payload,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED action', function () {
    it('updates the state and sets existing waivers with the response results', () => {
      let minimumState = {
        componentDetailsPage: {
          policyExistingWaivers: null,
          isLoadExistingWaivers: false,
          existingWaiversError: null,
        },
      };

      expect(
        reduce(minimumState, {
          type: 'FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FULFILLED',
          payload: { data: 'payload' },
        })
      ).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          policyExistingWaivers: { data: 'payload' },
          isLoadExistingWaivers: false,
          existingWaiversError: null,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_COMPONENT_LICENSES_FAILED action', function () {
    it('updates the state and sets componentLicensesError with the request error message', () => {
      let minimumState = {
        componentDetailsPage: {
          isLoadingComponentLicenses: false,
          componentLicensesError: null,
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_COMPONENT_LICENSES_FAILED', payload: 'Error' })).toEqual({
        componentDetailsPage: {
          isLoadingComponentLicenses: false,
          componentLicensesError: 'Error',
        },
      });
    });
  });

  describe('FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED action', function () {
    it('updates the state and sets existing waivers with the request error message', () => {
      let minimumState = {
        componentDetailsPage: {
          policyExistingWaivers: null,
          isLoadExistingWaivers: true,
          existingWaiversError: null,
          firewallViolationDetailsLoading: false,
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_EXISTING_WAIVERS_DATA_FAILED', payload: 'Error' })).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          policyExistingWaivers: null,
          isLoadExistingWaivers: false,
          existingWaiversError: 'Error',
        },
      });
    });
  });

  describe('FIREWALL_SHOW_MANAGE_WAIVER_PAGE action', () => {
    it('update state and set boolean value if opened waiver page', () => {
      let minimumState = {
        componentDetailsPage: {
          showManageWaiverPage: false,
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_SHOW_MANAGE_WAIVER_PAGE', payload: { data: 'payload' } })).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          showManageWaiverPage: { data: 'payload' },
        },
      });
    });
  });

  describe('FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED action', function () {
    it('updates the state and sets firewallViolationDetailsLoading to true', () => {
      let minimumState = {
        componentDetailsPage: {
          firewallViolationDetailsLoading: true,
          firewallViolationDetailsError: null,
          violationDetails: [],
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_VIOLATION_DETAIL_REQUESTED' })).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          violationDetails: [],
          firewallViolationDetailsLoading: true,
          firewallViolationDetailsError: null,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED action', () => {
    it('updates the state and sets selected violation details with the response results', () => {
      let minimumState = {
        componentDetailsPage: {
          firewallPolicyName: undefined,
          firewallThreatLevel: undefined,
          firewallViolationDetailsLoading: false,
          firewallViolationDetailsError: null,
          violationDetails: [],
        },
      };

      expect(
        reduce(minimumState, { type: 'FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED', payload: { data: 'payload' } })
      ).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          violationDetails: { data: 'payload' },
          hasWaivePermission: false,
          firewallViolationDetailsLoading: false,
          firewallViolationDetailsError: null,
        },
      });

      expect(
        reduce(minimumState, {
          type: 'FIREWALL_LOAD_VIOLATION_DETAIL_FULFILLED',
          payload: { data: 'payload', hasWaivePermission: true },
        })
      ).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          violationDetails: { data: 'payload', hasWaivePermission: true },
          hasWaivePermission: true,
          firewallViolationDetailsLoading: false,
          firewallViolationDetailsError: null,
        },
      });
    });
  });

  describe('FIREWALL_LOAD_VIOLATION_DETAIL_FAILED action', function () {
    it('updates the state and sets existing waivers with the request error message', () => {
      let minimumState = {
        componentDetailsPage: {
          violationDetails: [],
          firewallViolationDetailsLoading: false,
          firewallViolationDetailsError: 'Error',
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_LOAD_VIOLATION_DETAIL_FAILED', payload: 'Error' })).toEqual({
        ...minimumState,
        componentDetailsPage: {
          ...minimumState.componentDetailsPage,
          violationDetails: [],
          firewallViolationDetailsError: 'Error',
          firewallViolationDetailsLoading: false,
        },
      });
    });
  });

  describe('FIREWALL_CONTAINER_WAIVER_LIST_REQUESTED action', () => {
    it('updates the state and sets loadingContainerWaiverList to true', () => {
      let minimumState = {
        containerWaiverGridState: {
          loadingContainerWaiverList: false,
          loadContainerWaiverGridError: null,
          containerWaiverList: [],
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_CONTAINER_WAIVER_LIST_REQUESTED' })).toEqual({
        ...minimumState,
        containerWaiverGridState: {
          ...minimumState.containerWaiverGridState,
          loadingContainerWaiverList: true,
          loadContainerWaiverGridError: null,
          containerWaiverList: [],
        },
      });
    });
  });

  describe('FIREWALL_CONTAINER_WAIVER_LIST_FULFILLED action', () => {
    it('updates the state and sets containerWaiverList with the response results', () => {
      let minimumState = {
        containerWaiverGridState: {
          loadingContainerWaiverList: true,
          loadContainerWaiverGridError: null,
          containerWaiverList: [],
          containerWaiverCurrentPage: null,
          containerWaiverPageCount: 0,
        },
      };

      const payload = {
        total: 1,
        page: 1,
        pageSize: 10,
        pageCount: 1,
        results: [{ waiver1: 'waiver1Val' }, { waiver2: 'waiver2Val' }],
      };

      expect(reduce(minimumState, { type: 'FIREWALL_CONTAINER_WAIVER_LIST_FULFILLED', payload })).toEqual({
        ...minimumState,
        containerWaiverGridState: {
          ...minimumState.containerWaiverGridState,
          loadingContainerWaiverList: false,
          loadContainerWaiverGridError: null,
          containerWaiverList: payload.results,
          containerWaiverCurrentPage: 0,
          containerWaiverPageCount: 1,
        },
      });
    });
  });

  describe('FIREWALL_CONTAINER_WAIVER_LIST_FAILED action', () => {
    it('updates the state and sets containerWaiverListError with the request error message', () => {
      let minimumState = {
        containerWaiverGridState: {
          loadingContainerWaiverList: true,
          loadContainerWaiverGridError: null,
          containerWaiverList: [],
        },
      };

      expect(reduce(minimumState, { type: 'FIREWALL_CONTAINER_WAIVER_LIST_FAILED', payload: 'Error' })).toEqual({
        ...minimumState,
        containerWaiverGridState: {
          ...minimumState.containerWaiverGridState,
          loadingContainerWaiverList: false,
          loadContainerWaiverGridError: 'Error',
          containerWaiverList: [],
        },
      });
    });
  });

  describe('FIREWALL_CONTAINER_WAIVER_GRID_SET_LAST_UPDATED action', () => {
    it('updates the state and sets containerWaiverLastUpdated', () => {
      let minimumState = {
        containerWaiverGridState: {
          containerWaiverLastUpdated: null,
        },
      };

      const lastUpdated = new Date();
      const payload = { containerWaiverLastUpdated: lastUpdated };
      expect(reduce(minimumState, { type: 'FIREWALL_CONTAINER_WAIVER_GRID_SET_LAST_UPDATED', payload })).toEqual({
        ...minimumState,
        containerWaiverGridState: {
          ...minimumState.containerWaiverGridState,
          containerWaiverLastUpdated: lastUpdated,
        },
      });
    });
  });

  describe('FIREWALL_CONTAINER_WAIVER_GRID_SET_PAGE action', () => {
    it('updates the state and sets containerWaiverCurrentPage', () => {
      let minimumState = {
        containerWaiverGridState: {
          containerWaiverCurrentPage: null,
        },
      };
      const page = 2;
      const payload = { containerWaiverCurrentPage: page };
      expect(reduce(minimumState, { type: 'FIREWALL_CONTAINER_WAIVER_GRID_SET_PAGE', payload })).toEqual({
        ...minimumState,
        containerWaiverGridState: {
          ...minimumState.containerWaiverGridState,
          containerWaiverCurrentPage: page,
        },
      });
    });
  });
});
