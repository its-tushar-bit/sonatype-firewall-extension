/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * MOCK DATA — Static preview categories shown to Pro tier users in Custom mode.
 *
 * These are NOT fetched from the database. They exist solely to demonstrate
 * the enterprise categorization capability. When a Pro user upgrades to Enterprise,
 * their actual categories will come from the server and may differ.
 *
 * Enterprise categories shown in Custom mode for Pro tier users.
 *
 * PRO TIER FEATURE: These categories are displayed when Pro tier users switch to "Custom" mode
 * in the Application Category Assignment and Policy Inheritance views. This provides a preview
 * of the enterprise-level categorization capabilities available with a Full/Enterprise license.
 *
 * Used by:
 * - AssignAppCategory.jsx - Application Category Assignment page
 * - EditPolicyInheritance.jsx - Policy Inheritance section when selecting
 *   "Applications of the specified Application Categories"
 */
const enterpriseCategories = [
  { id: 'empty-apps', name: 'Empty Applications', color: 'light-green', isApplied: false, isEnterpriseMock: true },
  { id: 'fork', name: 'Fork', color: 'light-red', isApplied: false, isEnterpriseMock: true },
  { id: 'hosted', name: 'Hosted', color: 'light-blue', isApplied: false, isEnterpriseMock: true },
  { id: 'internal', name: 'Internal', color: 'dark-purple', isApplied: false, isEnterpriseMock: true },
  { id: 'legacy', name: 'Legacy', color: 'orange', isApplied: false, isEnterpriseMock: true },
  { id: 'experimental', name: 'Experimental', color: 'dark-green', isApplied: false, isEnterpriseMock: true },
  { id: 'production', name: 'Production', color: 'light-purple', isApplied: false, isEnterpriseMock: true },
  { id: 'development', name: 'Development', color: 'yellow', isApplied: false, isEnterpriseMock: true },
];

export default enterpriseCategories;
