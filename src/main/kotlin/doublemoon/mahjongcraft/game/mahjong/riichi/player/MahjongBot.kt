package doublemoon.mahjongcraft.game.mahjong.riichi.player

import doublemoon.mahjongcraft.entity.MahjongBotEntity
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongGame
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d


class MahjongBot(
    val world: ServerWorld,
    pos: Vec3d,
    gamePos: BlockPos,
) : MahjongPlayerBase() {

    override val entity: MahjongBotEntity = MahjongBotEntity(world = world).apply {
        code = MahjongTile.random().code 
        isSpawnedByGame = true
        gameBlockPos = gamePos
        isInvisible = true 
        refreshPositionAfterTeleport(pos) 
        world.spawnEntity(this) 
    }

    override var ready: Boolean = true

    override fun teleport(targetWorld: ServerWorld, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        with(entity) {
            if (this.world != targetWorld) {
                // Teleport to the new world
                teleport(targetWorld, x, y, z, setOf(), yaw, pitch)
            } else {
                // Teleport within the same world
                teleport(targetWorld, x, y, z, setOf(), yaw, pitch)
            }
        }
    }
}