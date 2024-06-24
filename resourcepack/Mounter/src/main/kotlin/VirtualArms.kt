package kr.kro.lanthanide

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f

fun Player.mountRightArm(itemForHand: ItemStack) {
    val rightArm = this.location.world.spawnEntity(
        this.location.apply { pitch = 0f; yaw = 0f },
        EntityType.ITEM_DISPLAY
    ) as ItemDisplay
    val rightHand = this.location.world.spawnEntity(
        this.location.apply { pitch = 0f; yaw = 0f },
        EntityType.ITEM_DISPLAY
    ) as ItemDisplay

    val rightArmItem = ItemStack(Material.PLAYER_HEAD)
    rightArmItem.itemMeta = (rightArmItem.itemMeta as SkullMeta).apply {
        setCustomModelData(1)
        owningPlayer = this@mountRightArm
    }

    val transformation = Transformation(
        Vector3f(0.7f, -1024.8f, -1.5f),
        Quaternionf(0.0f, 0.0f, 0.0f, 1.0f),
        Vector3f(1.5f),
        Quaternionf(0.0f, 0.97f, 0.2f, -0.03f)
    )

    rightArm.let {
        it.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND
        it.transformation = transformation
        it.viewRange = 0.015f
        it.setItemStack(rightArmItem)
//        it.itemStack = rightArmItem
        it.persistentDataContainer.set(idKey, PersistentDataType.STRING, "right_arm")
    }

    rightHand.let {
        it.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND
        it.transformation = transformation
        it.viewRange = 0.015f
        it.setItemStack(itemForHand)
//        it.itemStack = itemForHand
        it.persistentDataContainer.set(idKey, PersistentDataType.STRING, "right_hand")
    }

    persistentDataContainer.set(rightArmKey, MountPersistentDataType.UUID_TYPE, rightArm.uniqueId)
    persistentDataContainer.set(rightHandKey, MountPersistentDataType.UUID_TYPE, rightHand.uniqueId)

    addPassenger(rightArm)
    addPassenger(rightHand)
}

fun Player.setRightArmItem(itemForHand: ItemStack) {
    val rightArmHand = getRightArm()

    if (rightArmHand == null || rightArmHand.size != 2) {
        mountRightArm(itemForHand)
    } else {
        rightArmHand[0].setItemStack(itemForHand)
//        rightArmHand[1].itemStack = itemForHand
    }
}

fun Player.removeRightArm() {
    pluginLogger.info("removeRightArm")
    val rightArmHand = getRightArm() ?: return

    rightArmHand.forEach { passenger -> passenger.remove() }
}

fun Player.setTransRightArm(armTrans: Transformation, handTrans: Transformation, duration: Int) {
    val rightArmHand = getRightArm() ?: return

    val rightArm = rightArmHand[0]
    val rightHand = rightArmHand[1]

    rightArm.transformation = armTrans
    rightHand.transformation = handTrans

    rightArm.interpolationDuration = duration
    rightHand.interpolationDuration = duration

    rightArm.interpolationDelay = 0
    rightHand.interpolationDelay = 0
}

fun Player.moveRightArm(
    translation: Vector3f = Vector3f(0.0f),
    leftRotation: Quaternionf = Quaternionf(0.0f, 0.0f, 0.0f, 1.0f),
    scale: Vector3f = Vector3f(1.0f),
    rightRotation: Quaternionf = Quaternionf(0.0f, 0.0f, 0.0f, 1.0f),
    duration: Int = 10
) {
    val rightArmHand = getRightArm() ?: return

    val rightArm = rightArmHand[0]
    val rightHand = rightArmHand[1]

    val armTrans = Transformation(
        rightArm.transformation.translation.add(translation),
        rightArm.transformation.leftRotation,
        rightArm.transformation.scale,
        rightArm.transformation.rightRotation.mul(rightRotation)
    )

    val handTrans = Transformation(
        rightHand.transformation.translation.add(translation),
        rightHand.transformation.leftRotation,
        rightHand.transformation.scale,
        rightHand.transformation.rightRotation.mul(rightRotation)
    )

    rightArm.transformation = armTrans
    rightHand.transformation = handTrans

    rightArm.interpolationDuration = duration
    rightHand.interpolationDuration = duration

    rightArm.interpolationDelay = 0
    rightHand.interpolationDelay = 0
}

fun Player.getRightArm(): List<ItemDisplay>? {
    val rightArmHand = this.passengers.filter { passenger ->
        passenger is ItemDisplay &&
                (passenger.persistentDataContainer.get(idKey, PersistentDataType.STRING) == "right_arm" ||
                        passenger.persistentDataContainer.get(idKey, PersistentDataType.STRING) == "right_hand")
    }

    if (rightArmHand.size != 2) {
        return null
    }

    // 위에서 passenger is ItemDisplay 검사하므로 경고 무시 가능
    return rightArmHand as List<ItemDisplay>
}