import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.ArrayList; 
import java.util.Collections; 
import java.util.Arrays; 
import java.util.*; 
import java.util.concurrent.ThreadLocalRandom; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Tetris extends PApplet {







//GAME SETUP
Vector2 boardSize = new Vector2(10,22);
int vanishingLine = 20;
int linesPerLevel = 10;
int maxLevel = 15;
int queueSize = 6;
int waitToStartFrames = 60;

//ALTERNATE SPEED RATES
int lockOutRate = 30;
int lineClearedRate = 10;
int softDropModifier = 20;

//DIRECTIONS
public final Vector2 up = new Vector2(0, 1);
public final Vector2 down = new Vector2(0, -1);
public final Vector2 left = new Vector2( -1,0);
public final Vector2 right = new Vector2(1,0);

//RENDERING
Vector2 margin = new Vector2(160, 20);
int cellSize = 20;
int queueCellSpacing = 1;
int lineClearColor = color(125,125,125);
int ghostColor = color(125,125,125);

//STAT RENDERING
Vector2 queueTextOffsetCell = new Vector2(2, 0);
Vector2 holdTextOffsetCell = new Vector2( -5, 0);
Vector2 statsOffsetCells = new Vector2( -6, -6);
Vector2 controlTextOffsetCells = new Vector2( -6, 4);
int statsSpacing = 1;
String controlsText = "CONTROLS:\nLeft / Right = Move\nZ / X = Rotate\nC = Hold\nDown = Soft Drop\nSpace = Hard Drop\nP = Pause";

//SCORE SCALING PER LEVEL (SSPL)
int sspl_single = 100;
int sspl_double = 300;
int sspl_triple = 500;
int sspl_tetris = 800;
int sspl_miniTSpin = 100;
int sspl_miniTSpin_single = 200;
int sspl_miniTSpin_double = 400;
int sspl_tSpin = 400;
int sspl_tSpin_single = 800;
int sspl_tSpin_double = 1200;
int sspl_tSpin_triple = 1600;

//SCORE SCALING PER CELL DROP (SSPCD)
int sspcd_softDrop = 1;
int sspcdMAX_softDrop = 20;
int sspcd_hardDrop = 2;
int sspcdMAX_hardDrop = 40;

//TSPINS, TRICKS, AND BACK TO BACK SCORING
public enum TSpinType { none, mini, full }
Vector2[] tSpinCorners = {new Vector2( -1,1), new Vector2(1,1), new Vector2(1, -1), new Vector2( -1, -1)};
public enum Trick {
    //This first line includes all tricks that allow back to back scoring
    miniTSpin_double, tetris, tSpin_single, tSpin_double, tSpin_triple,
    //This second line of tricks DO NOT allow back to back scoring
    singleLine, doubleLine, tripleLine, tSpin, miniTSpin, miniTSpin_single, none}
//Tricks equal or under this integer value allow back to back scoring
int trickBackToBackCap = Trick.tSpin_triple.ordinal();
float backToBack_mod = 1.5f;

//INPUT
int delayedAutoShift = 10;
int autoRepeatRate = 2;
int maxInfinitySteps = 15;
public boolean resetButton() { return key == 'r'; }
public boolean pauseButton() { return keyCheck('p', ENTER); }
public boolean swapHoldButton() { return keyCheck('c', SHIFT); }
public boolean hardDropButton() { return key == ' '; }
public boolean softDropButton() { return keyCheck('s', DOWN); }
public boolean moveLeftButton() { return keyCheck('a', LEFT); }
public boolean moveRightButton() { return keyCheck('d', RIGHT); }
public boolean rotateLeftButton() { return keyCheck('z', CONTROL); }
public boolean rotateRightButton() { return keyCheck('x', UP); }

//INPUT STATES
int lastInputDir = 0;
int lastRotateInputDir = 0;
int leftMove_lastPressTime = 0;
int rightMove_lastPressTime = 0;
int leftRotate_lastPressTime = 0;
int rightRotate_lastPressTime = 0;
boolean leftMoveHeld = false;
boolean rightMoveHeld = false;
boolean leftRotateHeld = false;
boolean rightRotateHeld = false;
boolean resetHeld = false;
boolean pauseHeld = false;
boolean swapHoldHeld = false;
boolean hardDropHeld = false;

//TEMP STATES/VALUES
boolean softDrop = false;
int waitTillFrame = 0;
int clearedLines = 0;
int score = 0;
int level = 1;
boolean topOut = false;
int movementHeldFrames = 0;
int steps = 0;
boolean ignoreStepThisFrame = false;
Grid player1Grid;
Tetromino currentTetromino;
Tetromino heldTetromino;
boolean hasHeldThisTurn = false;
public ArrayList<Tetromino> tetrominoPool = new ArrayList<Tetromino>();
int infinitySteps;
int softDropScoreAccumulated;
int lastSuccessfullStep = 0;
Trick lastLineClearTrick = Trick.none;
public TSpinType lastMoveTSpinType = TSpinType.none;
boolean paused = false;

//ROTATION KICK TABLES
public KickTable empty_kickTable = new KickTable(
    new Vector2[] {new Vector2(0,0)} , //0 >> 1
    new Vector2[] {new Vector2(0,0)} , //1 >> 0
    new Vector2[] {new Vector2(0,0)} , //1 >> 2
    new Vector2[] {new Vector2(0,0)} , //2 >> 1
    new Vector2[] {new Vector2(0,0)} , //2 >> 3
    new Vector2[] {new Vector2(0,0)} , //3 >> 2
    new Vector2[] {new Vector2(0,0)} , //3 >> 0
    new Vector2[] {new Vector2(0,0)}); //0 >> 3

public KickTable standard_kickTable = new KickTable(
    new Vector2[] {new Vector2(0,0), new Vector2( -1, 0), new Vector2( -1, 1), new Vector2(0, -2), new Vector2( -1, -2)} ,  //0 >> 1
    new Vector2[] {new Vector2(0,0), new Vector2(1, 0), new Vector2(1, -1), new Vector2(0,2), new Vector2(1, 2)} ,          //1 >> 0
    new Vector2[] {new Vector2(0,0), new Vector2(1, 0), new Vector2(1, -1), new Vector2(0,2), new Vector2(1, 2)} ,          //1 >> 2
    new Vector2[] {new Vector2(0,0), new Vector2( -1, 0), new Vector2( -1, 1), new Vector2(0, -2), new Vector2( -1, -2)} ,  //2 >> 1
    new Vector2[] {new Vector2(0,0), new Vector2(1, 0), new Vector2(1, 1), new Vector2(0, -2), new Vector2(1, -2)} ,        //2 >> 3
    new Vector2[] {new Vector2(0,0), new Vector2( -1, 0), new Vector2( -1, -1), new Vector2(0,2), new Vector2( -1, 2)} ,    //3 >> 2
    new Vector2[] {new Vector2(0,0), new Vector2( -1, 0), new Vector2( -1, -1), new Vector2(0,2), new Vector2( -1, 2)} ,    //3 >> 0
    new Vector2[] {new Vector2(0,0), new Vector2(1, 0), new Vector2(1, 1), new Vector2(0, -2), new Vector2(1, -2)});        //0 >> 3

public KickTable iShape_kickTable = new KickTable(
    new Vector2[] {new Vector2(0,0), new Vector2( -2, 0), new Vector2(1, 0), new Vector2( -2, -1), new Vector2(1, 2)} ,     //0 >> 1
    new Vector2[] {new Vector2(0,0), new Vector2(2, 0), new Vector2( -1, 0), new Vector2(2,1), new Vector2( -1, -2)} ,      //1 >> 0
    new Vector2[] {new Vector2(0,0), new Vector2( -1, 0), new Vector2(2, 0), new Vector2( -1,2), new Vector2(2, -1)} ,      //1 >> 2
    new Vector2[] {new Vector2(0,0), new Vector2(1, 0), new Vector2( -2, 0), new Vector2(1, -2), new Vector2( -2, 1)} ,     //2 >> 1
    new Vector2[] {new Vector2(0,0), new Vector2(2, 0), new Vector2( -1, 0), new Vector2(2, 1), new Vector2( -1, -2)} ,     //2 >> 3
    new Vector2[] {new Vector2(0,0), new Vector2( -2, 0), new Vector2(1, 0), new Vector2( -2, -1), new Vector2(1, 2)} ,     //3 >> 2
    new Vector2[] {new Vector2(0,0), new Vector2(1, 0), new Vector2( -2, 0), new Vector2(1, -2), new Vector2( -2, 1)} ,     //3 >> 0
    new Vector2[] {new Vector2(0,0), new Vector2( -1, 0), new Vector2(2, 0), new Vector2( -1, 2), new Vector2(2, -1)});     //0 >> 3


//TETROMINO SHAPES
Tetromino[] allTetrominoTypes = {J_Shape(), L_Shape(), I_Shape(), S_Shape(), Z_Shape(), O_Shape(), T_Shape() };

public Tetromino I_Shape()
{
    int c = color(0,255,255);
    return new Tetromino(
        iShape_kickTable, false,
        new Block(c, new Vector2(0,2)),
        new Block(c, new Vector2(1,2)),
        new Block(c, new Vector2(2,2)),
        new Block(c, new Vector2(3,2)));
}

public Tetromino J_Shape()
{
    int c = color(0,0,255);
    return new Tetromino(
        standard_kickTable, false,
        new Block(c, new Vector2(0,2)),
        new Block(c, new Vector2(0,1)),
        new Block(c, new Vector2(1,1)),
        new Block(c, new Vector2(2,1)));
}

public Tetromino L_Shape()
{
    int c = color(255,127,0);
    return new Tetromino(
        standard_kickTable, false,
        new Block(c, new Vector2(2,2)),
        new Block(c, new Vector2(0,1)),
        new Block(c, new Vector2(1,1)),
        new Block(c, new Vector2(2,1)));
}

public Tetromino S_Shape()
{
    int c = color(0,255,0);
    return new Tetromino(
        standard_kickTable, false,
        new Block(c, new Vector2(1,2)),
        new Block(c, new Vector2(2,2)),
        new Block(c, new Vector2(0,1)),
        new Block(c, new Vector2(1,1)));
}

public Tetromino Z_Shape()
{
    int c = color(255,0,0);
    return new Tetromino(
        standard_kickTable, false,
        new Block(c, new Vector2(0,2)),
        new Block(c, new Vector2(1,2)),
        new Block(c, new Vector2(1,1)),
        new Block(c, new Vector2(2,1)));
}

public Tetromino T_Shape()
{
    int c = color(128,0,128);
    return new Tetromino(
        standard_kickTable, true,
        new Block(c, new Vector2(1,2)),
        new Block(c, new Vector2(0,1)),
        new Block(c, new Vector2(1,1)),
        new Block(c, new Vector2(2,1)));
}

public Tetromino O_Shape()
{
    int c = color(255,255,0);
    return new Tetromino(
        empty_kickTable, false,
        new Block(c, new Vector2(0,0)),
        new Block(c, new Vector2(0,1)),
        new Block(c, new Vector2(1,0)),
        new Block(c, new Vector2(1,1)));
}

public void setup()
{
    frameRate(60);
    
    player1Grid = new Grid(boardSize, cellSize, margin);
    
    resetGame();
}

public void resetGame()
{
    paused = false;
    clearedLines = 0;
    level = 1;
    score = 0;
    topOut = false;
    player1Grid.reset();
    
    waitTillFrame = frameCount + waitToStartFrames;
    
    tetrominoPool.clear();
    updateTetrominoPool();
    tetrominoStartState();
    heldTetromino = null;
    hasHeldThisTurn = false;
    spawnNewTetromino();
    lastLineClearTrick = Trick.none;
}

public void levelUp()
{
    //Level up effects here
}

public void levelUpCheck()
{
    int oldLevel = level;
    level = constrain(floor(clearedLines / linesPerLevel) + 1, 1, maxLevel);
    
    if (oldLevel < level)
        levelUp();
}

///Different variant of modulo that manages negative numbers. Good for arrays.
public static int Mod(float x, float m)
{
    return(int)(x - m * floor(x / m));
}

public float currentGravityFrames()
{
    float seconds = pow((0.8f - ((level - 1) * 0.007f)), level - 1);
    
    return seconds * frameRate;
}

public void updateTetrominoPool()
{
    //! Technically we don't need to make a new array here, since this function just shuffles the array and doesn't change anything else about it
    //Tetromino[] poolAddition = Arrays.copyOf(allTetrominoTypes, allTetrominoTypes.length);
    
    //Fisher-Yates shuffle
    //https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
    // If running on Java 6 or older, use `new Random()` on RHS here
    Random rnd = ThreadLocalRandom.current();
    for (int i = allTetrominoTypes.length - 1; i > 0; i--)
    {
        int index = rnd.nextInt(i + 1);
        // Simple swap
        Tetromino a = allTetrominoTypes[index];
        allTetrominoTypes[index] = allTetrominoTypes[i];
        allTetrominoTypes[i] = a;
    }
    
    Collections.addAll(tetrominoPool, allTetrominoTypes);
}

public void draw()
{   
    inputUpdate();
    
    for (int i = stepCheck(); i > 0; i--)
    {
        if (currentTetromino == null)
        {
            player1Grid.clearedLineCleanup();
            spawnNewTetromino();
        }
        else
            fallUpdate();
    }
    
    background(topOut ? 128 : 255);
    
    drawFullQueue();
    
    if (heldTetromino != null)
        drawHold();
    
    drawStats();
    
    if (currentTetromino != null)
    {
        drawGhost();
        currentTetromino.draw(player1Grid);
    }
    
    player1Grid.draw(vanishingLine);
}

public void drawGhost()
{
    int yPivot = -1;
    while(currentTetromino.canMove(player1Grid, new Vector2(0, yPivot)))
        yPivot --;
    
    yPivot++;
    for (int i = 0; i < currentTetromino.blocks.length; i++)
    {
        Vector2 ghostCell = new Vector2(currentTetromino.blocks[i].cell.x , currentTetromino.blocks[i].cell.y + yPivot);
        Vector2 center = player1Grid.getCellWorldPosition(ghostCell);
        
        fill(ghostColor);
        rect(center.x, center.y, player1Grid.cellSize, player1Grid.cellSize);
    }
}

public void drawStats()
{
    fill(0);
    
    Vector2 queueRoot = player1Grid.getCellWorldPosition(new Vector2(boardSize.x + queueTextOffsetCell.x, vanishingLine + queueTextOffsetCell.y));
    text("NEXT", queueRoot.x, queueRoot.y);
    
    Vector2 holdRoot = player1Grid.getCellWorldPosition(new Vector2(holdTextOffsetCell.x, vanishingLine + holdTextOffsetCell.y));
    text("HOLD", holdRoot.x, holdRoot.y);
    
    Vector2 leftRoot = player1Grid.getCellWorldPosition(new Vector2(statsOffsetCells.x, vanishingLine + statsOffsetCells.y));
    text("LEVEL " + level, leftRoot.x, leftRoot.y);
    text("LINES " + clearedLines, leftRoot.x, leftRoot.y + cellSize * statsSpacing);
    text("SCORE " + score, leftRoot.x, leftRoot.y + cellSize * 2 * statsSpacing);
    if (paused) text("PAUSED", leftRoot.x, leftRoot.y + cellSize * 3 * statsSpacing);
    
    Vector2 controlsRoot = player1Grid.getCellWorldPosition(controlTextOffsetCells);
    text(controlsText, controlsRoot.x, controlsRoot.y);
}

public void drawFullQueue()
{
    Vector2 offset = new Vector2(boardSize.x + 2, vanishingLine);
    
    int push = 0;
    for (int i = 0; i < queueSize; i++)
    {
        Block[] queue = tetrominoPool.get(i).blocks;
        push += drawCompressedBlocks(queue, new Vector2(offset.x, offset.y - push)) + queueCellSpacing;
    }
}

public void drawHold()
{
    Vector2 offset = new Vector2( -5, vanishingLine);
    drawCompressedBlocks(heldTetromino.blocks, offset);
}

public int drawCompressedBlocks(Block[] queue, Vector2 offset)
{
    Vector2[] shape = GetCompressedShape(queue);
    
    int len = queue.length;
    
    int shapeHeight = 0;
    for (int i = 0; i < len; i++)
    {
        shapeHeight = max(shapeHeight, shape[i].y);
    }
    shapeHeight++;
    
    for (int i = 0; i < len; i++)
    {
        Vector2 offsetPosition = new Vector2(offset.x + shape[i].x, offset.y - shapeHeight + shape[i].y);
        Vector2 worldDraw = player1Grid.getCellWorldPosition(offsetPosition);
        fill(queue[i].blockColor);
        rect(worldDraw.x, worldDraw.y, player1Grid.cellSize, player1Grid.cellSize);
    }
    
    return shapeHeight + 1;
}

public Vector2[] GetCompressedShape(Block[] shape)
{
    int len = shape.length;
    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
    
    for (int i = 0; i < len; i++)
    {
        Vector2 cell = shape[i].cell;
        
        minX = min(minX, cell.x);
        minY = min(minY, cell.y);
    }
    
    Vector2[] newShape = new Vector2[len];
    
    for (int i = 0; i < len; i++)
    {
        newShape[i] = new Vector2(shape[i].cell.x - minX, shape[i].cell.y - minY);
    }
    
    return newShape;
}

//returns the amount of steps to occur this frame
public int stepCheck()
{
    if (paused || topOut || frameCount < waitTillFrame)
        return 0;
    
    float stepRate = currentGravityFrames(); //default fall rate
    boolean hasCurrentTetromino = currentTetromino != null;
    if (player1Grid.hasLinesToClear())
        stepRate = lineClearedRate; //wait for line to clear before continuing
    else if (hasCurrentTetromino)
    {
        if (!currentTetromino.canMove(player1Grid, down))
            stepRate = lockOutRate; //lock out time (half a second)
        else if (softDrop && stepRate > 0)
            stepRate = round((float) stepRate / softDropModifier); //fast fall modifier of current rate
    }
    else // set the step rate to zero so we can create a new tetromino right away
        stepRate = 0;
    
    int stepsThisFrame = 0;
    if (!ignoreStepThisFrame)
    {
        int roundedStepRate = round(stepRate);
        boolean instantLand = roundedStepRate <= 0;
        if (instantLand && hasCurrentTetromino) //if stepRate is less than 0 (and there is a tetromino), jump to the bottom of the playspace
            stepsThisFrame = abs(currentTetromino.getDistanceToGround(player1Grid)) - 1;
        else if (instantLand || steps - lastSuccessfullStep >= roundedStepRate)//steps % round(roundedStepRate) == 0)//move one step this frame if we are at the point to move 
            stepsThisFrame = 1;
    }
    
    if (stepsThisFrame > 0)
        lastSuccessfullStep = steps;
    
    steps++;
    ignoreStepThisFrame = false;
    
    return stepsThisFrame;
}

public void resetSteps()
{
    lastSuccessfullStep = 0;
    steps = 0;
}

public boolean keyCheck(char keyASCII, int keyCodeID)
{
    if (key == CODED)
        return keyCode == keyCodeID;
    else
        return key == Character.toUpperCase(keyASCII) || key == Character.toLowerCase(keyASCII);
}

public void keyPressed()
{
    //Start the game immediately if we press a key
    if (frameCount < waitTillFrame)
    {
        waitTillFrame = 0;
        ignoreStepThisFrame = true;
    }
    
    if (resetButton())
    {
        if (!resetHeld)
            resetGame();
        
        resetHeld = true;
    }
    else if (pauseButton())
    {
        if (!pauseHeld)
            paused = !paused;
        
        pauseHeld = true;
    }
    else if (swapHoldButton())
    {
        if (!swapHoldHeld && currentTetromino != null)
            swapHoldTetromino();
        
        swapHoldHeld = true;
    }
    else if (hardDropButton())
    {
        if (!hardDropHeld && currentTetromino != null)
            hardDrop();
        
        hardDropHeld = true;
    }
    else if (softDropButton())
    {
        softDrop = true;
    }
    else if (moveLeftButton())
    {
        if (!leftMoveHeld)
            leftMove_lastPressTime = frameCount;
        
        leftMoveHeld = true;
    }
    else if (moveRightButton())
    {
        if (!rightMoveHeld)
            rightMove_lastPressTime = frameCount;
        
        rightMoveHeld = true;
    }
    else if (rotateLeftButton())
    {
        leftRotateHeld = true;
    }
    else if (rotateRightButton())
    {
        rightRotateHeld = true;
    }
}

public void keyReleased()
{
    if (resetButton())
        resetHeld = false;
    else if (pauseButton())
        pauseHeld = false;
    else if (swapHoldButton())
        swapHoldHeld = false;
    else if (hardDropButton())
        hardDropHeld = false;
    else if (softDropButton())
        softDrop = false;
    else if (moveLeftButton())
        leftMoveHeld = false;
    else if (moveRightButton())
        rightMoveHeld = false;
    else if (rotateLeftButton())
        leftRotateHeld = false;
    else if (rotateRightButton())
        rightRotateHeld = false;
}

public void inputUpdate()
{       
    if (paused || currentTetromino == null)
        return;
    
    boolean successfulMove = moveCheck();
    boolean successfulRotation = rotateInput();
    
    if ((successfulMove || successfulRotation) && infinitySteps > 0 && !currentTetromino.canMove(player1Grid, down))
    {
        resetSteps();
        ignoreStepThisFrame = true;
        infinitySteps--;
    }    
}

public void hardDrop()
{
    if (paused)
        return;
    
    int points = 0;
    while(currentTetromino.canMove(player1Grid, down))
    {
        points += sspcd_hardDrop;
        currentTetromino.move(player1Grid, down);
        lastMoveTSpinType = TSpinType.none;
    }
    
    score += min(points, sspcdMAX_hardDrop);
    endCurrentTetromino();
}

public boolean moveCheck()
{
    int dir = 0;
    if (leftMoveHeld && rightMoveHeld)
        dir = leftMove_lastPressTime > rightMove_lastPressTime ? - 1 : 1;
    else if (leftMoveHeld)
        dir = -1;
    else if (rightMoveHeld)
        dir = 1;
    
    boolean success = false;
    
    if (dir != 0)
    {
        if (dir != lastInputDir)
            movementHeldFrames = 0;
        
        if (movementHeldFrames == 0 || (movementHeldFrames >= delayedAutoShift && (movementHeldFrames - delayedAutoShift) % autoRepeatRate == 0))
        {
            Vector2 moveDir = new Vector2(dir, 0);
            if (currentTetromino.canMove(player1Grid, moveDir))
            {  
                currentTetromino.move(player1Grid, moveDir);
                lastMoveTSpinType = TSpinType.none;
                success = true;
            }
        }
        
        movementHeldFrames++;
    }
    else
        movementHeldFrames = 0;
    
    lastInputDir = dir;
    
    return success;
}

public boolean rotateInput()
{
    int dir = 0;
    if (leftRotateHeld && rightRotateHeld)
        dir = leftRotate_lastPressTime > rightRotate_lastPressTime ? - 1 : 1;
    else if (leftRotateHeld)
        dir = -1;
    else if (rightRotateHeld)
        dir = 1;
    
    boolean success;
    if (dir != 0 && dir != lastRotateInputDir)
        success = currentTetromino.tryRotate(player1Grid, dir);
    else
        success = false;
    
    lastRotateInputDir = dir;
    
    return success;
}

public void fallUpdate()
{
    if (currentTetromino.canMove(player1Grid, down))
    {
        currentTetromino.move(player1Grid, down);
        lastMoveTSpinType = TSpinType.none;
        
        softDropScoring();
    }
    else
        endCurrentTetromino();
}

public void softDropScoring()
{
    if (softDrop)
    {
        softDropScoreAccumulated += sspcd_softDrop;
        if (softDropScoreAccumulated <= sspcdMAX_softDrop)
            score += sspcd_softDrop;
    }
}

public void tetrominoStartState()
{
    softDropScoreAccumulated = 0;
    infinitySteps = maxInfinitySteps;
    lastMoveTSpinType = TSpinType.none;
    resetSteps();
}

public void spawnNewTetromino()
{
    levelUpCheck();
    currentTetromino = new Tetromino(tetrominoPool.get(0));
    currentTetromino.trySpawn(player1Grid);
    tetrominoPool.remove(0);
    
    if (tetrominoPool.size() < queueSize)
        updateTetrominoPool();
}

public void swapHoldTetromino()
{
    if (paused || hasHeldThisTurn)
        return;
    
    Tetromino buffer = new Tetromino(currentTetromino.original);
    currentTetromino.remove(player1Grid);
    
    if (heldTetromino != null)
    {
        currentTetromino = heldTetromino;
        currentTetromino.trySpawn(player1Grid);
    }
    else
        spawnNewTetromino();
    
    heldTetromino = buffer;
    tetrominoStartState();
    
    hasHeldThisTurn = true;
}

public void endCurrentTetromino()
{
    Collections.addAll(player1Grid.deadBlocks, currentTetromino.blocks);
    int linesCleared = player1Grid.lineClearCheck();
    ignoreStepThisFrame = linesCleared > 0;
    
    trickCalculation(linesCleared);
    
    if (currentTetromino.getLowestY() >= vanishingLine)
        topOutGameOver();
    
    currentTetromino = null;
    hasHeldThisTurn = false;
    
    tetrominoStartState();
}

public void topOutGameOver()
{
    currentTetromino = null;
    topOut = true;
}

public void trickCalculation(int linesCleared)
{
    TSpinType tSpin = tSpinCheck();
    
    switch(linesCleared)
    {
        case 0:
            if (tSpin == TSpinType.mini)
                processTrick(linesCleared, sspl_miniTSpin, Trick.miniTSpin);
            else if (tSpin == TSpinType.full)
                processTrick(linesCleared, sspl_tSpin, Trick.tSpin);
            
            break;
        
        case 1:
            if (tSpin == TSpinType.mini)
                processTrick(linesCleared, sspl_miniTSpin_single, Trick.miniTSpin_single);
            else if (tSpin == TSpinType.full)
                processTrick(linesCleared, sspl_tSpin_single, Trick.tSpin_single);
            else
                processTrick(linesCleared, sspl_single, Trick.singleLine);
            
            break;
        
        case 2:
            if (tSpin == TSpinType.mini)
                processTrick(linesCleared, sspl_miniTSpin_double, Trick.miniTSpin_double);
            else if (tSpin == TSpinType.full)
                processTrick(linesCleared, sspl_tSpin_double, Trick.tSpin_double);
            else
                processTrick(linesCleared, sspl_double, Trick.doubleLine);
            
            break;
        
        case 3:
            if (tSpin == TSpinType.full)
                processTrick(linesCleared, sspl_tSpin_triple, Trick.tSpin_triple);
            else
                processTrick(linesCleared, sspl_triple, Trick.tripleLine);
            
            break;
        
        case 4:
            processTrick(linesCleared, sspl_tetris, Trick.tetris);
            break;
    }
}

public void processTrick(int linesCleared, int baseScore, Trick trick)
{
    boolean isBackToBack = isBackToBack(trick);    
    float points = baseScore * (isBackToBack ? backToBack_mod : 1);
    score += points;
    
    //Only change/break a back-to-back if lines have been cleared
    if (linesCleared > 0)
        lastLineClearTrick = trick;
    
    if (isBackToBack) print("BACK TO BACK ");
    print(trick + ": " + points + "\n");
}

public boolean isBackToBack(Trick trick)
{
    return(int) trick.ordinal() <= trickBackToBackCap && lastLineClearTrick == trick;
}

//https://tetris.wiki/T-Spin
public TSpinType tSpinCheck()
{
    //The last maneuver of the T tetrimino must be a rotation.
    if (lastMoveTSpinType == TSpinType.none)
        return TSpinType.none;
    
    //If there are two minoes in the front corners of the 3 by 3 square occupied by the T (the front corners are ones next to the sticking out mino of the T) and at least one mino in the two other corners (to the back), it is a "proper" T-spin.
    TSpinType tSpinState = currentTetromino.getTSpinCornerState(player1Grid);
    
    //Otherwise, if there is only one mino in two front corners and two minoes to the back corners, it is a Mini T-spin.
    //However, if the last rotation that kicked the T moves its center 1 by 2 blocks (the last rotation offset of SRS), it is still a proper T-spin.
    if (tSpinState == TSpinType.mini)
        tSpinState = lastMoveTSpinType;
    
    return tSpinState;
}

public class Grid
{
    public Vector2 boardSize;
    public int cellSize;
    Vector2 worldOffset;
    public Block[][] cells;
    
    public ArrayList<Block> deadBlocks = new ArrayList<Block>();
    ArrayList<Integer> clearedRows = new ArrayList<Integer>();
    
    public Grid(Vector2 boardSize, int cellSize, Vector2 worldOffset)
    {
        this.boardSize = boardSize;
        this.cellSize = cellSize;
        this.worldOffset = worldOffset;
        this.cells = new Block[boardSize.x][boardSize.y];
    }
    
    public void reset()
    {
        deadBlocks.clear();
        clearedRows.clear();
        for (int x = 0; x < boardSize.x; x++)
        {
            for (int y = 0; y < boardSize.y; y++)
            {
                cells[x][y] = null;
            }
        }
    }
    
    public void draw(int vanishingLine)
    {
        for (Block block : deadBlocks)
        {
            block.draw(player1Grid);
        }
        
        Vector2 endPoint = getCellWorldPosition(new Vector2(boardSize.x, vanishingLine - 1));
        for (int x = 0; x <= boardSize.x; x++)
        {
            for (int y = vanishingLine - 1; y >= -1; y--)
            {
                Vector2 point = getCellWorldPosition(new Vector2(x, y));
                
                line(point.x, endPoint.y, point.x, point.y);
                line(endPoint.x, point.y, point.x, point.y);
            }
        }
    }
    
    public boolean hasLinesToClear()
    {
        return clearedRows.size() > 0;
    }
    
    public int lineClearCheck()
    {
        for (int y = 0; y < boardSize.y; y++)
        {
            boolean success = true;
            //Check for a complete line
            for (int x = 0; x < boardSize.x; x++)
            {
                if (cells[x][y] == null)
                {
                    success = false;
                    break;
                }
            }
            
            if (success)
            {
                if (!clearedRows.contains(y))
                    clearedRows.add(y);
                
                //paint the blocks the cleared line color
                for (int x = 0; x < boardSize.x; x++)
                {
                    cells[x][y].blockColor = lineClearColor;
                }
            }
        }      
        
        return clearedRows.size();
    }
    
    public void clearedLineCleanup()
    {
        if (!hasLinesToClear())
            return;
        
        Collections.sort(clearedRows);
        for (int i = clearedRows.size() - 1; i >= 0; i--, clearedLines++)
        {
            //clear blocks in row
            int rowY = clearedRows.get(i);
            for (int x = 0; x < boardSize.x; x++)
            {
                deadBlocks.remove(cells[x][rowY]);
                cells[x][rowY] = null;
            }
            
            //Move all blocks about
            for (int y = rowY + 1; y < boardSize.y; y++)
            {
                for (int x = 0; x < boardSize.x; x++)
                {
                    Block block = cells[x][y];
                    if (block != null)
                        block.move(this, down);
                }
            }
        }
        
        clearedRows.clear();
    }
    
    public boolean isOccupied(Vector2 cell, Block[] ignoredBlocks)
    {
        Block checkedCell = cells[cell.x][cell.y];
        if (checkedCell == null)
            return false;
        
        for (int i = 0; i < ignoredBlocks.length; i++)
        {
            if (checkedCell == ignoredBlocks[i])
                return false;
        }
        
        return true;
    }
    
    public boolean withinBounds(Vector2 cell)
    {
        return(cell.x >= 0 && cell.y >= 0 && cell.x < boardSize.x && cell.y < boardSize.y);
    }
    
    public Vector2 getCellWorldPosition(Vector2 cell)
    {
        return new Vector2(worldOffset.x + cell.x * cellSize, worldOffset.y + (boardSize.y - 1 - cell.y) * cellSize);
    }
}

public class Block
{
    public Vector2 cell;
    public int blockColor;
    
    public Block(int blockColor, Vector2 cell)
    {
        this.cell = cell;
        this.blockColor = blockColor;
    }
    
    public Block(Block block)
    {
        cell = new Vector2(block.cell);
        blockColor = block.blockColor;
    }
    
    public void draw(Grid grid)
    {
        Vector2 center = grid.getCellWorldPosition(cell);
        
        fill(blockColor);
        rect(center.x, center.y, grid.cellSize, grid.cellSize);
    }
    
    public boolean canMove(Grid grid, Vector2 direction, Block[] ignoredBlocks)
    {
        return canJump(grid,new Vector2(cell.x + direction.x, cell.y + direction.y), ignoredBlocks);
    }
    
    public boolean canJump(Grid grid, Vector2 cell, Block[] ignoredBlocks)
    {   
        return grid.withinBounds(cell) && !grid.isOccupied(cell, ignoredBlocks);
    }
    
    public void move(Grid grid, Vector2 direction)
    {
        jump(grid, new Vector2(cell.x + direction.x, cell.y + direction.y));
    }
    
    public void remove(Grid grid)
    {
        if (grid.cells[this.cell.x][this.cell.y] == this)
            grid.cells[this.cell.x][this.cell.y] = null;
    }
    
    public void jump(Grid grid, Vector2 cell)
    {
        remove(grid);
        
        this.cell = cell;
        
        grid.cells[cell.x][cell.y] = this;
    }
}

public class Tetromino
{
    public Vector2[] localShape;
    public Block[] blocks;
    public Vector2 shapeSize;
    public float boxSize;
    public KickTable kickTable;
    public int currentRotationState = 0;
    public boolean canTSpin = false;
    
    public Tetromino original;
    
    public Tetromino(KickTable kickTable, boolean canTSpin, Block...startingShape)
    {
        blocks = startingShape;
        localShape = new Vector2[blocks.length];
        shapeSize = getShapeSize(startingShape);
        boxSize = max(shapeSize.x, shapeSize.y);
        this.kickTable = kickTable;
        this.canTSpin = canTSpin;
        
        Vector2 fullSize = getShapeSize(startingShape);
        
        for (int i = 0; i < startingShape.length; i++)
        {
            localShape[i] = startingShape[i].cell;
        }
        
        original = this;
    }
    
    public Tetromino(Tetromino source)
    {
        int len = source.blocks.length;
        blocks = new Block[len];
        localShape = new Vector2[len];
        this.kickTable = source.kickTable;
        this.canTSpin = source.canTSpin;
        
        for (int i = 0; i < len; i++)
        {
            blocks[i] = new Block(source.blocks[i]);
            localShape[i] = new Vector2(source.localShape[i]);
        }
        
        shapeSize = source.shapeSize;
        boxSize = source.boxSize;
        
        original = source;
    }
    
    public TSpinType getTSpinCornerState(Grid grid)
    {
        if (!canTSpin)
            return TSpinType.none;
        
        //Find center of T block
        Block centerBlock = null;
        Vector2 centerCell = new Vector2(shapeSize.x / 2, shapeSize.y / 2);
        for (int i = 0; i < localShape.length; i++)
        {
            if (localShape[i].x == centerCell.x && localShape[i].y == centerCell.y)
            {
                centerBlock = blocks[i];
                break;
            }
        }
        
        //Count obstructions in the four corners surrounding the T block
        int frontCorners = 0;
        int backCorners = 0;
        for (int i = 0; i < 4; i++)
        {
            //we can shift the starting array by our currentRotationState in order to keep track of which corners are in front of us
            Vector2 corner = tSpinCorners[(i + currentRotationState) % 4];
            if (!centerBlock.canMove(grid, corner, blocks))
            {
                if (i < 2)
                    frontCorners++;
                else
                    backCorners++;
            }
        }
        
        //We need 3 corners total to count a T spin, if two of them are from our front corner, then it is a full t spin
        if (frontCorners + backCorners >= 3)
        {
            if (frontCorners == 2)
                return TSpinType.full;
            else
                return TSpinType.mini;
        }
        else
            return TSpinType.none;
    }
    
    public int getLowestY()
    {
        int lowestPoint = boardSize.y;
        
        for (int i = 0; i < blocks.length; i++)
        {
            lowestPoint = min(lowestPoint, blocks[i].cell.y);
        }
        
        return lowestPoint;
    }
    
    public int getDistanceToGround(Grid grid)
    {
        int distance = 0;
        do
        {
            distance--;
        }
        while(canMove(grid, new Vector2(0,distance)));
        
        return distance;
    }
    
    public boolean trySpawn(Grid grid)
    {
        Vector2 spawnPosition = new Vector2(grid.boardSize.x / 2, grid.boardSize.y - (int) boxSize);
        if (canJump(grid, spawnPosition))
        {
            jump(grid, spawnPosition);
            
            if (canMove(grid, down))
                move(grid, down);
            
            return true;
        }
        else //TOP OUT. LOSE CONDITION
        {
            topOutGameOver();
            return false;
        }
    }
    
    public Vector2 getShapeSize(Block[]blocks)
    {
        int maxX = 0, maxY = 0;
        for (int i = 0; i < blocks.length; i++)
        {
            Vector2 cell = blocks[i].cell;
            
            maxX = max(maxX, cell.x);
            maxY = max(maxY, cell.y);            
        }
        
        return new Vector2(maxX,maxY);
    }
    
    public boolean tryRotate(Grid grid, int direction)
    {
        float rad = -radians(direction * 90);
        float sin = sin(rad);
        float cos = cos(rad);
        float center = (boxSize) / 2;
        int nextRotationState = Mod(currentRotationState + direction, 4);
        
        Vector2[] kickOffsets = kickTable.getTable(currentRotationState, direction);
        Vector2[] rotateVectors = new Vector2[localShape.length];
        
        //Set up rotation vectors
        for (int i = 0; i < blocks.length; i++)
        {
            float xOffset = (float) localShape[i].x - center;
            float yOffset = (float) localShape[i].y - center;
            
            rotateVectors[i] = new Vector2(
                round(cos * xOffset - sin * yOffset + center),
                round(sin * xOffset - cos * yOffset + center));
        }
        
        //Run safety checks against kick table
        int sucessfulOffsetIndex = -1;
        for (int o = 0; o < kickOffsets.length; o++)
        {
            Vector2 offset = kickOffsets[o];
            
            boolean offsetSuccess = true;
            for (int i = 0; i < blocks.length; i++)
            {
                Vector2 worldRotation = new Vector2(
                    blocks[i].cell.x + (rotateVectors[i].x - localShape[i].x) + offset.x,
                    blocks[i].cell.y + (rotateVectors[i].y - localShape[i].y) + offset.y);
                
                if (!blocks[i].canJump(grid, worldRotation, blocks))
                {
                    offsetSuccess = false;
                    break;
                }
            }
            
            if (offsetSuccess)
            {
                sucessfulOffsetIndex = o;
                break;
            }
        }
        
        if (sucessfulOffsetIndex < 0)
            return false;
        
        Vector2 offset = kickOffsets[sucessfulOffsetIndex];
        
        //If we made it to this point, the rotation is valid. Begin the actual transformation
        for (int i = 0; i < blocks.length; i++)
        {
            Vector2 worldRotation = new Vector2(
                blocks[i].cell.x + (rotateVectors[i].x - localShape[i].x) + offset.x,
                blocks[i].cell.y + (rotateVectors[i].y - localShape[i].y) + offset.y);
            
            blocks[i].jump(grid,worldRotation);
            
            localShape[i].x = rotateVectors[i].x;
            localShape[i].y = rotateVectors[i].y;
        }
        
        currentRotationState = nextRotationState;
        
        //If the rotation uses the last value on the kick table (1 by 2 blocks), then it is a valid full t spin, otherwise it is a mini
        //https://tetris.wiki/T-Spin
        //https://winternebs.github.io/TETRIS-FAQ/tspin/
        if (sucessfulOffsetIndex == 4)//(sucessfulOffsetIndex == 0 || sucessfulOffsetIndex == 4)
            lastMoveTSpinType = TSpinType.full;
        else
            lastMoveTSpinType = TSpinType.mini;
        
        return true;
    }
    
    public boolean canJump(Grid grid, Vector2 cell)
    {
        Vector2 center = getCenter();
        for (int i = 0; i < blocks.length; i++)
        {
            Vector2 offsetPosition = new Vector2(cell.x - center.x + localShape[i].x, cell.y - center.y + localShape[i].y);
            if (!blocks[i].canJump(grid, offsetPosition, blocks))
                return false;
        }
        
        return true;
    }
    
    public void jump(Grid grid, Vector2 cell)
    {
        Vector2 center = getCenter();
        for (int i = 0; i < blocks.length; i++)
        {
            Vector2 offsetPosition = new Vector2(cell.x - center.x + localShape[i].x, cell.y - center.y + localShape[i].y);
            blocks[i].jump(grid,offsetPosition);
        }
    }
    
    public Vector2 getCenter()
    {
        return new Vector2(ceil((float)(shapeSize.x + 1) / 2), ceil((float)shapeSize.y / 2));
    }
    
    public boolean canMove(Grid grid, Vector2 direction)
        {
        for (int i = 0; i < blocks.length; i++)
        {
            if (!blocks[i].canMove(grid, direction, blocks))
                return false;
        }
        
        return true;
    }
    
    public void move(Grid grid, Vector2 direction)
        {
        for (int i = 0; i < blocks.length; i++)
        {
            blocks[i].move(grid,direction);
        }
    }
    
    public void remove(Grid grid)
    {
        for (int i = 0; i < blocks.length; i++)
        {
            blocks[i].remove(grid);
        }
    }
    
    public void draw(Grid grid)
    {
        for (int i = 0; i < blocks.length; i++)
        {
            blocks[i].draw(grid);
        }
    }
}

public class Vector2
{   
    public int x, y;
    
    public Vector2(Vector2 source)
    {
        x = source.x;
        y = source.y;
    }
    
    public Vector2(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
    
    public String toString()
    {
        return "(" + x + ", " + y + ")";
    }
}

public class KickTable
{
    Vector2[] table_0_1; //0 >> 1
    Vector2[] table_1_0; //1 >> 0
    Vector2[] table_1_2; //1 >> 2
    Vector2[] table_2_1; //2 >> 1
    Vector2[] table_2_3; //2 >> 3
    Vector2[] table_3_2; //3 >> 2
    Vector2[] table_3_0; //3 >> 0
    Vector2[] table_0_3; //0 >> 3
    
    public KickTable(Vector2[] table_0_1, Vector2[] table_1_0, Vector2[] table_1_2, Vector2[] table_2_1, Vector2[] table_2_3, Vector2[] table_3_2, Vector2[] table_3_0, Vector2[] table_0_3)
    {
        this.table_0_1 = table_0_1;
        this.table_1_0 = table_1_0;
        this.table_1_2 = table_1_2;
        this.table_2_1 = table_2_1;
        this.table_2_3 = table_2_3;
        this.table_3_2 = table_3_2;
        this.table_3_0 = table_3_0;
        this.table_0_3 = table_0_3;
    }
    
    public Vector2[] getTable(int currentState, int direction)
    {
        Vector2[] table = null;
        switch(currentState)
        {
            case 0:
                if (direction > 0)
                    table = table_0_1;
                else
                    table = table_0_3;
                break;
            
            case 1:
                if (direction > 0)
                    table = table_1_2;
                else
                    table = table_1_0;
                break;
            
            case 2:
                if (direction > 0)
                    table = table_2_3;
                else
                    table = table_2_1;
                break;
            
            case 3:
                if (direction > 0)
                    table = table_3_0;
                else
                    table = table_3_2;
                break;
        }
        
        return table;
    }
}
  public void settings() {  size(520,520); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Tetris" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
