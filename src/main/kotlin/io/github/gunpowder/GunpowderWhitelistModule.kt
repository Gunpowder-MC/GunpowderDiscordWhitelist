/*
 * MIT License
 *
 * Copyright (c) 2020 GunpowderMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.gunpowder

import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.api.GunpowderModule
import io.github.gunpowder.configs.DiscordConfig
import io.github.gunpowder.entities.DiscordListener
import io.github.gunpowder.models.DiscordLinkTable
import net.dv8tion.jda.api.JDABuilder
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.WhitelistEntry
import java.util.*

class GunpowderWhitelistModule : GunpowderModule {
    override val name = "gunpowder-discord-whitelist"
    override val toggleable = true
    val gunpowder: GunpowderMod
        get() = GunpowderMod.instance

    override fun registerConfigs() {
        gunpowder.registry.registerConfig(
            "gunpowder-discord-whitelist.yaml",
            DiscordConfig::class.java,
            "gunpowder-discord-whitelist.yaml"
        )
        gunpowder.registry.registerTable(DiscordLinkTable)
    }

    override fun registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register {
            gunpowder.server.playerManager.setWhitelistEnabled(true)
            val config = gunpowder.registry.getConfig(DiscordConfig::class.java)
            val builder = JDABuilder.createDefault(config.token)
            builder.addEventListeners(DiscordListener)
            val jda = builder.build()
            println("Waiting for ready")
            jda.awaitReady()
            println("Ready")
        }
    }

    companion object {
        private val server by lazy {
            GunpowderMod.instance.server
        }

        fun getUuidByUsername(username: String): UUID {
            val profile = server.userCache.findByName(username)
            return profile?.id ?: error("Log in on the server first!")
        }

        fun whitelist(user: UUID) {
            println("Whitelisting $user")
            val profile = server.userCache.getByUuid(user)
            if (server.playerManager.isWhitelisted(profile)) {
                error("User is already whitelisted!")
            }
            server.playerManager.whitelist.add(WhitelistEntry(profile))
        }

        fun unwhitelist(user: UUID) {
            println("Unwhitelisting $user")
            val profile = server.userCache.getByUuid(user)
            if (!server.playerManager.isWhitelisted(profile)) {
                error("User is not whitelisted!")
            }
            server.playerManager.whitelist.remove(profile)
        }
    }
}
