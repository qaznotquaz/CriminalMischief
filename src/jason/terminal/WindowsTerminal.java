package jason.terminal;

import com.sun.jna.Function;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

import java.io.IOException;

public class WindowsTerminal {
    /**
     * Send a console command to the command console itself.<br>
     *
     * @param command the command to send.
     * @throws IOException          if the ProcessBuilder encounters an I/O error.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public static void cmd(String command) throws IOException, InterruptedException {
        new ProcessBuilder("cmd", "/c", command).inheritIO().start().waitFor();
    }

    /**
     * Enables virtual terminal sequences on windows. This allows ANSI sequences to function.<br>
     * Modified from source to also disable quick edit mode and adjust console size.
     * <a href=https://stackoverflow.com/a/52767586><b>Source</b></a><br>
     */
    public static void enableVTS() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            // Set output mode to handle virtual terminal sequences
            Function GetStdHandleFunc = Function.getFunction("kernel32", "GetStdHandle");
            WinDef.DWORD STD_OUTPUT_HANDLE = new WinDef.DWORD(-11);
            WinNT.HANDLE hOut = (WinNT.HANDLE) GetStdHandleFunc.invoke(WinNT.HANDLE.class, new Object[]{STD_OUTPUT_HANDLE});

            WinDef.DWORDByReference p_dwMode = new WinDef.DWORDByReference(new WinDef.DWORD(0));
            Function GetConsoleModeFunc = Function.getFunction("kernel32", "GetConsoleMode");
            GetConsoleModeFunc.invoke(WinDef.BOOL.class, new Object[]{hOut, p_dwMode});

            int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 4;
            int extendedFlags = 128;
            WinDef.DWORD dwMode = p_dwMode.getValue();
            dwMode.setValue(dwMode.intValue() | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
            dwMode.setValue(dwMode.intValue() | extendedFlags);
            Function SetConsoleModeFunc = Function.getFunction("kernel32", "SetConsoleMode");
            SetConsoleModeFunc.invoke(WinDef.BOOL.class, new Object[]{hOut, dwMode});
        }
    }
}
