package doublemoon.mahjongcraft.client.gui

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.client.gui.widget.*
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongRule
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongTableBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongTile
import doublemoon.mahjongcraft.network.MahjongTablePacketHandler.sendMahjongTablePacket
import doublemoon.mahjongcraft.registry.ItemRegistry
import doublemoon.mahjongcraft.util.plus
import io.github.cottonmc.cotton.gui.client.CottonClientScreen
import io.github.cottonmc.cotton.gui.client.LibGui
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import java.util.*

@Environment(EnvType.CLIENT)
class MahjongTableWaitingScreen(description: MahjongTableGui) : CottonClientScreen(description) {

    constructor(mahjongTable: MahjongTableBlockEntity) : this(MahjongTableGui(mahjongTable))

    /**
     * 不取消暫停的話, 通過按按鈕導致的更新會卡在下個 tick, 畫面會更新不了
     * */
    override fun isPauseScreen(): Boolean = false

    /**
     * 刷新用, 在 [MahjongTableBlockEntity.fromClientTag] 的時候刷新,
     * 確保存在 [MahjongTableBlockEntity] 的資料能夠同步到 GUI 上
     * */
    fun refresh() {
        (description as MahjongTableGui).refresh()
    }
}

@Environment(EnvType.CLIENT)
class MahjongTableGui(
    val mahjongTable: MahjongTableBlockEntity
) : LightweightGuiDescription() {

    private val client = MinecraftClient.getInstance()
    private val player: ClientPlayerEntity = client.player!!

    //以下來自 mahjongTableBlockEntity
    private val players
        get() = mahjongTable.players
    private val playerEntityNames
        get() = mahjongTable.playerEntityNames
    private val bots
        get() = mahjongTable.bots
    private val ready
        get() = mahjongTable.ready
    private val rule
        get() = mahjongTable.rule

    //    private val playing
//        get() = mahjongTable.playing
    private val isInThisGame: Boolean
        get() = player.uuidAsString in players
    private val isHost: Boolean
        get() = players[0] == player.uuidAsString
    private val isReady: Boolean
        get() {
            val index = players.indexOf(player.uuidAsString)
            if (index == -1) return false
            return ready[index]
        }
    private val isAllReady: Boolean
        get() = (false !in mahjongTable.ready)
    private val isFull: Boolean
        get() = "" !in players
    private val playerAmount: Int
        get() = players.filterNot { it == "" }.size

    //控件相關
    private val darkMode: Boolean
        get() = LibGui.isDarkMode()
    private val ruleTexts = mutableListOf<WText>()
    private val playerInfoItems: MutableList<PlayerInfoItem> =
        mutableListOf<PlayerInfoItem>().apply { repeat(4) { this += PlayerInfoItem(it) } }
    private lateinit var buttonJoinOrLeave: WButton
    private var buttonReadyOrNot: WButton? = null
    private var buttonStart: WTooltipButton? = null
    private var buttonAddBot: WButton? = null
    private var buttonEditRules: WButton? = null
    private val buttonKick: MutableList<WButton?> = mutableListOf(null, null, null)  //最多三個, 房主不能踢自己

    init {
        rootPlainPanel(width = ROOT_WIDTH, height = ROOT_HEIGHT) {
            val icon = item(
                x = 0,
                y = 0,
                itemStack = ItemRegistry.mahjongTile.defaultStack.also { it.damage = MahjongTile.S1.code })
            label(
                x = icon.x + icon.width + 5,
                y = icon.y,
                height = icon.height,
                text = TranslatableText("$MOD_ID.game.riichi_mahjong"),
                verticalAlignment = VerticalAlignment.CENTER,
            )
            buttonJoinOrLeave = button(
                x = BUTTON_JOIN_OR_LEAVE_X,
                y = BUTTON_JOIN_OR_LEAVE_Y,
                width = BUTTON_WIDTH,
                onClick = {
                    if (!isInThisGame) {  //不在這個遊戲表示要加入遊戲
                        player.sendMahjongTablePacket(
                            behavior = MahjongTableBehavior.JOIN,
                            pos = mahjongTable.pos
                        )
                    } else {  //在遊戲中按表示要離開遊戲
                        player.sendMahjongTablePacket(
                            behavior = MahjongTableBehavior.LEAVE,
                            pos = mahjongTable.pos
                        )
                    }
                }
            )
            playerInfoItems.forEachIndexed { index, playerInfoItem ->
                this.add(
                    playerInfoItem,
                    36,
                    30 + index * playerInfoItem.height,
                    playerInfoItem.width,
                    playerInfoItem.height
                )
            }
            plainPanel(
                x = playerInfoItems[0].x + playerInfoItems[0].width,
                y = 14,
            ) {
                rule.toTexts(
                    color2 = if (!darkMode) Formatting.DARK_GRAY else Formatting.YELLOW,
                    color3 = if (!darkMode) Formatting.DARK_PURPLE else Formatting.GREEN,
                    color4 = if (!darkMode) Formatting.LIGHT_PURPLE else Formatting.AQUA,
                    color5 = if (!darkMode) Formatting.DARK_GRAY else Formatting.WHITE
                ).forEachIndexed { index, text ->
                    val y = if (index > 0) ruleTexts[index - 1].let { it.y + it.height } + 3 else 0
                    ruleTexts += when (index) {
                        3 -> tooltipText( //起始點數
                            x = 0,
                            y = y,
                            width = 140,
                            text = text,
                            tooltip = arrayOf(
                                TranslatableText(
                                    "$MOD_ID.game.starting_points.description",
                                    MahjongRule.MIN_POINTS,
                                    MahjongRule.MAX_POINTS
                                )
                            )
                        )
                        4 -> tooltipText( //一位必要點數
                            x = 0,
                            y = y,
                            width = 140,
                            text = text,
                            tooltip = arrayOf(TranslatableText("$MOD_ID.game.min_points_to_win.description"))
                        )
                        else -> text(x = 0, y = y, width = 140, text = text)
                    }
                }
            }
        }
        refresh()
    }

    /**
     * [MahjongTableBlockEntity] 更新時使用
     * */
    fun refresh() {
        with(buttonJoinOrLeave) {
            isEnabled = if (isInThisGame) true else "" in players  //空位的時候, 會以 "" 表示
            label =
                if (!isInThisGame) TranslatableText("$MOD_ID.gui.button.join") else TranslatableText("$MOD_ID.gui.button.leave")
        }
        playerInfoItems.forEach {
            it.entityName = playerEntityNames[it.number]
            it.stringUUID = players[it.number]
            it.isBot = bots[it.number]
            it.ready = ready[it.number]
            it.fresh()
        }
        rule.toTexts(
            color2 = if (!darkMode) Formatting.DARK_GRAY else Formatting.YELLOW,
            color3 = if (!darkMode) Formatting.DARK_PURPLE else Formatting.GREEN,
            color4 = if (!darkMode) Formatting.LIGHT_PURPLE else Formatting.AQUA,
            color5 = if (!darkMode) Formatting.DARK_GRAY else Formatting.WHITE
        ).forEachIndexed { index, text ->
            ruleTexts[index].also {
                it.text = text
                val y = if (index > 0) {
                    ruleTexts[index - 1].let { wText -> wText.y + wText.height } + 3
                } else {
                    0
                }
                it.setLocation(0, y)
            }
        }
        when {
            isHost -> {
                if (buttonStart == null) {
                    buttonStart = WTooltipButton(
                        label = TranslatableText("$MOD_ID.gui.button.start"),
                        tooltip = arrayOf(TranslatableText("$MOD_ID.gui.tooltip.clickable.when_all_ready"))
                    )
                    (rootPanel as WPlainPanel).add(
                        buttonStart,
                        buttonJoinOrLeave.x,
                        buttonJoinOrLeave.y - BUTTON_HEIGHT - BUTTON_PADDING,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                    )
                }
                buttonStart!!.apply {
                    isEnabled = isAllReady
                    onClick = Runnable {
                        if (isAllReady && isHost) {
                            player.sendMahjongTablePacket(
                                behavior = MahjongTableBehavior.START,
                                pos = mahjongTable.pos
                            )
                        }
                    }
                }
                if (buttonAddBot == null) {
                    buttonAddBot = WButton(TranslatableText("$MOD_ID.gui.button.add_bot"))
                    (rootPanel as WPlainPanel).add(
                        buttonAddBot,
                        buttonStart!!.x,
                        buttonStart!!.y - BUTTON_HEIGHT - BUTTON_PADDING,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                    )
                }
                buttonAddBot!!.apply {
                    isEnabled = !isFull
                    onClick = Runnable {
                        if (!isFull && isHost) {
                            player.sendMahjongTablePacket(
                                behavior = MahjongTableBehavior.ADD_BOT,
                                pos = mahjongTable.pos
                            )
                        }
                    }
                }
                if (buttonEditRules == null) {
                    buttonEditRules = WButton(TranslatableText("$MOD_ID.gui.button.edit_rules"))
                    (rootPanel as WPlainPanel).add(
                        buttonEditRules,
                        buttonAddBot!!.x,
                        buttonAddBot!!.y - BUTTON_HEIGHT - BUTTON_PADDING,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                    )
                }
                buttonEditRules!!.apply {
                    onClick = Runnable {
                        if (isHost) {
                            player.sendMahjongTablePacket(
                                behavior = MahjongTableBehavior.OPEN_RULES_EDITOR_GUI,
                                pos = mahjongTable.pos
                            )
                        }
                    }
                }
                repeat(3) {  //kick 按鈕只有 3 個
                    val playerIndex = it + 1
                    if (buttonKick[it] == null) {
                        val kickText = TranslatableText("$MOD_ID.gui.button.kick")
                        buttonKick[it] = WButton(kickText)
                        val buttonWidth = client.textRenderer.getWidth(kickText) + 12
                        (rootPanel as WPlainPanel).add(
                            buttonKick[it],
                            playerInfoItems[0].x - BUTTON_PADDING - buttonWidth,
                            playerInfoItems[playerIndex].y + 2,
                            buttonWidth,
                            BUTTON_HEIGHT
                        )
                    }
                    buttonKick[it]!!.apply {
                        isEnabled = playerAmount > playerIndex
                        onClick = Runnable {
                            if (isHost) {
                                player.sendMahjongTablePacket(
                                    behavior = MahjongTableBehavior.KICK,
                                    pos = mahjongTable.pos,
                                    extraData = playerIndex.toString()
                                )
                            }
                        }
                    }
                }
                clearNotHostButtons()
            }
            isInThisGame -> {
                if (buttonReadyOrNot == null) {
                    buttonReadyOrNot = WButton()
                    (rootPanel as WPlainPanel).add(
                        buttonStart,
                        buttonJoinOrLeave.x,
                        buttonJoinOrLeave.y - BUTTON_HEIGHT - BUTTON_PADDING,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                    )
                }
                buttonReadyOrNot!!.apply {
                    label = if (isReady) {
                        TranslatableText("$MOD_ID.gui.button.not_ready")
                    } else {
                        TranslatableText("$MOD_ID.gui.button.ready")
                    }
                    onClick = Runnable {
                        if (isReady) {
                            player.sendMahjongTablePacket(
                                behavior = MahjongTableBehavior.NOT_READY,
                                pos = mahjongTable.pos
                            )
                        } else {
                            player.sendMahjongTablePacket(
                                behavior = MahjongTableBehavior.READY,
                                pos = mahjongTable.pos
                            )
                        }
                    }
                }
                clearHostButtons()
            }
            else -> {
                clearHostButtons()
                clearNotHostButtons()
            }
        }
    }

    //只有 host 才有的按紐
    private fun clearHostButtons() {
        buttonStart?.let { rootPanel.remove(it) }
        buttonStart = null
        buttonAddBot?.let { rootPanel.remove(it) }
        buttonAddBot = null
        buttonEditRules?.let { rootPanel.remove(it) }
        buttonEditRules = null
        buttonKick.filterNotNull().forEach { rootPanel.remove(it) }
        repeat(3) { buttonKick[it] = null }
    }

    //只有在遊戲中非 host 才有的按紐
    private fun clearNotHostButtons() {
        buttonReadyOrNot?.let { rootPanel.remove(it) }
        buttonReadyOrNot = null
    }

    companion object {
        private const val ROOT_WIDTH = 400
        private const val ROOT_HEIGHT = 200

        private const val BUTTON_WIDTH = 80
        private const val BUTTON_HEIGHT = 20

        private const val BUTTON_JOIN_OR_LEAVE_X = 310
        private const val BUTTON_JOIN_OR_LEAVE_Y = 160

        private const val BUTTON_PADDING = 5
    }

    /**
     * @param number 顯示在玩家名稱上方, ex: Player 1 (1..4), number 最小從零開始算
     * */
    class PlayerInfoItem(
        val number: Int,
    ) : WPlainPanel() {
        /**
         * 對象的實體名稱, 不是 displayName, 是 gameProfile.name,
         * 請不要使用 [ServerPlayerEntity.getDisplayName],
         * 麻煩使用 [ServerPlayerEntity.getName] 或 [ServerPlayerEntity.getEntityName], empty 表示空位
         */
        var entityName: String = ""
        var stringUUID: String = ""
        var isBot: Boolean = false
        var ready: Boolean = false
        private val mc = MinecraftClient.getInstance()
        private val fontHeight = mc.textRenderer.fontHeight
        private val ttPlayer = TranslatableText("$MOD_ID.game.player")
        private val ttHost = TranslatableText("$MOD_ID.game.host")
        private val ttReady = TranslatableText("$MOD_ID.gui.button.ready")
        private val ttNotReady = TranslatableText("$MOD_ID.gui.button.not_ready")
        private val ttEmpty = TranslatableText("$MOD_ID.game.empty")

        init {
            this.setSize(WIDTH, HEIGHT)
        }

        private var face: WWidget =
            faceWidget(x = 0, y = 0, width = 22, height = 22, isBot = isBot, uuid = null, name = null)

        private val name: WLabel = label(
            x = face.x + face.width + 6,
            y = face.y + face.height - fontHeight,
            text = LiteralText(entityName)
        )
        private val playerNum = ttPlayer + " ${number + 1}"
        private val numberAndReady: WLabel = label(
            x = name.x,
            y = name.y - fontHeight - 3,
            text = if (number == 0) {
                playerNum + " (" + ttHost + ")"
            } else {
                playerNum
            }
        )

        fun fresh() {
            when {
                isBot -> {
                    if (face !is WBotFace) {
                        this.remove(face)
                        face = botFace(face.x, face.y, face.width, face.height)
                    }
                }
                stringUUID.isNotEmpty() -> {
                    if (face !is WPlayerFace) {
                        this.remove(face)
                        face = playerFace(
                            face.x,
                            face.y,
                            face.width,
                            face.height,
                            uuid = UUID.fromString(stringUUID),
                            name = entityName
                        )
                    } else {
                        (face as WPlayerFace).also {
                            val uuidFromStr = UUID.fromString(stringUUID)
                            if (entityName != it.name && uuidFromStr != it.uuid) {
                                it.setUuidAndName(uuid = uuidFromStr, name = entityName)
                            }
                        }
                    }
                }
                else -> {
                    if (face !is WSprite) {
                        this.remove(face)
                        face = image(
                            face.x,
                            face.y,
                            face.width,
                            face.height,
                            Identifier("minecraft:textures/item/structure_void.png")
                        )
                    }
                }
            }
            if (number != 0) {
                numberAndReady.text = if (entityName.isNotEmpty()) {
                    playerNum + " (" + (if (ready) ttReady else ttNotReady) + ")"
                } else {
                    playerNum
                }
            }
            name.text = when {
                entityName.isNotEmpty() ->
                    if (!isBot) LiteralText(entityName)
                    else TranslatableText("entity.$MOD_ID.mahjong_bot")
                else -> ttEmpty
            }
        }

        companion object {
            const val HEIGHT = 40
            const val WIDTH = 120

            private fun WPlainPanel.faceWidget(
                x: Int,
                y: Int,
                width: Int,
                height: Int,
                isBot: Boolean,
                uuid: UUID?,
                name: String?
            ): WWidget = when {
                isBot -> botFace(x, y, width, height)
                uuid != null && name != null -> playerFace(x, y, width, height, uuid, name)
                else -> image(x, y, width, height, Identifier("minecraft:textures/item/structure_void.png"))
            }
        }
    }
}