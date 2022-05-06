
package net.immersive_fission.item;

import net.minecraftforge.registries.ObjectHolder;

import net.minecraft.item.Rarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.block.BlockState;

import net.immersive_fission.itemgroup.ImmersiveFissionTabItemGroup;
import net.immersive_fission.ImmersiveFissionModElements;

@ImmersiveFissionModElements.ModElement.Tag
public class LeadIngotItem extends ImmersiveFissionModElements.ModElement {
	@ObjectHolder("immersive_fission:lead_ingot")
	public static final Item block = null;

	public LeadIngotItem(ImmersiveFissionModElements instance) {
		super(instance, 18);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new ItemCustom());
	}

	public static class ItemCustom extends Item {
		public ItemCustom() {
			super(new Item.Properties().group(ImmersiveFissionTabItemGroup.tab).maxStackSize(64).rarity(Rarity.COMMON));
			setRegistryName("lead_ingot");
		}

		@Override
		public int getItemEnchantability() {
			return 0;
		}

		@Override
		public int getUseDuration(ItemStack itemstack) {
			return 0;
		}

		@Override
		public float getDestroySpeed(ItemStack par1ItemStack, BlockState par2Block) {
			return 1F;
		}
	}
}
