package jason.storyteller;

import org.json.JSONObject;

public class Animations {
    public static void animate(String name, JSONObject settings) throws InterruptedException {
        switch (name){
            case "wipeBox":
                wipeBox(settings.getInt("1"), settings.getInt("0"),
                        settings.getInt("3"), settings.getInt("2"),
                        settings.getInt("4"));
                break;
            case "streak":
                streak();
                break;
            case "clearScreen":
                wipeBox(1, 1, Driver.consoleSize[0], Driver.consoleSize[1], settings.getInt("0"));
                break;
        }
    }

    public static void streak() throws InterruptedException {
        System.out.printf("%s%s", ANSI.CUP(1, 4), ANSI.BRIGHT_BG_RED);
        for (int i = 1; i < 50; i++) {
            System.out.printf(" %s", ANSI.CUP(i, 4));
            Thread.sleep(5);
        }
    }

    public static void wipeBox(int colLeft, int rowTop, int colRight, int rowBot, int speed) throws InterruptedException {
        System.out.printf("%s", ANSI.BG_WHITE);
        for (int col = colLeft; col <= colRight; col++) {
            for (int row = rowTop; row <= rowBot; row++) {
                System.out.printf(" %s", ANSI.CUP(row, col));
                Thread.sleep(speed);
            }
            col++;
            for (int row = rowBot; row >= rowTop; row--) {
                System.out.printf(" %s", ANSI.CUP(row, col));
                Thread.sleep(speed);
            }
        }
    }
}
