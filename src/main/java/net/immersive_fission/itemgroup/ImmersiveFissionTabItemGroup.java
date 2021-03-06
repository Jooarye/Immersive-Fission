
package net.immersive_fission.itemgroup;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup;

import net.immersive_fission.item.ThoriumIngotItem;
import net.immersive_fission.ImmersiveFissionModElements;

@ImmersiveFissionModElements.ModElement.Tag
public class ImmersiveFissionTabItemGroup extends ImmersiveFissionModElements.ModElement {
	public ImmersiveFissionTabItemGroup(ImmersiveFissionModElements instance) {
		super(instance, 1);
	}

	@Override
	public void initElements() {
		tab = new ItemGroup("tabimmersive_fission_tab") {
			@OnlyIn(Dist.CLIENT)
			@Override
			public ItemStack createIcon() {
				return new ItemStack(ThoriumIngotItem.block);
			}

			@OnlyIn(Dist.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}

	public static ItemGroup tab;
}
