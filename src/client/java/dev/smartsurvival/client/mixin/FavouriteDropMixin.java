package dev.smartsurvival.client.mixin;

import dev.smartsurvival.client.SmartSurvivalClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels before vanilla predicts inventory changes or sends a drop packet. */
@Mixin(MultiPlayerGameMode.class)
public abstract class FavouriteDropMixin {
    @Inject(method="dropItem",at=@At("HEAD"),cancellable=true)
    private void smartSurvival$guardHotbar(LocalPlayer player,boolean wholeStack,CallbackInfo ci) {
        SmartSurvivalClient app=SmartSurvivalClient.INSTANCE;
        if(app!=null && app.protectedStack(player.getInventory().getSelectedItem())) {
            app.blockedDrop();ci.cancel();
        }
    }
    @Inject(method="handleContainerInput",at=@At("HEAD"),cancellable=true)
    private void smartSurvival$guardContainer(int containerId,int slotId,int button,ContainerInput input,Player player,CallbackInfo ci) {
        SmartSurvivalClient app=SmartSurvivalClient.INSTANCE;
        if(app==null || !app.settings.protectFavorites) return;
        var menu=player.containerMenu;
        ItemStack dropping=ItemStack.EMPTY;
        if(input==ContainerInput.THROW && slotId>=0 && slotId<menu.slots.size())
            dropping=menu.getSlot(slotId).getItem();
        else if(input==ContainerInput.PICKUP && slotId==AbstractContainerMenu.SLOT_CLICKED_OUTSIDE)
            dropping=menu.getCarried();
        if(app.protectedStack(dropping)) {app.blockedDrop();ci.cancel();}
    }
}
