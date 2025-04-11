# MahjongCraft
A Fabric Minecraft mod that allows you to play Japanese (Riichi) Mahjong in game, supports multiplayer.


## RM3 Fork
* Updated to minecraft 1.21.1
* Fixed conflict with c2me (thread unsafe entity removals were fixed)
* Made tables indestructible if game is ongoing
* Player disconnects DO NOT end the game, players can rejoin mahjong table
* Player deaths DO NOT bug out the game, players can rejoin mahjong table
* Breaking a mahjong table drops a mahjong table
* Removed call voice sound effects (we use proximity chat)
* Replaced 1 sou with Philly Eagles (go birds)
* Removed AI bots (ruins gambling) (might bring this back and just treat bots as 0 weight for cash out?)
* mahjong server sends network payload with a more stable wrapper method (checks if player has reconnected/died/etc)


## Lifesteal Gambling
* Added Lifesteal Integration, hearts are used to buy in, heart fragments are rewarded on cash out
* 1 heart == 9 fragments == 9000 points (cash out points are rounded to neared fragment)
* Two Rounds: 3 hearts -> 27,000 points
* One Round: 2 hearts -> 18,000 points
* One Hand: 1 heart -> 9,000 points
* Payments are handed out based on a synthetic total where player that busted are treated as 0's.
* Weights are calculate from synthetic total ratios, rounding errors are given to first/last place. 


##  Future Ideas
* Bring Back Bot button (payout system distrubutes hearts paid in, so bots just count for 0 as is...)

## Dependencies
<div>
    <a href="https://www.curseforge.com/minecraft/mc-mods/fabric-api"><img alt="" src="https://i.imgur.com/Ol1Tcf8.png" height="50"/></a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin"><img alt="" src="https://i.imgur.com/c1DH9VL.png" height="50"/></a>
</div>

- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)
- [Cloth Config API (Fabric)](https://www.curseforge.com/minecraft/mc-mods/cloth-config)
- [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) (Optional)
