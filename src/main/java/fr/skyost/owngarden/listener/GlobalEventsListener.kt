package fr.skyost.owngarden.listener

import fr.skyost.owngarden.OwnGarden
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.StructureGrowEvent
import java.util.concurrent.ThreadLocalRandom

/**
 * Global events handled by the plugin.
 */
class GlobalEventsListener(private val plugin: OwnGarden) : Listener {

    private val fungusSpecies = setOf(TreeType.CRIMSON_FUNGUS, TreeType.WARPED_FUNGUS)
    private val fungusSaplings = setOf(Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS)

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        // Let fungus grow only with bone meal
        if (fungusSpecies.contains(event.species) && !event.isFromBonemeal) {
            event.isCancelled = true
            return
        }

        val location = event.location
        val schematics = plugin.pluginConfig!!.getSchematics(location.block.type)
        if (plugin.worldEditOperations!!.growTree(schematics, location)) {
            if (schematics === plugin.pluginConfig!!.saplingDarkOakSchematics) {
                val current = location.block
                for (blockFace in FACES) {
                    val relative = current.getRelative(blockFace)
                    if (relative.type == Material.DARK_OAK_SAPLING) {
                        relative.type = Material.AIR
                    }
                }
            }

            //event.getBlocks().clear();
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        // The player is not right-clicking a block or is not holding an item
        if (event.action != Action.RIGHT_CLICK_BLOCK || !event.hasItem()) {
            return
        }

        val item = event.item ?: return

        // The player is not holding bone meal
        if (item.type != Material.BONE_MEAL) {
            return
        }

        val block = event.clickedBlock ?: return

        // The block is not a fungus
        if (!fungusSaplings.contains(block.type)) {
            return
        }

        // The fungus is not on dirt
        if (!Tag.DIRT.isTagged(block.getRelative(BlockFace.DOWN).type)) {
            return
        }

        val player = event.player
        @Suppress("DEPRECATION") // the method that takes an int is deprecated, which is also the type of this effect
        player.playEffect(block.location.clone().add(0.5, 0.5, 0.5), Effect.BONE_MEAL_USE, 10)

        // Remove on bone meal
        if (player.gameMode != GameMode.CREATIVE) {
            item.amount = item.amount - 1
        }

        // 15% chance for the fungus to grow from bone meal
        if (ThreadLocalRandom.current().nextDouble() < 0.15) {
            val treeType = if (block.type == Material.CRIMSON_FUNGUS) {
                TreeType.CRIMSON_FUNGUS
            } else {
                TreeType.WARPED_FUNGUS
            }
            Bukkit.getPluginManager().callEvent(StructureGrowEvent(block.location, treeType, true, player, emptyList()))
        }
    }

    companion object {
        private val FACES = listOf(
                BlockFace.NORTH,
                BlockFace.NORTH_EAST,
                BlockFace.EAST,
                BlockFace.SOUTH_EAST,
                BlockFace.SOUTH,
                BlockFace.SOUTH_WEST,
                BlockFace.WEST,
                BlockFace.NORTH_WEST
        )
    }
}