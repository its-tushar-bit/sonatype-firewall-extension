package com.sonatype.insight.brain.model.label;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table( name = "component_label" )
public class ComponentLabel
    implements HasStringId
{
    /*
     * The delete_time column is used in a db unique constraint, so it cannot be nullable. The NULL_DELETE_TIME value is
     * used as fill in value for records that are not deleted (i.e. active).
     */
    public static final Date NULL_DELETE_TIME = new Date( 0 );

    @Id
    @Column( name = "component_label_id" )
    private String id;

    @Column( name = "application_id" )
    private String applicationId;

    @Column( name = "hash" )
    private String hash;

    @Column( name = "label" )
    private String label;

    @Column( name = "label_lowercase" )
    private String labelLowercase;

    @Column( name = "active" )
    private boolean active = true;

    @Column( name = "create_user_id" )
    private String createUserId;

    @Column( name = "create_time" )
    private Date createTime;

    @Column( name = "delete_user_id" )
    private String deleteUserId;

    @Column( name = "delete_time" )
    private Date deleteTime;

    public ComponentLabel()
    {
    }

    public ComponentLabel( String label )
    {
        setLabel( label );
    }

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

    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId( String applicationId )
    {
        this.applicationId = applicationId;
    }

    public String getHash()
    {
        return hash;
    }

    public void setHash( String hash )
    {
        this.hash = hash;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel( String label )
    {
        this.label = label;
        if ( label == null )
        {
            labelLowercase = null;
        }
        else
        {
            labelLowercase = label.toLowerCase();
        }
    }

    public String getLabelLowercase()
    {
        return labelLowercase;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive( boolean active )
    {
        this.active = active;
    }

    public String getCreateUserId()
    {
        return createUserId;
    }

    public void setCreateUserId( String createUserId )
    {
        this.createUserId = createUserId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime( Date createTime )
    {
        this.createTime = createTime;
    }

    public String getDeleteUserId()
    {
        return deleteUserId;
    }

    public void setDeleteUserId( String deleteUserId )
    {
        this.deleteUserId = deleteUserId;
    }

    public Date getDeleteTime()
    {
        return deleteTime;
    }

    public void setDeleteTime( Date deleteTime )
    {
        this.deleteTime = deleteTime;
    }

    /**
     * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
     * labelLowercase field. If this method is not defined, jackson will set/access the labelLowercase field directly
     * via reflection, possibly setting it to an incorrect value.
     * 
     * @deprecated This method should not be used explicitly.
     */
    public void setLabelLowercase( String labelLowercase )
    {
    }
}
