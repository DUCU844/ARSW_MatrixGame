# ARSW_MatrixGame

## Description
This is a console game inspired by the Matrix movie. The game has a board of 8x8 cells. **Neo** (`N`) needs to reach the **Phone** (`T`) before the **Smith agents** (`A`) catch him. The board also has **Walls** (`W`) that block movement. Each character moves one step per turn in a random direction. The game prints the board to the console after each turn.

## Board Characters

| Symbol | Name | Description |
|--------|------|-------------|
| `N` | Neo | The main character. Must reach the phone |
| `A` | Smith | Enemy agent. Tries to catch Neo |
| `T` | Phone | The goal. Neo must reach this to win |
| `W` | Wall | A static obstacle. Nobody can move through it |
| ` ` | Empty | Free space |

## How the Game Works

The game is **turn-based**:

1. **Neo moves** — Neo picks a random valid cell next to him and moves there
2. **All Smiths move** — Each Smith picks a random valid cell and moves there
3. Repeat until the game ends

The game ends when:
- Neo reaches the Phone → **Neo wins**
- A Smith moves into Neo's cell → **Neo loses**

Neo cannot walk into a Smith or a Wall. Smiths cannot walk into other Smiths, Walls, or the Phone.

Example board:

```
|N|W| | | | | |T|
|W| | | | | | |A|
|W| | | | | | | |
| | | | | | | | |
| | | | | | | | |
| | | | | |A| | |
| | | | | | | | |
| | | | | | | | |
```

## Project Structure

```
src/
└── domain/
    ├── Matrix1.java   ← Game board, Singleton, monitor, main method
    ├── Neo.java       ← Neo's thread
    └── Smith.java     ← Smith's thread
```

## Concurrency

This project uses **multi-threading**. Neo runs in one thread and each Smith agent runs in its own thread. Three concurrency patterns are used:

### 1. Singleton Pattern

`Matrix1` uses the Singleton pattern. This means only one game board exists. All threads (Neo and Smiths) share the same board.

```java
public static Matrix1 getInstance() {
    if (instance == null) {
        instance = new Matrix1();
    }
    return instance;
}
```

### 2. Synchronized Methods

Methods that read or change the board are marked as `synchronized`. This prevents two threads from changing the board at the same time (race condition).

Synchronized methods include:
- `neoMove()` — moves Neo on the board
- `smithMove()` — moves a Smith on the board
- `showMatrix()` — prints the board
- `setGameOver()` / `getIsGameOver()` — check or set the end of the game

### 3. Producer-Consumer Pattern (Turn Control)

The turn system uses `wait()` and `notifyAll()` to control the order of moves.

- Neo waits until `turns == 1`:

```java
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
```

- Each Smith waits until `turns == 2`:

```java
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
```

- After Neo moves, it changes the turn to 2 and wakes all Smiths:

```java
public synchronized void changingTurn(int nextTurn) {
    turns = nextTurn;
    notifyAll();
}
```

- Each Smith calls `smithDone()` after moving. When all Smiths finish, the turn goes back to 1:

```java
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
```

This creates a clean turn cycle: **Neo → All Smiths → Neo → ...**

`Matrix1` works as the **monitor** that all threads use to coordinate.

## Getting Started

### Requirements

- Java JDK 8 or higher
- A terminal or an IDE (IntelliJ, Eclipse, VS Code)

### How to Compile

Go to the project folder and run:

```
javac src/domain/*.java
```

### How to Run

```
java -cp src domain.Matrix1
```

### Example Output

```
=== MATRIX GAME START ===

| | | |W| | |T| |
| | | | | | | |A|
| | | | | |W| | |
| | | | |W| | |W|
| | | | | | | |W|
| | | | |A| | |W|
| | |N| | | | | |
| | | | | | | | |

...

Smith caught Neo! NEO LOSES!
=== GAME OVER ===
```

## Built With

- Java (JDK 8+)

## Authors

- Adrian Ducuara

## License

This project uses the MIT License. See [LICENSE](LICENSE) for details.
