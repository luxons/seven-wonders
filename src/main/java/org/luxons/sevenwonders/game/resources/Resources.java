package org.luxons.sevenwonders.game.resources;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

public class Resources {

    private final Map<ResourceType, Integer> quantities = new EnumMap<>(ResourceType.class);

    public void add(ResourceType type, int quantity) {
        quantities.merge(type, quantity, (x, y) -> x + y);
    }

    public void addAll(Resources resources) {
        resources.quantities.forEach(this::add);
    }

    public int getQuantity(ResourceType type) {
        return quantities.getOrDefault(type, 0);
    }

    public Map<ResourceType, Integer> getQuantities() {
        return quantities;
    }

    public boolean contains(Resources resources) {
        return resources.quantities.entrySet().stream().allMatch(this::hasAtLeast);
    }

    private boolean hasAtLeast(Entry<ResourceType, Integer> quantity) {
        return quantity.getValue() <= getQuantity(quantity.getKey());
    }

    public Resources minus(Resources resources) {
        Resources diff = new Resources();
        quantities.forEach((type, count) -> {
            int remainder = count - resources.getQuantity(type);
            diff.quantities.put(type, Math.max(0, remainder));
        });
        return diff;
    }

    public boolean isEmpty() {
        return quantities.values().stream().reduce(0, Integer::sum) == 0;
    }
}