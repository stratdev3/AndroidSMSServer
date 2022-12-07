package github.umer0586.smsserver.httpserver;

public class ServerInfo {

    private String IPAddress;
    private int port;
    private boolean isSecure;

    public ServerInfo(String IPAddress, int port, boolean isSecure)
    {
        this.IPAddress = IPAddress;
        this.port = port;
        this.isSecure = isSecure;
    }

    public boolean isSecure()
    {
        return isSecure;
    }

    public String getIPAddress()
    {
        return IPAddress;
    }

    public int getPort()
    {
        return port;
    }
}
