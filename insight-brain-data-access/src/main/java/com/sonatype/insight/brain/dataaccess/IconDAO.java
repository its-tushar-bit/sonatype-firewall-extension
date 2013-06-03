/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.sonatype.insight.error.exception.BadRequestException;

class IconDAO
{
    public byte[] getIcon( String ownerId, File iconDirectory )
        throws IOException
    {
        File applicationIconDirectory = new File( iconDirectory, ownerId );
        if ( !applicationIconDirectory.exists() )
        {
            return null;
        }
        File iconFile = new File( applicationIconDirectory, "icon420px.png" );
        if ( !iconFile.exists() )
        {
            return null;
        }

        BufferedImage image = ImageIO.read( iconFile );
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write( image, "png", byteArrayOutputStream );
        return byteArrayOutputStream.toByteArray();
    }

    public void setIcon( String ownerId, File iconDirectory, InputStream imageStream )
        throws IOException
    {
        final int dimension = 420;
        Image image = ImageIO.read( imageStream );

        // Invalid image types do not throw exception on ImageIO.read but instead returns null. Throw exception when
        // null is returned
        if ( image == null )
        {
            throw new BadRequestException( "Invalid image file." );
        }

        BufferedImage resizedImage = new BufferedImage( dimension, dimension, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage( image, 0, 0, dimension, dimension, null );
        g.dispose();

        File applicationIconDirectory = new File( iconDirectory, ownerId );
        if ( !applicationIconDirectory.exists() )
        {
            applicationIconDirectory.mkdirs();
        }

        File iconFile = new File( applicationIconDirectory, "icon420px.png" );
        if ( !iconFile.exists() )
        {
            iconFile.createNewFile();
        }

        ImageIO.write( resizedImage, "png", iconFile );
    }
}
