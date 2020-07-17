package jason.storyteller;

import java.io.IOException;
import java.util.Arrays;
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
import static jason.storyteller.Animations.animate;
import static jason.storyteller.Animations.wipeBox;
import static jason.storyteller.Script.getSceneLength;

public class Main {
    static int[] consoleSize;
    static int currentConsoleLine = 1;
    static int[] chatDefaultSpeeds = new int[]{20, 200, 30};

    public static void main(String[] args) throws InterruptedException, IOException {
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
        consoleSize = new int[]{62, getSceneLength()};

        setupConsole();

        for (JSONObject line : script) {
            actLine(line);
        }
    }

    public static void actLine(JSONObject line) throws InterruptedException {
        Script.LineType type = line.getEnum(Script.LineType.class, "type");
        Thread.sleep(line.getInt("delay"));

        switch (type) {
            case dm:
                displayDm(Objects.requireNonNull(Script.getActor(line.getString("from"))),
                        line);
                break;
            case diagnostic:
                writeGradually(otherHighlight(line.getString("text"), RESET), new int[]{10, 0, 0}, false);
                break;
            case animation:
                animate(line.getString("animation"), line.getJSONObject("settings"));
                break;
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
        String toPrint;
        int[] speeds = Arrays.copyOf(chatDefaultSpeeds, 3);
        JSONObject jsonSpeeds = line.optJSONObject("speeds");

        if(jsonSpeeds != null){
            if(jsonSpeeds.has("name")){
                speeds[0] = jsonSpeeds.getInt("name");
            }
            if(jsonSpeeds.has("tick")){
                speeds[1] = jsonSpeeds.getInt("tick");
            }
            if(jsonSpeeds.has("text")){
                speeds[2] = jsonSpeeds.getInt("text");
            }
        }

        /*if (formatWidth + othersLen + 18 > 61) {
            throw new ScriptFormattingException(String.format("line \"%s\" is too long.", text));
        }*/

        if (actor.isBackwards()) {
            toPrint = String.format(" %s%s%s < %s%s%s",
                    actor.getColor(), StringUtils.reverse(actor.getName()), RESET,
                    actor.getColor(), StringUtils.reverse(text), RESET);
            toPrint = otherHighlightBackwards(toPrint, actor.getColor());

            toPrint = processBackwardLinebreaks(toPrint);
        } else {
            toPrint = String.format(" %s%s%s > %s%s%s",
                    actor.getColor(), actor.getName(), RESET,
                    actor.getColor(), text, RESET);
            toPrint = otherHighlight(toPrint, actor.getColor());
        }

        writeGradually(toPrint, speeds, actor.isBackwards());
    }

    @SuppressWarnings("BusyWait")
    public static void writeGradually(String line, int[] speeds, boolean backwards) throws InterruptedException {
        boolean escaped = false;
        int speed = speeds[0];
        int linebreakCol = -1;

        int col = backwards ? consoleSize[0] : 1;
        System.out.print(CUP(currentConsoleLine, col));

        for (int i = 0; i < line.length(); i++) {
            if (col == consoleSize[0] && !backwards){
                while(line.charAt(i) != ' '){
                    i--;
                    col--;
                    System.out.printf(" %s", CUP(currentConsoleLine, col));
                }
                i++;
                col = linebreakCol;
                currentConsoleLine++;
                System.out.print(CUP(currentConsoleLine, col));
            }

            char c = line.charAt(i);

            if (c=='\n' && backwards){
                /*while(line.charAt(i) != ' '){
                    i--;
                    col--;
                    System.out.printf(" %s", CUP(currentConsoleLine, col));
                }*/
                col = linebreakCol;
                currentConsoleLine++;
                System.out.print(CUP(currentConsoleLine, col));
                c = ' ';
            }

            System.out.print(c);

            if (c == '\u001B') {
                escaped = true;
            }

            if (!escaped && !Character.isSpaceChar(c)){
                Thread.sleep(speed);
            }

            if(!escaped && !backwards){
                col++;
            } else if (!escaped){
                col--;
            }

            if (c == '>'){
                Thread.sleep(speeds[1]);
                speed = speeds[2];
                linebreakCol = col + 1;
            } else if (c == '<'){
                Thread.sleep(speeds[1]);
                speed = speeds[2];
                linebreakCol = col - 1;
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

    public static String processBackwardLinebreaks(String text){
        StringBuilder line = new StringBuilder(text).reverse();
        boolean escaped = false;
        int col = 56;
        int lastSpace = -1;
        int linebreakCol = col;

        for (int i = 0; i < line.length(); i++) {
            if (col == 1){
                i = lastSpace;
                col = linebreakCol;
                line.setCharAt(i, '\n');
            }

            char c = line.charAt(i);

            /*if (c == '\u001B') {
                escaped = true;
            }*/

            if (Character.isSpaceChar(c)){
                lastSpace = i;
            }

            if(!escaped){
                col--;
            }

            /*if (c == 'm'){
                escaped = false;
            }*/
        }

        return line.reverse().toString();
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

        cmd(String.format("mode con: cols=%s lines=%s", consoleSize[0], consoleSize[1]));
    }
}