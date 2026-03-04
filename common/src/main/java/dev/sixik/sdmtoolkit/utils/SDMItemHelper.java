package dev.sixik.sdmtoolkit.utils;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class SDMItemHelper {

    public static int countItem(Container container, ItemStack itemStack, boolean strictNbt, boolean ignoreDamage) {
        return countItemByPredicate(container, stack -> matches(itemStack, stack, strictNbt, ignoreDamage));
    }

    public static int countItem(Container container, TagKey<Item> tagKey) {
        return countItemByPredicate(container, stack -> stack.is(tagKey));
    }

    public static int countItemByPredicate(Container container, Predicate<ItemStack> predicate) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotItem = container.getItem(i);
            if (!slotItem.isEmpty() && predicate.test(slotItem)) {
                count += slotItem.getCount();
            }
        }
        return count;
    }


    public static boolean shrinkItem(Container container, ItemStack itemStack, int amount, boolean strictNbt, boolean ignoreDamage) {
        return shrinkItemByPredicate(container, stack -> matches(itemStack, stack, strictNbt, ignoreDamage), amount);
    }

    public static boolean shrinkItemByPredicate(Container container, Predicate<ItemStack> predicate, int amount) {
        if (amount <= 0) return true;

        if (countItemByPredicate(container, predicate) < amount) {
            return false;
        }

        int remainingToRemove = amount;

        for (int i = 0; i < container.getContainerSize(); i++) {
            if (remainingToRemove <= 0) break;

            ItemStack slotItem = container.getItem(i);
            if (slotItem.isEmpty() || !predicate.test(slotItem)) continue;

            int count = slotItem.getCount();

            if (count <= remainingToRemove) {
                remainingToRemove -= count;
                container.setItem(i, ItemStack.EMPTY);
            } else {
                slotItem.shrink(remainingToRemove);
                remainingToRemove = 0;
                container.setChanged();
            }
        }
        return true;
    }


    /**
     * Выдает предметы игроку. Если не влезло - дропает под ноги.
     */
    public static boolean giveItems(Player player, ItemStack itemStack, long amount) {
        if (itemStack.isEmpty() || amount <= 0) return false;

        long remaining = distributeItems(player.getInventory(), itemStack, amount);

        if (remaining > 0) {
            // Дропаем остаток в мир
            player.drop(itemStack.copyWithCount((int) remaining), false);
        }
        return true;
    }

    /**
     * Выдает предметы в контейнер.
     * @return true, если ВСЕ предметы поместились. false, если часть не влезла (но влезшая часть добавлена).
     */
    public static boolean giveItems(Container container, ItemStack itemStack, long amount) {
        if (itemStack.isEmpty() || amount <= 0) return false;
        long remaining = distributeItems(container, itemStack, amount);
        return remaining == 0;
    }

    private static long distributeItems(Container container, ItemStack prototype, long amountToGive) {
        long left = amountToGive;
        int maxStack = prototype.getMaxStackSize();

        ItemStack checkStack = prototype.copy();

        // Пытаемся стакнуть с существующими
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (left <= 0) break;

            ItemStack slotItem = container.getItem(i);


            /*
                Используем strictNbt = true, ignoreDamage = false для стаканья
                так как мы не можем стакать поврежденные предметы с целыми в одну кучу
             */
            if (!slotItem.isEmpty() && matches(checkStack, slotItem, true, false)) {
                int space = maxStack - slotItem.getCount();
                if (space > 0) {
                    int toAdd = (int) Math.min(space, left);
                    slotItem.grow(toAdd);
                    left -= toAdd;
                    container.setChanged();
                }
            }
        }

        // Заполняем пустые слоты
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (left <= 0) break;

            ItemStack slotItem = container.getItem(i);
            if (slotItem.isEmpty() && container.canPlaceItem(i, prototype)) { // Проверка canPlaceItem
                int toAdd = (int) Math.min(maxStack, left);
                container.setItem(i, prototype.copyWithCount(toAdd));
                left -= toAdd;
                container.setChanged();
            }
        }

        return left;
    }

    public static boolean matches(ItemStack shopItem, ItemStack targetItem, boolean strictNbt, boolean ignoreDamage) {
        if (shopItem.isEmpty() || targetItem.isEmpty()) return false;
        if (!shopItem.is(targetItem.getItem())) return false;

        if (!strictNbt) return true;

        /*
            Если NBT не важен, или его нет у обоих - всё ок.
         */
        boolean shopHasTag = shopItem.hasTag();
        boolean targetHasTag = targetItem.hasTag();

        if (!shopHasTag && !targetHasTag) return true;

        /*
            Если в магазине предмет без NBT (например, просто меч),
            мы не должны принимать меч с NBT (зачарованный), если strictNbt = true.
            Но если ignoreDamage=true, то наличие тега Damage у игрока допустимо.
         */
        if (!shopHasTag) {
            if (ignoreDamage && targetItem.isDamageableItem()) {

                // У игрока есть тег, но это ТОЛЬКО Damage?
                CompoundTag targetTag = targetItem.getTag();
                return isTagEmptyExceptDamage(targetTag);
            }
            return false; // У магазина нет тега, у игрока есть (и это не просто damage)
        }

        // Если у магазина есть тег
        if (!targetHasTag) return false;

        CompoundTag shopTag = shopItem.getTag();
        CompoundTag targetTag = targetItem.getTag();

        if (ignoreDamage && shopItem.isDamageableItem()) {
            // Сложная проверка: сравниваем теги, игнорируя ключ "Damage"
            return areTagsEqualIgnoringDamage(shopTag, targetTag);
        }

        return shopTag.equals(targetTag);
    }

    /**
     * Проверяет, пуст ли тег, если убрать из него Damage
     */
    private static boolean isTagEmptyExceptDamage(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return true;
        if (tag.size() == 1 && tag.contains("Damage")) return true;
        return false;
    }

    /**
     * Сравнивает два NBT, игнорируя прочность
     */
    private static boolean areTagsEqualIgnoringDamage(CompoundTag tag1, CompoundTag tag2) {
        if (tag1 == tag2) return true;
        if (tag1 == null || tag2 == null) return false;

        // Быстрая проверка: если они равны с учетом Damage, то и без него равны
        if (tag1.equals(tag2)) return true;

        // Медленная проверка: копируем и чистим (самый надежный способ)
        CompoundTag c1 = tag1.copy();
        CompoundTag c2 = tag2.copy();
        c1.remove("Damage");
        c2.remove("Damage");

        return c1.equals(c2);
    }

    public static boolean isSearch(String search, ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        String lowerSearch = search.toLowerCase();

        if (itemStack.getHoverName().getString().toLowerCase().contains(lowerSearch)) return true;

        String registryName = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        return registryName.contains(lowerSearch.replace(" ", "_"));
    }

    public static boolean isSearch(String search, HolderSet.Named<Item> tag) {
        if (tag == null) return false;
        return tag.stream().anyMatch(holder -> isSearch(search, holder.value().getDefaultInstance()));
    }
}
