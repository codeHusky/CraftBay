/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Copyright 2012 StarTux
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

import edu.self.startux.craftBay.event.AuctionCreateEvent;
import edu.self.startux.craftBay.event.AuctionEndEvent;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class AuctionHouse implements Listener {
        private CraftBayPlugin plugin;
        private Map<String, Date> lastAuctions = new HashMap<String, Date>();

        public AuctionHouse(CraftBayPlugin plugin) {
                this.plugin = plugin;
        }

        public void enable() {
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }

        public boolean checkCooldown(Merchant merchant) {
                int cooldown = plugin.getConfig().getInt("auctioneercooldown");
                if (cooldown == 0) return true;
                if (merchant instanceof BankMerchant) return true;
                Date date = lastAuctions.get(merchant.getName());
                if (date == null) return true;
                if (System.currentTimeMillis() - date.getTime() < cooldown * 1000) return false;
                return true;
        }

        public int getCooldown(Merchant merchant) {
                int cooldown = plugin.getConfig().getInt("auctioneercooldown");
                if (cooldown == 0) return 0;
                if (merchant instanceof BankMerchant) return 0;
                Date date = lastAuctions.get(merchant.getName());
                if (date == null) return 0;
                int delay = (int)((System.currentTimeMillis() - date.getTime()) / 1000l);
                int remain = cooldown - delay;
                if (remain <= 0) return 0;
                return remain;
        }

        public void touchCooldown(Merchant merchant) {
                if (merchant instanceof BankMerchant) return;
                lastAuctions.put(merchant.getName(), new Date());
        }

        public Auction createAuction(Merchant owner, Item item, int startingBid, boolean takeItems) {
                // check
                if (!checkCooldown(owner)) {
                        owner.warn(plugin.getMessage("auction.create.OwnerCooldown").set(owner).set("cooldown", new AuctionTime(getCooldown(owner))));
                        return null;
                }
                if (!plugin.getAuctionScheduler().canQueue()) {
                        owner.warn(plugin.getMessage("auction.create.QueueFull").set(owner));
                        return null;
                }
                if (takeItems && !item.has(owner)) {
                        owner.warn(plugin.getMessage("auction.create.NotEnoughItems").set(item).set(owner));
                        return null;
                }
                int fee = plugin.getConfig().getInt("auctionfee");
                if (startingBid > plugin.getConfig().getInt("startingbid")) {
                        int tax = (plugin.getConfig().getInt("auctiontax") * (startingBid - plugin.getConfig().getInt("startingbid"))) / 100;
                        fee += tax;
                }
                if (fee > 0) {
                        if (!owner.hasAmount(fee)) {
                                owner.warn(plugin.getMessage("auction.create.FeeTooHigh").set(owner).set("fee", new MoneyAmount(fee)));
                                return null;
                        }
                } else fee = 0;
                // take
                if (fee > 0) {
                        owner.takeAmount(fee);
                        owner.msg(plugin.getMessage("auction.create.FeeDebited").set(owner).set("fee", new MoneyAmount(fee)));
                }
                if (takeItems) {
                        item = item.take(owner);
                }
                touchCooldown(owner);
                // create
                Auction auction = new TimedAuction(plugin, owner, item);
                auction.setState(AuctionState.QUEUED);
                if (startingBid != 0) auction.setStartingBid(startingBid);
                auction.setFee(fee);
                plugin.getAuctionScheduler().queueAuction(auction);
                plugin.getServer().getPluginManager().callEvent(new AuctionCreateEvent(auction));
                return auction;
        }

        public Auction createAuction(Merchant owner, Item item, int startingBid) {
                return createAuction(owner, item, startingBid, true);
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlayerJoin(PlayerJoinEvent event) {
                ItemDelivery.deliverAll();
        }

        public void endAuction(Auction auction) {
                AuctionEndEvent event = new AuctionEndEvent(auction);
		if (auction.getWinner() == null) {
                        plugin.getServer().getPluginManager().callEvent(event);
                        ItemDelivery.schedule(auction.getOwner(), auction.getItem(), auction);
                } else if (!auction.getWinner().hasAmount(auction.getWinningBid())) {
                        event.setPaymentError(true);
                        plugin.getServer().getPluginManager().callEvent(event);
                        ItemDelivery.schedule(auction.getOwner(), auction.getItem(), auction);
                } else {
                        plugin.getServer().getPluginManager().callEvent(event);
                        auction.getWinner().takeAmount(auction.getWinningBid());
                        auction.getOwner().giveAmount(auction.getWinningBid());
                        ItemDelivery.schedule(auction.getWinner(), auction.getItem(), auction);
                }
        }

        public void cancelAuction(Auction auction) {
                ItemDelivery.schedule(auction.getOwner(), auction.getItem(), auction);
                if (auction.getFee() > 0) {
                        auction.getOwner().giveAmount(auction.getFee());
                        auction.getOwner().msg(plugin.getMessage("auction.cancel.FeeReturn").set(auction));
                }
        }
}
