/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectSourceControlConfigurationSlice,
  selectValidationError,
  selectShowGitHubAppSuccessModal,
  selectShowGitHubAppReplacedAlert,
  selectIsGitHubAppReplacement,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';

import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';

describe('selectSourceControlConfigurationSelectors', () => {
  const mockState = {
    orgsAndPolicies: {
      sourceControlConfiguration: {
        sourceControl: {
          provider: {
            value: 'github',
            validationErrors: [],
            isPristine: false,
          },
        },
        serverSourceControl: null,
        sourceControlMetrics: undefined,
        isShowAccessTokenWarning: null,
        isResetModalOpen: false,
        someOtherfields: true,
      },
    },
  };

  describe('sourceControlConfiguration', () => {
    it('selects source control configuration state', () => {
      expect(selectSourceControlConfigurationSlice(mockState)).toEqual({
        sourceControl: {
          provider: {
            value: 'github',
            validationErrors: [],
            isPristine: false,
          },
        },
        serverSourceControl: null,
        sourceControlMetrics: undefined,
        isShowAccessTokenWarning: null,
        isResetModalOpen: false,
        someOtherfields: true,
      });
    });
  });

  describe('selectValidationError', () => {
    it('returns validation error if no provider is chosen', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: '',
                  validationErrors: ['Must be non-empty'],
                  isPristine: false,
                },
              },
              username: {
                rscValue: {
                  value: 'admin',
                  trimmedValue: 'admin',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              token: {
                rscValue: {
                  value: 'password1',
                  trimmedValue: 'password1',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              baseBranch: {
                rscValue: {
                  value: 'main',
                  trimmedValue: 'main',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              closePrOnFailedChecksEnabled: {
                value: null,
              },
              closePrAfterDaysOpenEnabled: {
                value: null,
              },
              closePrAfterDays: {
                rscValue: {
                  value: null,
                },
              },
            },
          },
        },
      };
      const actual = selectValidationError(state);
      expect(actual).toEqual(GLOBAL_FORM_VALIDATION_ERROR);
    });

    it('returns validation error if user insert not valid username', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'azure',
                  validationErrors: [],
                  isPristine: false,
                },
              },
              username: {
                rscValue: {
                  value: 'a'.repeat(256),
                  trimmedValue: 'a'.repeat(256),
                  validationErrors: ['Please enter less than 255 characters'],
                  isPristine: false,
                },
              },
              token: {
                rscValue: {
                  value: 'password1',
                  trimmedValue: 'password1',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              baseBranch: {
                rscValue: {
                  value: 'main',
                  trimmedValue: 'main',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              closePrOnFailedChecksEnabled: {
                value: null,
              },
              closePrAfterDaysOpenEnabled: {
                value: null,
              },
              closePrAfterDays: {
                rscValue: {
                  value: null,
                },
              },
            },
          },
        },
      };
      const actual = selectValidationError(state);
      expect(actual).toEqual(GLOBAL_FORM_VALIDATION_ERROR);
    });

    it('returns validation error if provider requires username, username has value, but there is no token value', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'azure',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              username: {
                rscValue: {
                  value: 'admin',
                  trimmedValue: 'admin',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              token: {
                rscValue: {
                  value: '',
                  trimmedValue: '',
                  validationErrors: ['Must be non-empty'],
                  isPristine: false,
                },
              },
              baseBranch: {
                rscValue: {
                  value: 'main',
                  trimmedValue: 'main',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              closePrOnFailedChecksEnabled: {
                value: null,
              },
              closePrAfterDaysOpenEnabled: {
                value: null,
              },
              closePrAfterDays: {
                rscValue: {
                  value: null,
                },
              },
            },
          },
        },
      };
      const actual = selectValidationError(state);
      expect(actual).toEqual(GLOBAL_FORM_VALIDATION_ERROR);
    });

    it('returns validation error if provider requires username, token has value, but there is no username value', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'azure',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              username: {
                rscValue: {
                  value: '',
                  trimmedValue: '',
                  validationErrors: ['Must be non-empty'],
                  isPristine: false,
                },
              },
              token: {
                rscValue: {
                  value: 'token',
                  trimmedValue: 'token',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              baseBranch: {
                rscValue: {
                  value: 'main',
                  trimmedValue: 'main',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              closePrOnFailedChecksEnabled: {
                value: null,
              },
              closePrAfterDaysOpenEnabled: {
                value: null,
              },
              closePrAfterDays: {
                rscValue: {
                  value: null,
                },
              },
            },
          },
        },
      };
      const actual = selectValidationError(state);
      expect(actual).toEqual(GLOBAL_FORM_VALIDATION_ERROR);
    });

    it('does not return validation error when switching from GitHub PAT to Bitbucket with token persisted', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                isInherited: false,
                rscValue: {
                  value: 'bitbucket',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              authenticationType: {
                value: 'PAT',
                isInherited: false,
                parentValue: null,
              },
              username: {
                isInherited: false,
                rscValue: {
                  value: 'bitbucket-user',
                  trimmedValue: 'bitbucket-user',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              token: {
                isInherited: false,
                rscValue: {
                  value: 'token-from-github-pat',
                  trimmedValue: 'token-from-github-pat',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              baseBranch: {
                rscValue: {
                  value: 'main',
                  trimmedValue: 'main',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              closePrOnFailedChecksEnabled: {
                value: null,
              },
              closePrAfterDaysOpenEnabled: {
                value: null,
              },
              closePrAfterDays: {
                rscValue: {
                  value: null,
                },
              },
            },
            serverSourceControl: {
              provider: {
                parentValue: {
                  value: 'github',
                },
              },
            },
          },
        },
      };

      expect(selectValidationError(state)).toBeNull();
    });

    it('shows no validation error if provider requires username, but token and username have"t any values', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'azure',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              username: {
                rscValue: {
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              token: {
                rscValue: {
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              baseBranch: {
                rscValue: {
                  value: 'main',
                  trimmedValue: 'main',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              closePrOnFailedChecksEnabled: {
                value: null,
              },
              closePrAfterDaysOpenEnabled: {
                value: null,
              },
              closePrAfterDays: {
                rscValue: {
                  value: null,
                },
              },
            },
          },
        },
      };
      const actual = selectValidationError(state);
      expect(actual).toEqual(null);
    });

    it('shows no validation error if provider doesn"t require username, and token has"t any value', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'github',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              authenticationType: {
                value: 'GITHUB_APP',
              },
              githubApp: {
                value: {
                  installationId: 12345,
                },
              },
              username: {
                rscValue: {
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              token: {
                rscValue: {
                  value: '',
                  trimmedValue: '',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              baseBranch: {
                rscValue: {
                  value: 'main',
                  trimmedValue: 'main',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              closePrOnFailedChecksEnabled: {
                value: null,
              },
              closePrAfterDaysOpenEnabled: {
                value: null,
              },
              closePrAfterDays: {
                rscValue: {
                  value: null,
                },
              },
            },
          },
        },
      };
      const actual = selectValidationError(state);
      expect(actual).toEqual(null);
    });

    it('returns validation error if branch name is missed', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'azure',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              username: {
                rscValue: {
                  value: 'admin',
                  trimmedValue: 'admin',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              token: {
                rscValue: {
                  value: 'password1',
                  trimmedValue: 'password1',
                  validationErrors: null,
                  isPristine: false,
                },
              },
              baseBranch: {
                rscValue: {
                  value: '',
                  trimmedValue: '',
                  validationErrors: ['Must be non-empty'],
                  isPristine: false,
                },
              },
              closePrOnFailedChecksEnabled: {
                value: null,
              },
              closePrAfterDaysOpenEnabled: {
                value: null,
              },
              closePrAfterDays: {
                rscValue: {
                  value: null,
                },
              },
            },
          },
        },
      };
      const actual = selectValidationError(state);
      expect(actual).toEqual(GLOBAL_FORM_VALIDATION_ERROR);
    });

    describe('GitHub App authentication', () => {
      it('does not validate token when provider is GitHub and authenticationType is GITHUB_APP', () => {
        const state = {
          productFeatures: {
            productFeatures: {
              'github-app-authentication': true,
            },
          },
          orgsAndPolicies: {
            sourceControlConfiguration: {
              sourceControl: {
                provider: {
                  rscValue: {
                    value: 'github',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                authenticationType: {
                  value: 'GITHUB_APP',
                },
                githubApp: {
                  value: {
                    installationId: 12345,
                    name: 'test-app',
                    accountName: 'test-org',
                  },
                },
                username: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                token: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                baseBranch: {
                  rscValue: {
                    value: 'main',
                    trimmedValue: 'main',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                closePrOnFailedChecksEnabled: {
                  value: null,
                },
                closePrAfterDaysOpenEnabled: {
                  value: null,
                },
                closePrAfterDays: {
                  rscValue: {
                    value: null,
                  },
                },
              },
            },
          },
        };
        const actual = selectValidationError(state);
        expect(actual).toEqual(null);
      });

      it('returns error when GitHub App authentication is selected but not configured', () => {
        const state = {
          productFeatures: {
            productFeatures: {
              'github-app-authentication': true,
            },
          },
          orgsAndPolicies: {
            sourceControlConfiguration: {
              sourceControl: {
                provider: {
                  rscValue: {
                    value: 'github',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                authenticationType: {
                  value: 'GITHUB_APP',
                },
                githubApp: {
                  value: null,
                },
                username: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                token: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                baseBranch: {
                  rscValue: {
                    value: 'main',
                    trimmedValue: 'main',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                closePrOnFailedChecksEnabled: {
                  value: null,
                },
                closePrAfterDaysOpenEnabled: {
                  value: null,
                },
                closePrAfterDays: {
                  rscValue: {
                    value: null,
                  },
                },
              },
            },
          },
        };
        const actual = selectValidationError(state);
        expect(actual).toEqual(
          'Please configure and install a GitHub App or switch to Personal Access Token authentication.'
        );
      });

      it('validates token when authenticationType is null for backwards compatibility', () => {
        const state = {
          productFeatures: {
            productFeatures: {
              'github-app-authentication': true, // Feature flag enabled for this test
            },
          },
          orgsAndPolicies: {
            sourceControlConfiguration: {
              sourceControl: {
                provider: {
                  rscValue: {
                    value: 'github',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                authenticationType: {
                  value: null,
                },
                username: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                token: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: ['Must be non-empty'],
                    isPristine: false,
                  },
                },
                baseBranch: {
                  rscValue: {
                    value: 'main',
                    trimmedValue: 'main',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                closePrOnFailedChecksEnabled: {
                  value: null,
                },
                closePrAfterDaysOpenEnabled: {
                  value: null,
                },
                closePrAfterDays: {
                  rscValue: {
                    value: null,
                  },
                },
              },
            },
          },
        };
        const actual = selectValidationError(state);
        expect(actual).toEqual('Please select an authentication method (GitHub App or Personal Access Token)');
      });

      it('validates token field when GitHub App feature is DISABLED', () => {
        const state = {
          productFeatures: {
            productFeatures: {
              'github-app-authentication': false, // Feature DISABLED
            },
          },
          orgsAndPolicies: {
            sourceControlConfiguration: {
              sourceControl: {
                provider: {
                  rscValue: {
                    value: 'github',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                authenticationType: {
                  value: null, // No auth type selection when feature is disabled
                },
                token: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: ['Must be non-empty'], // Token has validation error
                    isPristine: false,
                  },
                },
                baseBranch: {
                  rscValue: {
                    value: 'main',
                    trimmedValue: 'main',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                username: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: null,
                    isPristine: false,
                  },
                },
                closePrOnFailedChecksEnabled: {
                  value: null,
                },
                closePrAfterDaysOpenEnabled: {
                  value: null,
                },
                closePrAfterDays: {
                  rscValue: {
                    value: null,
                  },
                },
              },
            },
          },
        };
        const actual = selectValidationError(state);
        // When feature is disabled, should return generic validation error from token field
        expect(actual).toEqual(GLOBAL_FORM_VALIDATION_ERROR);
      });

      describe('Inheritance scenarios', () => {
        it('shows NO error when inheriting and parent has valid GitHub App configured', () => {
          const state = {
            productFeatures: {
              productFeatures: {
                'github-app-authentication': true,
              },
            },
            orgsAndPolicies: {
              sourceControlConfiguration: {
                sourceControl: {
                  provider: {
                    rscValue: {
                      value: 'github',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  authenticationType: {
                    value: 'GITHUB_APP',
                  },
                  githubApp: {
                    isInherited: true,
                    parentValue: {
                      installationId: 67890,
                      name: 'parent-app',
                      accountName: 'parent-org',
                    },
                    value: null,
                  },
                  username: {
                    rscValue: {
                      value: '',
                      trimmedValue: '',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  token: {
                    rscValue: {
                      value: '',
                      trimmedValue: '',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  baseBranch: {
                    rscValue: {
                      value: 'main',
                      trimmedValue: 'main',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  closePrOnFailedChecksEnabled: {
                    value: null,
                  },
                  closePrAfterDaysOpenEnabled: {
                    value: null,
                  },
                  closePrAfterDays: {
                    rscValue: {
                      value: null,
                    },
                  },
                },
              },
            },
          };
          const actual = selectValidationError(state);
          expect(actual).toEqual(null);
        });

        it('shows error when inheriting but parent has NO GitHub App configured', () => {
          const state = {
            productFeatures: {
              productFeatures: {
                'github-app-authentication': true,
              },
            },
            orgsAndPolicies: {
              sourceControlConfiguration: {
                sourceControl: {
                  provider: {
                    rscValue: {
                      value: 'github',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  authenticationType: {
                    value: 'GITHUB_APP',
                  },
                  githubApp: {
                    isInherited: true,
                    parentValue: null, // Parent has no GitHub App configured
                    value: null,
                  },
                  username: {
                    rscValue: {
                      value: '',
                      trimmedValue: '',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  token: {
                    rscValue: {
                      value: '',
                      trimmedValue: '',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  baseBranch: {
                    rscValue: {
                      value: 'main',
                      trimmedValue: 'main',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  closePrOnFailedChecksEnabled: {
                    value: null,
                  },
                  closePrAfterDaysOpenEnabled: {
                    value: null,
                  },
                  closePrAfterDays: {
                    rscValue: {
                      value: null,
                    },
                  },
                },
              },
            },
          };
          const actual = selectValidationError(state);
          expect(actual).toEqual(
            'Please configure and install a GitHub App or switch to Personal Access Token authentication.'
          );
        });

        it('shows error when overriding but own GitHub App is NOT configured', () => {
          const state = {
            productFeatures: {
              productFeatures: {
                'github-app-authentication': true,
              },
            },
            orgsAndPolicies: {
              sourceControlConfiguration: {
                sourceControl: {
                  provider: {
                    rscValue: {
                      value: 'github',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  authenticationType: {
                    value: 'GITHUB_APP',
                  },
                  githubApp: {
                    isInherited: false, // Overriding
                    parentValue: {
                      installationId: 67890,
                      name: 'parent-app',
                      accountName: 'parent-org',
                    },
                    value: null, // But own config is empty
                  },
                  username: {
                    rscValue: {
                      value: '',
                      trimmedValue: '',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  token: {
                    rscValue: {
                      value: '',
                      trimmedValue: '',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  baseBranch: {
                    rscValue: {
                      value: 'main',
                      trimmedValue: 'main',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  closePrOnFailedChecksEnabled: {
                    value: null,
                  },
                  closePrAfterDaysOpenEnabled: {
                    value: null,
                  },
                  closePrAfterDays: {
                    rscValue: {
                      value: null,
                    },
                  },
                },
              },
            },
          };
          const actual = selectValidationError(state);
          expect(actual).toEqual(
            'Please configure and install a GitHub App or switch to Personal Access Token authentication.'
          );
        });

        it('shows NO error when inheriting and parent uses PAT authentication', () => {
          const state = {
            orgsAndPolicies: {
              sourceControlConfiguration: {
                sourceControl: {
                  provider: {
                    rscValue: {
                      value: 'github',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  authenticationType: {
                    value: 'PAT', // Parent uses PAT
                    isInherited: true,
                  },
                  githubApp: {
                    isInherited: true,
                    parentValue: null, // No GitHub App because parent uses PAT
                    value: null,
                  },
                  username: {
                    rscValue: {
                      value: '',
                      trimmedValue: '',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  token: {
                    rscValue: {
                      value: '',
                      trimmedValue: '',
                      validationErrors: null,
                      isPristine: false,
                    },
                    isInherited: true,
                    parentValue: {
                      value: 'masked-token',
                    },
                  },
                  baseBranch: {
                    rscValue: {
                      value: 'main',
                      trimmedValue: 'main',
                      validationErrors: null,
                      isPristine: false,
                    },
                  },
                  closePrOnFailedChecksEnabled: {
                    value: null,
                  },
                  closePrAfterDaysOpenEnabled: {
                    value: null,
                  },
                  closePrAfterDays: {
                    rscValue: {
                      value: null,
                    },
                  },
                },
              },
            },
          };
          const actual = selectValidationError(state);
          expect(actual).toEqual(null);
        });
      });
    });

    describe('GitHub App authentication validation', () => {
      it('skips token and username validation when GitHub App is selected (feature enabled)', () => {
        const state = {
          router: {
            currentState: { name: 'root.application.sourceControl' },
            currentParams: { applicationId: 'app-1' },
          },
          productFeatures: {
            productFeatures: {
              'github-app-authentication': true,
            },
          },
          orgsAndPolicies: {
            sourceControlConfiguration: {
              sourceControl: {
                provider: {
                  rscValue: { value: 'github' },
                  isInherited: false,
                },
                authenticationType: {
                  value: 'GITHUB_APP',
                  isInherited: false,
                  parentValue: null,
                },
                githubApp: {
                  value: { installationId: '12345', accountName: 'testorg' },
                  isInherited: false,
                },
                token: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: ['Token is required'], // This should be ignored
                  },
                  isInherited: false,
                },
                username: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: ['Username is required'], // This should be ignored
                  },
                  isInherited: false,
                },
                baseBranch: {
                  rscValue: { value: 'main', validationErrors: [] },
                  isInherited: false,
                },
                closePrAfterDaysOpenEnabled: { value: false },
                closePrAfterDays: {
                  rscValue: { value: '' },
                  isInherited: false,
                },
                repositoryUrl: {
                  value: 'https://github.com/org/repo',
                  trimmedValue: 'https://github.com/org/repo',
                  validationErrors: [],
                },
              },
              serverSourceControl: {
                provider: { parentValue: { value: 'bitbucket' } },
              },
            },
          },
        };

        const actual = selectValidationError(state);
        expect(actual).toEqual(null);
      });

      it('skips token and username validation when GitHub App is inherited from parent', () => {
        const state = {
          router: {
            currentState: { name: 'root.application.sourceControl' },
            currentParams: { applicationId: 'app-1' },
          },
          productFeatures: {
            productFeatures: {
              'github-app-authentication': true,
            },
          },
          orgsAndPolicies: {
            sourceControlConfiguration: {
              sourceControl: {
                provider: {
                  rscValue: { value: 'github' },
                  isInherited: false,
                },
                authenticationType: {
                  value: null,
                  isInherited: true,
                  parentValue: 'GITHUB_APP', // Inherited GitHub App
                },
                githubApp: {
                  value: null,
                  isInherited: true,
                  parentValue: { installationId: '12345', accountName: 'testorg' },
                },
                token: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: [], // Should not validate
                  },
                  isInherited: false,
                },
                username: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: [], // Should not validate
                  },
                  isInherited: false,
                },
                baseBranch: {
                  rscValue: { value: 'main', validationErrors: [] },
                  isInherited: false,
                },
                closePrAfterDaysOpenEnabled: { value: false },
                closePrAfterDays: {
                  rscValue: { value: '' },
                  isInherited: false,
                },
                repositoryUrl: {
                  value: 'https://github.com/org/repo',
                  trimmedValue: 'https://github.com/org/repo',
                  validationErrors: [],
                },
              },
              serverSourceControl: {
                provider: { parentValue: { value: 'github' } },
              },
            },
          },
        };

        const actual = selectValidationError(state);
        expect(actual).toEqual(null);
      });

      it('skips cross-provider validation when GitHub App is used', () => {
        const state = {
          router: {
            currentState: { name: 'root.application.sourceControl' },
            currentParams: { applicationId: 'app-1' },
          },
          productFeatures: {
            productFeatures: {
              'github-app-authentication': true,
            },
          },
          orgsAndPolicies: {
            sourceControlConfiguration: {
              sourceControl: {
                provider: {
                  rscValue: { value: 'github' },
                  isInherited: false, // Provider overridden to GitHub
                },
                authenticationType: {
                  value: 'GITHUB_APP',
                  isInherited: false,
                },
                githubApp: {
                  value: { installationId: '12345', accountName: 'testorg' },
                  isInherited: false,
                },
                token: {
                  rscValue: { value: '', trimmedValue: '', validationErrors: [] },
                  isInherited: true, // Token still inherited from Azure (cross-provider)
                  parentValue: { value: 'azure-token' },
                },
                username: {
                  rscValue: { value: '', trimmedValue: '', validationErrors: [] },
                  isInherited: true,
                  parentValue: { value: 'azure-user' },
                },
                baseBranch: {
                  rscValue: { value: 'main', validationErrors: [] },
                  isInherited: false,
                },
                closePrAfterDaysOpenEnabled: { value: false },
                closePrAfterDays: {
                  rscValue: { value: '' },
                  isInherited: false,
                },
                repositoryUrl: {
                  value: 'https://github.com/org/repo',
                  trimmedValue: 'https://github.com/org/repo',
                  validationErrors: [],
                },
              },
              serverSourceControl: {
                provider: { parentValue: { value: 'azure' } }, // Parent was Azure
              },
            },
          },
        };

        // Should NOT trigger cross-provider validation error because GitHub App doesn't use tokens
        const actual = selectValidationError(state);
        expect(actual).toEqual(null);
      });

      it('validates token when PAT authentication is selected (not GitHub App)', () => {
        const state = {
          router: {
            currentState: { name: 'root.application.sourceControl' },
            currentParams: { applicationId: 'app-1' },
          },
          productFeatures: {
            productFeatures: {
              'github-app-authentication': true,
            },
          },
          orgsAndPolicies: {
            sourceControlConfiguration: {
              sourceControl: {
                provider: {
                  rscValue: { value: 'github' },
                  isInherited: false,
                },
                authenticationType: {
                  value: 'PAT', // Using PAT, not GitHub App
                  isInherited: false,
                },
                token: {
                  rscValue: {
                    value: '',
                    trimmedValue: '',
                    validationErrors: [], // Empty but should fail at selector level
                  },
                  isInherited: false,
                },
                username: {
                  rscValue: { value: '', trimmedValue: '', validationErrors: [] },
                  isInherited: false,
                },
                baseBranch: {
                  rscValue: { value: 'main', validationErrors: [] },
                  isInherited: false,
                },
                closePrAfterDaysOpenEnabled: { value: false },
                closePrAfterDays: {
                  rscValue: { value: '' },
                  isInherited: false,
                },
                repositoryUrl: {
                  value: 'https://github.com/org/repo',
                  trimmedValue: 'https://github.com/org/repo',
                  validationErrors: [],
                },
              },
              serverSourceControl: {
                provider: { parentValue: { value: 'github' } },
              },
            },
          },
        };

        // Should fail validation because PAT requires token
        const actual = selectValidationError(state);
        expect(actual).toEqual('Access Token is required when using Personal Access Token authentication');
      });
    });
  });

  describe('selectIsAccessTokenRequiredOnNode', () => {
    const {
      selectIsAccessTokenRequiredOnNode,
    } = require('MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors');

    it('returns false when GitHub App auth is selected locally', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'github',
                },
              },
              authenticationType: {
                value: 'GITHUB_APP',
                isInherited: false,
              },
            },
            serverSourceControl: {},
          },
        },
        router: {
          currentState: {
            data: {
              isApp: false,
            },
          },
        },
      };

      const result = selectIsAccessTokenRequiredOnNode(state);

      expect(result).toBe(false);
    });

    it('returns false when GitHub App auth is inherited from parent', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'github',
                },
              },
              authenticationType: {
                value: 'PAT', // Local value is PAT
                isInherited: true, // But it's inherited
                parentValue: 'GITHUB_APP', // Parent has GitHub App
              },
            },
            serverSourceControl: {
              provider: {
                rscValue: {
                  value: 'github',
                },
              },
            },
          },
        },
        router: {
          currentState: {
            data: {
              isApp: false,
            },
          },
        },
      };

      const result = selectIsAccessTokenRequiredOnNode(state);

      expect(result).toBe(false);
    });

    it('returns false for non-GitHub providers regardless of auth type', () => {
      const state = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            sourceControl: {
              provider: {
                rscValue: {
                  value: 'gitlab',
                },
              },
              authenticationType: {
                value: 'PAT',
                isInherited: false,
              },
            },
            serverSourceControl: {
              provider: {
                rscValue: {
                  value: 'gitlab',
                },
              },
            },
          },
        },
        router: {
          currentState: {
            data: {
              isApp: false,
            },
          },
        },
      };

      const result = selectIsAccessTokenRequiredOnNode(state);

      expect(result).toBe(false);
    });
  });

  describe('GitHub App modal and alert selectors', () => {
    describe('selectShowGitHubAppSuccessModal', () => {
      it('returns true when showGitHubAppSuccessModal is true', () => {
        const state = {
          orgsAndPolicies: {
            sourceControlConfiguration: {
              showGitHubAppSuccessModal: true,
            },
          },
        };
        expect(selectShowGitHubAppSuccessModal(state)).toBe(true);
      });

      it('returns false when showGitHubAppSuccessModal is false', () => {
        const state = {
          orgsAndPolicies: {
            sourceControlConfiguration: {
              showGitHubAppSuccessModal: false,
            },
          },
        };
        expect(selectShowGitHubAppSuccessModal(state)).toBe(false);
      });
    });

    describe('selectShowGitHubAppReplacedAlert', () => {
      it('returns true when showGitHubAppReplacedAlert is true', () => {
        const state = {
          orgsAndPolicies: {
            sourceControlConfiguration: {
              showGitHubAppReplacedAlert: true,
            },
          },
        };
        expect(selectShowGitHubAppReplacedAlert(state)).toBe(true);
      });

      it('returns false when showGitHubAppReplacedAlert is false', () => {
        const state = {
          orgsAndPolicies: {
            sourceControlConfiguration: {
              showGitHubAppReplacedAlert: false,
            },
          },
        };
        expect(selectShowGitHubAppReplacedAlert(state)).toBe(false);
      });
    });

    describe('selectIsGitHubAppReplacement', () => {
      it('returns true when isGitHubAppReplacement is true', () => {
        const state = {
          orgsAndPolicies: {
            sourceControlConfiguration: {
              isGitHubAppReplacement: true,
            },
          },
        };
        expect(selectIsGitHubAppReplacement(state)).toBe(true);
      });

      it('returns false when isGitHubAppReplacement is false', () => {
        const state = {
          orgsAndPolicies: {
            sourceControlConfiguration: {
              isGitHubAppReplacement: false,
            },
          },
        };
        expect(selectIsGitHubAppReplacement(state)).toBe(false);
      });
    });
  });
});
