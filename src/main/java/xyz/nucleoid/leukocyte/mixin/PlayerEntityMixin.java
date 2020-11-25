package xyz.nucleoid.leukocyte.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.leukocyte.ProtectionManager;
import xyz.nucleoid.leukocyte.rule.ProtectionRule;
import xyz.nucleoid.leukocyte.rule.RuleResult;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    private PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void isInvulnerableTo(DamageSource source, CallbackInfoReturnable<Boolean> ci) {
        if (this.world.isClient || source != DamageSource.FALL) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        ProtectionManager protection = ProtectionManager.get(player.server);

        RuleResult result = protection.test(this.world, player.getBlockPos(), ProtectionRule.FALL_DAMAGE, player);
        if (result == RuleResult.ALLOW) {
            ci.setReturnValue(false);
        } else if (result == RuleResult.DENY) {
            ci.setReturnValue(true);
        }
    }

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void dropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable<Boolean> ci) {
        if (this.world.isClient) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        ProtectionManager protection = ProtectionManager.get(player.server);

        // TODO: this will cause incompatibility with the event
        RuleResult result = protection.test(this.world, player.getBlockPos(), ProtectionRule.THROW_ITEMS, player);
        if (result == RuleResult.DENY) {
            int slot = player.inventory.selectedSlot;
            ItemStack stack = player.inventory.getStack(slot);
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, slot, stack));
            ci.setReturnValue(false);
        }
    }
}