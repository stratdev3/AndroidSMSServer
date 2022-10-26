package github.umer0586.smsserver.smssender;

public class SMSResult {

    public static final int STATUS_SENT_SUCCESS = 0;
    public static final int STATUS_SENT_FAIL = 1;
    public static final int STATUS_EXCEPTION_OCCURRED = 3;

    private int status;
    private String reason;

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

}
