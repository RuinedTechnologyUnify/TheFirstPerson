package kr.kro.lanthanide

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.nio.ByteBuffer
import java.util.*

object MountPersistentDataType {
    val PLAYER_DELAY = PlayerDelayPersistentDataType()
    val UUID_TYPE = UUIDPersistentDataType()

    class PlayerDelayPersistentDataType : PersistentDataType<IntArray, PlayerDelay> {
        override fun getPrimitiveType(): Class<IntArray> {
            return IntArray::class.java
        }

        override fun getComplexType(): Class<PlayerDelay> {
            return PlayerDelay::class.java
        }

        override fun fromPrimitive(primitive: IntArray, context: PersistentDataAdapterContext): PlayerDelay {
            return PlayerDelay(primitive[0], primitive[1], primitive[2], primitive[3], primitive[4], primitive[5])
        }

        override fun toPrimitive(complex: PlayerDelay, context: PersistentDataAdapterContext): IntArray {
            return listOf(
                complex.head,
                complex.rightArm,
                complex.leftArm,
                complex.body,
                complex.rightLeg,
                complex.leftLeg
            ).toIntArray()
        }
    }

    class UUIDPersistentDataType : PersistentDataType<LongArray, UUID> {
        override fun getPrimitiveType(): Class<LongArray> {
            return LongArray::class.java
        }

        override fun getComplexType(): Class<UUID> {
            return UUID::class.java
        }

        override fun fromPrimitive(primitive: LongArray, context: PersistentDataAdapterContext): UUID {
            return UUID(primitive[0], primitive[1])
        }

        override fun toPrimitive(complex: UUID, context: PersistentDataAdapterContext): LongArray {
            return listOf(complex.mostSignificantBits, complex.leastSignificantBits).toLongArray()
        }
    }
}