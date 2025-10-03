package chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {


    private ChessPiece [][] squares = new ChessPiece[8][8];
    /**
     * Copy constructor: creates a deep copy of another ChessBoard
     */
    public ChessBoard(){
    }
    // Private copy constructor (hidden)
    private ChessBoard(ChessBoard other) {
        this.squares = new ChessPiece[8][8];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = other.squares[row][col];
                if (piece != null) {
                    this.squares[row][col] = new ChessPiece(piece.getTeamColor(), piece.getPieceType());
                }
            }
        }
    }

    //Static factory method for clarity
    public static ChessBoard copyOf(ChessBoard other) {
        return new ChessBoard(other);
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        squares[position.getRow()-1][position.getColumn()-1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return squares[position.getRow()-1][position.getColumn()-1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
         squares = new ChessPiece[8][8];

         //Adding Pawns
         for (int col = 1; col <= 8; col++){
             addPiece(new ChessPosition(2,col), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN));
             addPiece(new ChessPosition(7, col), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.PAWN));
         }

        //Adding Rooks
        addPiece(new ChessPosition(1,1), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.ROOK));
        addPiece(new ChessPosition(1,8), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.ROOK));
        addPiece(new ChessPosition(8,1), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.ROOK));
        addPiece(new ChessPosition(8,8), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.ROOK));

        //Adding Knights
        addPiece(new ChessPosition(1, 2), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(1, 7), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(8, 2), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(8, 7), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KNIGHT));

        //Adding Bishops
        addPiece(new ChessPosition(1, 3), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(1, 6), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(8, 3), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(8, 6), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.BISHOP));

        //Adding Queens
        addPiece(new ChessPosition(1, 4), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.QUEEN));
        addPiece(new ChessPosition(8, 4), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.QUEEN));

        //Adding Kings
        addPiece(new ChessPosition(1, 5), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KING));
        addPiece(new ChessPosition(8, 5), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KING));

    }
    // Helper to find the king position on the board
    public ChessPosition findKing(ChessGame.TeamColor team) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = getPiece(pos);

                if (piece != null &&
                        piece.getPieceType() == ChessPiece.PieceType.KING &&
                        piece.getTeamColor() == team) {
                    return pos;
                }
            }
        }
        return null; // should never happen in a valid game
    }

    // Helper for checking if the square is being attacked for check
    public boolean isSquareAttacked(ChessPosition target, ChessGame.TeamColor byTeam) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                if (isSquareAttacking(pos, target, byTeam)){
                    return true;
                }
            }
        }
        return false;
    }
    // Method added to break down the nesting depth
    private boolean isSquareAttacking(ChessPosition position, ChessPosition target, ChessGame.TeamColor byTeam){
        ChessPiece piece = getPiece(position);
        if (piece != null && piece.getTeamColor() == byTeam) {
            Collection<ChessMove> moves = piece.pieceMoves(this, position);

            for (ChessMove move : moves) {
                if (move.getEndPosition().equals(target)) {
                    return true; // square is attacked
                }
            }
        }
        return false;
    }

    // Get the positions of pieces on the board
    public Collection<ChessPosition> allTeamPosition(ChessGame.TeamColor color){
        Collection<ChessPosition> positions = new ArrayList<>();
        for (int row = 1; row <=8; row++){
            for (int col = 1; col <=8; col++){
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = getPiece(pos);
                if (piece != null && piece.getTeamColor() == color){
                    positions.add(pos);
                }

            }
        }
        return positions;
    }

    @Override
    public String toString() {
        StringBuilder grid = new StringBuilder(); // to help see the board
        for (int row = 8; row >= 1; row--) {
            grid.append(" ");
            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = getPiece(new ChessPosition(row, col));
                if (piece == null) {
                    grid.append(". ");
                } else {
                    grid.append(pieceSymb(piece)).append(" ");
                }
            }
            grid.append("\n");
        }
        return (grid.toString());
    }
    private char pieceSymb(ChessPiece piece){
        char symbol;
        switch (piece.getPieceType()){
            case KING -> symbol = 'K';
            case QUEEN -> symbol = 'Q';
            case BISHOP -> symbol = 'B';
            case KNIGHT -> symbol = 'N';
            case ROOK -> symbol = 'R';
            case PAWN -> symbol = 'P';
            default -> symbol = '?';
        }
        if (piece.getTeamColor() == ChessGame.TeamColor.BLACK){
            symbol = Character.toLowerCase(symbol);
        }
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessBoard that = (ChessBoard) o;
        return Objects.deepEquals(squares, that.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }
}
