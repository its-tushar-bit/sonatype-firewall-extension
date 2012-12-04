package com.sonatype.insight.brain.model.label;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table( name = "app_label" )
public class ApplicationLabel
    implements HasStringId
{
    @Id
    @Column( name = "app_label_id" )
    private String id;

    @Column( name = "app_id" )
    private String applicationId;

    @Column( name = "label" )
    private String label;

    @Column( name = "label_lowercase" )
    private String labelLowercase;

    @Column( name = "color" )
    @Enumerated( EnumType.STRING )
    private Color color;

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

    public Color getColor()
    {
        return color;
    }

    public void setColor( Color color )
    {
        this.color = color;
    }

    @Override
    public String toString()
    {
        return "Label=" + label;
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
