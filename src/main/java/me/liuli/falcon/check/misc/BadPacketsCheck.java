package me.liuli.falcon.check.misc;

import cn.nukkit.Player;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.InventoryTransactionPacket;
import cn.nukkit.network.protocol.MovePlayerPacket;
import cn.nukkit.network.protocol.PlayerActionPacket;
import cn.nukkit.network.protocol.types.NetworkInventoryAction;
import me.liuli.falcon.cache.CheckCache;
import me.liuli.falcon.manager.AnticheatManager;
import me.liuli.falcon.manager.CheckResult;
import me.liuli.falcon.manager.CheckType;
import me.liuli.falcon.utils.MoveUtil;

public class BadPacketsCheck {
    public static CheckResult runCheck(Player player, DataPacket packet) {
        if (packet instanceof MovePlayerPacket) {
            if (AnticheatManager.canCheckPlayer(player, CheckType.BADPACKETS))
                return checkMove(player, (MovePlayerPacket) packet);
        } else if (packet instanceof PlayerActionPacket) {
            if (AnticheatManager.canCheckPlayer(player, CheckType.BADPACKETS))
                return checkAction(player, (PlayerActionPacket) packet);
        }/*else if(packet instanceof AnimatePacket) {
            if (AnticheatManager.canCheckPlayer(player, CheckType.BADPACKETS))
                return checkAnimate(player);
        }else if(packet instanceof InventoryTransactionPacket){
            if (AnticheatManager.canCheckPlayer(player, CheckType.BADPACKETS)) {
                return checkInv(player, (InventoryTransactionPacket) packet);
            }
        }*/
        return CheckResult.PASSED;
    }

    private static CheckResult checkAnimate(Player player) {
        CheckCache checkCache = CheckCache.get(player);
        if (checkCache == null) return CheckResult.PASSED;

        long lastAnimate = checkCache.lastAnimate;
        checkCache.lastAnimate = System.currentTimeMillis();

        if ((System.currentTimeMillis() - lastAnimate) < CheckType.BADPACKETS.otherData.getInteger("animateDelay")) {
            return new CheckResult("animate too fast(time=" + (System.currentTimeMillis() - checkCache.lastAnimate) + ")");
        }

        return CheckResult.PASSED;
    }

    private static CheckResult checkAction(Player player, PlayerActionPacket packet) {
        CheckCache checkCache = CheckCache.get(player);
        if (checkCache == null) return CheckResult.PASSED;

        if (packet.action == PlayerActionPacket.ACTION_JUMP) {
            if (!(MoveUtil.isNearSolid(player.clone().add(0, 2, 0))) && (System.currentTimeMillis() - checkCache.lastJump) < CheckType.BADPACKETS.otherData.getInteger("jumpCoolDown")) {
                return new CheckResult("jump too fast(time=" + (System.currentTimeMillis() - checkCache.lastJump) + ")");
            }
            if (player.onGround) {
                checkCache.lastJump = System.currentTimeMillis();
            } else {
                return new CheckResult("trying to jump offground");
            }
        }
        return CheckResult.PASSED;
    }

    private static CheckResult checkMove(Player player, MovePlayerPacket packet) {
        if (packet.eid != player.getId() && CheckType.BADPACKETS.otherData.getBoolean("badId")) {
            return new CheckResult("Bad entity id in packet(id=" + packet.eid + ",realId=" + player.getId());
        }
        if (Math.abs(packet.pitch) > 90 && CheckType.BADPACKETS.otherData.getBoolean("derp")) {
            return new CheckResult("Had an illegal pitch(pitch=" + packet.pitch + ")");
        }
        return CheckResult.PASSED;
    }

    private static CheckResult checkInv(Player player, InventoryTransactionPacket packet) {
        if (!CheckType.BADPACKETS.otherData.getBoolean("inventory")) return CheckResult.PASSED;
        PlayerInventory inventory = player.getInventory();
        for (NetworkInventoryAction action : packet.actions) {
            if (action.windowId == 0) {
                if (action.inventorySlot < 0 || action.inventorySlot > 40) {
                    return new CheckResult("Trying move a item from unknown slot");
                }
                Item realFromItem = inventory.getItem(action.inventorySlot);
                Item packetFromItem = action.oldItem;
                if (!realFromItem.equals(packetFromItem)) {
                    return new CheckResult("Trying move a item not exists(packet=" + packetFromItem.getName() + ",real=" + realFromItem + ")");
                }
            }
        }
        return CheckResult.PASSED;
    }
}
