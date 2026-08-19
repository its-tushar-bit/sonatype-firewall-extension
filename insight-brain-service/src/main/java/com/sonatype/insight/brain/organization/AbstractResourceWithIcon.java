/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.IconDAO;
import com.sonatype.insight.brain.service.AssetPaths;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.NgUploadResponseGenerator;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

public abstract class AbstractResourceWithIcon
{
  public static final String GENERATE_ICON_PATH = "services/generateIcon/{hashcode}";

  public static final String ICON_PATH = "icon";

  private final BaseUrl baseUrl;

  private final NgUploadResponseGenerator ngUploadResponseGenerator;

  private final RobotImageService robotImageService;

  protected AbstractResourceWithIcon(
      BaseUrl baseUrl,
      NgUploadResponseGenerator ngUploadResponseGenerator,
      RobotImageService robotImageService)
  {
    this.baseUrl = baseUrl;
    this.ngUploadResponseGenerator = ngUploadResponseGenerator;
    this.robotImageService = robotImageService;
  }

  private void setIcon(
      String ownerId,
      File iconDir,
      boolean hasRobotSource,
      String hashcode,
      InputStream uploadedInputStream,
      FormDataContentDisposition fileDetail) throws IOException
  {
    if (hasRobotSource) {
      AuditData.get().setData("iconType", "robot");
      try (InputStream robotStream = new ByteArrayInputStream(robotImageService.getImage(hashcode))) {
        // robot image is expected to be small, so avoid size check
        new IconDAO().setIcon(ownerId, iconDir, robotStream);
        return;
      }
    }
    byte[] imageByteArray = {};
    if (uploadedInputStream != null) {
      // Copy the uploadInputStream to bytes to enforce size limitation (5 MB)
      try (ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream()) {
        for (int b = 0; (b = uploadedInputStream.read()) != -1;) {
          if (imageOutputStream.size() > 5242880) {
            throw new BadRequestException("Icon file size must be smaller than 5 MB.");
          }
          imageOutputStream.write(b);
        }
        imageByteArray = imageOutputStream.toByteArray();
      }
      finally {
        uploadedInputStream.close();
      }
    }
    if (imageByteArray.length > 0) {
      AuditData.get().setData("iconType", "file").setData("iconFilename", fileDetail.getFileName());
      try (InputStream sizeCheckedInputStream = new ByteArrayInputStream(imageByteArray)) {
        new IconDAO().setIcon(ownerId, iconDir, sizeCheckedInputStream);
      }
      catch (IllegalArgumentException | IOException | BadRequestException e) {
        throw new BadRequestException(fileDetail.getFileName()
            + " is not a valid image. Make sure the image is in PNG, JPEG, GIF, BMP, or WBMP format.", e);
      }
    }
    else {
      AuditData.get().setData("iconType", "default");
      new IconDAO().deleteIcon(ownerId, iconDir);
    }
  }

  protected Response setIcon(
      final String ownerId,
      final File iconDir,
      final boolean hasRobotSource,
      final String hashcode,
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
        setIcon(ownerId, iconDir, hasRobotSource, hashcode, uploadedInputStream, fileDetail);
        return null;
      }
    });
  }

  protected Response generateIcon(final String hashcode) {
    return Response.ok(robotImageService.getImage(hashcode)).build();
  }

  protected Response getIcon(final String ownerId, File iconDir) throws IOException {
    byte[] imageBytes = null;
    if (ownerId != null) {
      imageBytes = new IconDAO().getIcon(ownerId, iconDir);
    }
    if (imageBytes == null) {
      UriBuilder defaultIconUriBuilder = baseUrl.redirect()
          .path(AssetPaths.BRAIN_ASSET_PATH)
          .path("img/" + getDefaultIconFilename(ownerId));
      return Response.temporaryRedirect(defaultIconUriBuilder.build()).build();
    }
    final byte[] imageOutputBytes = imageBytes;
    return Response.ok(imageOutputBytes).build();
  }

  protected abstract String getDefaultIconFilename(String ownerId);
}
