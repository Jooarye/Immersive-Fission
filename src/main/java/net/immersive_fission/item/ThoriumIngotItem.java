
package net.immersive_fission.item;

import net.minecraftforge.registries.ObjectHolder;

import net.minecraft.item.Rarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.block.BlockState;

import net.immersive_fission.itemgroup.ImmersiveFissionTabItemGroup;
import net.immersive_fission.ImmersiveFissionModElements;

@ImmersiveFissionModElements.ModElement.Tag
public class ThoriumIngotItem extends ImmersiveFissionModElements.ModElement {
	@ObjectHolder("immersive_fission:thorium_ingot")
	public static final Item block = null;

	public ThoriumIngotItem(ImmersiveFissionModElements instance) {
		super(instance, 7);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new ItemCustom());
	}

	public static class ItemCustom extends Item {
		public ItemCustom() {
			super(new Item.Properties().group(ImmersiveFissionTabItemGroup.tab).maxStackSize(64).rarity(Rarity.UNCOMMON));
			setRegistryName("thorium_ingot");
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
