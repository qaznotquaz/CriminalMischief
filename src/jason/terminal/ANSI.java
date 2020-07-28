package jason.terminal;

import java.util.HashMap;
import java.util.Map;

//todo: cite this code
public class ANSI {
    public static final String RESET  = "\u001B[0m";

    public static final String BLACK  = "\u001B[30m";
    public static final String RED    = "\u001B[31m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN   = "\u001B[36m";
    public static final String WHITE  = "\u001B[37m";

    public static final String BRIGHT_BLACK  = "\u001B[90m";
    public static final String BRIGHT_RED    = "\u001B[91m";
    public static final String BRIGHT_GREEN  = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE   = "\u001B[94m";
    public static final String BRIGHT_PURPLE = "\u001B[95m";
    public static final String BRIGHT_CYAN   = "\u001B[96m";
    public static final String BRIGHT_WHITE  = "\u001B[97m";

    public static final String BG_BLACK  = "\u001B[40m";
    public static final String BG_RED    = "\u001B[41m";
    public static final String BG_GREEN  = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE   = "\u001B[44m";
    public static final String BG_PURPLE = "\u001B[45m";
    public static final String BG_CYAN   = "\u001B[46m";
    public static final String BG_WHITE  = "\u001B[47m";

    public static final String BRIGHT_BG_BLACK  = "\u001B[100m";
    public static final String BRIGHT_BG_RED    = "\u001B[101m";
    public static final String BRIGHT_BG_GREEN  = "\u001B[102m";
    public static final String BRIGHT_BG_YELLOW = "\u001B[103m";
    public static final String BRIGHT_BG_BLUE   = "\u001B[104m";
    public static final String BRIGHT_BG_PURPLE = "\u001B[105m";
    public static final String BRIGHT_BG_CYAN   = "\u001B[106m";
    public static final String BRIGHT_BG_WHITE  = "\u001B[107m";

    public static final String UNDERLINE = "\u001B[4m";
    public static final String INVERT    = "\u001B[7m";

    public static Map<String, String> dynamicColor = new HashMap<>();

    public static void initDynamicColors() {
        dynamicColor.put("BLACK" , BLACK);
        dynamicColor.put("RED"   , RED);
        dynamicColor.put("GREEN" , GREEN);
        dynamicColor.put("YELLOW", YELLOW);
        dynamicColor.put("BLUE"  , BLUE);
        dynamicColor.put("PURPLE", PURPLE);
        dynamicColor.put("CYAN"  , CYAN);
        dynamicColor.put("WHITE" , WHITE);
        
        dynamicColor.put("BRIGHT_BLACK" , BRIGHT_BLACK);
        dynamicColor.put("BRIGHT_RED"   , BRIGHT_RED);
        dynamicColor.put("BRIGHT_GREEN" , BRIGHT_GREEN);
        dynamicColor.put("BRIGHT_YELLOW", BRIGHT_YELLOW);
        dynamicColor.put("BRIGHT_BLUE"  , BRIGHT_BLUE);
        dynamicColor.put("BRIGHT_PURPLE", BRIGHT_PURPLE);
        dynamicColor.put("BRIGHT_CYAN"  , BRIGHT_CYAN);
        dynamicColor.put("BRIGHT_WHITE" , BRIGHT_WHITE);
        
        dynamicColor.put("BG_BLACK" , BG_BLACK);
        dynamicColor.put("BG_RED"   , BG_RED);
        dynamicColor.put("BG_GREEN" , BG_GREEN);
        dynamicColor.put("BG_YELLOW", BG_YELLOW);
        dynamicColor.put("BG_BLUE"  , BG_BLUE);
        dynamicColor.put("BG_PURPLE", BG_PURPLE);
        dynamicColor.put("BG_CYAN"  , BG_CYAN);
        dynamicColor.put("BG_WHITE" , BG_WHITE);
        
        dynamicColor.put("BRIGHT_BG_BLACK" , BRIGHT_BG_BLACK);
        dynamicColor.put("BRIGHT_BG_RED"   , BRIGHT_BG_RED);
        dynamicColor.put("BRIGHT_BG_GREEN" , BRIGHT_BG_GREEN);
        dynamicColor.put("BRIGHT_BG_YELLOW", BRIGHT_BG_YELLOW);
        dynamicColor.put("BRIGHT_BG_BLUE"  , BRIGHT_BG_BLUE);
        dynamicColor.put("BRIGHT_BG_PURPLE", BRIGHT_BG_PURPLE);
        dynamicColor.put("BRIGHT_BG_CYAN"  , BRIGHT_BG_CYAN);
        dynamicColor.put("BRIGHT_BG_WHITE" , BRIGHT_BG_WHITE);
        
        dynamicColor.put("UNDERLINE", UNDERLINE);
        dynamicColor.put("INVERT"   , INVERT);
    }

    public static String code(String code){
        return String.format("\u001B[%s", code);
    }

    public static String CUU(int n){
        return String.format("\u001B[%sA", n);
    }

    public static String CUD(int n){
        return String.format("\u001B[%sB", n);
    }

    public static String CUF(int n){
        return String.format("\u001B[%sC", n);
    }

    public static String CUB(int n){
        return String.format("\u001B[%sD", n);
    }

    public static String CNL(int n){
        return String.format("\u001B[%sE", n);
    }

    public static String CPL(int n){
        return String.format("\u001B[%sF", n);
    }

    public static String CHA(int n){
        return String.format("\u001B[%sG", n);
    }

    public static String CUP(int row, int col){
        return String.format("\u001B[%s;%sH", row, col);
    }

    public static String ED(int n){
        return String.format("\u001B[%sJ", n);
    }

    public static String EL(int n){
        return String.format("\u001B[%sK", n);
    }
}