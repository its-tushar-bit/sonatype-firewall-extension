/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/** Which section a row belongs to. */
export type SettingsPageSection = 'my' | 'admin';

/**
 * Every Settings hub item id. Single source of truth shared with
 * SETTINGS_ITEM_SHOW_IF in settingsGating.ts so a new item without a matching
 * visibility predicate — or a predicate with no matching item — is a compile
 * error rather than a render-time `undefined is not a function` that blanks the
 * whole page.
 */
export type SettingsItemId =
  | 'change-password'
  | 'user-details'
  | 'display-theme'
  | 'user-token'
  | 'users'
  | 'user-activity'
  | 'roles'
  | 'administrators'
  | 'product-license'
  | 'ldap'
  | 'nexus-one-ui'
  | 'saml'
  | 'oidc'
  | 'waived-components'
  | 'atlassian-crowd'
  | 'email'
  | 'zscaler'
  | 'proxy'
  | 'webhooks'
  | 'system-notice'
  | 'success-metrics'
  | 'automatic-applications'
  | 'automatic-scm'
  | 'base-url'
  | 'user-tokens';

export interface SettingsPageItem {
  readonly id: SettingsItemId;
  readonly section: SettingsPageSection;
  readonly label: string;
  readonly description: string;
  /**
   * NOUX destination UI-Router state name for items whose admin page is already
   * embedded in the Nexus One shell — see settingsGating.ts for the per-item
   * visibility predicates that mirror this routing. When set, the row links to
   * that page; when omitted, the row falls back to the generic
   * /coming-soon/settings placeholder until its native page is ported.
   */
  readonly stateName?: string;
  /**
   * Extra hash-path prefixes that also belong to this item's experience —
   * needed when create/edit sub-pages live at a URL prefix different from the
   * list page's own URL. E.g. LDAP list is `/ldap-servers` but its create/edit
   * pages are under `/ldap/*`; Webhooks list is `/webhooks/list` but its
   * add/edit pages are `/webhooks/{id}` and `/webhooks/create`. LeftNav uses
   * these to keep the Settings rail entry lit while the user is on any such
   * sub-page. Simple hash paths in this list are matched by
   * `LeftNav.hrefMatches` (exact-or-descendant-prefix).
   */
  readonly additionalActivePrefixes?: readonly string[];
}

export const SETTINGS_PAGE_SECTIONS: ReadonlyArray<{ readonly id: SettingsPageSection; readonly label: string }> = [
  { id: 'my', label: 'My Settings' },
  { id: 'admin', label: 'Admin Console' },
];

export const SETTINGS_PAGE_ITEMS: readonly SettingsPageItem[] = [
  { id: 'change-password', section: 'my', label: 'Change Password', description: 'Update your account password' },
  { id: 'user-details', section: 'my', label: 'User Details', description: 'View and edit your profile information' },
  { id: 'display-theme', section: 'my', label: 'Display Theme', description: 'Choose light or dark interface theme' },
  { id: 'user-token', section: 'my', label: 'User Token', description: 'Create tokens for API authentication' },
  { id: 'users', section: 'admin', label: 'Users', description: 'Create and manage user accounts and their access permissions', stateName: 'users' },
  { id: 'user-activity', section: 'admin', label: 'User Activity', description: 'Track user sign-in and activity history', stateName: 'userActivity' },
  { id: 'roles', section: 'admin', label: 'Roles', description: 'Define permission sets that control what users can do', stateName: 'rolesList' },
  { id: 'administrators', section: 'admin', label: 'Administrators', description: 'Manage users with full system access', stateName: 'administrators' },
  { id: 'product-license', section: 'admin', label: 'Product License', description: 'View and manage your product licenses', stateName: 'productlicense' },
  { id: 'ldap', section: 'admin', label: 'LDAP', description: 'Connect corporate directories for user authentication', stateName: 'ldap-list', additionalActivePrefixes: ['/ldap'] },
  { id: 'nexus-one-ui', section: 'admin', label: 'Nexus One UI', description: 'Customize the new user interface experience', stateName: 'nexusOneUiSettings' },
  { id: 'saml', section: 'admin', label: 'SAML', description: 'Configure single sign-on via your identity provider', stateName: 'saml' },
  { id: 'oidc', section: 'admin', label: 'OIDC', description: 'Configure single sign-on via OpenID Connect' },
  { id: 'waived-components', section: 'admin', label: 'Waived Components', description: 'Manage waived components to suppress policy violations', stateName: 'waivedComponentUpgradesConfiguration' },
  { id: 'atlassian-crowd', section: 'admin', label: 'Atlassian Crowd', description: 'Connect Atlassian Crowd for authentication', stateName: 'atlassianCrowdConfiguration' },
  { id: 'email', section: 'admin', label: 'Email', description: 'Configure email settings for notifications and alerts', stateName: 'mailConfig' },
  { id: 'zscaler', section: 'admin', label: 'Zscaler', description: 'Configure Zscaler network integration' },
  { id: 'proxy', section: 'admin', label: 'Proxy', description: 'Configure proxy settings for outbound network traffic', stateName: 'proxyConfig' },
  { id: 'webhooks', section: 'admin', label: 'Webhooks', description: 'Send notifications to external systems on events', stateName: 'listWebhooks', additionalActivePrefixes: ['/webhooks'] },
  { id: 'system-notice', section: 'admin', label: 'System Notice', description: 'Display banners to communicate with users', stateName: 'systemNoticeConfiguration' },
  { id: 'success-metrics', section: 'admin', label: 'Success Metrics', description: 'Track your security posture improvements', stateName: 'successMetricsConfiguration' },
  { id: 'automatic-applications', section: 'admin', label: 'Automatic Applications', description: 'Auto-create applications from repositories', stateName: 'automaticApplicationsConfiguration' },
  { id: 'automatic-scm', section: 'admin', label: 'Automatic SCM Configuration', description: 'Auto-discover repositories from SCMs', stateName: 'automaticSourceControlConfiguration' },
  { id: 'base-url', section: 'admin', label: 'Base URL', description: 'Set the server URL for correct notification links', stateName: 'baseUrlConfiguration' },
  { id: 'user-tokens', section: 'admin', label: 'User Tokens', description: 'Configure tokens for API authentication', stateName: 'userTokensConfiguration' },
];
