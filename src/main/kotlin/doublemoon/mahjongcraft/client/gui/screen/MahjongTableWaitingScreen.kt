package doublemoon.mahjongcraft.client.gui.screen

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.client.gui.widget.*
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongRule
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTableBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.network.mahjong_table.MahjongTablePayload
import doublemoon.mahjongcraft.network.sendPayloadToServer
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
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import java.util.*


@Environment(EnvType.CLIENT)
class MahjongTableWaitingScreen(
    mahjongTable: MahjongTableBlockEntity,
) : CottonClientScreen(MahjongTableGui(mahjongTable)) {

    override fun shouldPause(): Boolean = false

    fun refresh() {
        (description as MahjongTableGui).refresh()
    }
}


@Environment(EnvType.CLIENT)
class MahjongTableGui(
    val mahjongTable: MahjongTableBlockEntity,
) : LightweightGuiDescription() {

    private val client = MinecraftClient.getInstance()
    private val player: ClientPlayerEntity = client.player!!

    //以下來自 mahjongTableBlockEntity
    private val players = mahjongTable.players
    private val playerEntityNames = mahjongTable.playerEntityNames
    private val bots = mahjongTable.bots
    private val ready = mahjongTable.ready
    private val rule get() = mahjongTable.rule

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
    private val playerInfoItems: List<PlayerInfoItem> = List(4) { PlayerInfoItem(it) }

    //Buttons
    private lateinit var joinOrLeave: WButton
    private var readyOrNot: WButton? = null
    private var start: WTooltipButton? = null
    private var editRules: WButton? = null
    private val kick: MutableList<WButton?> = mutableListOf(null, null, null)  //最多三個, 房主不能踢自己

    private fun formatStartingPointsText(originalText: Text, rule: MahjongRule): Text {
        val points = rule.startingPoints
        // Using integer division for buy-in calculation
        val buyIn = points / 9000
        // Append the buy-in text to the original text. Use copy() for immutability if originalText might be used elsewhere.
        // Apply formatting (e.g., GOLD color) to the appended part.
        return originalText.copy().append(Text.literal(" ($buyIn ♥ buy in)").formatted(Formatting.RED))
    }

    init {
        rootPlainPanel(width = ROOT_WIDTH, height = ROOT_HEIGHT) {
            val icon = item(
                x = BORDER_MARGIN,
                y = BORDER_MARGIN,
                itemStack = ItemRegistry.mahjongTile.defaultStack.also { it.damage = MahjongTile.S1.code })
            label(
                x = icon.x + icon.width + 5,
                y = icon.y,
                height = icon.height,
                text = Text.translatable("$MOD_ID.game.riichi_mahjong"),
                verticalAlignment = VerticalAlignment.CENTER,
            )
            joinOrLeave = button(
                x = BUTTON_JOIN_OR_LEAVE_X,
                y = BUTTON_JOIN_OR_LEAVE_Y,
                width = BUTTON_WIDTH,
                onClick = {
                    if (!isInThisGame) {  //不在這個遊戲表示要加入遊戲
                        sendPayloadToServer(
                            payload = MahjongTablePayload(
                                behavior = MahjongTableBehavior.JOIN,
                                pos = mahjongTable.pos
                            )
                        )
                    } else {  //在遊戲中按表示要離開遊戲
                        sendPayloadToServer(
                            payload = MahjongTablePayload(
                                behavior = MahjongTableBehavior.LEAVE,
                                pos = mahjongTable.pos
                            )
                        )
                    }
                }
            )
            playerInfoItems.forEachIndexed { index, playerInfoItem ->
                this.add(
                    playerInfoItem,
                    44,
                    30 + index * playerInfoItem.height,
                    playerInfoItem.width,
                    playerInfoItem.height
                )
            }
            scrollPanel(
                x = playerInfoItems[0].x + playerInfoItems[0].width,
                y = 14,
                width = 140,
                height = ROOT_HEIGHT - 14 * 2
            ) {
                // --- Filtering Logic ---
                val originalRuleTextsList = rule.toTexts(
                    color2 = if (!darkMode) Formatting.DARK_GRAY else Formatting.YELLOW,
                    color3 = if (!darkMode) Formatting.DARK_PURPLE else Formatting.GREEN,
                    color4 = if (!darkMode) Formatting.LIGHT_PURPLE else Formatting.AQUA,
                    color5 = if (!darkMode) Formatting.DARK_GRAY else Formatting.WHITE
                )
                // Filter out the item originally at index 4
                val visibleRuleTextsList = originalRuleTextsList.filterIndexed { index, _ -> index != 4 }
                // Store the original starting points text to identify it after filtering
                val startingPointsText = originalRuleTextsList.getOrNull(3)
                // --- End Filtering Logic ---

                visibleRuleTextsList.forEachIndexed { visibleIndex, text -> // <-- Iterate filtered list
                    // Calculate y based on the *last added widget* in ruleTexts list
                    val y = if (visibleIndex > 0) ruleTexts[visibleIndex - 1].let { it.y + it.height } + 3 else 0

                    // Create widget based on whether it's the starting points text
                    val widget = if (text == startingPointsText) { // Check if it's the original starting points text
                        tooltipText( // 起始點數
                            x = 0,
                            y = y,
                            width = 132,
                            text = formatStartingPointsText(text, rule),
                            tooltip = arrayOf(
                                Text.translatable("$MOD_ID.game.starting_points.description")
                            )
                        )
                    } else { // All other visible texts
                        text(x = 0, y = y, width = 132, text = text)
                    }
                    ruleTexts += widget // Add the created widget to our list
                    // Explicitly add the widget to the panel inside the scrollPanel
                    this.add(widget, widget.x, widget.y, widget.width, widget.height)
                }
            }
        }
        refresh()
    }

    /**
     * [MahjongTableBlockEntity] 更新時使用
     * */
    fun refresh() {
        with(joinOrLeave) {
            isEnabled = if (isInThisGame) true else "" in players  //空位的時候, 會以 "" 表示
            label =
                if (!isInThisGame) Text.translatable("$MOD_ID.gui.button.join") else Text.translatable("$MOD_ID.gui.button.leave")
        }
        playerInfoItems.forEach {
            it.entityName = playerEntityNames[it.number]
            it.stringUUID = players[it.number]
            it.isBot = bots[it.number]
            it.ready = ready[it.number]
            it.fresh()
        }
        // --- Filtering Logic ---
        val originalRuleTextsList = rule.toTexts(
            color2 = if (!darkMode) Formatting.DARK_GRAY else Formatting.YELLOW,
            color3 = if (!darkMode) Formatting.DARK_PURPLE else Formatting.GREEN,
            color4 = if (!darkMode) Formatting.LIGHT_PURPLE else Formatting.AQUA,
            color5 = if (!darkMode) Formatting.DARK_GRAY else Formatting.WHITE
        )
        // Filter out the item originally at index 4
        val visibleRuleTextsList = originalRuleTextsList.filterIndexed { index, _ -> index != 4 }
        val startingPointsText = originalRuleTextsList.getOrNull(3)

        if (ruleTexts.size != visibleRuleTextsList.size) {
            System.err.println("Rule text count mismatch in MahjongTableGui refresh!")
            // Potentially add logic here to clear and rebuild ruleTexts if needed
            return // Avoid IndexOutOfBounds errors
        }

        visibleRuleTextsList.forEachIndexed { visibleIndex, text -> // <-- Iterate filtered list
            ruleTexts[visibleIndex].also { widget -> // <-- Use visibleIndex and get widget
                // Check if this widget corresponds to starting points text
                if (text == startingPointsText) {
                    widget.text = formatStartingPointsText(text, rule) // <-- Use helper function
                } else {
                    widget.text = text // <-- Use the standard text for other rules
                }
                // --- Calculate Y position ---
                val y = if (visibleIndex > 0) {
                    ruleTexts[visibleIndex - 1].let { wText -> wText.y + wText.height } + 3
                } else {
                    0
                }
                widget.setLocation(0, y)
            }
        }
        when {
            isHost -> {
                if (start == null) {
                    start = WTooltipButton(
                        label = Text.translatable("$MOD_ID.gui.button.start"),
                        tooltip = arrayOf(Text.translatable("$MOD_ID.gui.tooltip.clickable.when_all_ready"))
                    )
                    (rootPanel as WPlainPanel).add(
                        start,
                        joinOrLeave.x,
                        joinOrLeave.y - BUTTON_HEIGHT - BUTTON_PADDING,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                    )
                }
                start!!.apply {
                    isEnabled = isAllReady
                    onClick = Runnable {
                        if (isAllReady && isHost) {
                            sendPayloadToServer(
                                payload = MahjongTablePayload(
                                    behavior = MahjongTableBehavior.START,
                                    pos = mahjongTable.pos
                                )
                            )
                        }
                    }
                }
                if (editRules == null) {
                    editRules = WButton(Text.translatable("$MOD_ID.gui.button.edit_rules"))
                    (rootPanel as WPlainPanel).add(
                        editRules,
                        start!!.x,
                        start!!.y - BUTTON_HEIGHT - BUTTON_PADDING,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                    )
                }
                editRules!!.apply {
                    onClick = Runnable {
                        if (isHost) {
                            sendPayloadToServer(
                                payload = MahjongTablePayload(
                                    behavior = MahjongTableBehavior.OPEN_RULES_EDITOR_GUI,
                                    pos = mahjongTable.pos
                                )
                            )
                        }
                    }
                }
                repeat(3) {  //kick 按鈕只有 3 個
                    val playerIndex = it + 1
                    if (kick[it] == null) {
                        val kickText = Text.translatable("$MOD_ID.gui.button.kick")
                        kick[it] = WButton(kickText)
                        val buttonWidth = client.textRenderer.getWidth(kickText) + 12
                        (rootPanel as WPlainPanel).add(
                            kick[it],
                            playerInfoItems[0].x - BUTTON_PADDING - buttonWidth,
                            playerInfoItems[playerIndex].y + 2,
                            buttonWidth,
                            BUTTON_HEIGHT
                        )
                    }
                    kick[it]!!.apply {
                        isEnabled = playerAmount > playerIndex
                        onClick = Runnable {
                            if (isHost) {
                                sendPayloadToServer(
                                    payload = MahjongTablePayload(
                                        behavior = MahjongTableBehavior.KICK,
                                        pos = mahjongTable.pos,
                                        extraData = playerIndex.toString()
                                    )
                                )
                            }
                        }
                    }
                }
                clearNotHostButtons()
            }
            isInThisGame -> {
                if (readyOrNot == null) {
                    readyOrNot = WButton()
                    (rootPanel as WPlainPanel).add(
                        readyOrNot,
                        joinOrLeave.x,
                        joinOrLeave.y - BUTTON_HEIGHT - BUTTON_PADDING,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                    )
                }
                readyOrNot!!.apply {
                    label = if (isReady) {
                        Text.translatable("$MOD_ID.gui.button.not_ready")
                    } else {
                        Text.translatable("$MOD_ID.gui.button.ready")
                    }
                    onClick = Runnable {
                        if (isReady) {
                            sendPayloadToServer(
                                payload = MahjongTablePayload(
                                    behavior = MahjongTableBehavior.NOT_READY,
                                    pos = mahjongTable.pos
                                )
                            )
                        } else {
                            sendPayloadToServer(
                                payload = MahjongTablePayload(
                                    behavior = MahjongTableBehavior.READY,
                                    pos = mahjongTable.pos
                                )
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
        start?.let { rootPanel.remove(it) }
        start = null
        editRules?.let { rootPanel.remove(it) }
        editRules = null
        kick.filterNotNull().forEach { rootPanel.remove(it) }
        repeat(3) { kick[it] = null }
    }

    //只有在遊戲中非 host 才有的按紐
    private fun clearNotHostButtons() {
        readyOrNot?.let { rootPanel.remove(it) }
        readyOrNot = null
    }

    companion object {
        private const val ROOT_WIDTH = 400
        private const val ROOT_HEIGHT = 200
        private const val BUTTON_WIDTH = 80
        private const val BORDER_MARGIN = 8
        private const val BUTTON_HEIGHT = 20
        private const val BUTTON_JOIN_OR_LEAVE_X = 310
        private const val BUTTON_JOIN_OR_LEAVE_Y = 160
        private const val BUTTON_PADDING = 5
    }

    /**
     * @param number 顯示在玩家名稱上方, ex: Player 1 (1..4), number 最小從 0 開始算
     * */
    class PlayerInfoItem(
        val number: Int,
    ) : WPlainPanel() {
        /**
         * 對象的實體名稱, 不是 displayName, 是 gameProfile.name,
         * 請不要使用 [ServerPlayerEntity.getDisplayName],
         * 麻煩使用 [ServerPlayerEntity.getName], empty 表示空位
         */
        var entityName: String = ""
        var stringUUID: String = ""
        var isBot: Boolean = false
        var ready: Boolean = false
        private val client = MinecraftClient.getInstance()
        private val fontHeight = client.textRenderer.fontHeight
        private val ttPlayer = Text.translatable("$MOD_ID.game.player")
        private val ttHost = Text.translatable("$MOD_ID.game.host")
        private val ttReady = Text.translatable("$MOD_ID.gui.button.ready")
        private val ttNotReady = Text.translatable("$MOD_ID.gui.button.not_ready")
        private val ttEmpty = Text.translatable("$MOD_ID.game.empty")

        init {
            this.setSize(WIDTH, HEIGHT)
        }

        private var face: WWidget =
            faceWidget(x = 0, y = 0, width = 22, height = 22, isBot = isBot, uuid = null, name = null)

        private val name: WLabel = label(
            x = face.x + face.width + 6,
            y = face.y + face.height - fontHeight,
            text = Text.of(entityName)
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
                            Identifier.of("minecraft:textures/item/structure_void.png")
                        )
                    }
                }
            }
            if (number != 0) {
                numberAndReady.text = if (entityName.isNotEmpty()) {
                    Text.literal("") + playerNum + " (" + (if (ready) ttReady else ttNotReady) + ")"
                } else {
                    playerNum
                }
            }
            name.text = when {
                entityName.isNotEmpty() ->
                    if (!isBot) Text.of(entityName)
                    else Text.translatable("entity.$MOD_ID.mahjong_bot")
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
                name: String?,
            ): WWidget = when {
                isBot -> botFace(x, y, width, height)
                uuid != null && name != null -> playerFace(x, y, width, height, uuid, name)
                else -> image(x, y, width, height, Identifier.of("minecraft:textures/item/structure_void.png"))
            }
        }
    }
}