/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import groovy.json.JsonSlurper
import groovy.transform.Field

@Field
def validDashboardAttributes = [
    'dashboardId', 'groupId', 'accessButtonText', 'category', 'dashboardPath',
    'description', 'featuresCsv', 'modelsCsv', 'previewImageFilename',
    'previewImageIcon', 'priorityOrder', 'sinceIQVersion', 'spotlight',
    'spotlightColor', 'spotlightText', 'title', 'allowedSalesforceAccountIdsCsv'
]

@Field
def validGroupAttributes = [
    'groupId', 'description', 'featuresCsv', 'previewImageIcon',
    'sinceIQVersion', 'spotlight', 'spotlightColor', 'spotlightText', 'title'
]

@Field
def requiredDashboardFields = [
    'dashboardId', 'accessButtonText', 'category', 'dashboardPath',
    'description', 'featuresCsv', 'modelsCsv',
    'previewImageFilename', 'previewImageIcon', 'priorityOrder', 'title'
]

@Field
def requiredGroupFields = [
    'groupId', 'description', 'featuresCsv', 'previewImageIcon', 'title'
]

@Field
def dashboardOnlyFields = [
    'accessButtonText', 'category', 'dashboardPath', 'modelsCsv', 'priorityOrder'
]

/**
 * Parse and validate a JSON parameter.
 *
 * @param paramValue The parameter value to parse
 * @param paramName The name of the parameter (for error messages)
 * @return A list of parsed objects, or an empty list if the parameter is empty
 */
def parseJsonParameter(paramValue, paramName) {
  def result = []
  if (paramValue.trim() != '') {
    try {
      def jsonSlurper = new JsonSlurper()
      result = jsonSlurper.parseText(paramValue)
      if (!(result instanceof List)) {
        error "The ${paramName} parameter must be a JSON array."
      }
      if (result.isEmpty()) {
        error "The ${paramName} parameter cannot be an empty JSON array. Please provide at least one entry."
      }
    } catch (Exception e) {
      error "Failed to parse ${paramName} as JSON: ${e.message}"
    }
  }
  return result
}

/**
 * Validate that each item in a list has a required field.
 *
 * @param items The list of items to validate
 * @param requiredField The name of the required field
 * @param itemType The type of item (for error messages)
 */
def validateRequiredFields(items, requiredField, itemType) {
  items.each { item ->
    if (!item[requiredField]) {
      error "Each ${itemType} must include a ${requiredField}. Found ${itemType} without ${requiredField}: ${item}"
    }
  }
}

/**
 * Check if a parameter is empty (either blank text or empty JSON array).
 *
 * @param paramValue The parameter value to check
 * @return true if the parameter is empty, false otherwise
 */
def isEmptyParameter(paramValue) {
  def trimmed = paramValue.trim()
  return trimmed == '' || trimmed == '[]'
}

/**
 * Validate an entry based on its type (dashboard or group).
 *
 * @param entry The entry to validate
 * @param idField The name of the field that contains the entry ID
 * @param isNewEntry Whether this is a new entry (true) or an existing one being updated (false)
 */
def validateEntry(entry, idField, isNewEntry = true) {
  // Determine if this is a dashboard or a group
  // If dashboardId is missing, it's a group
  boolean isDashboard = entry.dashboardId != null

  // Set spotlight to false by default if not specified
  if (entry.spotlight == null) {
    entry.spotlight = false
  }

  // Check for unrecognized attributes
  def validAttributes = isDashboard ? validDashboardAttributes : validGroupAttributes
  def entryId = isDashboard ? entry.dashboardId : entry.groupId
  def entryType = isDashboard ? "dashboard" : "group"

  entry.each { key, value ->
    if (!validAttributes.contains(key)) {
      error "${entryType.capitalize()} with ${idField} '${entryId}' contains unrecognized attribute: ${key}"
    }
  }

  // For existing entries, we only need to validate that the ID field is present
  // which is already done in the processEntries function
  if (!isNewEntry) {
    return
  }

  if (isDashboard) {
    // Validate dashboard required fields
    requiredDashboardFields.each { field ->
      if (entry[field] == null) {
        error "Dashboard with dashboardId '${entry.dashboardId}' is missing required field: ${field}"
      }
    }
  } else {
    // This is a group
    // Ensure groupId is present
    if (entry.groupId == null) {
      error "Group entry is missing required field: groupId"
    }

    // Validate group required fields
    requiredGroupFields.each { field ->
      if (entry[field] == null) {
        error "Group with groupId '${entry.groupId}' is missing required field: ${field}"
      }
    }

    // Ensure groups don't have dashboard-specific attributes
    dashboardOnlyFields.each { field ->
      if (entry[field] != null) {
        error "Group with groupId '${entry.groupId}' contains dashboard-specific field: ${field}"
      }
    }
  }
}

return this
