package doublemoon.mahjongcraft.client.gui.screen

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.blockentity.MahjongTableBlockEntity
import doublemoon.mahjongcraft.client.gui.widget.*
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongRule
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTableBehavior
import doublemoon.mahjongcraft.network.mahjong_table.MahjongTablePayload
import doublemoon.mahjongcraft.network.sendPayloadToServer
import doublemoon.mahjongcraft.util.TextFormatting
import io.github.cottonmc.cotton.gui.client.CottonClientScreen
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription
import io.github.cottonmc.cotton.gui.widget.WButton
import io.github.cottonmc.cotton.gui.widget.WPlainPanel
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

@Environment(EnvType.CLIENT)
class RuleEditorScreen(
    mahjongTable: MahjongTableBlockEntity,
) : CottonClientScreen(RuleEditorGui(mahjongTable)) {
    override fun shouldPause(): Boolean = false
    override fun tick() {
        super.tick()
        (description as RuleEditorGui).tick()
    }
}

@Environment(EnvType.CLIENT)
class RuleEditorGui(
    private val mahjongTable: MahjongTableBlockEntity,
) : LightweightGuiDescription() {

    private val client = MinecraftClient.getInstance()

    //編輯中的設定
    private val editingRule: MahjongRule = mahjongTable.rule.copy()
    private val lengthItem = RuleSelectItem(
        name = Text.translatable("$MOD_ID.game.length"),
        value = { editingRule.length },
        label = { it.toText() },
        tooltip = { length -> getTooltip(length) }
    ) {
        val values = MahjongRule.GameLength.entries.toTypedArray()
        val nextValue = values[(editingRule.length.ordinal + 1) % values.size]
        editingRule.length = nextValue
        editingRule.startingPoints = editingRule.length.startingPoints
    }
    private val thinkingTimeItem = RuleSelectItem(
        name = Text.translatable("$MOD_ID.game.thinking_time"),
        value = { editingRule.thinkingTime },
        label = { it.toText() },
        tooltip = { thinkingTime -> getTooltip(thinkingTime) }
    ) {
        val values = MahjongRule.ThinkingTime.entries.toTypedArray()
        val nextValue = values[(editingRule.thinkingTime.ordinal + 1) % values.size]
        editingRule.thinkingTime = nextValue
    }

    private val minimumHanItem = RuleSelectItem(
        name = Text.translatable("$MOD_ID.game.minimum_han"),
        value = { editingRule.minimumHan },
        label = { it.toText() },
        tooltip = { minimumHan -> getTooltip(minimumHan) }
    ) {
        val values = MahjongRule.MinimumHan.entries.toTypedArray()
        val nextValue = values[(editingRule.minimumHan.ordinal + 1) % values.size]
        editingRule.minimumHan = nextValue
    }
    private val spectateItem = RuleToggleItem(
        name = Text.translatable("$MOD_ID.game.spectate"),
        enabled = editingRule.spectate,
        onClick = { editingRule.spectate = it }
    )
    private val redFiveItem = RuleSelectItem(
        name = Text.translatable("$MOD_ID.game.red_five"),
        value = { editingRule.redFive },
        label = { it.toText() },
        tooltip = { redFive -> getTooltip(redFive) }
    ) {
        val values = MahjongRule.RedFive.entries.toTypedArray()
        val nextValue = values[(editingRule.redFive.ordinal + 1) % values.size]
        editingRule.redFive = nextValue
    }
    private val openTanyaoItem = RuleToggleItem(
        name = Text.translatable("$MOD_ID.game.open_tanyao"),
        enabled = editingRule.openTanyao,
        onClick = { editingRule.openTanyao = it }
    )

    private val items = listOf(
        lengthItem,
        thinkingTimeItem,
        minimumHanItem,
        spectateItem,
        redFiveItem,
        openTanyaoItem
    )

    private lateinit var back: WButton
    private lateinit var confirm: WTooltipButton
    private lateinit var apply: WTooltipButton

    init {
        rootPlainPanel(width = ROOT_WIDTH, height = ROOT_HEIGHT) {
            scrollPanel(x = INSET, y = INSET, width = ROOT_WIDTH - INSET * 2, height = ROOT_HEIGHT - INSET * 2) {
                plainPanel(x = 24, y = 0) {
                    items.forEachIndexed { index, item ->
                        val previousItem = if (index > 0) items[index - 1] else null
                        val previousItemY = previousItem?.y ?: 0
                        val previousItemHeight = previousItem?.height ?: 0
                        val y = previousItemY + previousItemHeight + ITEM_PADDING
                        this.add(item, 0, y)
                    }
                }
            }
            back = button(
                x = ROOT_WIDTH - INSET * 2 - BUTTON_WIDTH - INSET,
                y = ROOT_HEIGHT - INSET * 2 - INSET * 2,
                width = BUTTON_WIDTH,
                label = Text.translatable("$MOD_ID.gui.button.back"),
                onClick = { back() }
            )
            confirm = tooltipButton(
                x = back.x,
                y = back.y - back.height - BUTTON_PADDING,
                width = BUTTON_WIDTH,
                label = Text.translatable("$MOD_ID.gui.button.confirm"),
                tooltip = arrayOf(),
                onClick = { confirm() }
            ) {
                isEnabled = false
            }
            apply = tooltipButton(
                x = confirm.x,
                y = confirm.y - confirm.height - BUTTON_PADDING,
                width = BUTTON_WIDTH,
                label = Text.translatable("$MOD_ID.gui.button.apply"),
                tooltip = arrayOf(),
                onClick = { apply() }
            ) {
                isEnabled = false
            }
        }
    }

    fun tick() {
        confirm.isEnabled = confirmEnabled
        apply.isEnabled = confirm.isEnabled
    }

    private val confirmEnabled: Boolean
        get() {
            val origin = mahjongTable.rule
            val lengthDiff = origin.length != editingRule.length
            val thinkingTimeDiff = origin.thinkingTime != editingRule.thinkingTime
            val minimumHanDiff = origin.minimumHan != editingRule.minimumHan
            val spectateDiff = origin.spectate != editingRule.spectate
            val redFiveDiff = origin.redFive != editingRule.redFive
            val openTanyaoDiff = origin.openTanyao != editingRule.openTanyao
            return lengthDiff || thinkingTimeDiff || minimumHanDiff || spectateDiff || redFiveDiff || openTanyaoDiff
        }

    private fun apply() {
        sendPayloadToServer(
            payload = MahjongTablePayload(
                behavior = MahjongTableBehavior.CHANGE_RULE,
                pos = mahjongTable.pos,
                extraData = editingRule.toJsonString()
            )
        )
    }

    private fun confirm() {
        apply()
        back()
    }

    private fun back() {
        client.setScreen(MahjongTableWaitingScreen(mahjongTable = mahjongTable))
    }

    companion object {
        //背景圖片的大小
        private const val ROOT_WIDTH = 400
        private const val ROOT_HEIGHT = 200
        private const val INSET = 8
        private const val ITEM_PADDING = 8
        private const val BUTTON_WIDTH = 80
        private const val BUTTON_HEIGHT = 20
        private const val BUTTON_PADDING = 5

        private inline fun <reified T : Enum<T>> getTooltip(nowValue: T): Array<Text> =
            enumValues<T>().map {
                if (it is TextFormatting) {
                    it.toText().also { text ->
                        if (text is MutableText) {
                            val color = if (it == nowValue) Formatting.GREEN else Formatting.RED
                            text.formatted(color)
                        }
                    }
                } else {
                    Text.of(it.name)
                }
            }.toTypedArray()
    }


    abstract class SettingItem(
        name: Text,
    ) : WPlainPanel() {

        init {
            this.setSize(WIDTH, HEIGHT)
        }

        val text = text(
            x = 0,
            y = 0,
            width = TEXT_WIDTH,
            horizontalAlignment = HorizontalAlignment.RIGHT,
            verticalAlignment = VerticalAlignment.CENTER,
            text = name
        )
        val space = plainPanel(
            x = text.x + text.width,
            y = text.y,
            width = SPACE_WIDTH,
            height = text.height
        )

        companion object {
            private const val TEXT_WIDTH = 120
            private const val SPACE_WIDTH = 16
            const val HEIGHT = 36
            const val WIDTH = 296
        }
    }

    class RuleSelectItem<T : Enum<T>>(
        val name: Text,
        val value: () -> T,
        private val label: (T) -> Text,
        private val tooltip: (T) -> Array<Text>,
        private val onSelect: RuleSelectItem<T>.() -> Unit,
    ) : SettingItem(name) {
        private val nowLabel: Text
            get() = label.invoke(value.invoke())
        private val nowTooltip: Array<Text>
            get() = tooltip.invoke(value.invoke())

        private val selectButton = tooltipButton(
            x = space.x + space.width,
            y = text.y + (text.height - BUTTON_HEIGHT) / 2,
            width = BUTTON_WIDTH,
            label = nowLabel,
            tooltip = nowTooltip,
            onClick = {
                onSelect.invoke(this@RuleSelectItem)
                refreshButtonLabel()
                refreshButtonTooltip()
            }
        )

        private fun refreshButtonLabel() {
            selectButton.label = nowLabel
        }

        private fun refreshButtonTooltip() {
            selectButton.tooltip = nowTooltip
        }
    }

    class RuleToggleItem(
        val name: Text,
        var enabled: Boolean,
        private val onClick: (Boolean) -> Unit,
    ) : SettingItem(name) {

        init {
            button(
                x = space.x + space.width,
                y = text.y + (text.height - BUTTON_HEIGHT) / 2,
                width = BUTTON_WIDTH,
                label = buttonText,
                onClick = {
                    enabled = !enabled
                    label = buttonText
                    this@RuleToggleItem.onClick.invoke(enabled)
                }
            )
        }

        private val buttonText: Text
            get() = if (enabled) Text.translatable("$MOD_ID.game.enabled") else Text.translatable("$MOD_ID.game.disabled")
    }

    class RuleIntegerTextFieldItem(
        val name: Text,
        description: Array<Text> = arrayOf(),
        defaultValue: Int,
    ) : SettingItem(name) {

        var hint: Text
            get() = hintLabel.text
            set(value) {
                hintLabel.text = value
            }

        /**
         * null 表示輸入的 [textField] 之中的值不是 [Int]
         * */
        var value: Int?
            get() = textField.text.toIntOrNull()
            set(value) {
                textField.text = (value?.toString() ?: "")
            }

        private val textField = tooltipTextField(
            x = space.x + space.width,
            y = text.y + (text.height - BUTTON_HEIGHT) / 2,
            width = BUTTON_WIDTH,
            text = "$defaultValue",
            tooltip = description,
            suggestion = Text.of("$defaultValue"),
        )

        private val hintLabel = label(
            x = textField.x + textField.width + space.width,
            y = textField.y,
            height = textField.height,
            verticalAlignment = VerticalAlignment.CENTER,
            text = Text.of("")
        )
    }
}
