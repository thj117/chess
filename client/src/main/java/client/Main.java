package client;

import chess.*;

public class Main {
    public static void main(String[] args) throws Exception {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        ChessClient client = new ChessClient(8080);
        client.run();

    }
}