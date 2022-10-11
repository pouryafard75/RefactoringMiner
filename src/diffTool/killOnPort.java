package diffTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class killOnPort {
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String WIN_COMMAND_1 = String.format("netstat -ano | findstr :%d",5678); //TODO USE PORT


    public static void main(String[] args) throws IOException {
        if (isWindows())
        {
            System.out.println(WIN_COMMAND_1);
            Process process = Runtime.getRuntime().exec(WIN_COMMAND_1);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

        }
        else if (isMac())
        {

        }
        else if (isUnix())
        {

        }
    }
    public static boolean isWindows() {
        return (OS.contains("win"));
    }

    public static boolean isMac() {
        return (OS.contains("mac"));
    }

    public static boolean isUnix() {
        return (OS.contains("nix")
                || OS.contains("nux")
                || OS.contains("aix"));
    }

}
