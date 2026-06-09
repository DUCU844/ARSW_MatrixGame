package domain;

public class Smith extends Thread {

    private int row;
    private int col;
    private final Matrix1 matrix;

    public Smith(int row, int col) {
        this.row = row;
        this.col = col;
        this.matrix = Matrix1.getInstance();
    }

    @Override
    public void run() {
        while (!matrix.getIsGameOver()) {
            matrix.waitForSmithTurn();
            if (matrix.getIsGameOver()) break;

            int[] result = matrix.smithMove(row, col);
            row = result[0];
            col = result[1];
            int status = result[2];

            if (status == 2) {
                System.out.println("Smith caught Neo! NEO LOSES!");
                matrix.setGameOver();
                matrix.smithDone();
                break;
            }

            matrix.smithDone();
        }
    }
}
