package net.Indyuce.mmoitems.stat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.GemStoneStat;

public class ItemDamage extends DoubleStat implements GemStoneStat {
	public ItemDamage() {
		super("DURABILITY", new ItemStack(Material.FISHING_ROD), "Item Damage",
				new String[] { "Default item damage. This does &cNOT", "impact the item's max durability." }, new String[] { "!block", "all" });
	}

	@Override
	public void whenApplied(ItemStackBuilder item, StatData data) {
		if (item.getMeta() instanceof Damageable)
			((Damageable) item.getMeta()).setDamage((int) ((DoubleData) data).getValue());
	}

	@Override
	public void whenLoaded(ReadMMOItem mmoitem) {
		if (mmoitem.getNBT().getItem().getItemMeta() instanceof Damageable)
			mmoitem.setData(ItemStat.DURABILITY, new DoubleData(((Damageable) mmoitem.getNBT().getItem().getItemMeta()).getDamage()));
	}
}
