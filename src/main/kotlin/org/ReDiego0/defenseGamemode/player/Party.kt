package org.ReDiego0.defenseGamemode.player

import java.util.UUID

class Party(val leader: UUID) {
    val members = mutableSetOf<UUID>()

    init {
        members.add(leader)
    }

    fun addMember(uuid: UUID): Boolean {
        if (members.size >= 4) return false
        members.add(uuid)
        return true
    }

    fun removeMember(uuid: UUID) {
        members.remove(uuid)
    }

    fun isLeader(uuid: UUID): Boolean = leader == uuid
}