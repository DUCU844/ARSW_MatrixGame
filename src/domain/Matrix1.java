package domain;

import java.util.Random;

public class Matrix1 {

    private static final int SIZE = 8;
    private static Matrix1 instance = null;

    private final char[][] board;
    private int turns = 1; // 1 = Neo's turn, 2 = Smith's turn
    private boolean isGameOver = false;
    private int smithCount;
    private int smithMovesDone = 0;

    private Matrix1() {
        board = new char[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                board[i][j] = ' ';
    }

    public static Matrix1 getInstance() {
        if (instance == null) {
            instance = new Matrix1();
        }
        return instance;
    }

    /**
     * Places walls and the phone on the board before threads start.
     */
    public void initBoard(int numSmiths) {
        this.smithCount = numSmiths;
        Random rand = new Random();
        int placed = 0;
        while (placed < 6) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);
            if (board[r][c] == ' ') {
                board[r][c] = 'W';
                placed++;
            }
        }
        while (true) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);
            if (board[r][c] == ' ') {
                board[r][c] = 'T';
                break;
            }
        }
    }

    /**
     * Places a character on a random empty cell before threads start.
     * Returns the [row, col] chosen.
     */
    public synchronized int[] placeCharacter(char ch) {
        Random rand = new Random();
        while (true) {
            int r = rand.nextInt(SIZE);
            int c = rand.nextInt(SIZE);
            if (board[r][c] == ' ') {
                board[r][c] = ch;
                return new int[]{r, c};
            }
        }
    }

    // --- Turn coordination (Producer-Consumer pattern) ---

    public synchronized void waitForNeoTurn() {
        while (turns != 1 && !isGameOver) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public synchronized void waitForSmithTurn() {
        while (turns != 2 && !isGameOver) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public synchronized void changingTurn(int nextTurn) {
        turns = nextTurn;
        notifyAll();
    }

    /**
     * Called by each Smith after its move. When all Smiths finish, passes turn back to Neo.
     */
    public synchronized void smithDone() {
        if (!isGameOver) {
            smithMovesDone++;
            if (smithMovesDone >= smithCount) {
                smithMovesDone = 0;
                turns = 1;
                notifyAll();
            }
        }
    }

    public synchronized boolean getIsGameOver() {
        return isGameOver;
    }

    public synchronized void setGameOver() {
        isGameOver = true;
        notifyAll();
    }

    // --- Board operations (all synchronized to prevent race conditions) ---

    /**
     * Moves Neo from (row,col) to a random valid adjacent cell.
     * Valid: within bounds, not Wall, not Smith.
     * Returns [newRow, newCol, status]: status 0=continue, 1=Neo wins (reached phone).
     */
    public synchronized int[] neoMove(int row, int col) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        java.util.List<int[]> valid = new java.util.ArrayList<>();
        for (int[] d : dirs) {
            int nr = row + d[0];
            int nc = col + d[1];
            if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                char cell = board[nr][nc];
                if (cell != 'W' && cell != 'A') {
                    valid.add(new int[]{nr, nc});
                }
            }
        }
        if (valid.isEmpty()) {
            return new int[]{row, col, 0};
        }
        int[] chosen = valid.get(new Random().nextInt(valid.size()));
        int newRow = chosen[0];
        int newCol = chosen[1];
        char target = board[newRow][newCol];
        board[row][col] = ' ';
        board[newRow][newCol] = 'N';
        if (target == 'T') {
            return new int[]{newRow, newCol, 1};
        }
        return new int[]{newRow, newCol, 0};
    }

    /**
     * Moves a Smith from (row,col) to a random valid adjacent cell.
     * Valid: within bounds, not Wall, not another Smith, not Phone.
     * Smith CAN move into Neo's cell (catches Neo).
     * Returns [newRow, newCol, status]: status 0=continue, 2=caught Neo.
     */
    public synchronized int[] smithMove(int row, int col) {
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        java.util.List<int[]> valid = new java.util.ArrayList<>();
        for (int[] d : dirs) {
            int nr = row + d[0];
            int nc = col + d[1];
            if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                char cell = board[nr][nc];
                if (cell != 'W' && cell != 'A' && cell != 'T') {
                    valid.add(new int[]{nr, nc});
                }
            }
        }
        if (valid.isEmpty()) {
            return new int[]{row, col, 0};
        }
        int[] chosen = valid.get(new Random().nextInt(valid.size()));
        int newRow = chosen[0];
        int newCol = chosen[1];
        char target = board[newRow][newCol];
        board[row][col] = ' ';
        board[newRow][newCol] = 'A';
        if (target == 'N') {
            return new int[]{newRow, newCol, 2};
        }
        return new int[]{newRow, newCol, 0};
    }

    public synchronized void showMatrix() {
        System.out.println();
        for (int i = 0; i < SIZE; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < SIZE; j++) {
                sb.append("|").append(board[i][j]);
            }
            sb.append("|");
            System.out.println(sb.toString());
        }
    }

    // --- Entry point ---

    public static void main(String[] args) throws InterruptedException {
        int numSmiths = 2;
        Matrix1 matrix = Matrix1.getInstance();
        matrix.initBoard(numSmiths);

        int[] neoPos = matrix.placeCharacter('N');
        Neo neo = new Neo(neoPos[0], neoPos[1]);

        Smith[] smiths = new Smith[numSmiths];
        for (int i = 0; i < numSmiths; i++) {
            int[] pos = matrix.placeCharacter('A');
            smiths[i] = new Smith(pos[0], pos[1]);
        }

        System.out.println("=== MATRIX GAME START ===");
        matrix.showMatrix();

        neo.start();
        for (Smith smith : smiths) {
            smith.start();
        }

        neo.join();
        for (Smith smith : smiths) {
            smith.join();
        }

        System.out.println("=== GAME OVER ===");
        matrix.showMatrix();
    }
}
