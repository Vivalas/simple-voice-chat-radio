package de.maxhenkel.radio.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.maxhenkel.radio.Radio;
import de.maxhenkel.radio.radio.RadioData;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public class RadioCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandBuildContext ctx, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> literalBuilder = CommandManager.literal("radio")
                .requires((commandSource) -> commandSource.hasPermissionLevel(Radio.SERVER_CONFIG.commandPermissionLevel.get()));

        literalBuilder.then(CommandManager.literal("create").then(CommandManager.argument("url", StringArgumentType.string()).then(CommandManager.argument("station_name", StringArgumentType.string()).executes(context -> {
            String url = StringArgumentType.getString(context, "url");
            String stationName = StringArgumentType.getString(context, "station_name");
            ServerPlayerEntity player = context.getSource().getPlayer();

            RadioData radioData = new RadioData(UUID.randomUUID(), url, stationName, false);
            player.getInventory().insertStack(radioData.toItemWithNoId());
            return 1;
        }))));

        dispatcher.register(literalBuilder);
    }

}
