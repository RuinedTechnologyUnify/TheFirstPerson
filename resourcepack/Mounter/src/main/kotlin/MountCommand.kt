package kr.kro.lanthanide

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

val mountedEntityList = mutableListOf<Pair<Entity, Entity>>()

val mountItemList = HashMap<String, ItemStack>()


fun initMountItemList() {
    val testTome = ItemStack(Material.TURTLE_SCUTE)
    testTome.itemMeta = testTome.itemMeta.apply {
        setCustomModelData(100)
        itemName(Component.text("test book"))
        persistentDataContainer.set(idKey, PersistentDataType.STRING, "test_book")
        persistentDataContainer.set(NamespacedKey("mounter", "is_mount_needed"), PersistentDataType.BOOLEAN, true)
    }
    mountItemList["test_book"] = testTome

    val testSword = ItemStack(Material.TURTLE_SCUTE)
    testSword.itemMeta = testSword.itemMeta.apply {
        setCustomModelData(200)
        itemName(Component.text("test sword"))
        persistentDataContainer.set(idKey, PersistentDataType.STRING, "test_sword")
        persistentDataContainer.set(NamespacedKey("mounter", "is_mount_needed"), PersistentDataType.BOOLEAN, true)
    }
    mountItemList["test_sword"] = testSword
}

fun mountCommand(ctx: CommandContext<CommandSourceStack>): Int {
    println(ctx.range)
    val from = Bukkit.selectEntities(ctx.source.sender, ctx.input.split(" ")[1])[0]
    val to = Bukkit.selectEntities(ctx.source.sender, ctx.input.split(" ")[2])[0]

    to.addPassenger(from)
    if (!mountedEntityList.contains(Pair(from, to))) mountedEntityList.add(Pair(from, to))
//    val entities = ArgumentTypes.entity().parse(StringReader(ctx.input)).resolve(ctx.source)
//    println(entities.size)
    return Command.SINGLE_SUCCESS
}

fun giveCommand(ctx: CommandContext<CommandSourceStack>): Int {
    val name = StringArgumentType.getString(ctx, "id")
    if (!mountItemList.contains(name) || ctx.source.sender !is Player) return 0

    mountItemList[name]?.let { (ctx.source.sender as Player).inventory.addItem(it) }

    return Command.SINGLE_SUCCESS
}