package org.luxons.sevenwonders.game.effects;

import java.util.List;

import org.luxons.sevenwonders.game.boards.Board;
import org.luxons.sevenwonders.game.RelativePlayerPosition;
import org.luxons.sevenwonders.game.cards.Color;

public class BonusPerBoardElement implements Effect {

    private List<RelativePlayerPosition> boards;

    private int gold;

    private int points;

    private BoardElementType type;

    // only relevant if type=CARD
    private List<Color> colors;

    public List<RelativePlayerPosition> getBoards() {
        return boards;
    }

    public void setBoards(List<RelativePlayerPosition> boards) {
        this.boards = boards;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public BoardElementType getType() {
        return type;
    }

    public void setType(BoardElementType type) {
        this.type = type;
    }

    public List<Color> getColors() {
        return colors;
    }

    public void setColors(List<Color> colors) {
        this.colors = colors;
    }

    @Override
    public void apply(Board board, Board leftNeighbourBoard, Board rightNeighbourBoard) {
        int goldGain = gold * computeNbOfMatchingElementsIn(board, leftNeighbourBoard, rightNeighbourBoard);
        board.setGold(board.getGold() + goldGain);
    }

    @Override
    public int computePoints(Board board, Board leftNeighbourBoard, Board rightNeighbourBoard) {
        return points * computeNbOfMatchingElementsIn(board, leftNeighbourBoard, rightNeighbourBoard);
    }

    private int computeNbOfMatchingElementsIn(Board board, Board leftNeighbourBoard, Board rightNeighbourBoard) {
        int totalCount = 0;
        if (boards.contains(RelativePlayerPosition.SELF)) {
            totalCount += computeNbOfMatchingElementsIn(board);
        }
        if (boards.contains(RelativePlayerPosition.LEFT_PLAYER)) {
            totalCount += computeNbOfMatchingElementsIn(leftNeighbourBoard);
        }
        if (boards.contains(RelativePlayerPosition.RIGHT_PLAYER)) {
            totalCount += computeNbOfMatchingElementsIn(rightNeighbourBoard);
        }
        return totalCount;
    }

    private int computeNbOfMatchingElementsIn(Board board) {
        switch (type) {
            case CARD:
                return board.getNbCardsOfColor(colors);
            case WONDER_LEVEL:
                return board.getWonderLevel();
            case DEFEAT_TOKEN:
                return board.getNbDefeatTokens();
        }
        throw new UnsupportedBoardElementType();
    }

    private class UnsupportedBoardElementType extends RuntimeException {
    }
}