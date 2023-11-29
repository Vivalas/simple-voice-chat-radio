package de.maxhenkel.radio.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.radio.radio.RadioData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class RadioCommands2 {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        dispatcher.register(Commands.literal("radio")
                .then(Commands.argument("url", StringArgumentType.string())
                        .then(Commands.argument("station_name", StringArgumentType.string()).executes(RadioCommands2::run))));
    }

    public static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String url = StringArgumentType.getString(context, "url");
        String stationName = StringArgumentType.getString(context, "station_name");
        ServerPlayer player = context.getSource().getPlayerOrException();

        RadioData radioData = new RadioData(UUID.randomUUID(), url, stationName, false);
        player.getInventory().add(radioData.toItemWithNoId());
        return 1;
    }

}
