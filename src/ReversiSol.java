// an experiment to see how much JavaFX code is required
// to build a game of reversi

// imports
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

// class defnition for reversi game
public class ReversiSol extends Application {
	// overridden init method
	public void init() {
		// create the stack pane and attach a reversi control to it
		sp_mainlayout = new StackPane();
		rc_reversi = new ReversiControl();
		sp_mainlayout.getChildren().add(rc_reversi);
	}
	
	// overridden start method
	public void start(Stage primaryStage) {
		// set a title, size and the stack pane as the root of the scene graph
		primaryStage.setTitle("reversi example");
		primaryStage.setScene(new Scene(sp_mainlayout, 800, 800));
		primaryStage.show();
	}
	
	// overridden stop method
	public void stop() {
		
	}
	
	// entry point into our program for launching our javafx applicaton
	public static void main(String[] args) {
		launch(args);
	}
	
	// private fields for a stack pane and a reversi control
	private StackPane sp_mainlayout;
	private ReversiControl rc_reversi;
	
}

// class definition for a custom reversi control
class ReversiControl extends Control {
	// constructor for the class
	public ReversiControl() {
		// set the default skin and generate a reversi board
		setSkin(new ReversiControlSkin(this));
		rb_board = new ReversiBoard();
		getChildren().add(rb_board);
		
		// add a mouse clicked listener that will try to place a piece on
		// the reversi board
		setOnMouseClicked(new EventHandler<MouseEvent>() {
			// overridden method to handle a mouse event
			@Override
			public void handle(MouseEvent event) {
				rb_board.placePiece(event.getX(), event.getY());
			}
		});
		
		// add a key press listener that will reset the board if space is
		// pressed
		setOnKeyPressed(new EventHandler<KeyEvent>() {
			// overridden handle method for key events
			@Override
			public void handle(KeyEvent event) {
				if(event.getCode() == KeyCode.SPACE)
					rb_board.resetGame();
			}
		});
		
	}
	
	// overridden version of the resize method
	@Override
	public void resize(double width, double height) {
		// call the super class method and resize the board
		super.resize(width, height);
		rb_board.resize(width, height);
	}
	
	// private fields of a reversi board
	ReversiBoard rb_board;
}

// class definition for a skin for the reversi control
// NOTE: to keep JavaFX happy we dont use the skin here
class ReversiControlSkin extends SkinBase<ReversiControl> {
	// default constructor for the class
	public ReversiControlSkin(ReversiControl rc) {
		super(rc);
	}
}

// class definition for the reversi board
class ReversiBoard extends Pane {
	// default constructor for the class
	public ReversiBoard() {
		// initialise the arrays for the board and renders and lines
		// and translates
		render = new ReversiPiece[8][8];
		horizontal = new Line[8];
		vertical = new Line[8];
		horizontal_t = new Translate[8];
		vertical_t = new Translate[8];
		surrounding = new int[3][3];
		can_reverse = new boolean[3][3];
		
		// initialise the grid lines and background, renders and reset the game
		initialiseLinesBackground();
		initialiseRender();
		resetGame();
	}
	
	// public method that will try to place a piece in the given x,y coordinate
	public void placePiece(final double x, final double y) {
		// figure out which cell the current player has clicked on
		final int cellx = (int) (x / cell_width);
		final int celly = (int) (y / cell_height);
		
		// if the game is not in play then do nothing
		if(!in_play)
			return;
		
		// if there is a piece already placed then return and do nothing
		if(render[cellx][celly].getPiece() != 0)
			return;

		// determine what pieces surround the current piece. if there is no opposing
		// pieces then a valid move cannot be made.
		determineSurrounding(cellx, celly);
		if(!adjacentOpposingPiece())
			return;

		// see if a reverse can be made in any direction if none can be made then return
		if(!determineReverse(cellx, celly))
			return;
		
		// at this point we have done all the checks and they have passed so now we can place
		// the piece and perform the reversing also check if the game has ended
		placeAndReverse(cellx, celly);
		
		// if we get to this point then a successful move has been made so swap the
		// players and update the scores
		swapPlayers();
		updateScores();
		determineEndGame();
		
		// print out some information
		System.out.println("placed at: " + cellx + ", " + celly);
		System.out.println("White: " + player1_score + " Black: " + player2_score);
		if(current_player == 1)
			System.out.println("current player is White");
		else
			System.out.println("current player is Black");
	}
	
	// overridden version of the resize method to give the board the correct size
	@Override
	public void resize(double width, double height) {
		// call the superclass method
		super.resize(width, height);
		
		// get a new cell width and cell height
		cell_width = width / 8.0;
		cell_height = height / 8.0;
		
		// resize the background. resize the lines and reposition them
		background.setWidth(width); background.setHeight(height);
		horizontalResizeRelocate(width);
		verticalResizeRelocate(height);
		
		// resize and relocate all pieces that are in the board
		pieceResizeRelocate();
	}
	
	// public method for resetting the game
	public void resetGame() {
		// reset the board state
		resetRenders();
		
		// set the pieces for the starting of the game
		render[3][3].setPiece(1); render[4][4].setPiece(1);
		render[3][4].setPiece(2); render[4][3].setPiece(2);
		
		// set the game in play, current player to one, and that neither player has won
		// and both players have a score of two
		in_play = true;
		current_player = 2;
		opposing = 1;
		player1_score = player2_score = 2;
		
	}
	
	// private method that will reset the renders
	private void resetRenders() {
		for(int i = 0; i < 8; i++)
			for(int j = 0; j < 8; j++)
					render[i][j].setPiece(0);
	}
	
	// private method that will initialise the background and the lines
	private void initialiseLinesBackground() {
		// set a background to be cyan in colour and add it to the render list
		background = new Rectangle();
		background.setFill(Color.CYAN);
		getChildren().add(background);
		
		// generate the eight horizontal lines and their translates.
		// set the stroke colour to be while and set x1,y1,y2 to all be
		// zero as they will not change. add them to the render list
		for(int i = 0; i < 8; i++) {
			horizontal[i] = new Line();
			horizontal_t[i] = new Translate();
			horizontal[i].setStartX(0);
			horizontal[i].setStartY(0);
			horizontal[i].setEndY(0);
			horizontal[i].getTransforms().add(horizontal_t[i]);
			getChildren().add(horizontal[i]);
		}
		
		// generate the eight vertical lines and their translates.
		// set the stroke colour to be while and set x1,y1,x2 to all be
		// zero as they will not change. add them to the render list
		for(int i = 0; i < 8; i++) {
			vertical[i] = new Line();
			vertical_t[i] = new Translate();
			vertical[i].setStartX(0);
			vertical[i].setStartY(0);
			vertical[i].setEndX(0);
			vertical[i].getTransforms().add(vertical_t[i]);
			getChildren().add(vertical[i]);
		}
	}
	
	// private method for resizing and relocating the horizontal lines
	private void horizontalResizeRelocate(final double width) {
		// go through the horizontal lines and fix the value of x2.
		// generate the appropriate translate value for each line
		for(int i = 0; i < 8; i++) {
			horizontal[i].setEndX(width);
			horizontal_t[i].setY(i * cell_height);
		}
	}
	
	// private method for resizing and relocating the vertical lines
	private void verticalResizeRelocate(final double height) {
		// go through the vertical lines and fix the value of y2.
		// generate the appropriate translate value for each line
		for(int i = 0; i < 8; i++) {
			vertical[i].setEndY(height);
			vertical_t[i].setX(i * cell_width);
		}
	}
	
	// private method for swapping the players
	private void swapPlayers() {
		if(current_player == 1) {
			current_player = 2;
			opposing = 1;
		} else {
			current_player = 1;
			opposing = 2;
		}
	}
	
	// private method for updating the player scores
	private void updateScores() {
		player1_score = 0;
		player2_score = 0;
		
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				if(render[i][j].getPiece() == 1)
					player1_score++;
				else if(render[i][j].getPiece() == 2)
					player2_score++;
			}
		}
			
	}
	
	// private method for resizing and relocating all the pieces
	private void pieceResizeRelocate() {
		// for each piece set a new position and a new size
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
					render[i][j].resize(cell_width, cell_height);
					render[i][j].relocate(i * cell_width, j * cell_height);
			}
		}
	}
	
	// private method for determining which pieces surround x,y will update the
	// surrounding array to reflect this
	private void determineSurrounding(final int x, final int y) {
		for(int i = x - 1; i < x + 2; i++)
			for(int j = y - 1; j < y + 2; j++)
				surrounding[i - (x - 1)][j - (y - 1)] = getPiece(i, j);
	}
	
	// private method for determining if a reverse can be made will update the can_reverse
	// array to reflect the answers will return true if a single reverse is found
	private boolean determineReverse(final int x, final int y) {
		// result for checking if we can reverse
		boolean reverse = false;
		
		// if there is an opposing player piece in any of the surrounding cells then see if a reverse
		// can be made, if one is found keep determining
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				// if we have the center piece then ignore it
				if(i == 1 && j == 1)
					continue;
				
				if(surrounding[i][j] == opposing)
					can_reverse[i][j] = isReverseChain(x, y, i - 1, j - 1);
				else
					can_reverse[i][j] = false;
				
				if(can_reverse[i][j])
					reverse = true;
			}
		}
		
		// return the status of the reverse
		return reverse;
	}
	
	// private method for determining if a reverse can be made from a position (x,y) for
	// a player piece in the given direction (dx,dy) returns true if possible
	// assumes that the first piece has already been checked
	private boolean isReverseChain(final int x, final int y, final int dx, final int dy) {
		// values for tracking our current move in the chain test
		int newx = x + dx;
		int newy = y + dy;
		
		// we know the first piece is an opposing piece so keep iterating until we hit
		// one of our pieces, empty cell, or an invalid space.
		for(; getPiece(newx, newy) == opposing; newx += dx, newy += dy);
		
		// if we have an invalid cell or an empty cell then return false, otherwise
		// return true
		if(getPiece(newx, newy) == 0 || getPiece(newx, newy) == -1 || getPiece(newx, newy) == opposing)
			return false;
		else
			return true;
	}
	
	// private method for determining if any of the surrounre an opposing
	// piece. if a single one exists then return true otherwise false
	private boolean adjacentOpposingPiece() {
		// go through the surrounding pieces and ignore the center piece. if a match is found
		// then return true
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				if(i == 1 && j == 1)
					continue;
				else if(surrounding[i][j] == opposing)
					return true;
			}
		}
		
		// if no piece has been found return false
		return false;
	}
	
	// private method for placing a piece and reversing pieces
	private void placeAndReverse(final int x, final int y) {
		// place the player piece on the board
		if(current_player == 1)
			render[x][y].setPiece(1);
		else
			render[x][y].setPiece(2);
		
		// go through each direction and if there is a chain available then reverse it
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				if(can_reverse[i][j])
					reverseChain(x, y, i - 1, j - 1);
			}
		}
				
	}
	
	// private method to reverse a chain
	private void reverseChain(final int x, final int y, final int dx, final int dy) {
		// reverse any pieces that are of the opposing colour and stop
		// after we have no opposing pieces left
		for(int newx = x + dx, newy = y + dy; render[newx][newy].getPiece() == opposing; newx += dx, newy += dy) {
			render[newx][newy].setPiece(current_player);
		}
			
	}
	
	// private method for getting a piece on the board. this will return the board
	// value unless we access an index that doesnt exist. this is to make the code
	// for determing reverse chains much easier
	private int getPiece(final int x, final int y) {
		// if the x value or y value is out of range then return -1 otherwise
		// return the board piece
		if(x < 0 || x >= 8 || y < 0 || y >= 8)
			return -1;
		else
			return render[x][y].getPiece();
		
	}
	
	// private method that will determine if the end of the game has been reached
	private void determineEndGame() {
		// if the board has been filled or either player has lost their pieces then
		// we have reached the end of the game
		if(player1_score + player2_score == 64 || player1_score == 0 || player2_score == 0) {
			determineWinner();
		}
		
		// see if the current player can make a move if not see if the opposing player can make a move
		// if neither can make a move then we have reached end game
		if(canMove()) {
			return;
		} else {
			swapPlayers();
			if(!canMove())
				determineWinner();
		}
			
		
		
	}
	
	// private method to determine if a player has a move available
	private boolean canMove() {
		// go through the entire board
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				// if the cell is already occupied then do nothing
				if(render[i][j].getPiece() > 0)
					continue;
				
				// determine if there are any surrounding pieces that are opponents
				// and if a reverse can be made if so then return true
				determineSurrounding(i, j);
				if(adjacentOpposingPiece() && determineReverse(i, j))
					return true;
			}
		}
		
		
		// if we get to this point then this player cannot make a move
		return false;
	}
	
	// private method that determines who won the game
	private void determineWinner() {
		if(player1_score > player2_score) {
			System.out.println("white wins");
		} else if(player1_score < player2_score) {
			System.out.println("black wins");
		} else {
			System.out.println("game is a draw");
		}
		in_play = false;
			
	}
	
	// private method that will initialise everything in the render array
	private void initialiseRender() {
		// initialise a render object to initialise all elements and add them to the scene graph
		// they should all have a type of zero to begin with
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				render[i][j] = new ReversiPiece(0);
				getChildren().add(render[i][j]);
			}
		}
	}
	
	// XXX start of methods that only exist to enable testing these should be removed from production code
/*	public Rectangle getRBackground() { return background; }
	public Line[] getHorizontal() { return horizontal; }
	public Line[] getVertical() { return vertical; }
	public Translate[] getHorizontalT() { return horizontal_t; }
	public Translate[] getVerticalT() { return vertical_t; }
	public ReversiPiece[][] getRender() { return render; }
	public int getCurrentPlayer() { return current_player; }
	public int getOpposing() { return opposing; }
	public boolean getInPlay() { return in_play; }
	public int getPlayer1Score() { return player1_score; }
	public int getPlayer2Score() { return player2_score; }
	public double getCellWidth() { return cell_width; }
	public double getCellHeight() { return cell_height; }
	public int[][] getSurrounding() { return surrounding; }
	public boolean[][] getCanReverse() { return can_reverse; }
	public void testSwapPlayers() { swapPlayers(); }
	public void testResetRenders() { resetRenders(); }
	public void testResetGame() { resetGame(); }
	public int testGetPiece(final int x, final int y) { return getPiece(x, y); }
	public void testDetermineSurrounding(final int x, final int y) { determineSurrounding(x, y); }
	public boolean testAdjacentOpposing() { return adjacentOpposingPiece(); }
	public boolean testReverseChain(final int x, final int y, final int dx, final int dy) { return isReverseChain(x, y, dx, dy); }
	public boolean testDetermineReverse(final int x, final int y) { return determineReverse(x, y); }
	public void testPlaceAndReverse(final int x, final int y) { placeAndReverse(x, y); }
	public void testUpdateScores() { updateScores(); }
	public void testPlacePiece(final int x, final int y) { placePiece(x, y); }
	*/// XXX end of methods that only exist to enable testing these should be removed from production code
	
	// private fields that make the reversi board work
	
	// rectangle that makes the background of the board
	private Rectangle background;
	// arrays for the lines that makeup the horizontal and vertical grid lines
	private Line[] horizontal;
	private Line[] vertical;
	// arrays holding translate objects for the horizontal and vertical grid lines
	private Translate[] horizontal_t;
	private Translate[] vertical_t;
	// arrays for the internal representation of the board and the pieces that are
	// in place
	private ReversiPiece[][] render;
	// the current player who is playing and who is his opposition
	private int current_player;
	private int opposing;
	// is the game currently in play
	private boolean in_play;
	// current scores of player 1 and player 2
	private int player1_score;
	private int player2_score;
	// the width and height of a cell in the board
	private double cell_width;
	private double cell_height;
	// 3x3 array that holds the pieces that surround a given piece
	private int[][] surrounding;
	// 3x3 array that determines if a reverse can be made in any direction
	private boolean[][] can_reverse;
}

// class definition for a reversi piece
class ReversiPiece extends Group {
	// default constructor for the class
	public ReversiPiece(int player) {
		// take a copy of the player
		this.player = player;
		
		// generate the ellipse for the player. if this is player 1 then this should
		// be white otherwise it should be black
		piece = new Ellipse();
		t = new Translate();
		piece.getTransforms().add(t);
		if(player == 1)
			piece.setFill(Color.WHITE);
		else
			piece.setFill(Color.BLACK);
		getChildren().add(piece);
		
		// if the player is set to zero then hide this piece
		if(player == 0)
			piece.setVisible(false);
	}
	
	// overridden version of the resize method to give the piece the correct size
	@Override
	public void resize(double width, double height) {
		// call the superclass method
		super.resize(width, height);
		
		// resize and relocate the ellipse
		piece.setCenterX(width / 2.0); piece.setCenterY(height / 2.0);
		piece.setRadiusX(width / 2.0); piece.setRadiusY(height / 2.0);
	}
	
	// overridden version of the relocate method to position the piece correctly
	@Override
	public void relocate(double x, double y) {
		// call the superclass method
		super.relocate(x, y);
		
		// update the translate with the new position
		t.setX(x); t.setY(y);
	}
	
	// public method that will swap the colour and type of this piece
	public void swapPiece() {
		if(player == 1) {
			player = 2;
			piece.setFill(Color.WHITE);
		} else {
			player = 1;
			piece.setFill(Color.BLACK);
		}
	}
	
	// method that will set the piece type
	public void setPiece(final int type) {
		player = type;
		
		// set the colour of the piece and if necessary make it visible
		if(type == 1) {
			piece.setFill(Color.WHITE);
			piece.setVisible(true);
		} else if (type == 2) {
			piece.setFill(Color.BLACK);
			piece.setVisible(true);
		} else if (type == 0) {
			piece.setVisible(false);
		}
		
		
	}
	
	// returns the type of this piece
	public int getPiece() { return player; }
	
/*	// XXX start of methods that only exist to faciliate testing
	public Ellipse getRenderPiece() { return piece; }
	public Translate getTranslate() { return t; }
	// XXX end of methods that only exist to facilite testing
	*/
	// private fields
	private int player;		// the player that this piece belongs to
	private Ellipse piece;	// ellipse representing the player's piece
	private Translate t;	// translation for the player piece
}