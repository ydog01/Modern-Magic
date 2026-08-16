package top.ydog01.mmagic.client;

import net.minecraft.client.Minecraft;
import top.ydog01.mmagic.network.ModNetwork;

public final class QuestBookClient {
    private QuestBookClient() {
    }

    public static void receive(ModNetwork.QuestSyncPacket packet) {
        QuestBookScreen.receive(packet.progress());
        if (packet.open()) {
            Minecraft.getInstance().setScreen(new QuestBookScreen(packet.progress()));
        }
    }
}
