import java.util.Map.Entry

/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Process an attribute value based on the following rules:
 * - If the value is null or empty, return null (don't include in the entry)
 * - If the value is "-" and the field is not required, return an empty string (clear the existing value)
 * - If the value is "-" and the field is required, return null (don't include it in the update)
 * - Otherwise, return the value as is
 *
 * @param value The attribute value to process
 * @param fieldName The name of the field being processed
 * @param validator The validation functions to use
 * @return The processed value, or null if the attribute should be excluded
 */
def processAttributeValue(def value, String fieldName = null, validator) {
  def processedValue = null
  if (value != null && value.toString().trim().length() > 0) {
    if (value == "-") {
      // Check if this is a required field
      if (fieldName != null) {
        // If it's a required field, keep processedValue as null to not include it in the update
        // This will preserve the existing value
        if (!validator.requiredDashboardFields.contains(fieldName) &&
            !validator.requiredGroupFields.contains(fieldName)) {
          // For non-required fields, clear the value
          processedValue = ""
        }
      } else {
        // No field name provided, clear the value
        processedValue = ""
      }
    } else {
      // Use the value as is
      processedValue = value
    }
  }
  return processedValue
}

/**
 * Process entry attributes and create/update an entry.
 *
 * @param existingEntry The existing entry to update, or an empty map for new entries
 * @param attributeMap Map of attribute names to values
 * @param idField The name of the field that contains the entry ID
 * @param validator The validation functions to use
 * @return The updated entry
 */
def processEntryAttributes(Map existingEntry, Map attributeMap, String idField, validator) {
  def entry = [:]

  // Process each attribute
  for (Entry<String, Object> mapEntry : attributeMap.entrySet()) {
    String key = mapEntry.getKey()
    def value = mapEntry.getValue()

    // Process the attribute value
    def processedValue = processAttributeValue(value, key, validator)

    // Add to entry if not null
    if (processedValue != null) {
      // Convert priorityOrder to integer
      if (key == 'priorityOrder') {
        entry[key] = processedValue.toInteger()
      }
      // Boolean values should be added directly
      else if (key == 'spotlight') {
        entry[key] = value
      }
      // All other values
      else {
        entry[key] = processedValue
      }
    }
  }

  // Determine if this is a new entry or an existing one
  boolean isNewEntry = existingEntry.isEmpty()

  // Update with new values, preserving existing values for fields not specified
  def updatedEntry = existingEntry + entry

  // Validate the entry
  validator.validateEntry(updatedEntry, idField, isNewEntry)

  def entryId = attributeMap[idField]
  echo "${isNewEntry ? 'Created' : 'Updated'} ${idField == 'dashboardId' ? 'dashboard' : 'dashboard group'}: ${entryId}"

  return updatedEntry
}

/**
 * Save JSON contents to an S3 bucket.
 *
 * @param bucketName The name of the S3 bucket
 * @param fileName The name of the file to save
 * @param jsonContents The JSON contents to save
 */
def saveToBucket(bucketName, fileName, jsonContents) {
  // The jsonContents parameter is already a JSON string, so we can use it directly
  sh """
cat <<EOF | aws s3 cp - s3://${bucketName}/${fileName}
${jsonContents}
EOF
  """
}

/**
 * Sort dashboards by priorityOrder.
 *
 * @param config The configuration object containing dashboards and dashboardGroups
 */
def sortDashboardsByPriorityOrder(dashboardConfigs) {
  dashboardConfigs.dashboards = sortByPriorityOrder(dashboardConfigs.dashboards, 'dashboardId')
}

def sortByPriorityOrder(dashboardData, idField) {
  def unsorted = dashboardData.collect({ it.value })
  def sorted = sortMapList(unsorted, 'priorityOrder')
  def resultMap = [:]

  // Convert sorted list back to a map using the specified idField
  sorted.each { entry ->
    def entryId = entry[idField]
    resultMap[entryId] = entry
  }
  return resultMap
}

@NonCPS
def sortMapList(List<Map> mapList, String sortKey) {
  return mapList.sort { a, b -> a[sortKey] <=> b[sortKey] }
}

return this
