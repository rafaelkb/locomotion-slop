package com.locomotionslop.animation.animator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import org.jetbrains.annotations.Nullable;

/**
 * Which of Trainguy's hand rigs an item should use.
 *
 * <p>Each entry names the sequences that exist for that rig; nulls mean "no animation for that
 * action", and the layer simply keeps holding the pose.</p>
 */
public enum ItemKind {

    EMPTY("hand/empty/pose", "hand/empty/raise", "hand/empty/lower", "hand/empty/attack", "hand/empty/mine_swing", "hand/empty/mine_finish"),

    SWORD("hand/tool/pose", "hand/tool/raise", "hand/tool/lower", "hand/tool/sword/attack", null, null),
    PICKAXE("hand/tool/pose", "hand/tool/raise", "hand/tool/lower", "hand/tool/pickaxe/attack", "hand/tool/pickaxe/mine_swing", "hand/tool/pickaxe/mine_finish"),
    AXE("hand/tool/pose", "hand/tool/raise", "hand/tool/lower", "hand/tool/axe/attack", "hand/tool/axe/mine_swing", null),
    SHOVEL("hand/tool/pose", "hand/tool/raise", "hand/tool/lower", null, "hand/tool/shovel/mine_swing", null),
    TOOL("hand/tool/pose", "hand/tool/raise", "hand/tool/lower", null, null, null),

    BLOCK("hand/generic_item/block_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),
    DOOR("hand/generic_item/door_block_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),
    ROD("hand/generic_item/rod_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),
    FISHING_ROD("hand/generic_item/fishing_rod_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),
    SHEARS("hand/generic_item/shears_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),
    ARROW("hand/generic_item/arrow_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),
    BANNER("hand/generic_item/banner_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),
    FLAT("hand/generic_item/2d_item_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),
    FIXED("hand/generic_item/fixed_item_pose", "hand/generic_item/raise", "hand/generic_item/lower", null, null, null),

    BOW("hand/bow/pose", null, null, null, null, null),
    CROSSBOW("hand/crossbow/pose", "hand/crossbow/raise", null, null, null, null),
    SHIELD("hand/shield/pose", null, null, null, null, null),
    TRIDENT("hand/trident/pose", null, null, "hand/trident/jab", null, null),
    SPYGLASS("hand/spyglass/pose", null, null, null, null, null),
    MAP("hand/map/single_hand_pose", null, null, null, null, null),
    MACE("hand/mace/pose", null, null, "hand/mace/attack", null, null),
    BRUSH("hand/brush/pose", null, null, null, null, null);

    public final ResourceLocation pose;
    @Nullable
    public final ResourceLocation raise;
    @Nullable
    public final ResourceLocation lower;
    @Nullable
    public final ResourceLocation attack;
    @Nullable
    public final ResourceLocation mine;
    @Nullable
    public final ResourceLocation mineFinish;

    ItemKind(String pose, @Nullable String raise, @Nullable String lower, @Nullable String attack, @Nullable String mine, @Nullable String mineFinish) {
        this.pose = FirstPersonJoints.sequence(pose);
        this.raise = raise == null ? null : FirstPersonJoints.sequence(raise);
        this.lower = lower == null ? null : FirstPersonJoints.sequence(lower);
        this.attack = attack == null ? null : FirstPersonJoints.sequence(attack);
        this.mine = mine == null ? null : FirstPersonJoints.sequence(mine);
        this.mineFinish = mineFinish == null ? null : FirstPersonJoints.sequence(mineFinish);
    }

    public static ItemKind of(ItemStack stack) {
        if (stack.isEmpty()) {
            return EMPTY;
        }
        Item item = stack.getItem();
        if (item instanceof SwordItem) {
            return SWORD;
        }
        if (item instanceof PickaxeItem) {
            return PICKAXE;
        }
        if (item instanceof AxeItem) {
            return AXE;
        }
        if (item instanceof ShovelItem) {
            return SHOVEL;
        }
        if (item instanceof BowItem) {
            return BOW;
        }
        if (item instanceof CrossbowItem) {
            return CROSSBOW;
        }
        if (item instanceof ShieldItem) {
            return SHIELD;
        }
        if (item instanceof TridentItem) {
            return TRIDENT;
        }
        if (item instanceof SpyglassItem) {
            return SPYGLASS;
        }
        if (item instanceof MaceItem) {
            return MACE;
        }
        if (item instanceof MapItem) {
            return MAP;
        }
        if (item instanceof BrushItem) {
            return BRUSH;
        }
        if (item instanceof ShearsItem) {
            return SHEARS;
        }
        if (item instanceof FishingRodItem) {
            return FISHING_ROD;
        }
        if (item instanceof ArrowItem) {
            return ARROW;
        }
        if (item instanceof BannerItem) {
            return BANNER;
        }
        // There is no DoorItem class in 1.21.1 - door items are plain block items, so the block is
        // what identifies them.
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof DoorBlock) {
            return DOOR;
        }
        if (item instanceof BlockItem) {
            return BLOCK;
        }
        if (stack.getUseAnimation() == UseAnim.DRINK) {
            return FLAT;
        }
        if (item.isEdible()) {
            return FLAT;
        }
        if (item instanceof TieredItem || item instanceof HoeItem) {
            return TOOL;
        }
        return FIXED;
    }
}
