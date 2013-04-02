/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "application")
public class Application
    implements HasStringId
{
    @Id
    @Column(name = "application_id")
    private String id;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "public_id_lowercase")
    private String publicIdLowercase;

    @Column( name = "name" )
    private String name;

    @Column( name = "name_lowercase_no_whitespace" )
    private String nameLowercaseNoWhitespace;

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public void setId( String id )
    {
        this.id = id;
    }

    public String getPublicId()
    {
        return publicId;
    }

    public void setPublicId( String publicId )
    {
        if ( publicId != null )
        {
            publicId = publicId.trim();
            publicIdLowercase = publicId.toLowerCase( Locale.ENGLISH );
        }
        else
        {
            publicIdLowercase = null;
        }
        this.publicId = publicId;
    }

    public String getPublicIdLowercase()
    {
        return publicIdLowercase;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        if ( name != null )
        {
            name = name.trim();
            nameLowercaseNoWhitespace = name.replaceAll( "\\s", "" ).toLowerCase( Locale.ENGLISH );
        }
        else
        {
            nameLowercaseNoWhitespace = null;
        }
        this.name = name;
    }

    public String getNameLowercaseNoWhitespace()
    {
        return nameLowercaseNoWhitespace;
    }

    /**
     * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
     * publicIdLowercase field. If this method is not defined, jackson will set/access the publicIdLowercase field
     * directly via reflection, possibly setting it to an incorrect value.
     * 
     * @deprecated This method should not be used explicitly.
     */
    public void setPublicIdLowercase( String publicIdLowercase )
    {
    }

    /**
     * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
     * nameLowercaseNoWhitespace field. If this method is not defined, jackson will set/access the
     * nameLowercaseNoWhitespace field directly via reflection, possibly setting it to an incorrect value.
     * 
     * @deprecated This method should not be used explicitly.
     */
    public void setNameLowercaseNoWhitespace( String nameLowercaseNoWhitespace )
    {
    }
}
