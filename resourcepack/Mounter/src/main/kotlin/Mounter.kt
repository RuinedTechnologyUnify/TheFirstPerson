package kr.kro.lanthanide

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.logging.Logger

lateinit var pluginLogger: Logger;
val idKey = NamespacedKey("mounter", "id")
val rightArmKey = NamespacedKey("mounter", "right_arm")
val rightHandKey = NamespacedKey("mounter", "right_hand")
val leftArmKey = NamespacedKey("mounter", "left_arm")
val leftHandKey = NamespacedKey("mounter", "left_hand")
val delayKey = NamespacedKey("mounter", "delay")

@SuppressWarnings("unused", "UnstableApiUsage")
class Mounter : JavaPlugin(), Listener {

    override fun onEnable() {
        pluginLogger = logger
        pluginLogger.info("Mounter is enabled")
        Bukkit.getPluginManager().registerEvents(this, this)
        val manager = this.lifecycleManager;

        manager.registerEventHandler<ReloadableRegistrarEvent<Commands?>>(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()
            commands.register(
                Commands.literal("mount")
                    .then(
                        Commands.argument("from", ArgumentTypes.entity())
                            .then(
                                Commands.argument("to", ArgumentTypes.entity())
                                    .executes(::mountCommand)
                            )
                    )
                    .build(),
                "Mounts a entity to another entity"
            )
            commands.register(
                Commands.literal("giveitem")
                    .then(
                        Commands.argument("id", StringArgumentType.string())
                            .executes(::giveCommand)
                    )
                    .build(),
                "Gives special item to command user"
            )
        }

        initMountItemList()
    }

    override fun onDisable() {
        logger.info("Mounter is disabled")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.sendMessage(Component.text("Hello, " + event.player.name + "!"));
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.player.sendMessage(Component.text("Bye, " + event.player.name + "!"));

        val rightArmUUID = event.player.persistentDataContainer.get(rightArmKey, MountPersistentDataType.UUID_TYPE)
        val rightHandUUID = event.player.persistentDataContainer.get(rightHandKey, MountPersistentDataType.UUID_TYPE)
        val leftArmUUID = event.player.persistentDataContainer.get(leftArmKey, MountPersistentDataType.UUID_TYPE)
        val leftHandUUID = event.player.persistentDataContainer.get(leftHandKey, MountPersistentDataType.UUID_TYPE)

        if (rightArmUUID != null) {
            Bukkit.getEntity(rightArmUUID)?.remove()
        }
        if (rightHandUUID != null) {
            Bukkit.getEntity(rightHandUUID)?.remove()
        }
        if (leftArmUUID != null) {
            Bukkit.getEntity(leftArmUUID)?.remove()
        }
        if (leftHandUUID != null) {
            Bukkit.getEntity(leftHandUUID)?.remove()
        }


        event.player.persistentDataContainer.set(delayKey, MountPersistentDataType.PLAYER_DELAY, PlayerDelay())
    }

    @EventHandler
    fun onTickStart(event: ServerTickStartEvent) {
        mountedEntityList.forEach { entity ->
            if (entity.first.isValid && entity.second.isValid) {
                entity.second.addPassenger(entity.first)
            }
        }
    }

    @EventHandler
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        var newItem = event.player.inventory.getItem(event.newSlot)
        var prevItem = event.player.inventory.getItem(event.previousSlot)

        if (newItem == null) newItem = ItemStack(Material.AIR)
        if (prevItem == null) prevItem = ItemStack(Material.AIR)

        val newItemMountable = if (newItem.hasItemMeta()) newItem.itemMeta.persistentDataContainer.get(
            NamespacedKey(this, "is_mount_needed"),
            PersistentDataType.BOOLEAN
        ) == true else false;
        val prevItemMountable = if (prevItem.hasItemMeta()) prevItem.itemMeta.persistentDataContainer.get(
            NamespacedKey(this, "is_mount_needed"),
            PersistentDataType.BOOLEAN
        ) == true else false;

        if (prevItemMountable) {
            event.player.removeRightArm()
        }
        if (newItemMountable) {
            event.player.mountRightArm(newItem)
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val playerRightArmDelay =
            event.player.persistentDataContainer.get(delayKey, MountPersistentDataType.PLAYER_DELAY)?.rightArm ?: 0
        if (playerRightArmDelay > Bukkit.getCurrentTick()) return

        if (event.action.isRightClick) {
            val item = event.item ?: return
            val itemId = item.itemMeta.persistentDataContainer.get(idKey, PersistentDataType.STRING) ?: return

            if (itemId == "test_sword") {
                val quaternion = Quaternionf(-0.144f, 0.193f, -0.38f, 0.88f)
                event.player.moveRightArm(
                    translation = Vector3f(-0.5f, 0.2f, -0.3f),
                    rightRotation = quaternion,
                    duration = 2
                )
                event.player.persistentDataContainer.set(
                    delayKey,
                    MountPersistentDataType.PLAYER_DELAY,
                    PlayerDelay(rightArm = Bukkit.getCurrentTick() + 18)
                )

                Bukkit.getScheduler().runTaskLater(this, Runnable {
                    event.player.moveRightArm(
                        Vector3f(0.5f, -0.2f, 0.3f),
                        rightRotation = quaternion.invert(),
                        duration = 6
                    )
                }, 10)
            }
        } else if (event.action.isLeftClick) {
            val item = event.item ?: return
            val itemId = item.itemMeta.persistentDataContainer.get(idKey, PersistentDataType.STRING) ?: return

            if (itemId == "test_sword") {
                val quaternion1 = Quaternionf(-0.41f, 0.18f, 0.0f, 0.89f)
                val quaternion2 = Quaternionf(0.75f, 0.0f, 0.0f, 0.66f)


                event.player.moveRightArm(
                    Vector3f(0.2f, 0.6f, 0.2f),
                    rightRotation = quaternion1,
                    duration = 2
                )
                event.player.persistentDataContainer.set(
                    delayKey,
                    MountPersistentDataType.PLAYER_DELAY,
                    PlayerDelay(rightArm = Bukkit.getCurrentTick() + 30)
                )

                Bukkit.getScheduler().runTaskLater(this, Runnable {
                    event.player.moveRightArm(
                        Vector3f(-1.0f, -1.3f, -0.2f),
                        rightRotation = quaternion2,
                        duration = 4
                    )
                }, 6)
                Bukkit.getScheduler().runTaskLater(this, Runnable {
                    event.player.moveRightArm(
                        Vector3f(0.8f, 0.7f, 0.0f),
                        rightRotation = quaternion2.invert().mul(quaternion1.invert()),
                        duration = 15
                    )
                }, 15)
            }
        }
    }
}