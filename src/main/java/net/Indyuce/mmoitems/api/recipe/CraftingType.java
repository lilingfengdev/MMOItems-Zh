package net.Indyuce.mmoitems.api.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.version.VersionMaterial;
import net.Indyuce.mmoitems.MMOUtils;

public enum CraftingType {
	SHAPED(21, "The C. Table Recipe (Shaped) for this item", VersionMaterial.CRAFTING_TABLE),
	SHAPELESS(22, "The C. Table Recipe (Shapeless) for this item", VersionMaterial.CRAFTING_TABLE),
	FURNACE(23, "The Furnace Recipe for this item", Material.FURNACE),
	BLAST(29, "The Blast Furnace Recipe for this item", VersionMaterial.BLAST_FURNACE, 1, 14),
	SMOKER(30, "The Smoker Recipe for this item", VersionMaterial.SMOKER, 1, 14),
	CAMPFIRE(32, "The Campfire Recipe for this item", VersionMaterial.CAMPFIRE, 1, 14),
	SMITHING(33, "The Smithing Recipe for this item", VersionMaterial.SMITHING_TABLE, 1, 15);

	private final int slot;
	private final String lore;
	private final Material material;
	private final int[] mustBeHigher;

	private CraftingType(int slot, String lore, VersionMaterial material, int... mustBeHigher) {
		this(slot, lore, material.toMaterial(), mustBeHigher);
	}

	private CraftingType(int slot, String lore, Material material, int... mustBeHigher) {
		this.slot = slot;
		this.lore = lore;
		this.material = material;
		this.mustBeHigher = mustBeHigher;
	}

	public ItemStack getItem() {
		return new ItemStack(material);
	}

	public int getSlot() {
		return slot;
	}

	public String getName() {
		return MMOUtils.caseOnWords(name().toLowerCase());
	}

	public String getLore() {
		return lore;
	}

	public boolean shouldAdd() {
		return mustBeHigher.length == 0 || MythicLib.plugin.getVersion().isStrictlyHigher(mustBeHigher);
	}

	public static CraftingType getBySlot(int slot) {
		for (CraftingType type : values())
			if (type.getSlot() == slot)
				return type;
		return null;
	}
}