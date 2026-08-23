package com.sagakenichi.job;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Inventory GUI for selling job materials for Job's built-in yen currency. */
final class JobSellMenu implements Listener {
    private static final int MAIN_SIZE = 27;
    private static final int CATEGORY_SIZE = 54;
    private static final int[] MAIN_CATEGORY_SLOTS = {10, 12, 14, 16, 22};
    private static final int[] ITEM_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    private final YenService yen;
    private final BeginnerService beginners;
    private final RateTable rateTable;
    private final SellCatalog catalog;

    JobSellMenu(YenService yen, BeginnerService beginners, RateTable rateTable, SellCatalog catalog) {
        this.yen = yen;
        this.beginners = beginners;
        this.rateTable = rateTable;
        this.catalog = catalog;
    }

    void open(Player player) {
        MainHolder holder = new MainHolder();
        Inventory inv = Bukkit.createInventory(holder, MAIN_SIZE, "Job - 売却所");
        holder.inventory = inv;
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(4, balanceItem(player));
        SellCatalog.Category[] categories = SellCatalog.Category.values();
        for (int i = 0; i < categories.length; i++) {
            SellCatalog.Category category = categories[i];
            int slot = MAIN_CATEGORY_SLOTS[i];
            holder.categories.put(slot, category);
            inv.setItem(slot, categoryItem(category));
        }
        player.openInventory(inv);
    }

    private void openCategory(Player player, SellCatalog.Category category) {
        CategoryHolder holder = new CategoryHolder(category);
        Inventory inv = Bukkit.createInventory(holder, CATEGORY_SIZE, "Job - " + category.displayName());
        holder.inventory = inv;
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        List<SellCatalog.Entry> entries = catalog.entries(category);
        for (int i = 0; i < entries.size() && i < ITEM_SLOTS.length; i++) {
            int slot = ITEM_SLOTS[i];
            SellCatalog.Entry entry = entries.get(i);
            holder.entries.put(slot, entry);
            inv.setItem(slot, saleItem(player, entry));
        }
        inv.setItem(45, named(Material.ARROW, ChatColor.YELLOW + "← ジャンル一覧へ", List.of()));
        inv.setItem(49, balanceItem(player));
        inv.setItem(53, named(Material.BARRIER, ChatColor.RED + "閉じる", List.of()));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize()) return;
        if (holder instanceof MainHolder main) {
            SellCatalog.Category category = main.categories.get(rawSlot);
            if (category != null) openCategory(player, category);
            return;
        }
        CategoryHolder categoryHolder = (CategoryHolder) holder;
        if (rawSlot == 45) { open(player); return; }
        if (rawSlot == 53) { player.closeInventory(); return; }
        SellCatalog.Entry entry = categoryHolder.entries.get(rawSlot);
        if (entry == null) return;
        sell(player, entry, event.isRightClick());
        openCategory(player, categoryHolder.category);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    private void sell(Player player, SellCatalog.Entry entry, boolean sellAll) {
        RateTable.Rate rate = rateTable.rate(entry.rule());
        int available = countPlain(player, entry.material());
        int units = available / rate.unitCount();
        if (!sellAll) units = Math.min(units, 1);
        if (units <= 0) {
            player.sendMessage(ChatColor.RED + entry.displayName() + " が足りません。" + ChatColor.GRAY + " 必要: " + rate.unitCount() + "個");
            return;
        }
        int multiplier = beginners.multiplier(player);
        long payout;
        try {
            payout = Math.multiplyExact(Math.multiplyExact((long) units, rate.yenPerUnit()), multiplier);
        } catch (ArithmeticException ex) {
            player.sendMessage(ChatColor.RED + "売却額が大きすぎるため処理できません。");
            return;
        }
        if (!yen.canDeposit(player, payout)) {
            player.sendMessage(ChatColor.RED + "円の残高が上限に達するため売却できません。");
            return;
        }
        int removeAmount = Math.multiplyExact(units, rate.unitCount());
        if (!removePlain(player, entry.material(), removeAmount)) {
            player.sendMessage(ChatColor.RED + "アイテム数が変化したため売却を中止しました。");
            return;
        }
        if (!yen.deposit(player, payout)) {
            player.getInventory().addItem(new ItemStack(entry.material(), removeAmount));
            player.sendMessage(ChatColor.RED + "売却代金を保存できなかったため取引を取り消しました。");
            return;
        }
        String bonus = multiplier > 1 ? ChatColor.GOLD + " [初心者×" + multiplier + "]" : "";
        player.sendMessage(ChatColor.GREEN + "[Job] " + ChatColor.WHITE + entry.displayName()
                + " x" + removeAmount + " を " + ChatColor.YELLOW + YenService.format(payout)
                + ChatColor.WHITE + " で売却しました。" + bonus);
    }

    private ItemStack saleItem(Player player, SellCatalog.Entry entry) {
        RateTable.Rate rate = rateTable.rate(entry.rule());
        int count = countPlain(player, entry.material());
        int multiplier = beginners.multiplier(player);
        long displayedPayout = (long) rate.yenPerUnit() * multiplier;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "販売単位: " + ChatColor.WHITE + rate.unitCount() + "個");
        lore.add(ChatColor.GRAY + "価格: " + ChatColor.YELLOW + YenService.format(displayedPayout));
        if (multiplier > 1) lore.add(ChatColor.GOLD + "初心者ボーナス ×" + multiplier + " 適用中");
        lore.add(ChatColor.GRAY + "所持: " + ChatColor.WHITE + count + "個");
        lore.add("");
        lore.add(ChatColor.GREEN + "左クリック: 1単位売却");
        lore.add(ChatColor.AQUA + "右クリック: 売れるだけ全て売却");
        int iconAmount = Math.max(1, Math.min(64, rate.unitCount()));
        ItemStack item = new ItemStack(entry.material(), iconAmount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + entry.displayName());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack categoryItem(SellCatalog.Category category) {
        return named(category.icon(), ChatColor.GOLD + category.displayName(), List.of(ChatColor.GRAY + "クリックして売却アイテムを見る"));
    }

    private ItemStack balanceItem(Player player) {
        return named(Material.GOLD_INGOT, ChatColor.GOLD + "所持金: " + YenService.format(yen.balance(player)), List.of(ChatColor.GRAY + "Job独自通貨。Vaultとは別の残高です。"));
    }

    private static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (!lore.isEmpty()) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void fill(Inventory inventory, Material material) {
        ItemStack pane = named(material, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, pane);
    }

    private static int countPlain(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) if (isPlain(stack, material)) total += stack.getAmount();
        return total;
    }

    private static boolean removePlain(Player player, Material material, int amount) {
        if (amount <= 0 || countPlain(player, material) < amount) return false;
        ItemStack[] storage = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < storage.length && remaining > 0; i++) {
            ItemStack stack = storage[i];
            if (!isPlain(stack, material)) continue;
            int take = Math.min(stack.getAmount(), remaining);
            remaining -= take;
            if (take == stack.getAmount()) storage[i] = null;
            else stack.setAmount(stack.getAmount() - take);
        }
        if (remaining != 0) return false;
        player.getInventory().setStorageContents(storage);
        return true;
    }

    private static boolean isPlain(ItemStack stack, Material material) {
        return stack != null && stack.getType() == material && !stack.hasItemMeta();
    }

    private interface MenuHolder extends InventoryHolder {}
    private static final class MainHolder implements MenuHolder {
        private final Map<Integer, SellCatalog.Category> categories = new HashMap<>();
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }
    private static final class CategoryHolder implements MenuHolder {
        private final SellCatalog.Category category;
        private final Map<Integer, SellCatalog.Entry> entries = new HashMap<>();
        private Inventory inventory;
        private CategoryHolder(SellCatalog.Category category) { this.category = category; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
