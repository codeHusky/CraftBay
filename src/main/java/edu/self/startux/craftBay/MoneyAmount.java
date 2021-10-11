/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Copyright 2012-2021 StarTux
 *
 * This file is part of CraftBay.
 *
 * CraftBay is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CraftBay is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CraftBay.  If not, see <http://www.gnu.org/licenses/>.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package edu.self.startux.craftBay;

import lombok.Value;
import net.kyori.adventure.text.Component;

@Value
public final class MoneyAmount implements Comparable<MoneyAmount> {
    public static MoneyAmount ZERO = new MoneyAmount(0.0);
    private final int amount;

    public MoneyAmount(final int amount) {
        this.amount = amount;
    }

    /**
     * This constructor is there mostly for support of legacy
     * auction.yml files where money wasexpressed solely as
     * Integer. The additional support for String and Object
     * is just a catch-call precaution. Use it sparingly.
     */
    public MoneyAmount(final Object o) {
        if (o instanceof Number) {
            amount = ((Number) o).intValue();
        } else if (o instanceof String) {
            int v = 0;
            try {
                v = Integer.parseInt((String) o);
            } catch (NumberFormatException nfe) { }
            amount = v;
        } else {
            int v = 0;
            try {
                v = Integer.parseInt(o.toString());
            } catch (NumberFormatException nfe) { }
            amount = v;
        }
    }

    public int getInt() {
        return amount;
    }

    @Override
    public int compareTo(MoneyAmount other) {
        return Integer.compare(amount, other.amount);
    }

    @Override
    public String toString() {
        try {
            return CraftBayPlugin.getInstance().getEco().format(amount);
        } catch (RuntimeException e) {
            return "" + amount;
        }
    }

    public Component toComponent() {
        return Component.text(toString());
    }
}
