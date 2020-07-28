package jason.storyteller;

import jason.terminal.ANSI;
import jason.terminal.WindowsTerminal;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static jason.storyteller.Animations.animate;
import static jason.terminal.ANSI.*;

/**
 * This is the driver class that contains the main function.
 */
public class Driver {
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
     * The default text speeds for {@code dm}s, ordered as {@code [pre-message, username, arrow pause, message]}.
     */
    static DelaySet chatDefaultSpeeds = new DelaySet(1500, 20, 1000, 50);
    /**
     * The default text speeds for {@code diagnostic messages}, ordered as {@code [pre-message, username, arrow pause, message]}.
     */
    static DelaySet diagnosticDefaultSpeeds = new DelaySet(1000, 30);
    /**
     * The default text speeds for {@code quick mode}, ordered as {@code [pre-message, username, arrow pause, message]}.
     */
    static DelaySet quickSpeeds = new DelaySet(50, 5, 10, 10);
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

    static final int WIDTH = 62;

    /**
     * The entry point of application.
     *
     * @param args the input arguments.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep().
     * @throws IOException          possible io exception, because of file operations.
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // Open up the config file, read it, and apply the options specified therein.
        BufferedReader reader = new BufferedReader(new FileReader(new File("config.txt")));
        reader.lines().forEach(Driver::parseCfgLine);

        ANSI.initDynamicColors();

        // if quick mode is set, override the chat and diag. speeds with the quick speeds
        if (quickMode) {
            chatDefaultSpeeds.copy(quickSpeeds);
            diagnosticDefaultSpeeds.copy(quickSpeeds);
        }

        try {
            dateRecord = new DateRecord(date, filter);
        } catch (Exception e) {
            // todo: this error handling is miserable, which is to say nonexistent
            e.printStackTrace();
        }

        // currently, we set the selected snippet to the first snippet found.
        // todo: i'll have a more complex way to decide between multiple snippets eventually.
        selectedSnippet = dateRecord.getSnippet(0);

        displayHeader();

        // finally, step through a selected snippet and act each line!
        for (JSONObject line:selectedSnippet) {
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
        // if we're in verbose mode, then the console's height should be taller.
        // 5 is a nonspecific number that should always accommodate what's needed.
        if (verboseMode){
            extraHeight += 5;
        }
        // now that we have a snippet selected and know how tall the console should be, we initialize the console.
        consoleSize = new int[]{WIDTH, selectedSnippet.getSnippetLength()+extraHeight};
        WindowsTerminal.enableVTS();
        WindowsTerminal.cmd(String.format("mode con: cols=%s lines=%s", consoleSize[0], consoleSize[1]));

        // these are the lines of extra information that verbose mode is for!
        if (verboseMode) {
            displayDiag(String.format("Searching for snippets [BRIGHT_BLUE]from[RESET]: [UNDERLINE]%s[RESET].", date));
            displayDiag(String.format("with [BRIGHT_BLUE]%s[RESET] of these tags: [UNDERLINE]%s[RESET]", filter.getAnyAll(), filter.getIncludedTags()));
            if(filter.hasExcludedTags()) {
                displayDiag(String.format("with [BRIGHT_RED]none[RESET] of these tags: [UNDERLINE]%s[RESET]", filter.getExcludedTags()));
            }
        }

        // this is the line that will always be part of the header.
        displayDiag(String.format("Found %s snippets. Displaying the first.", dateRecord.countSnippets()), new DelaySet(3000, diagnosticDefaultSpeeds.getDiagTextDelay()));
        displayDiag("");
        displayDiag("Conversation recovered from [BG_WHITE][BLUE]HRMNY[RESET] archives.");
        displayDiag(String.format("Original conversation began [UNDERLINE]%s[RESET], [UNDERLINE]%s[RESET]", date, selectedSnippet.getTimestamp()));
        displayDiag("");
    }

    /**
     * Parse a line from the configuration file and set options as necessary.
     * <br>todo: add error handling
     *
     * @param lineRaw the raw line from the config file to be parsed.
     */
    private static void parseCfgLine(String lineRaw) {
        String[] line = lineRaw.toLowerCase().split(":");
        String option;
        String choice;

        // if a line doesn't have a ":" in it to split on, then it's not a line with an option on it.
        if (line.length == 2) { //todo: describe the config file somewhere, probably
            option = line[0].trim();
            choice = line[1].trim();

            switch (option) {
                case "date":
                    date = choice;
                    break;

                case "any/all":
                    if (choice.equals("any")) {
                        filter.setAnyOrAll(true);
                    } else if (choice.equals("all")) {
                        filter.setAnyOrAll(false);
                    }
                    break;
                case "tags":
                    if(!choice.equals("")){
                        filter.setIncludedTags(new ArrayList<>(Arrays.asList(choice.split(","))));
                    }
                    break;
                case "exclude":
                    if(!choice.equals("")) {
                        filter.setExcludedTags(new ArrayList<>(Arrays.asList(choice.split(","))));
                    }
                    break;

                case "quick mode":
                    quickMode = Boolean.parseBoolean(choice);
                    break;
                case "verbose":
                    verboseMode = Boolean.parseBoolean(choice);
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
        JSONObject jsonDelays = line.optJSONObject("speeds");
        DelaySet delaySet;

        switch (type) {
            case "dm":
                // dm lines are 'direct messages' sent from a user.
                delaySet = new DelaySet(jsonDelays, chatDefaultSpeeds);
                Thread.sleep(delaySet.getPreDelay());
                displayDm(Objects.requireNonNull(selectedSnippet.getActor(line.getString("from"))),
                        line, delaySet);
                break;
            case "diagnostic":
                // diagnostic lines are messages sent "by the system" to provide information.
                delaySet = new DelaySet(jsonDelays, diagnosticDefaultSpeeds);
                Thread.sleep(delaySet.getPreDelay());
                writeGradually(otherHighlight(line.getString("text"), WHITE, true), delaySet, true);
                break;
            case "animation":
                // animation lines are commands to draw animations on the console.
                delaySet = new DelaySet(jsonDelays, quickSpeeds);
                Thread.sleep(delaySet.getPreDelay());
                animate(line.getString("animation"), line.getJSONObject("settings"));
                break;
        }
    }

    /**
     * This function makes custom color controls like {@code [BRIGHT_RED]} into actual ansi sequences that will
     * affect the console.
     *
     * @param line     the line of text to be altered
     * @param defCol   the default color for this line of text (e.g., WHITE for diagnostic messages or CYAN for a particular actor)
     * @param forwards whether the line is being printed from the left or right. 'true' means left.
     * @return the line of text with ansi sequences inserted correctly.
     */
    public static String otherHighlight(String line, String defCol, boolean forwards) {
        // we use an atomic reference so that we can run a lambda function on it.
        AtomicReference<String> text = new AtomicReference<>();
        text.set(line);

        // this is where the key/value pairs of the dynamicColor list are actually used!
        // reset is separate because if this is being used by an actor, we don't want to turn their normal color off.
        if(forwards) {
            dynamicColor.forEach((k, v) -> text.set(text.get().replace(String.format("[%s]", k), v)));
            text.set(text.get().replace("[RESET]", String.format("%s%s", RESET, defCol)));
        } else {
            // we reverse the ansi sequences so that when the string is reversed again, they are printed forwards.
            dynamicColor.forEach((k, v) -> text.set(text.get().replace(StringUtils.reverse(String.format("[%s]", k)), v)));
            text.set(text.get().replace(StringUtils.reverse("[RESET]"), String.format("%s%s", RESET, defCol)));
        }

        return text.get();
    }

    /**
     * Display a {@code dm} type message.
     *
     * @param actor    the actor sending the message
     * @param line     the line of text that they say
     * @param delaySet the set of delay controls to be used.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    public static void displayDm(DateRecord.Snippet.MiniActor actor, JSONObject line, DelaySet delaySet) throws InterruptedException {
        // we get the original text, as well as a new string to build our final printed line in.
        String text = line.getString("text");
        String toPrint;

        // the string needs to be handled differently based on the direction it's being printed in.
        if (actor.isForwards()) {
            // we build our toPrint string simply in this case before passing it over to writeGradually.
            toPrint = String.format(" %s%s%s > %s%s%s", // todo: refactor these to use a StringBuilder instead
                    actor.getColor(), actor.getName(), RESET,
                    actor.getColor(), text, RESET);
            toPrint = otherHighlight(toPrint, actor.getColor(), true);
        } else {
            // building the backwards string is a little bit more complex.
            // the name and arrow need to be kept separate for the processing of backwards line breaks
            // todo: maybe make that not the case?
            String namePrint = String.format(" %s%s%s < ",
                    actor.getColor(), StringUtils.reverse(actor.getName()), RESET);

            // we have to reverse the text separately from the ansi sequences, since the ansi sequences must be printed forwards.
            toPrint = String.format("%s%s%s ",
                    actor.getColor(), StringUtils.reverse(processBackwardLinebreaks(text, (WIDTH - 4 - actor.getName().length()))), RESET);
            toPrint = otherHighlight(toPrint, actor.getColor(), false);

            toPrint = String.format("%s%s", namePrint, toPrint);
        }

        // once we've fully prepared our line, we send it off to be printed.
        writeGradually(toPrint, delaySet, actor.isForwards());
    }

    /**
     * Displays a {@code diagnostic} type message, assuming default diagnostic message text speeds.<br>
     *     All diagnostic messages will be printed forwards.
     *
     * @param line the line to be printed.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    public static void displayDiag(String line) throws InterruptedException {
        Thread.sleep(diagnosticDefaultSpeeds.getPreDelay());
        writeGradually(otherHighlight(line, WHITE, true), diagnosticDefaultSpeeds, true);
    }

    /**
     * Displays a {@code diagnostic} type message, with specified text speeds.<br>
     *     All diagnostic messages will be printed forwards.
     *
     * @param line the line to be printed.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    public static void displayDiag(String line, DelaySet delaySet) throws InterruptedException {
        Thread.sleep(delaySet.getPreDelay());
        writeGradually(otherHighlight(line, WHITE, true), delaySet, true);
    }

    /**
     * Writes a line gradually to the screen, based on specified speed controls.
     *
     * @param line     the line to be written.
     * @param delaySet the delay controls.
     * @param forwards whether the line is being printed forwards or backwards.
     * @throws InterruptedException possible interrupted exception, because of Thread.sleep()
     */
    @SuppressWarnings("BusyWait")
    public static void writeGradually(String line, DelaySet delaySet, boolean forwards) throws InterruptedException {
        // this boolean keeps track of whether we're printing an escape sequence or not
        boolean escaped = false;

        // this integer is the wait between each printed character.
        // initially, it's set to element [1] of the speeds array, which is the name printing delay.
        int delay = delaySet.getNameDelay();

        // this is the column that the cursor will return to on line breaks.
        //   there should never be a situation where this initial value is actually used.
        int linebreakCol = -1;

        // this is the column that the cursor is currently printing on.
        // the row/column coordinates index from 1, and start at (1, 1) in the top left corner.
        int col = forwards ? 1 : consoleSize[0];

        // this simply holds the character we're about to print.
        char c;

        // move the cursor to the correct edge of the line it should be printing on.
        System.out.print(CUP(currentConsoleLine, col));

        if (forwards) {
            // we're going to iterate through every character in the string, but there are a few
            //   key points where we have to do things a little differently.
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

                c = line.charAt(i);
                System.out.print(c);

                // this unicode character is quite literally the 'ESC' character.
                //   this means we're starting an ansi escape sequence.
                if (c == '\u001B') {
                    escaped = true;
                }

                // when we're printing an escape sequence or a blank space, we want to instantly print and move on.
                if (!escaped && !Character.isSpaceChar(c)) {
                    // otherwise, every character of normal text has a small pause between it.
                    Thread.sleep(delay);
                }

                // we don't progress the column while printing an escape sequence, of course.
                if (!escaped) {
                    col++;
                }

                // this arrow means that we're transitioning from the name printing into the text itself.
                if (c == '>') {
                    // we take a pause here, using the 'arrow' space of the delay control to determine how long.
                    Thread.sleep(delaySet.getArrowDelay());
                    // change the delay setting to the 'text' delay instead of the name delay
                    delay = delaySet.getTextDelay();
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
        } else {
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

            // we're going to iterate through every character in the string and print it, starting from the right and moving left.
            //   when we encounter a \n character, that means we need to break to a new line - but, because we're printing backwards,
            //   we have to erase the entire current line and then reprint it one line lower. this will repeat until the end of the string.
            //   each time we add a new line, though, we want to reprint the old lines instantly.
            for (int i = 0; i < line.length(); i++) {
                // if we've encountered a \n character, we need to do quite a bit before moving ahead.
                c = line.charAt(i);
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

                    // if we're back to printing new material, then the printing delay
                    //   can go back to having a minor delay.
                    if (printingLine == origLine) {
                        delay = delaySet.getTextDelay();

                        // otherwise, we print instantly.
                    } else {
                        delay = 0;
                    }

                    // no matter what, since we've encountered a \n, we reset our column.
                    col = linebreakCol;
                    // i don't want to actually print a literal \n, though, so we transmute it into a space.
                    c = ' ';

                    // once we have all that figured out, we can reposition the cursor at the new line and continue printing.
                    System.out.print(CUP(printingLine, col));
                }

                System.out.print(c);

                // this unicode character is quite literally the 'ESC' character.
                //   this means we're starting an ansi escape sequence.
                if (c == '\u001B') {
                    escaped = true;
                }

                // when we're printing an escape sequence or a blank space, we want to instantly print and move on.
                if (!escaped && !Character.isSpaceChar(c)) {
                    // otherwise, every character of normal text has a small pause between it.
                    Thread.sleep(delay);
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
                    // we take a pause here, using the 'arrow' space of the delay control to determine how long.
                    Thread.sleep(delaySet.getArrowDelay());
                    // change the delay setting to the 'text' delay instead of the name delay
                    delay = delaySet.getTextDelay();
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
        }

        // after printing the entire line, we step the current line down again so that the next thing that prints
        //   doesn't run this line over accidentally.
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
    public static String processBackwardLinebreaks(String text, int width) {
        StringBuilder line = new StringBuilder(text);
        // this is the current column that the "cursor" is on. it starts at the column that is considered
        //   the 'right' edge of the line space, which is also the column on which the line will break.
        int col = width;
        // this is the index of the most recent space character that was encountered. since we're breaking lines
        //   on spaces, we need to keep track of where these are.
        int lastSpace = -1;
        // this is the column the "cursor" would return to on a line break. it starts where the cursor starts.
        int linebreakCol = col;

        // we step through the entire string from the start and gradually decrement our column counter.
        for (int i = 0; i < line.length(); i++, col--) {
            if (line.charAt(i) == '['){
                while (line.charAt(i) != ']'){
                    i++;
                }
                i++;
            }

            // if the cursor would've reached the left edge of the screen by this point in the printing, then we
            //   need to perform a line break.
            if (col == 1) {
                // we jump back to the most recent space, replace it with a \n, and move our column counter back
                i = lastSpace;
                line.setCharAt(i, '\n');
                col = linebreakCol;
            }

            // if we've found a space character, then we keep track of it.
            if (Character.isSpaceChar(line.charAt(i))) {
                lastSpace = i;
            }
        }

        return line.toString();
    }
}