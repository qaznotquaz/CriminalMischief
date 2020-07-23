package jason.storyteller;

import com.sun.jna.Function;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static jason.storyteller.ANSI.*;
import static jason.storyteller.Animations.animate;
import static jason.storyteller.Script.getSceneLength;

public class Main { //todo: implement triage handling and logical operators for tag system
    static String date;
    static boolean anyOfTags;
    static ArrayList<String> searchTags = new ArrayList<>();
    static ArrayList<String> excludeTags = new ArrayList<>();
    static int[] consoleSize;
    static int currentConsoleLine = 1;
    static int[] chatDefaultSpeeds = new int[]{3000, 20, 2000, 50};
    static int[] diagnosticDefaultSpeeds = new int[]{1000, 30, 0, 0};
    static int[] quickSpeeds = new int[]{50, 5, 10, 10};

    static ArrayList<String[]> config = new ArrayList<>();
    static Script script;

    static boolean quickMode = false;

    public static void main(String[] args) throws InterruptedException, IOException {
        File cfgFile = new File("config.txt");
        BufferedReader reader = new BufferedReader(new FileReader(cfgFile));
        reader.lines().forEach(Main::parseCfgLine);

        Map<String, String> myColors = new HashMap<>();
        myColors.put("BRIGHT_RED", BRIGHT_RED);
        myColors.put("CYAN", CYAN);
        myColors.put("BG_WHITE", BG_WHITE);
        myColors.put("BLUE", BLUE);
        myColors.put("RED", RED);
        myColors.put("UNDERLINE", UNDERLINE);
        myColors.put("INVERT", INVERT);
        ANSI.setDynamicColor(myColors);

        if(quickMode){
            chatDefaultSpeeds = Arrays.copyOf(quickSpeeds, 4);
            diagnosticDefaultSpeeds = Arrays.copyOf(quickSpeeds, 4);
        }

        try {
            script = new Script(date, anyOfTags, searchTags, excludeTags);
        } catch (Exception e) {
            e.printStackTrace();
        }
        consoleSize = new int[]{62, getSceneLength()};

        setupConsole();

        for (JSONObject line:script) {
            actLine(line);
        }
    }

    private static void parseCfgLine(String lineRaw){
        String[] line = lineRaw.split(": ");

        //todo: add error handling
        if(line.length == 2){
            switch (line[0]) {
                case "date":
                    date = line[1];
                    break;

                case "any/all":
                    if(line[1].equals("any")){
                        anyOfTags = true;
                    } else if(line[1].equals("all")){
                        anyOfTags = false;
                    }
                    break;
                case "tags":
                    Collections.addAll(searchTags, line[1].split(", "));
                    break;
                case "exclude":
                    Collections.addAll(excludeTags, line[1].split(", "));
                    break;

                case "quickMode":
                    quickMode = Boolean.parseBoolean(line[1]);
                    break;
            }
        }
    }

    public static void actLine(JSONObject line) throws InterruptedException {
        Script.LineType type = line.getEnum(Script.LineType.class, "type");
        int[] speeds;
        JSONObject jsonSpeeds = line.optJSONObject("speeds");

        switch (type) {
            case dm:
                speeds = adjustSpeeds(jsonSpeeds, chatDefaultSpeeds);
                Thread.sleep(speeds[0]);
                displayDm(Objects.requireNonNull(Script.getActor(line.getString("from"))),
                        line, speeds);
                break;
            case diagnostic:
                speeds = adjustSpeeds(jsonSpeeds, diagnosticDefaultSpeeds);
                Thread.sleep(speeds[0]);
                writeGradually(otherHighlight(line.getString("text"), RESET), speeds, false);
                break;
            case animation:
                speeds = adjustSpeeds(jsonSpeeds, quickSpeeds);
                Thread.sleep(speeds[0]);
                animate(line.getString("animation"), line.getJSONObject("settings"));
                break;
        }
    }

    public static int[] adjustSpeeds(JSONObject jsonSpeeds, int[] defaultSpeeds){
        int[] speeds = Arrays.copyOf(defaultSpeeds, 4);

        if(!quickMode && jsonSpeeds != null){
            if(jsonSpeeds.has("pre")){
                speeds[0] = jsonSpeeds.getInt("pre");
            }
            if(jsonSpeeds.has("name")){
                speeds[1] = jsonSpeeds.getInt("name");
            }
            if(jsonSpeeds.has("tick")){
                speeds[2] = jsonSpeeds.getInt("tick");
            }
            if(jsonSpeeds.has("text")){
                speeds[3] = jsonSpeeds.getInt("text");
            }
        }

        return speeds;
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
    public static void displayDm(Script.MiniActor actor, JSONObject line, int[] speeds) throws InterruptedException {
        String text = line.getString("text");
        String toPrint;


        /*if (formatWidth + othersLen + 18 > 61) {
            throw new ScriptFormattingException(String.format("line \"%s\" is too long.", text));
        }*/

        if (actor.isBackwards()) {
            String namePrint = String.format(" %s%s%s < ",
                    actor.getColor(), StringUtils.reverse(actor.getName()), RESET);
            toPrint = String.format("%s%s%s ",
                    actor.getColor(), StringUtils.reverse(text), RESET);
            toPrint = otherHighlightBackwards(toPrint, actor.getColor());

            toPrint = String.format("%s%s", namePrint, processBackwardLinebreaks(toPrint));
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
        if(backwards){
            writeGraduallyBackwards(line, speeds);
        } else {
            boolean escaped = false;
            int speed = speeds[1];
            int linebreakCol = -1;
            int col = 1;

            System.out.print(CUP(currentConsoleLine, col));

            for (int i = 0; i < line.length(); i++) {
                if (col == consoleSize[0]) {
                    while (line.charAt(i) != ' ') {
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

                System.out.print(c);

                if (c == '\u001B') {
                    escaped = true;
                }

                if (!escaped && !Character.isSpaceChar(c)) {
                    Thread.sleep(speed);
                }

                if (!escaped) {
                    col++;
                }

                if (c == '>') {
                    Thread.sleep(speeds[2]);
                    speed = speeds[3];
                    linebreakCol = col + 1;
                }

                if (c == 'm') {
                    escaped = false;
                }
            }

            currentConsoleLine++;
        }
    }

    @SuppressWarnings("BusyWait")
    public static void writeGraduallyBackwards(String line, int[] speeds) throws InterruptedException {
        boolean escaped = false;
        int arrow = 0;
        int totalNewlines = 0;
        int currentNewline = 0;
        int printingLine = currentConsoleLine;
        int origLine = printingLine;
        int speed = speeds[1];
        int linebreakCol = -1;

        int col = consoleSize[0];
        System.out.print(CUP(printingLine, col));

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c=='\n'){
                if(currentNewline == totalNewlines){
                    while(col != linebreakCol){
                        System.out.printf(" %s", CUP(printingLine, col));
                        col++;
                    }
                    totalNewlines++;
                    currentConsoleLine++;
                    printingLine = printingLine+totalNewlines;
                    currentNewline = 0;
                    i = arrow;
                } else {
                    printingLine--;
                    currentNewline++;
                }

                if(printingLine == origLine){
                    speed = speeds[3];
                } else {
                    speed = 0;
                }

                col = linebreakCol;
                c = ' ';

                System.out.print(CUP(printingLine, col));
            }

            System.out.print(c);

            if (c == '\u001B') {
                escaped = true;
            }

            if (!escaped && !Character.isSpaceChar(c)){
                Thread.sleep(speed);
            }

            if(!escaped){
                col--;
            }

            if (c == '<'){
                Thread.sleep(speeds[2]);
                speed = speeds[3];
                linebreakCol = col - 1;
                arrow = i+1;
            }

            if(!escaped){
                System.out.print(CUB(2));
            }

            if (c == 'm'){
                escaped = false;
            }
        }
        System.out.print(CUF(1));

        currentConsoleLine++;
    }

    public static String processBackwardLinebreaks(String text){
        StringBuilder line = new StringBuilder(text).reverse();
        boolean possiblyEscaped = false;
        int charsEscaped = 0;
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

            if (c == 'm') {
                possiblyEscaped = true;
                charsEscaped = 0;
            }

            if (Character.isSpaceChar(c)){
                lastSpace = i;
            }

            col--;

            if(possiblyEscaped){
                charsEscaped++;
            }

            if (c == '\u001B'){
                possiblyEscaped = false;
                col = col+charsEscaped;
                charsEscaped = 0;
            }
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