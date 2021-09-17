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

package io.github.gunpowder.entities

import io.github.gunpowder.GunpowderWhitelistModule
import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.configs.DiscordConfig
import io.github.gunpowder.modelhandlers.AccountLinkHandler
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object DiscordListener : ListenerAdapter() {
    private val config by lazy {
        GunpowderMod.instance.registry.getConfig(DiscordConfig::class.java)
    }

    override fun onReady(event: ReadyEvent) {
        // Check if everyone in whitelist
        // still has the role on discord,
        // And then check if anyone not whitelisted
        // DOES have the role on discord
        event.jda.guilds.first().members.forEach { member ->
            if (AccountLinkHandler.isRegistered(member.idLong)) {
                val entry = AccountLinkHandler.get(member.idLong)
                if (member.roles.any { it.id in config.roles }) {
                    GunpowderWhitelistModule.whitelist(entry.minecraft)
                } else {
                    GunpowderWhitelistModule.unwhitelist(entry.minecraft)
                }
            }
        }
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        try {
            if (event.channel.id == config.channel && event.message.contentRaw.startsWith("!register")) {
                if (AccountLinkHandler.isRegistered(event.member!!.idLong)) {
                    error("You already linked an account!")
                }
                val split = event.message.contentRaw.split(" ")
                if (split.size != 2) {
                    error("Usage: `!register <minecraft_username>`")
                }
                val username = split[1]
                val uuid = GunpowderWhitelistModule.getUuidByUsername(username)
                AccountLinkHandler.registerUser(
                    StoredLink(
                        event.member!!.idLong,
                        uuid
                    )
                )
                if (event.member!!.roles.any { it.id in config.roles }) {
                    GunpowderWhitelistModule.whitelist(uuid)
                }
                event.channel.sendMessage("Registered!").queue()
            }
        } catch (e: Exception) {
            val message = e.message ?: "An error occurred"
            println("Error $message")
            event.channel.sendMessage("Error: $message").queue()
        }
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        // Check if correct role and registered
        // If so, whitelist
        try {
            if (AccountLinkHandler.isRegistered(event.user.idLong) && event.member.roles.any { it.id in config.roles }) {
                val entry = AccountLinkHandler.get(event.user.idLong)
                GunpowderWhitelistModule.whitelist(entry.minecraft)
            }
        } catch (e: Exception) {
            val message = e.message ?: "An error occurred"
            println("Error $message")
        }
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        // Check if still has correct roles and registered
        // remove from whitelist if not
        try {
            if (AccountLinkHandler.isRegistered(event.user.idLong) && event.member.roles.none { it.id in config.roles }) {
                val entry = AccountLinkHandler.get(event.user.idLong)
                GunpowderWhitelistModule.unwhitelist(entry.minecraft)
            }
        } catch (e: Exception) {
            val message = e.message ?: "An error occurred"
            println("Error $message")
        }
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        // Remove from whitelist
        try {
            if (AccountLinkHandler.isRegistered(event.user.idLong)) {
                val entry = AccountLinkHandler.get(event.user.idLong)
                GunpowderWhitelistModule.unwhitelist(entry.minecraft)
            }
        } catch (e: Exception) {
            val message = e.message ?: "An error occurred"
            println("Error $message")
        }
    }
}
