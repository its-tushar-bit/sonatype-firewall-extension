/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.signature.ComponentWithSignaturesList;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

public class ScanClient
    extends AbstractRequestClient
{
  private static final ContentType GZIP_CONTENT_TYPE = ContentType.create("application/x-gzip");

  private final String serverUrl;

  private final String applicationPublicId;

  public ScanClient(final Configuration config, final String applicationPublicId) {
    super(config);

    this.serverUrl = config.getServerUrl();
    this.applicationPublicId =
        UrlUtils.encodeUrlComponent(ApplicationIdUtils.normalizeApplicationPublicId(applicationPublicId));
  }

  public ScanReceipt uploadCLIScan(final File scanFile, ClientScanType clientScanType) throws IOException {
    return handleUpload("rest/cli/scan", scanFile, clientScanType);
  }

  private ScanReceipt handleUpload(String url, File scanFile, ClientScanType clientScanType) throws IOException {
    final Result result = path(url, applicationPublicId).query("scanType", clientScanType.name())
        .put(new FileEntity(scanFile, GZIP_CONTENT_TYPE));
    return parseResult(result, ScanReceipt.class);
  }

  /**
   * Exports links to the results of the scan to the specified output JSON file for use by 3rd-party tools.
   * 
   * @since 1.9.1
   */
  public void saveResultData(File resultFile,
                             ScanReceipt receipt,
                             PolicyEvaluationResult eval,
                             String outcome) throws IOException
  {
    ResultData resultData = new ResultData();
    resultData.applicationId = applicationPublicId;
    resultData.scanId = receipt.getScanId();
    resultData.reportHtmlUrl = receipt.resolveReportUrl(serverUrl);
    resultData.reportPdfUrl = receipt.resolvePdfUrl(serverUrl);
    resultData.reportDataUrl = receipt.resolveDataUrl(serverUrl);
    resultData.policyEvaluationResult = eval;
    resultData.policyAction = outcome;
    JsonUtils.write(resultFile, resultData);
  }

  /**
   * Exports error information to the specified output JSON file for use by 3rd-party tools.
   *
   * @since 1.72.0
   */
  public void saveErrorData(File errorFile, String errorMessage, boolean isSystemError) throws IOException {
    saveErrorData(errorFile, errorMessage, isSystemError, false);
  }

  /**
   * Exports error information to the specified output JSON file for use by 3rd-party tools.
   *
   * @since 1.163.0
   */
  public void saveErrorData(File errorFile, String errorMessage, boolean isSystemError, boolean isScanningError)
      throws IOException
  {
    ErrorData errorData = new ErrorData();
    errorData.errorMessage = errorMessage;
    errorData.isSystemError = isSystemError;
    errorData.isScanningError = isScanningError;
    JsonUtils.write(errorFile, errorData);
  }

  /**
   * Fetches the vulnerable function/method signatures of the components in a scan.
   * 
   * @param scanId ID for the scan/report.
   * @return DTO containing the list of vulnerable function/method signatures.
   * @throws IOException If there is an error calling IQ Server.
   * @since 1.152.0
   */
  public ComponentWithSignaturesList getVulnerableComponentsWithSignatures(String scanId) throws IOException {
    Result result =
        path("api/experimental/signatures/vulnerability/application/publicId/", applicationPublicId, "/report", scanId)
            .post(null);
    return parseResult(result, ComponentWithSignaturesList.class);
  }
}
