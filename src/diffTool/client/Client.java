package diffTool.client;


public abstract class Client {
    @SuppressWarnings("unused")
    public Client(String[] args) {}

    public abstract void run() throws Exception;
}
