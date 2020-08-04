package io.github.gunpowder

import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.api.GunpowderModule

class GunpowderTemplateModule : GunpowderModule {
    override val name = "template"
    override val toggleable = true
    val gunpowder: GunpowderMod
        get() = GunpowderMod.instance
}