package net.Indyuce.mmoitems.stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.SupportedNBTTagValues;
import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackCategory;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackProvider;
import io.lumine.mythic.lib.api.util.ui.PlusMinusPercent;
import io.lumine.mythic.lib.api.util.ui.SilentNumbers;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.MMOUtils;
import net.Indyuce.mmoitems.api.edition.StatEdition;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.api.util.message.FFPMMOItems;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.data.EnchantListData;
import net.Indyuce.mmoitems.stat.data.random.RandomEnchantListData;
import net.Indyuce.mmoitems.stat.data.random.RandomStatData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.data.type.UpgradeInfo;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.StatHistory;
import net.Indyuce.mmoitems.stat.type.Upgradable;

public class Enchants extends ItemStat implements Upgradable {
	public Enchants() {
		super("ENCHANTS", Material.ENCHANTED_BOOK, "Enchantments", new String[] { "The item enchants." }, new String[] { "all" });
	}

	@Override
	public RandomStatData whenInitialized(Object object) {
		Validate.isTrue(object instanceof ConfigurationSection, "Must specify a config section");
		return new RandomEnchantListData((ConfigurationSection) object);
	}

	@Override
	public void whenClicked(@NotNull EditionInventory inv, @NotNull InventoryClickEvent event) {
		if (event.getAction() == InventoryAction.PICKUP_ALL)
			new StatEdition(inv, ItemStats.ENCHANTS).enable("Write in the chat the enchant you want to add.",
					ChatColor.AQUA + "Format: {Enchant Name} {Enchant Level Numeric Formula}");

		if (event.getAction() == InventoryAction.PICKUP_HALF) {
			if (inv.getEditedSection().contains("enchants")) {
				Set<String> set = inv.getEditedSection().getConfigurationSection("enchants").getKeys(false);
				String last = Arrays.asList(set.toArray(new String[0])).get(set.size() - 1);
				inv.getEditedSection().set("enchants." + last, null);
				if (set.size() <= 1)
					inv.getEditedSection().set("enchants", null);
				inv.registerTemplateEdition();
				inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Successfully removed " + last.substring(0, 1).toUpperCase()
						+ last.substring(1).toLowerCase().replace("_", " ") + ".");
			}
		}
	}

	/*
	 * getByName is deprecated, but it's safe to
	 * use and will make the user experience better
	 */
	@Override
	public void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info) {
		String[] split = message.split(" ");
		Validate.isTrue(split.length >= 2, "Use this format: {Enchant Name} {Enchant Level Numeric Formula}. Example: 'sharpness 5 0.3' "
				+ "stands for Sharpness 5, plus 0.3 level per item level (rounded up to lower integer)");

		Enchantment enchant = getEnchant(split[0]);
		Validate.notNull(enchant, split[0]
				+ " is not a valid enchantment! All enchants can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/enchantments/Enchantment.html");

		NumericStatFormula formula = new NumericStatFormula(message.substring(message.indexOf(" ") + 1));
		formula.fillConfigurationSection(inv.getEditedSection(), "enchants." + enchant.getKey().getKey());
		inv.registerTemplateEdition();
		inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + enchant.getKey().getKey() + " " + formula.toString() + " successfully added.");
	}

	@Override
	public void whenDisplayed(List<String> lore, Optional<RandomStatData> statData) {

		if (statData.isPresent()) {
			lore.add(ChatColor.GRAY + "Current Value:");
			RandomEnchantListData data = (RandomEnchantListData) statData.get();
			data.getEnchants().forEach(enchant -> lore.add(ChatColor.GRAY + "* " + MMOUtils.caseOnWords(enchant.getKey().getKey().replace("_", " "))
					+ " " + data.getLevel(enchant).toString()));

		} else
			lore.add(ChatColor.GRAY + "Current Value: " + ChatColor.RED + "None");

		lore.add("");
		lore.add(ChatColor.YELLOW + AltChar.listDash + " Click to add an enchant.");
		lore.add(ChatColor.YELLOW + AltChar.listDash + " Right click to remove the last enchant.");
	}

	@NotNull
	@Override
	public StatData getClearStatData() { return new EnchantListData(); }

	@Override
	public void whenLoaded(@NotNull ReadMMOItem mmoitem) {

		// Create enchant data from this items' enchantments
		EnchantListData enchants = new EnchantListData();

		// For each enchantment
		for (Enchantment enchant : mmoitem.getNBT().getItem().getItemMeta().getEnchants().keySet()) {

			// Add Level
			enchants.addEnchant(enchant, mmoitem.getNBT().getItem().getItemMeta().getEnchantLevel(enchant));
		}

		// Recognize the Stat Data
		mmoitem.setData(ItemStats.ENCHANTS, enchants);

		// Regardless, the stat history must be kept.
		ItemTag hisTag = ItemTag.getTagAtPath(ItemStackBuilder.histroy_keyword + getId(), mmoitem.getNBT(), SupportedNBTTagValues.STRING);
		if (hisTag == null) {

			if (// If either enchanting is allowed
				!mmoitem.getNBT().getBoolean(ItemStats.DISABLE_ENCHANTING.getNBTPath()) ||

				// Or repairing is allowed
				!mmoitem.getNBT().getBoolean(ItemStats.DISABLE_REPAIRING.getNBTPath()))  {

				/*
				 *   Capture the stat history of enchantments of this item.
				 *
				 *   If the item has no enchantments, it will record that as the
				 *   original value of the item, which can later be assumed to be
				 *   true to the template.
				 *
				 *   todo for a few months, we must not assume that the 'Original'
				 *    stat values are actually the original ones, they may be old
				 *    enchantments applied by players before StatHistory existed.
				 *
				 */
				StatHistory.from(mmoitem, ItemStats.ENCHANTS);
			}
		}
	}


	/**
	 * Since GemStones shall be removable, the enchantments must also be stored.
	 */
	@Nullable
	@Override
	public StatData getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags) {

		// Find tag
		ItemTag enchantTag = ItemTag.getTagAtPath(getNBTPath(), storedTags);

		// Found?
		if (enchantTag != null) {

			// Must be thay string list shit
			ArrayList<String> enchants = ItemTag.getStringListFromTag(enchantTag);

			// New
			EnchantListData data = new EnchantListData();

			// Examine each
			for (String str : enchants) {

				// Split
				String[] split = str.split(" ");
				if (split.length >= 2) {

					// Find
					String enchantment = split[0];
					String level = split[1];

					// Get Namespaced
					Enchantment ench = null;
					try { ench = getEnchant(enchantment); } catch (Exception ignored) {}

					// Parse Integer
					Integer lvl = SilentNumbers.IntegerParse(level);

					// Worked?
					if (ench != null && lvl != null) {

						// Add
						data.addEnchant(ench, lvl);
					}
				}
			}

			// Thats it
			return data;
		}

		return null;
	}

	@Override
	public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StatData data) {

		// Enchant Item
		EnchantListData enchants = (EnchantListData) data;
		for (Enchantment enchant : enchants.getEnchants()) {

			// Get level
			int lvl = enchants.getLevel(enchant);

			// Not ZERO bruh
			if (lvl != 0) { item.getMeta().addEnchant(enchant, enchants.getLevel(enchant), true); }
		}

		// Apply tags
		item.addItemTag(getAppliedNBT(data));
	}

	/**
	 * Since GemStones shall be removable, the enchantments must also be stored.
	 */
	@NotNull
	@Override
	public ArrayList<ItemTag> getAppliedNBT(@NotNull StatData data) {
		ArrayList<ItemTag> ret = new ArrayList<>();

		// Add enchantment pair data
		ArrayList<String> enchantments = new ArrayList<>();
		for (Enchantment enchantment : ((EnchantListData) data).getEnchants()) {
			enchantments.add(enchantment.getKey().getKey() + " " + ((EnchantListData) data).getLevel(enchantment)); }

		// Add that one tag
		ret.add(ItemTag.fromStringList(getNBTPath(), enchantments));

		return ret;
	}

	@NotNull
	@Override
	public UpgradeInfo loadUpgradeInfo(@Nullable Object obj) throws IllegalArgumentException {
		//UPGRD//MMOItems. Log("\u00a7a  --> \u00a77Loading Enchants");
		return EnchantUpgradeInfo.GetFrom(obj);
	}

	@NotNull
	@Override
	public StatData apply(@NotNull StatData original, @NotNull UpgradeInfo info, int level) {
		//UPGRD//MMOItems. Log("\u00a7a  --> \u00a77Applying Enchants Upgrade");

		// Must be DoubleData
		if (original instanceof EnchantListData && info instanceof EnchantUpgradeInfo) {
			//UPGRD//MMOItems. Log("\u00a7a   -> \u00a77Valid Instances");

			// Get value
			EnchantListData dataEnchants = ((EnchantListData) original);
			EnchantUpgradeInfo eui = (EnchantUpgradeInfo) info;

			// For every enchantment
			for (Enchantment e : eui.getAffectedEnchantments()) {
				int lSimulation = level;
				//UPGRD//MMOItems. Log("\u00a7b    >\u00a79> \u00a77Enchantment \u00a7f" + e.getName());

				// Get current level
				double value = dataEnchants.getLevel(e);
				//UPGRD//MMOItems. Log("\u00a7b      -> \u00a77Original Level \u00a7f" + value);
				PlusMinusPercent pmp = eui.getPMP(e);
				if (pmp == null) { continue; }
				//UPGRD//MMOItems. Log("\u00a7b      -> \u00a77Operation \u00a7f" + pmp.toString());

				// If leveling up
				if (lSimulation > 0) {

					// While still positive
					while (lSimulation > 0) {

						// Apply PMP Operation Positively
						//UPGRD//MMOItems. Log("\u00a7c       -> \u00a77Preop \u00a7f" + value);
						//UPGRD//MMOItems. Log("\u00a78       -> Operation \u00a77" + pmp.toString());
						value = pmp.apply(value);
						//UPGRD//MMOItems. Log("\u00a76       -> \u00a77Postop \u00a7f" + value);
						//UPGRD//MMOItems. Log("\u00a77       -> \u00a77------------------");

						// Decrease
						lSimulation--;
					}

					// Degrading the item
				} else if (lSimulation < 0) {

					// While still negative
					while (lSimulation < 0) {

						// Apply PMP Operation Reversibly
						//UPGRD//MMOItems. Log("\u00a73       -> \u00a77Preop \u00a7f" + value);
						value = pmp.reverse(value);
						//UPGRD//MMOItems. Log("\u00a7d       -> \u00a77Postop \u00a7f" + value);
						//UPGRD//MMOItems. Log("\u00a77       -> \u00a77------------------");

						// Decrease
						lSimulation++;
					}
				}

				// Update
				//UPGRD//MMOItems. Log("\u00a7b      -> \u00a77Final level \u00a7f" + value);
				dataEnchants.addEnchant(e, SilentNumbers.floor(value));
			}

			// Yes
			return dataEnchants;
		}

		// Upgraded
		return original;
	}

	/**
	 * Players may enchant an item via 'vanilla' means (Not Gemstones, not Upgrades).
	 * If they do so, the enchantments of their item will be wiped if they try to put
	 * a gemstone in or upgrade the item, which is not nice.
	 * <p></p>
	 * This operation classifies these discrepancies in the enchantment levels as
	 * 'externals' in the Stat History of enchantments. From then on, they will be
	 * accounted for.
	 */
	public static void separateEnchantments(@NotNull MMOItem mmoitem) {

		// Cancellation because the player could not have done so HMMM
		if (mmoitem.hasData(ItemStats.DISABLE_REPAIRING) && mmoitem.hasData(ItemStats.DISABLE_ENCHANTING)) { return; }
		boolean additiveMerge = MMOItems.plugin.getConfig().getBoolean("stat-merging.additive-enchantments", false);
		//SENCH//MMOItems.Log(" \u00a79>\u00a73>\u00a77 Separating Enchantments");

		// Does it have enchantment data?
		if (mmoitem.hasData(ItemStats.ENCHANTS)) {

			// Get that data
			EnchantListData data = (EnchantListData) mmoitem.getData(ItemStats.ENCHANTS);
			StatHistory hist = StatHistory.from(mmoitem, ItemStats.ENCHANTS);

			//SENCH//MMOItems.Log(" \u00a7b:\u00a73:\u00a79: \u00a77Early Analysis: \u00a73o-o-o-o-o-o-o-o-o-o-o-o-o-o-o-o");
			//SENCH//MMOItems.Log("  \u00a73> \u00a77History:");
			//SENCH//MMOItems.Log("  \u00a73=\u00a7b> \u00a77Original:");
			//SENCH//for (Enchantment e : ((EnchantListData) hist.getOriginalData()).getEnchants()) { MMOItems.Log("  \u00a7b * \u00a77" + e.getName() + " \u00a7f" + ((EnchantListData) hist.getOriginalData()).getLevel(e)); }
			//SENCH//MMOItems.Log("  \u00a73=\u00a7b> \u00a77Stones:");
			//SENCH//for (UUID date : hist.getAllGemstones()) { MMOItems.Log("  \u00a7b==\u00a73> \u00a77" + date.toString()); for (Enchantment e : ((EnchantListData) hist.getGemstoneData(date)).getEnchants()) { MMOItems.Log("  \u00a7b    *\u00a73* \u00a77" + e.getName() + " \u00a7f" + ((EnchantListData) hist.getGemstoneData(date)).getLevel(e)); } }
			//SENCH//MMOItems.Log("  \u00a73=\u00a7b> \u00a77Externals:");
			//SENCH//for (StatData date : hist.getExternalData()) { MMOItems.Log("  \u00a7b==\u00a73> \u00a77 --------- "); for (Enchantment e : ((EnchantListData) date).getEnchants()) { MMOItems.Log("  \u00a7b    *\u00a73* \u00a77" + e.getName() + " \u00a7f" + ((EnchantListData) date).getLevel(e)); } }

			// All right, whats the expected enchantment levels?
			//HSY//MMOItems.log(" \u00a73-\u00a7a- \u00a77Enchantment Separation Recalculation \u00a73-\u00a7a-\u00a73-\u00a7a-\u00a73-\u00a7a-\u00a73-\u00a7a-");
			EnchantListData expected = (EnchantListData) hist.recalculate(mmoitem.getUpgradeLevel());

			// Gather a list of extraneous enchantments
			HashMap<Enchantment, Integer> discrepancies = new HashMap<>();

			// For every enchantment
			for (Enchantment e : Enchantment.values()) {

				// Get both
				int actual = data.getLevel(e);
				int ideal = expected.getLevel(e);

				// Intuitive additive merge
				if (additiveMerge) {

					// Get difference, and register.
					int offset = actual - ideal;
					if (offset != 0) { 
						discrepancies.put(e, offset);
						//SENCH//MMOItems.Log("\u00a77 Act \u00a7f" + actual + "\u00a77 Ide \u00a73" + ideal + "\u00a77 Off \u00a79" + offset + " \u00a77 -- of \u00a7b" + e.getName() ); 
					}

				// Weird maximum enchantment
				} else {

					// We can only know that, if the actual is greater than the ideal, there is a greater external.
					if (actual > ideal) { 
						discrepancies.put(e, actual);
						//SENCH//MMOItems.Log("\u00a77 Act \u00a7f" + actual + "\u00a77 Ide \u00a73" + ideal + " \u00a77 -- of \u00a7b" + e.getName() );
					}
				}
			}

			// It has been extracted
			if (discrepancies.size() > 0) {
				// Generate enchantment list with offsets
				EnchantListData extraneous = new EnchantListData();
				for (Enchantment e : discrepancies.keySet()) { extraneous.addEnchant(e, discrepancies.get(e));}

				// Register extraneous
				hist.registerExternalData(extraneous);
			}

			//SENCH//MMOItems.Log(" \u00a7b:\u00a73:\u00a79: \u00a77Results \u00a79o-o-o-o-o-o-o-o-o-o-o-o-o-o-o-o");
			//SENCH//MMOItems.Log("  \u00a73> \u00a77History:");
			//SENCH//MMOItems.Log("  \u00a73=\u00a7b> \u00a77Original:");
			//SENCH//for (Enchantment e : ((EnchantListData) hist.getOriginalData()).getEnchants()) { MMOItems.Log("  \u00a7b * \u00a77" + e.getName() + " \u00a7f" + ((EnchantListData) hist.getOriginalData()).getLevel(e)); }
			//SENCH//MMOItems.Log("  \u00a73=\u00a7b> \u00a77Stones:");
			//SENCH//for (UUID date : hist.getAllGemstones()) { MMOItems.Log("  \u00a7b==\u00a73> \u00a77" + date.toString()); for (Enchantment e : ((EnchantListData) hist.getGemstoneData(date)).getEnchants()) { MMOItems.Log("  \u00a7b    *\u00a73* \u00a77" + e.getName() + " \u00a7f" + ((EnchantListData) hist.getGemstoneData(date)).getLevel(e)); } }
			//SENCH//MMOItems.Log("  \u00a73=\u00a7b> \u00a77Externals:");
			//SENCH//for (StatData date : hist.getExternalData()) { MMOItems.Log("  \u00a7b==\u00a73> \u00a77 --------- "); for (Enchantment e : ((EnchantListData) date).getEnchants()) { MMOItems.Log("  \u00a7b    *\u00a73* \u00a77" + e.getName() + " \u00a7f" + ((EnchantListData) date).getLevel(e)); } }

		}
	}

	@SuppressWarnings("deprecation")
	public static Enchantment getEnchant(String key) {
		key = key.toLowerCase().replace("-", "_");
		Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(key));

		if (enchant == null) {
			if (MMOItems.plugin.getMythicEnchantsSupport() != null) {
//				Object enchants = MythicEnchants.inst();
//				enchant = Enchantment.getByKey(new NamespacedKey((Plugin) MythicEnchants.inst(), key));
//				if (enchant != null)
//					return enchant;
			}
			enchant = Enchantment.getByName(key);
		}

		return enchant;
	}

	public static class EnchantUpgradeInfo implements UpgradeInfo {
		@NotNull HashMap<Enchantment, PlusMinusPercent> perEnchantmentOperations = new HashMap<>();

		/**
		 * Generate a <code>DoubleUpgradeInfo</code> from this <code><b>String List</b></code>
		 * that represents several pairs of {@link Enchantment}-{@link PlusMinusPercent}.
		 * <p></p>
		 * To keep older MMOItems versions working the same way, instead of having no prefix
		 * to use the <i>set</i> function of the PMP, one must use an <b><code>s</code></b> prefix.
		 * @param obj A <code><u>String List</u></code> in the format:
		 *            <p><code>Enchantment PMP</code>
		 *            </p><code>Enchantment PMP</code>
		 *            <p><code>Enchantment PMP</code>
		 *            </p><code>...</code>
		 * @throws IllegalArgumentException If any part of the operation goes wrong (including reading any PMP).
		 */
		@NotNull public static EnchantUpgradeInfo GetFrom(@Nullable Object obj) throws IllegalArgumentException {

			// Shall not be null
			Validate.notNull(obj, FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), "Upgrade operation list must not be null"));

			// Does the string exist?
			if (!(obj instanceof List)) {

				// Throw exception
				throw new IllegalArgumentException(
						FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), "Expected a list of strings instead of $i{0}", obj.toString()));
			}

			ArrayList<String> strlst = new ArrayList<>(); boolean failure = false;
			StringBuilder unreadableStatements = new StringBuilder();
			for (Object entry : (List) obj) {

				// Only strings
				if (entry instanceof String) {
					strlst.add((String) entry);

				} else {

					// No
					failure = true;

					// Append info
					unreadableStatements.append(FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), " Invalid list entry $i{0}$b;", obj.toString()));
				}
			}
			if (failure) {
				// Throw exception
				throw new IllegalArgumentException(
						FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), "Could not read enchantment list:") + unreadableStatements.toString());
			}

			// No empty lists
			if (strlst.isEmpty()) {
				throw new IllegalArgumentException(
						FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), "Upgrade operation list is empty"));
			}

			// Create ret
			EnchantUpgradeInfo eui = new EnchantUpgradeInfo();

			for (String str : strlst) {
				//UPGRD//MMOItems. Log("\u00a7e  --> \u00a77Entry \u00a76" + str);
				String[] split = str.split(" ");

				// At least two
				if (split.length >= 2) {

					// Get
					String enchStr = split[0];
					String pmpStr = split[1];

					// Adapt to PMP format
					char c = pmpStr.charAt(0); if (c == 's') { pmpStr = pmpStr.substring(1); } else if (c != '+' && c != '-' && c != 'n') { pmpStr = '+' + pmpStr; }

					// Is it a valid plus minus percent?
					FriendlyFeedbackProvider ffp = new FriendlyFeedbackProvider(FFPMMOItems.get());
					PlusMinusPercent pmpRead = PlusMinusPercent.getFromString(pmpStr, ffp);

					Enchantment ench = null;
					try { ench = getEnchant(enchStr); } catch (Exception ignored) {}

					// L
					if (pmpRead == null) { unreadableStatements.append(' ').append(ffp.getFeedbackOf(FriendlyFeedbackCategory.ERROR).get(0).forConsole(ffp.getPalette())); failure = true; }
					if (ench == null) { unreadableStatements.append(FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), " Invalid Enchantment $i{0}$b.", enchStr)); failure = true; }

					// Valid? add
					if (pmpRead != null && ench != null) {
						//UPGRD//MMOItems. Log("\u00a7a   s-> \u00a77Added");
						eui.addEnchantmentOperation(ench, pmpRead);
					}

				// Not enough arguments to be read
				} else {

					// Nope
					failure = true;
					unreadableStatements.append(FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), " Invalid list entry $i{0}$b. List entries are of the format 'esharpness +1$b'.", str));
				}

			}
			if (failure) {
				// Throw exception
				throw new IllegalArgumentException(
						FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), "Could not read enchantment list:") + unreadableStatements.toString());
			}

			// Success
			return eui;
		}

		public EnchantUpgradeInfo() { }

		/**
		 * The operation every level will perform.
		 * @see PlusMinusPercent
		 */
		@Nullable public PlusMinusPercent getPMP(@NotNull Enchantment ench) { return perEnchantmentOperations.get(ench); }

		/**
		 * Includes an enchantment to be upgraded by this template.
		 */
		public void addEnchantmentOperation(@NotNull Enchantment e, @NotNull PlusMinusPercent op) { perEnchantmentOperations.put(e, op); }

		/**
		 * Which enchantments have operations defined here?
		 */
		@NotNull public Set<Enchantment> getAffectedEnchantments() { return perEnchantmentOperations.keySet(); }
	}
}
