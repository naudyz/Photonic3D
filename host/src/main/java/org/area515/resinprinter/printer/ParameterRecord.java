package org.area515.resinprinter.printer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Created by zyd on 2017/10/10.
 */

@XmlRootElement(name="ParameterRecord")
public class ParameterRecord
{
    @XmlElement(name="LedUsedTime")
    private long ledUsedTime = 0;
    @XmlElement(name="ScreenUsedTime")
    private long screenUsedTime = 0;

    @XmlTransient
    public long getLedUsedTime()
    {
        return ledUsedTime;
    }
    public void setLedUsedTime(long ledUsedTime)
    {
        this.ledUsedTime = ledUsedTime;
    }

    @XmlTransient
    public long getScreenUsedTime()
    {
        return screenUsedTime;
    }
    public void setScreenUsedTime(long screenUsedTime)
    {
        this.screenUsedTime = screenUsedTime;
    }
}
