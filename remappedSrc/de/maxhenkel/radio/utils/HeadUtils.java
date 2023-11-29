package de.maxhenkel.radio.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class HeadUtils {

    public static ItemStack createHead(String itemName, List<Text> loreComponents, GameProfile gameProfile) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        NbtCompound tag = stack.getOrCreateNbt();

        MutableText nameComponent = Text.literal(itemName).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE));

        NbtList lore = new NbtList();
        for (int i = 0; i < loreComponents.size(); i++) {
            lore.add(i, NbtString.of(Text.Serializer.toJson(loreComponents.get(i))));
        }
        NbtCompound display = new NbtCompound();
        display.putString(ItemStack.NAME_KEY, Text.Serializer.toJson(nameComponent));
        display.put(ItemStack.LORE_KEY, lore);
        tag.put(ItemStack.DISPLAY_KEY, display);
        tag.putInt("HideFlags", ItemStack.TooltipSection.ADDITIONAL.getFlag());

        tag.put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), gameProfile));
        return stack;
    }

    private static final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    public static GameProfile getGameProfile(UUID uuid, String name, String skinUrl) {
        GameProfile gameProfile = new GameProfile(uuid, name);
        PropertyMap properties = gameProfile.getProperties();

        List<Property> textures = new ArrayList<>();


        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textureMap = new HashMap<>();
        textureMap.put(MinecraftProfileTexture.Type.SKIN, new MinecraftProfileTexture(skinUrl, null));

        String json = gson.toJson(new MinecraftTexturesPayload(textureMap));

        String base64Payload = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        textures.add(new Property("Value", base64Payload));

        properties.putAll("textures", textures);

        return gameProfile;
    }

    private static class MinecraftTexturesPayload {

        private final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;

        public MinecraftTexturesPayload(Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures) {
            this.textures = textures;
        }

        public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures() {
            return textures;
        }
    }

}
