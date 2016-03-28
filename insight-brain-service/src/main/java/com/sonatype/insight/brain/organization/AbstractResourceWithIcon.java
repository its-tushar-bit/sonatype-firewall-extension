/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.dataaccess.IconDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.utils.NgUploadResponseGenerator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.sun.jersey.core.header.FormDataContentDisposition;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractResourceWithIcon
{
  public static final String GENERATE_ICON_PATH = "services/generateIcon/{hashcode}";

  public static final String ICON_PATH = "icon";

  private static final Logger log = LoggerFactory.getLogger(AbstractResourceWithIcon.class);

  private final HdsClient client;

  private final BaseUrl baseUrl;

  private final NgUploadResponseGenerator ngUploadResponseGenerator;

  protected AbstractResourceWithIcon(HdsClient client,
                                     BaseUrl baseUrl,
                                     NgUploadResponseGenerator ngUploadResponseGenerator)
  {
    this.client = client;
    this.baseUrl = baseUrl;
    this.ngUploadResponseGenerator = ngUploadResponseGenerator;
  }

  private void setIcon(String ownerId,
                       File iconDir,
                       boolean hasRobotSource,
                       String robotHash,
                       InputStream uploadedInputStream,
                       FormDataContentDisposition fileDetail) throws IOException
  {
    if (hasRobotSource) {
      try {
        HttpResponse iconResponse = client.getResponse(null, "rest/application/icon/generate/" + robotHash, null,
            (String) null);
        uploadedInputStream = iconResponse.getEntity().getContent();
      }
      catch (Exception e) {
        log.error(e.getMessage(), e);
        if (uploadedInputStream != null) {
          uploadedInputStream.close();
          uploadedInputStream = null;
        }
      }
    }

    byte[] imageByteArray = null;
    if (uploadedInputStream != null) {
      // Copy the uploadInputStream to bytes to enforce size limitation (5 MB)
      ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
      try {
        for (int b = 0; (b = uploadedInputStream.read()) != -1;) {
          if (imageOutputStream.size() > 5242880) {
            throw new BadRequestException("Icon file size must be smaller than 5 MB.");
          }
          imageOutputStream.write(b);
        }
        imageByteArray = imageOutputStream.toByteArray();
      }
      finally {
        imageOutputStream.close();
        uploadedInputStream.close();
      }

      if (imageByteArray != null && imageByteArray.length > 0) {
        InputStream sizeCheckedInputStream = new ByteArrayInputStream(imageByteArray);
        try {
          new IconDAO().setIcon(ownerId, iconDir, sizeCheckedInputStream);
        }
        catch (IllegalArgumentException | IOException | BadRequestException e) {
          throw new BadRequestException(fileDetail.getFileName()
              + " is not a valid image. Make sure the image is in PNG, JPEG, GIF, BMP, or WBMP format.", e);
        }
        finally {
          sizeCheckedInputStream.close();
        }
      }
    }
  }

  protected Response setIcon(final String ownerId,
                             final File iconDir,
                             final boolean hasRobotSource,
                             final String robotHash,
                             final InputStream uploadedInputStream,
                             final FormDataContentDisposition fileDetail,
                             String csrfToken,
                             HttpHeaders headers,
                             boolean noFormData) throws Exception
  {
    return ngUploadResponseGenerator.run(csrfToken, headers, noFormData, new Callable<Void>()
    {
      @Override
      public Void call() throws Exception {
        setIcon(ownerId, iconDir, hasRobotSource, robotHash, uploadedInputStream, fileDetail);
        return null;
      }
    });
  }

  protected Response generateIcon(final String hashcode, final HttpServletRequest req) throws IOException {
    if (hashcode == null || hashcode.isEmpty()) {
      throw new NotFoundException("Null or empty hashcode.");
    }
    return client.doProxy(req, "rest/application/icon/generate/" + hashcode);
  }

  protected Response getIcon(final String ownerId, File iconDir) throws IOException {
    byte[] imageBytes = null;
    if (ownerId != null) {
      imageBytes = new IconDAO().getIcon(ownerId, iconDir);
    }
    if (imageBytes == null) {
      UriBuilder defaultIconUriBuilder = baseUrl.redirect().path(InsightBrainService.BRAIN_ASSET_PATH)
          .path("img/" + getDefaultIconFilename());
      return Response.temporaryRedirect(defaultIconUriBuilder.build()).build();
    }
    final byte[] imageOutputBytes = imageBytes;
    StreamingOutput stream = new StreamingOutput()
    {
      @Override
      public void write(OutputStream output) throws IOException, WebApplicationException {
        output.write(imageOutputBytes);
      }
    };
    return Response.ok(stream).build();
  }

  protected abstract String getDefaultIconFilename();
}
