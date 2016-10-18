// OthelloAI.java
//
// ICS 23 / CSE 23 Fall 2007
// Project #2: Black and White
//
// Your AI should be implemented in a class that implements this OthelloAI
// interface.  Remember that your AI class needs to follow a specific
// naming convention.  It should be called "OthelloAI[yourID#]".  So, if
// your student ID# is 12345678, your AI class should be called
// "OthelloAI12345678".  Naturally, you should write that class in a file
// called "OthelloAI12345678.java".


public interface OthelloAI
{
	// chooseMove() takes an OthelloGameState and chooses the best move,
	// returning an OthelloMove that indicates what the move is.  For
	// example, if the appropriate move is to place a tile in row 0 column 3,
	// you'd return a new OthelloMove with row 0 and column 3.
	public OthelloMove chooseMove(OthelloGameState state);
}
