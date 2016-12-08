package org.luxons.sevenwonders.game.boards;

import java.util.ArrayList;
import java.util.List;

import org.luxons.sevenwonders.game.Settings;
import org.luxons.sevenwonders.game.cards.Card;
import org.luxons.sevenwonders.game.resources.Production;
import org.luxons.sevenwonders.game.wonders.Wonder;

public class Board {

    private final Wonder wonder;

    private final List<Card> playedCards = new ArrayList<>();

    private final Production production = new Production();

    private final Science science = new Science();

    private final TradingRules tradingRules;

    private int gold;

    private int wonderLevel;

    public Board(Wonder wonder, Settings settings) {
        this.wonder = wonder;
        this.wonderLevel = 0;
        this.gold = settings.getInitialGold();
        this.tradingRules = new TradingRules(settings.getDefaultTradingCost());
        production.addFixedResource(wonder.getInitialResource(), 1);
    }

    public Wonder getWonder() {
        return wonder;
    }

    public List<Card> getPlayedCards() {
        return playedCards;
    }

    public void addCard(Card card) {
        playedCards.add(card);
    }

    public Production getProduction() {
        return production;
    }

    public TradingRules getTradingRules() {
        return tradingRules;
    }

    public Science getScience() {
        return science;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int amount) {
        this.gold = amount;
    }

    public int getWonderLevel() {
        return wonderLevel;
    }

    public void upgradeWonderLevel() {
        int maxLevel = wonder.getLevels().size();
        if (maxLevel == wonderLevel) {
            throw new IllegalStateException("This wonder has already reached its maximum level");
        }
        this.wonderLevel++;
        wonder.getLevels().get(wonderLevel).getEffect().apply(this, null, null);
    }
}