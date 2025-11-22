package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.PermissionLevel;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdatePermissionS2CPacket(PermissionLevel permission) implements CustomPayload {
    public static final CustomPayload.Id<UpdatePermissionS2CPacket> ID = new CustomPayload.Id<>(Identifier.of(Buildnotes.MOD_ID, "update_permission_s2c"));

    public static final PacketCodec<PacketByteBuf, UpdatePermissionS2CPacket> CODEC = CustomPayload.codecOf(
            UpdatePermissionS2CPacket::write,
            UpdatePermissionS2CPacket::new
    );

    public UpdatePermissionS2CPacket(PacketByteBuf buf) {
        this(buf.readEnumConstant(PermissionLevel.class));
    }

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(permission);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}