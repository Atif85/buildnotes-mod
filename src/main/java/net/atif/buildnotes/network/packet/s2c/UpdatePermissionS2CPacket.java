package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.PermissionLevel;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdatePermissionS2CPacket(PermissionLevel permission) implements CustomPacketPayload {
    public static final Type<UpdatePermissionS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "update_permission_s2c"));

    public static final StreamCodec<FriendlyByteBuf, UpdatePermissionS2CPacket> CODEC = CustomPacketPayload.codec(
            UpdatePermissionS2CPacket::write,
            UpdatePermissionS2CPacket::new
    );

    public UpdatePermissionS2CPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(PermissionLevel.class));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(permission);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}