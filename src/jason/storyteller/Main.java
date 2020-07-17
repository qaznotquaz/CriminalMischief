package jason.storyteller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import static jason.storyteller.ANSI.*;

public class Main {
    static int formatWidth = 62;
    //todo: clean up this animation formatting
    static String[][] dotdotdot = {{"\u001B[42m \n ", " \n "}, {".\n ", " \n "}, {".\n.", " \n "}, {".\n.", "\u001B[104m \n."}, {".\n.", ".\n."}, {" \n.", ".\n."}, {" \n ", ".\n."}, {" \n ", ".\n \u001B[0m"}};
    static int currentConsoleLine = 1;

    public static void main(String[] args) throws InterruptedException, IOException {
        setupConsole();

        Map<String, String> myColors = new HashMap<>();
        myColors.put("BRIGHT_RED", BRIGHT_RED);
        myColors.put("CYAN", CYAN);
        myColors.put("BG_WHITE", BG_WHITE);
        myColors.put("BLUE", BLUE);
        myColors.put("RED", RED);
        myColors.put("UNDERLINE", UNDERLINE);
        myColors.put("INVERT", INVERT);
        ANSI.setDynamicColor(myColors);

        Script script = new Script("test.json");

        for (JSONObject line : script) {
            actLine(line);
        }

        Thread.sleep(10000);
    }

    public static void actLine(JSONObject line) throws InterruptedException {
        Script.LineType type = line.getEnum(Script.LineType.class, "type");

        switch (type) {
            case dm:
                displayDm(Objects.requireNonNull(Script.getActor(line.getString("from"))),
                        line);
                Thread.sleep(line.getInt("delay"));
                break;
            case diagnostic:
                writeGradually(otherHighlight(line.getString("text"), RESET), new int[]{30, 30, 30}, false);

        }
    }

    //todo: label
    public static String otherHighlight(String line, String defCol) {
        AtomicReference<String> text = new AtomicReference<>();
        text.set(line);
        dynamicColor.forEach((k, v) -> text.set(text.get().replace(String.format("[%s]", k), v)));

        text.set(text.get().replace("[RESET]", defCol));

        return text.get();
    }

    public static String otherHighlightBackwards(String line, String defCol) {
        AtomicReference<String> text = new AtomicReference<>();
        text.set(line);
        dynamicColor.forEach((k, v) -> text.set(text.get().replace(StringUtils.reverse(String.format("[%s]", k)), v)));

        text.set(text.get().replace(StringUtils.reverse("[RESET]"), defCol));

        return text.get();
    }

    //todo: label
    public static void displayDm(Script.MiniActor actor, JSONObject line) throws InterruptedException {
        String text = line.getString("text");
        int othersLen = 0;

        /*if (formatWidth + othersLen + 18 > 61) {
            throw new ScriptFormattingException(String.format("line \"%s\" is too long.", text));
        }*/

        if (actor.isBackwards()) {
            /*String toPrint = String.format("%s%s%s < %s%s%s ",
                    actor.getColor(), text, RESET,
                    actor.getColor(), actor.getName(), RESET);*/
            String toPrint = String.format(" %s%s%s > %s%s%s",
                    actor.getColor(), StringUtils.reverse(actor.getName()), RESET,
                    actor.getColor(), StringUtils.reverse(text), RESET);
            toPrint = otherHighlightBackwards(toPrint, actor.getColor());
            //StringBuilder finalLine = new StringBuilder(String.format("%" + (formatWidth + 18 + othersLen) + "s\n", toPrint));
            //finalLine.reverse();

            writeGradually(toPrint, new int[]{10, 200, 30}, true);
        } else {
            String toPrint = String.format(" %s%s%s > %s%s%s",
                    actor.getColor(), actor.getName(), RESET,
                    actor.getColor(), text, RESET);
            toPrint = otherHighlight(toPrint, actor.getColor());

            writeGradually(toPrint, new int[]{10, 200, 30}, false);
        }
    }

    @SuppressWarnings("BusyWait")
    public static void writeGradually(String line, int[] speeds, boolean backwards) throws InterruptedException {
        boolean escaped = false;
        int speed = speeds[0];

        if(backwards){
            System.out.print(CUP(currentConsoleLine, formatWidth));
        } else {
            System.out.print(CUP(currentConsoleLine, 1));
        }

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            System.out.print(c);

            if (c == '\u001B') {
                escaped = true;
            }

            if (!escaped && !Character.isSpaceChar(c)){
                Thread.sleep(speed);
            }

            if (c == '>'){
                Thread.sleep(speeds[1]);
                speed = speeds[2];
            }

            if(backwards && !escaped){
                System.out.print(CUB(2));
            }

            if (c == 'm'){
                escaped = false;
            }
        }

        currentConsoleLine++;
    }

    //todo: label
    //todo: clean up animation formatting
    public static void animate(String[] frames, int speed, int length) throws InterruptedException {
        for (int i = 0; i < length; i++) {
            System.out.print(frames[i % frames.length]);
            Thread.sleep(speed);
            System.out.print(new String(new char[frames[i % frames.length].length()]).replace("\0", "\b"));
            //cls();
        }
    }

    //todo: label
    //todo: clean up animation formatting
    public static void complexAnimate(String[][] frames, int horOrig, int verOrig, int width, int height, int speed, int length) throws InterruptedException {
        System.out.print(code("s"));
        String[] frame;
        for (int i = 0; i < length; i++) {
            for (int j = verOrig; j < verOrig + height; j++) {
                frame = frames[i % frames.length][j - verOrig].split("\n");
                for (int k = horOrig; k < horOrig + width; k++) {
                    System.out.print(CUP(j, k));
                    System.out.print(frame[k - horOrig]);
                }
            }
            Thread.sleep(speed);
        }
        System.out.print(code("u"));
    }

    public static void cmd(String command) throws IOException, InterruptedException {
        new ProcessBuilder("cmd", "/c", command).inheritIO().start().waitFor();
    }

    /**
     * Enables virtual terminal sequences on windows. This allows ANSI sequences to function.<br>
     * Modified from source to also disable quick edit mode and adjust console size.
     * <a href=https://stackoverflow.com/a/52767586>Source</a>
     */
    public static void setupConsole() throws IOException, InterruptedException {
        if (System.getProperty("os.name").startsWith("Windows")) {
            // Set output mode to handle virtual terminal sequences
            Function GetStdHandleFunc = Function.getFunction("kernel32", "GetStdHandle");
            DWORD STD_OUTPUT_HANDLE = new DWORD(-11);
            HANDLE hOut = (HANDLE) GetStdHandleFunc.invoke(HANDLE.class, new Object[]{STD_OUTPUT_HANDLE});

            DWORDByReference p_dwMode = new DWORDByReference(new DWORD(0));
            Function GetConsoleModeFunc = Function.getFunction("kernel32", "GetConsoleMode");
            GetConsoleModeFunc.invoke(BOOL.class, new Object[]{hOut, p_dwMode});

            int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 4;
            int extendedFlags = 128;
            DWORD dwMode = p_dwMode.getValue();
            dwMode.setValue(dwMode.intValue() | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
            dwMode.setValue(dwMode.intValue() | extendedFlags);
            Function SetConsoleModeFunc = Function.getFunction("kernel32", "SetConsoleMode");
            SetConsoleModeFunc.invoke(BOOL.class, new Object[]{hOut, dwMode});
        }

        cmd(String.format("mode con: cols=%s", formatWidth));
    }
}