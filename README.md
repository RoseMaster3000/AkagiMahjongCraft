# MahjongCraft
A Fabric Minecraft mod that allows you to play Japanese (Riichi) Mahjong in game, supports multiplayer.


## RM3 Fork
* Updated to minecraft 1.21.1
* fixed conflict with c2me (thread unsafe entity removals were replaced)

* Removed AI bots (ruins gambling)
* Added Lifesteal Integration, hearts are used to buy in, heart fragments are rewarded on cash out
* 1 heart == 9 fragments == 9000 points (cash out points are rounded to neared fragment)
* Two Rounds: 3 hearts -> 27,000 points
* One Round: 2 hearts -> 18,000 points
* One Hand: 1 heart -> 9,000 points

* Made tables indestructible if game is ongoing
* If a player busts, they give as many points as they have and the game immediately ends
* If a player disconnects, the game ends BUT the DC player get none of their money (its lost to the void)
  (or it just autodiscards?)


## Dependencies
<div>
    <a href="https://www.curseforge.com/minecraft/mc-mods/fabric-api"><img alt="" src="https://i.imgur.com/Ol1Tcf8.png" height="50"/></a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin"><img alt="" src="https://i.imgur.com/c1DH9VL.png" height="50"/></a>
</div>

- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)
- [Cloth Config API (Fabric)](https://www.curseforge.com/minecraft/mc-mods/cloth-config)
- [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) (Optional)
