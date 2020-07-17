package jason.storyteller;

public class Animations {
    public static void streak() throws InterruptedException {
        System.out.printf("%s%s", ANSI.CUP(1, 4), ANSI.BRIGHT_BG_RED);
        for (int i = 1; i < 50; i++) {
            System.out.printf(" %s", ANSI.CUP(i, 4));
            Thread.sleep(5);
        }
    }

    public static void wipeBox(int rowTop, int colLeft, int rowBot, int colRight, int speed) throws InterruptedException {
        System.out.printf("%s", ANSI.RESET);
        for (int row = rowTop; row < rowBot; row++) {
            for (int col = colLeft; col <= colRight; col++) {
                System.out.printf(" %s", ANSI.CUP(row, col));
                Thread.sleep(speed);
            }
            row++;
            for (int col = colRight; col >= colLeft; col--) {
                System.out.printf(" %s", ANSI.CUP(row, col));
                Thread.sleep(speed);
            }
        }
    }
}
