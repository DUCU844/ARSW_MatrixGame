package domain;

public class Neo extends Thread {

    private int row;
    private int col;
    private final Matrix1 matrix;

    public Neo(int row, int col) {
        this.row = row;
        this.col = col;
        this.matrix = Matrix1.getInstance();
    }

    @Override
    public void run() {
        while (!matrix.getIsGameOver()) {
            matrix.waitForNeoTurn();
            if (matrix.getIsGameOver()) break;

            int[] result = matrix.neoMove(row, col);
            row = result[0];
            col = result[1];
            int status = result[2];

            matrix.showMatrix();

            if (status == 1) {
                System.out.println("Neo reached the phone! NEO WINS!");
                matrix.setGameOver();
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            matrix.changingTurn(2);
        }
    }
}
