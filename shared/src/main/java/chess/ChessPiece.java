package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }



    public enum TeamColor { WHITE, BLACK}
    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        ChessPiece piece = board.getPiece(myPosition);
        List<ChessMove> moves = new ArrayList<>(); // list of all the possible moves
        switch(type) {
            case BISHOP -> {
                int [][] bishopDirections = {
                        {1,1},{1,-1},{-1,1},{-1,-1} // directions a bishop can move
                };
                slidingMove(board, myPosition,moves, bishopDirections);
            }
            case KING -> {
                int [][] kingDirections = {
                        {1,1},{1,0},{1,-1},{0,-1},{-1,-1},{-1,0},{-1,1},{0,1} // directions a king can move
                };
            }
            case QUEEN -> {
                int [][] queenDirections = {
                        {1,1},{1,0},{1,-1},{0,-1},{-1,-1},{-1,0},{-1,1},{0,1} // directions a queen can move
                };
            }
            case ROOK -> {
                int [][] rookDirections = {
                        {0,1},{1,0},{0,-1},{-1,0} // directions a rook can move
                };
            }
            case KNIGHT -> {
                int [][] knightDirections = {
                        {2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2},{1,2} // directions a knight can move
                };
            }
            case PAWN -> {
                int [][] pawnDirections = {
                        {0,1},{-1,1},{1,1} // directions a pawn can move
                };
            }

        }
        return moves;
    }

    private void slidingMove (ChessBoard board, ChessPosition position, List<ChessMove> moves, int[][] directions) {
        int startRow = position.getRow();
        int startCol = position.getColumn();

        for (int[] dir: directions) {  // iterating through all the directions
            int row = startRow + dir[0];
            int col = startCol + dir[1];

            while (row >= 1 && row <= 8 && col <= 8 && col >= 1) {// while loop to make sure that I am staying within the bounds of the map
                ChessPosition newpos = new ChessPosition(row,col);
                ChessPiece ocupied = board.getPiece(newpos); // checking if the space is occupied
                if (ocupied == null) {
                    moves.add(new ChessMove(position, newpos, null));
                } else {
                    if (ocupied.getTeamColor() != this.pieceColor) {
                        moves.add(new ChessMove(position, newpos, null ));
                    }
                    break;
                }
            row += dir[0];
            col += dir[1];

            }
        }
    }

    @Override
    public String toString() {
        return "ChessPiece{" +
                "pieceColor=" + pieceColor +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }
}
