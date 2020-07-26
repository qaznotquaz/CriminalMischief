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

/**
 * This is the driver class that contains the main function.
 */
public class Driver {
    /**
     * {@todo this has not been implemented yet.}
     * This string will be used for automating the intro message for each snippet.
     */
    static final String snippetIntro1 = "Conversation recovered from [BG_WHITE][BLUE]HRMNY[RESET] archives.";
    /**
     * {@todo this has not been implemented yet.}
     * This string will be used for automating the intro message for each snippet.
     */
    static final String snippetIntro2 = "Original conversation began [UNDERLINE]%s[RESET], [UNDERLINE]%s[RESET]";
    /**
     * This holds the date of snippets to be searched for.
     */
    static String date;
    /**
     * This is the {@link SnippetFilter} specified in the config file.
     */
    static SnippetFilter filter = new SnippetFilter();
    /**
     * The intended console dimensions, ordered as {@code [columns, rows]}
     */
    static int[] consoleSize;
    /**
     * The current line being printed on.
     */
    static int currentConsoleLine = 1;
    /**
     * {@todo text speed controls can be made into a class, probably.}
     * The default text speeds for {@code dm}s, ordered as {@code [pre-message, username, arrow pause, message]}.
     */
    static int[] chatDefaultSpeeds = new int[]{3000, 20, 2000, 50};
    /**
     * {@todo text speed controls can be made into a class, probably.}
     * The default text speeds for {@code diagnostic messages}, ordered as {@code [pre-message, username, arrow pause, message]}.
     */
    static int[] diagnosticDefaultSpeeds = new int[]{1000, 30, 0, 0};
    /**
     * {@todo text speed controls can be made into a class, probably.}
     * The default text speeds for {@code quick mode}, ordered as {@code [pre-message, username, arrow pause, message]}.
     */
    static int[] quickSpeeds = new int[]{50, 5, 10, 10};
    /**
     * This is the {@link DateRecord} that has been loaded, as per the user's specification in the config file.
     */
    static DateRecord dateRecord;
    /**
     * The quick mode toggle, set as per the user's specification in the config file.
     */
    static boolean quickMode = false;
    /**
     * The verbose mode toggle, set as per the user's specification in the config file.
     */
    static boolean verboseMode = false;
    /**
     * This is extra height to be added when calculating the console's height.
     */
    static int extraHeight = 2;

    private static DateRecord.Snippet selectedSnippet;

    /**
     * The entry point of application.
     *
     * @param args the input arguments.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep().
     * @throws IOException          possible io exception, because of file operations.
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // Open up the config file, read it, and apply the options specified therein.
        File cfgFile = new File("config.txt");
        BufferedReader reader = new BufferedReader(new FileReader(cfgFile));
        reader.lines().forEach(Driver::parseCfgLine);

        // Fill out the dictionary of colors actually used.
        // todo: this probably needs to be cleaned somehow? i will probably just move this to the ANSI file and fill it out for every single value.
        Map<String, String> myColors = new HashMap<>();
        myColors.put("BRIGHT_RED", BRIGHT_RED);
        myColors.put("BRIGHT_BLUE", BRIGHT_BLUE);
        myColors.put("CYAN", CYAN);
        myColors.put("BG_WHITE", BG_WHITE);
        myColors.put("BLUE", BLUE);
        myColors.put("RED", RED);
        myColors.put("UNDERLINE", UNDERLINE);
        myColors.put("INVERT", INVERT);
        myColors.put("BRIGHT_WHITE", BRIGHT_WHITE);
        myColors.put("WHITE", WHITE);
        ANSI.setDynamicColor(myColors);

        // if quick mode is set, override the chat and diag. speeds with the quick speed
        // todo: text speed controls can be made into a class, probably.
        if (quickMode) {
            chatDefaultSpeeds = Arrays.copyOf(quickSpeeds, 4);
            diagnosticDefaultSpeeds = Arrays.copyOf(quickSpeeds, 4);
        }

        // attempt to load the DateRecord with the given date and filter.
        try {
            dateRecord = new DateRecord(date, filter);
        } catch (Exception e) {
            // todo: this error handling is miserable, which is to say nonexistent
            e.printStackTrace();
        }

        displayHeader();

        // finally, step through a selected snippet and act each line!
        for (JSONObject line:dateRecord.getSnippet(0)) {
            actLine(line);
        }
    }

    /**
     * Display a header with information about the {@link jason.storyteller.DateRecord.Snippet} that's about to be displayed.
     *
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep().
     * @throws IOException          possible io exception, because setupConsole has to use a ProcessBuilder.
     */
    public static void displayHeader() throws IOException, InterruptedException {
        // currently, we set the selected snippet to the first snippet found.
        // todo: i'll have a more complex way to decide between multiple snippets eventually.
        selectedSnippet = dateRecord.getSnippet(0);

        // if we're in verbose mode, then the console's height should be taller.
        // 5 is a nonspecific number that should always accommodate what's needed.
        if (verboseMode)
            extraHeight += 5;

        // now that we have a snippet selected and know how tall the console should be, we initialize the console.
        consoleSize = new int[]{62, selectedSnippet.getSnippetLength()+extraHeight};
        setupConsole();

        // these are the lines of extra information that verbose mode is for!
        if (verboseMode) {
            displayDiag(String.format("Searching for snippets [BRIGHT_BLUE]from[RESET]: [UNDERLINE]%s[RESET].", date));
            displayDiag(String.format("with [BRIGHT_BLUE]%s[RESET] of these tags: [UNDERLINE]%s[RESET]", filter.getAnyAll(), filter.getIncludedTags()));
            displayDiag(String.format("with [BRIGHT_RED]none[RESET] of these tags: [UNDERLINE]%s[RESET]", filter.getExcludedTags()));
        }

        // this is the line that will always be part of the header.
        // todo: implement snippet intro bits
        displayDiag(String.format("Found %s snippets. Displaying the first.", dateRecord.countSnippets()), new int[]{3000, diagnosticDefaultSpeeds[1], diagnosticDefaultSpeeds[2], diagnosticDefaultSpeeds[3]});
        displayDiag("");
    }

    /**
     * Parse a line from the configuration file and set options as necessary.
     * <br>todo: add error handling
     *
     * @param lineRaw the raw line from the config file to be parsed.
     */
    private static void parseCfgLine(String lineRaw) {
        String[] line = lineRaw.split(": ");

        // if a line doesn't have a ": " in it to split on, then it's not a line with an option on it.
        // todo: should this be enumerated?
        if (line.length == 2) {
            switch (line[0]) {
                case "date":
                    date = line[1];
                    break;

                case "any/all":
                    if (line[1].equals("any")) {
                        filter.setAnyOrAll(true);
                    } else if (line[1].equals("all")) {
                        filter.setAnyOrAll(false);
                    }
                    break;
                case "tags":
                    filter.setIncludedTags(new ArrayList<>(Arrays.asList(line[1].split(", "))));
                    break;
                case "exclude":
                    filter.setExcludedTags(new ArrayList<>(Arrays.asList(line[1].split(", "))));
                    break;

                case "quick mode":
                    quickMode = Boolean.parseBoolean(line[1]);
                    break;
                case "verbose":
                    verboseMode = Boolean.parseBoolean(line[1]);
                    break;
            }
        }
    }

    /**
     * Act a line from a snippet out.
     *
     * @param line the line to be acted
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    public static void actLine(JSONObject line) throws InterruptedException {
        // get the line type and any custom speed settings for the line.
        String type = line.getString("type");
        JSONObject jsonSpeeds = line.optJSONObject("speeds");
        int[] speeds;

        // todo: should this be enumerated?
        switch (type) {
            case "dm":
                // dm lines are 'direct messages' sent from a user.
                speeds = adjustSpeeds(jsonSpeeds, chatDefaultSpeeds);
                Thread.sleep(speeds[0]);
                displayDm(Objects.requireNonNull(selectedSnippet.getActor(line.getString("from"))),
                        line, speeds);
                break;
            case "diagnostic":
                // diagnostic lines are messages sent "by the system" to provide information.
                speeds = adjustSpeeds(jsonSpeeds, diagnosticDefaultSpeeds);
                Thread.sleep(speeds[0]);
                writeGradually(otherHighlightForwards(line.getString("text"), WHITE), speeds, true);
                break;
            case "animation":
                // animation lines are commands to draw animations on the console.
                speeds = adjustSpeeds(jsonSpeeds, quickSpeeds);
                Thread.sleep(speeds[0]);
                animate(line.getString("animation"), line.getJSONObject("settings"));
                break;
        }
    }

    /**
     * Adjust a chat speed controller based on provided values.
     * <br>todo: these speed adjustment things should be a class, probably
     *
     * @param jsonSpeeds    the jsonobject containing and specific override speeds for the relevant line.
     * @param defaultSpeeds the default values for this controller.
     * @return the adjusted speeds
     */
    public static int[] adjustSpeeds(JSONObject jsonSpeeds, int[] defaultSpeeds) {
        int[] speeds = Arrays.copyOf(defaultSpeeds, 4);

        // if we're in quick mode, then these speed adjustments shouldn't apply.
        // and, if jsonSpeeds is null, that means that this line didn't have and specified adjustments to make to speed.
        if (!quickMode && jsonSpeeds != null) {
            // once those conditions are clear, we check each possible speed and override the relevant speed in the given array
            if (jsonSpeeds.has("pre")) {
                speeds[0] = jsonSpeeds.getInt("pre");
            }
            if (jsonSpeeds.has("name")) {
                speeds[1] = jsonSpeeds.getInt("name");
            }
            if (jsonSpeeds.has("tick")) {
                speeds[2] = jsonSpeeds.getInt("tick");
            }
            if (jsonSpeeds.has("text")) {
                speeds[3] = jsonSpeeds.getInt("text");
            }
        }

        return speeds;
    }

    /**
     * This function makes custom color controls like {@code [BRIGHT_RED]} into actual ansi sequences that will
     * affect the console.
     *
     * @param line   the line of text to be altered
     * @param defCol the default color for this line of text (e.g., WHITE for diagnostic messages or CYAN for a particular actor)
     * @return the line of text with ansi sequences inserted correctly.
     */
    public static String otherHighlightForwards(String line, String defCol) {
        // we use an atomic reference so that we can run a lambda function on it.
        AtomicReference<String> text = new AtomicReference<>();
        text.set(line);
        // this is where the key/value pairs of the dynamicColor list are actually used!
        dynamicColor.forEach((k, v) -> text.set(text.get().replace(String.format("[%s]", k), v)));

        // ansi reset is separate because if this is being used by an actor, we don't want to turn their normal color off.
        text.set(text.get().replace("[RESET]", String.format("%s%s", RESET, defCol)));

        return text.get();
    }

    /**
     * Because the ansi sequences need to be reversed correctly if the string is being printed backwards, we have
     * a separate version of the above function.
     *
     * @param line   the line of text to be altered
     * @param defCol the default color for this line of text (e.g., WHITE for diagnostic messages or CYAN for a particular actor)
     * @return the line of text with ansi sequences inserted correctly.
     */
    public static String otherHighlightBackwards(String line, String defCol) {
        // we use an atomic reference so that we can run a lambda function on it.
        AtomicReference<String> text = new AtomicReference<>();
        text.set(line);
        // this is where the key/value pairs of the dynamicColor list are actually used!
        // we reverse the ansi sequences so that when the string is reversed again, they are printed forwards.
        dynamicColor.forEach((k, v) -> text.set(text.get().replace(StringUtils.reverse(String.format("[%s]", k)), v)));

        // ansi reset is separate because if this is being used by an actor, we don't want to turn their normal color off.
        text.set(text.get().replace(StringUtils.reverse("[RESET]"), String.format("%s%s", RESET, defCol)));

        return text.get();
    }

    /**
     * Display a {@code dm} type message.
     *
     * @param actor  the actor sending the message
     * @param line   the line of text that they say
     * @param speeds the set of speed controls to be used. todo: these speed controls should be a class
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    public static void displayDm(DateRecord.Snippet.MiniActor actor, JSONObject line, int[] speeds) throws InterruptedException {
        // we get the original text, as well as a new string to build our final printed line in.
        String text = line.getString("text");
        String toPrint;

        // the string needs to be handled differently based on the direction it's being printed in.
        if (actor.isForwards()) {
            // we build our toPrint string simply in this case before passing it over to writeGradually.
            toPrint = String.format(" %s%s%s > %s%s%s",
                    actor.getColor(), actor.getName(), RESET,
                    actor.getColor(), text, RESET);
            toPrint = otherHighlightForwards(toPrint, actor.getColor());
        } else {
            // building the backwards string is a little bit more complex.
            // the name and arrow need to be kept separate for the processing of backwards line breaks
            // todo: maybe make that not the case?
            String namePrint = String.format(" %s%s%s < ",
                    actor.getColor(), StringUtils.reverse(actor.getName()), RESET);

            // we have to reverse the text separately from the ansi sequences, since the ansi sequences must be printed forwards.
            toPrint = String.format("%s%s%s ",
                    actor.getColor(), StringUtils.reverse(processBackwardLinebreaks(text)), RESET);
            toPrint = otherHighlightBackwards(toPrint, actor.getColor());

            toPrint = String.format("%s%s", namePrint, toPrint);
        }

        // once we've fully prepared our line, we send it off to be printed.
        writeGradually(toPrint, speeds, actor.isForwards());
    }

    /**
     * Displays a {@code diagnostic} type message, assuming default diagnostic message text speeds.<br>
     *     All diagnostic messages will be printed forwards.
     *
     * @param line the line to be printed.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    public static void displayDiag(String line) throws InterruptedException {
        Thread.sleep(diagnosticDefaultSpeeds[0]);
        writeGradually(otherHighlightForwards(line, WHITE), diagnosticDefaultSpeeds, true);
    }

    /**
     * Displays a {@code diagnostic} type message, with specified text speeds.<br>
     *     All diagnostic messages will be printed forwards.
     *
     * @param line the line to be printed.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    public static void displayDiag(String line, int[] speeds) throws InterruptedException {
        Thread.sleep(speeds[0]);
        writeGradually(otherHighlightForwards(line, WHITE), speeds, true);
    }

    /**
     * Writes a line gradually to the screen, based on specified speed controls.
     *
     * @param line      the line to be written.
     * @param speeds    the speed controls. todo: these should be a class
     * @param forwards whether the line is being printed forwards or backwards.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    @SuppressWarnings("BusyWait")
    public static void writeGradually(String line, int[] speeds, boolean forwards) throws InterruptedException {
        // we immediately pass it all over to a backwards version of this function if applicable,
        //      since backwards text needs to be printed so differently.
        if (!forwards) {
            writeGraduallyBackwards(line, speeds);
        } else {
            // this boolean keeps track of whether we're printing an escape sequence or not
            boolean escaped = false;
            // this integer is the wait between each printed character.
            // initially, it's set to element [1] of the speeds array, which is the name printing speed.
            // todo: speed controls need to be moved to their own class!
            // todo: also, 'speed' is a bit of a misnomer, higher numbers mean longer waits instead of faster speeds.
            int speed = speeds[1];
            // this is the column that the cursor will return to on line breaks.
            //   there should never be a situation where this initial value is actually used.
            int linebreakCol = -1;
            // this is the column that the cursor is currently printing on.
            // the row/column coordinates index from 1, and start at (1, 1) in the top left corner.
            int col = 1;

            // move the cursor to the line it should be printing on, at the left edge.
            System.out.print(CUP(currentConsoleLine, col));

            // we're going to iterate through every character in the string, but there are a few
            //   key points where we have to do things a little differently.
            //   todo: i don't know if this needs to be cleaned up or if it's destined to be messy
            for (int i = 0; i < line.length(); i++) {
                // before doing anything else, we check if we're at the edge of the console window (consoleSize[0] is the width of the window)
                if (col == consoleSize[0]) {
                    // if we are, we run backwards in the string until we find a space.
                    while (line.charAt(i) != ' ') {
                        i--;
                        // while we're doing this, we're also erasing the word that broke the line.
                        col--;
                        System.out.printf(" %s", CUP(currentConsoleLine, col));
                    }
                    // once we've found a space, we step back forward to begin printing on the word instead of the space.
                    i++;
                    // then, we move the cursor back to the beginning, and down a line.
                    col = linebreakCol;
                    currentConsoleLine++;
                    System.out.print(CUP(currentConsoleLine, col));
                }

                // once we're okay to start printing, we grab the character we're on and print it.
                char c = line.charAt(i);
                System.out.print(c);

                // this unicode character is quite literally an escape character.
                //   this means we're starting an ansi escape sequence.
                if (c == '\u001B') {
                    escaped = true;
                }

                // when we're printing an escape sequence or a blank space, we want to instantly print and move on.
                if (!escaped && !Character.isSpaceChar(c)) {
                    // otherwise, every character of normal text has a small pause between it.
                    Thread.sleep(speed);
                }

                // we don't progress the column while printing an escape sequence, of course.
                if (!escaped) {
                    col++;
                }

                // this arrow means that we're transitioning from the name printing into the text itself.
                if (c == '>') {
                    // we take a pause here, using the 'arrow' space of the speed control to determine how long.
                    Thread.sleep(speeds[2]);
                    // change the speed setting to the 'text' speed instead of the name speed
                    speed = speeds[3];
                    // and set the line-break column to just after this arrow.
                    //   the arrow should always be encountered far before the line has to break.
                    linebreakCol = col + 1;
                }

                // if we encounter a letter m, it could mean one of two things:
                //   - we've encountered a plain letter m, OR
                //   - we've reached the end of an escape sequence.
                if (c == 'm') {
                    // in either case, we turn off the 'escaped' setting. we make sure to do this *after*
                    //   treating the current character as an escaped character.
                    escaped = false;
                }
            }

            // after printing the entire line, we step the current line down again so that the next thing that prints
            //   doesn't run this line over accidentally.
            currentConsoleLine++;
        }
    }

    /**
     * Writes a line gradually to the screen, based on specified speed controls.<br>
     *     This version of the function handles text written from the right.
     *
     * @param line      the line to be written.
     * @param speeds    the speed controls. todo: these should be a class
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    @SuppressWarnings("BusyWait")
    public static void writeGraduallyBackwards(String line, int[] speeds) throws InterruptedException {
        // this boolean keeps track of whether we're printing an escape sequence or not
        boolean escaped = false;
        // this integer is the index the arrow between the username and text is in the text string,
        //   which is where our for loop will return to on every newline.
        int arrow = 0;
        // this integer is how many \n characters have been found in the string overall.
        int totalNewlines = 0;
        // this integer is how many \n characters the counter has encountered on this pass.
        int currentNewline = 0;
        // this integer is the console line that we are currently printing on.
        //  this is kept separate from currentConsoleLine because it will end on the same line that we began,
        //  even if we've printed several lines.
        int printingLine = currentConsoleLine;
        // this is the top line of the message.
        int origLine = printingLine;
        // this integer is the wait between each printed character.
        // initially, it's set to element [1] of the speeds array, which is the name printing speed.
        // todo: speed controls need to be moved to their own class!
        // todo: also, 'speed' is a bit of a misnomer, higher numbers mean longer waits instead of faster speeds.
        int speed = speeds[1];
        // this is the column that the cursor will return to on line breaks.
        //   there should never be a situation where this initial value is actually used.
        int linebreakCol = -1;
        // this is the column that the cursor is currently printing on.
        //   the console row/column coordinates index from 1, and start at (1, 1) in the top left corner.
        //   consoleSize[0] is going to give us the far right edge.
        int col = consoleSize[0];

        // move the cursor to the line it should be printing on, at the right edge.
        System.out.print(CUP(printingLine, col));

        // we're going to iterate through every character in the string and print it, starting from the right and moving left.
        //   when we encounter a \n character, that means we need to break to a new line - but, because we're printing backwards,
        //   we have to erase the entire current line and then reprint it one line lower. this will repeat until the end of the string.
        //   each time we add a new line, though, we want to reprint the old lines instantly.
        for (int i = 0; i < line.length(); i++) {
            // if we've encountered a \n character, we need to do quite a bit before moving ahead.
            char c = line.charAt(i);
            if (c == '\n') {
                // if we're at the newest \n character...
                if (currentNewline == totalNewlines) {
                    // erase the current line of text,
                    while (col != linebreakCol) {
                        System.out.printf(" %s", CUP(printingLine, col));
                        col++;
                    }
                    // add another new line to our message,
                    totalNewlines++;
                    currentConsoleLine++;
                    // and move our cursor line (and counter) down to begin reprinting from that line.
                    printingLine = printingLine + totalNewlines;
                    currentNewline = 0;
                    i = arrow;

                // otherwise, this means that we're still reprinting old lines.
                } else {
                    // so, we move the cursor up and keep track of how many old lines we've printed.
                    printingLine--;
                    currentNewline++;
                }

                // if we're back to printing new material, then the printing speed
                //   can go back to having a minor delay.
                if (printingLine == origLine) {
                    speed = speeds[3];

                // otherwise, we print instantly.
                } else {
                    speed = 0;
                }

                // no matter what, since we've encountered a \n, we reset our column.
                col = linebreakCol;
                // i don't want to actually print a literal \n, though, so we transmute it into a space.
                c = ' ';

                // once we have all that figured out, we can reposition the cursor at the new line and continue printing.
                System.out.print(CUP(printingLine, col));
            }

            System.out.print(c);

            // this unicode character is quite literally an escape character.
            //   this means we're starting an ansi escape sequence.
            if (c == '\u001B') {
                escaped = true;
            }

            // when we're printing an escape sequence or a blank space, we want to instantly print and move on.
            if (!escaped && !Character.isSpaceChar(c)) {
                // otherwise, every character of normal text has a small pause between it.
                Thread.sleep(speed);
            }

            // we don't progress the column while printing an escape sequence, of course.
            if (!escaped) {
                col--;
                // after printing a character, the cursor automatically moves forward one space.
                //   so, we have to force it to go backwards two spaces to print backwards normally.
                System.out.print(CUB(2));
            }

            // this arrow means that we're transitioning from the name printing into the text itself.
            if (c == '<') {
                // we take a pause here, using the 'arrow' space of the speed control to determine how long.
                Thread.sleep(speeds[2]);
                // change the speed setting to the 'text' speed instead of the name speed
                speed = speeds[3];
                // and set the line-break column to just after this arrow.
                //   the arrow should always be encountered far before the line has to break.
                linebreakCol = col - 1;
                // also, we keep track of this arrow's index in the text.
                arrow = i + 1;
            }

            // if we encounter a letter m, it could mean one of two things:
            //   - we've encountered a plain letter m, OR
            //   - we've reached the end of an escape sequence.
            if (c == 'm') {
                // in either case, we turn off the 'escaped' setting. we make sure to do this *after*
                //   treating the current character as an escaped character.
                escaped = false;
            }
        }

        // this cursor movement is entirely cosmetic. without it, the cursor would land and begin blinking idly on the
        //   space two characters to the left of the last character in our message, instead of immedaiately after it.
        System.out.print(CUF(1));

        // after printing the entire line, we step the current line down again so that the next thing that prints
        //   doesn't run anything we've just printed over accidentally.
        currentConsoleLine++;
    }

    /**
     * Processes where to place linebreaks in strings to be printed backwards.<br>
     * Essentially, even though this string will be written from back to front, we want it to split naturally as though
     * it were being written from the front. This would put half-lines at the bottom of the stack instead of the top.
     *
     * @param text the text to be processed.
     * @return the text with \n characters placed at normal linebreak spots.
     */
    public static String processBackwardLinebreaks(String text) {
        StringBuilder line = new StringBuilder(text);
        // this is the current column that the "cursor" is on. it starts at the column that is considered
        //   the 'right' edge of the line space, which is also the column on which the line will break.
        // TODO: THIS SHOULD NOT BE HARDCODED. i need to see about calculating this relative to the space the name takes up.
        int col = 56;
        // this is the index of the most recent space character that was encountered. since we're breaking lines
        //   on spaces, we need to keep track of where these are.
        int lastSpace = -1;
        // this is the column the "cursor" would return to on a line break. it starts where the cursor starts.
        int linebreakCol = col;

        // we step through the entire string from the start and gradually decrement our column counter.
        for (int i = 0; i < line.length(); i++, col--) {
            // if the cursor would've reached the left edge of the screen by this point in the printing, then we
            //   need to perform a line break.
            if (col == 1) {
                // we jump back to the most recent space, replace it with a \n, and move our column counter back
                i = lastSpace;
                line.setCharAt(i, '\n');
                col = linebreakCol;
            }

            //todo: add handling for control sequences like [BRIGHT_RED]

            // if we've found a space character, then we keep track of it.
            if (Character.isSpaceChar(line.charAt(i))) {
                lastSpace = i;
            }
        }

        return line.toString();
    }

    /**
     * Send a console command to the command console itself.<br>
     * todo: do i want to move this to its own class, or perhaps even package? it could be useful in other projects.
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
     * todo: do i want to move this to its own class, or perhaps even package? it could be useful in other projects.
     *
     * @throws IOException          in case an I/O error is encountered when sending commands to the window.
     * @throws InterruptedException if the current thread is interrupted while waiting.
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

        // after setting those flags, i also use the 'mode' console command to change the size of the window.
        cmd(String.format("mode con: cols=%s lines=%s", consoleSize[0], consoleSize[1]));
    }
}