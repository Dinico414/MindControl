package com.xenonware.mindcontrol

import android.view.KeyCharacterMap
import android.view.KeyEvent

/**
 * Single source of truth for the "Key input" action, shared by the picker UI and the service.
 *
 * Stored format: KEY_INPUT:<shift>,<ctrl>,<alt>||<key name>
 * e.g. "KEY_INPUT:1,0,0||a" = Shift + A, "KEY_INPUT:0,0,0||Up" = Arrow up
 */
object KeyInput {

    data class Spec(val shift: Boolean, val ctrl: Boolean, val alt: Boolean, val key: String)

    fun encode(spec: Spec): String {
        fun bit(v: Boolean) = if (v) "1" else "0"
        return "${SettingsManager.PREFIX_KEY_INPUT}${bit(spec.shift)},${bit(spec.ctrl)},${bit(spec.alt)}||${spec.key}"
    }

    fun decode(action: String): Spec? {
        if (!action.startsWith(SettingsManager.PREFIX_KEY_INPUT)) return null
        val data = action.removePrefix(SettingsManager.PREFIX_KEY_INPUT)
        val parts = data.split("||", limit = 2)
        val mods = parts.getOrNull(0)?.split(",").orEmpty()
        return Spec(
            shift = mods.getOrNull(0) == "1",
            ctrl = mods.getOrNull(1) == "1",
            alt = mods.getOrNull(2) == "1",
            key = parts.getOrNull(1).orEmpty()
        )
    }

    /** Turns raw text-field input into a stored key name (a typed " " becomes "Space"). */
    fun normalizeName(input: String): String =
        if (input.isNotEmpty() && input.isBlank()) "Space" else input.trim()

    private val aliases: Map<String, Int> = mapOf(
        "space" to KeyEvent.KEYCODE_SPACE,
        "enter" to KeyEvent.KEYCODE_ENTER,
        "return" to KeyEvent.KEYCODE_ENTER,
        "backspace" to KeyEvent.KEYCODE_DEL,
        "bksp" to KeyEvent.KEYCODE_DEL,
        "delete" to KeyEvent.KEYCODE_FORWARD_DEL,
        "del" to KeyEvent.KEYCODE_FORWARD_DEL,
        "tab" to KeyEvent.KEYCODE_TAB,
        "shift_tab" to KeyEvent.KEYCODE_TAB,
        "reversetab" to KeyEvent.KEYCODE_TAB,
        "reverse_tab" to KeyEvent.KEYCODE_TAB,
        "esc" to KeyEvent.KEYCODE_ESCAPE,
        "escape" to KeyEvent.KEYCODE_ESCAPE,
        // "Home"/"End" mean cursor movement here, not the system Home button
        "home" to KeyEvent.KEYCODE_MOVE_HOME,
        "end" to KeyEvent.KEYCODE_MOVE_END,
        "pgup" to KeyEvent.KEYCODE_PAGE_UP,
        "pageup" to KeyEvent.KEYCODE_PAGE_UP,
        "page_up" to KeyEvent.KEYCODE_PAGE_UP,
        "pgdn" to KeyEvent.KEYCODE_PAGE_DOWN,
        "pagedown" to KeyEvent.KEYCODE_PAGE_DOWN,
        "page_down" to KeyEvent.KEYCODE_PAGE_DOWN,
        "insert" to KeyEvent.KEYCODE_INSERT,
        "ins" to KeyEvent.KEYCODE_INSERT,
        "up" to KeyEvent.KEYCODE_DPAD_UP,
        "↑" to KeyEvent.KEYCODE_DPAD_UP,
        "arrow_up" to KeyEvent.KEYCODE_DPAD_UP,
        "dpad_up" to KeyEvent.KEYCODE_DPAD_UP,
        "down" to KeyEvent.KEYCODE_DPAD_DOWN,
        "↓" to KeyEvent.KEYCODE_DPAD_DOWN,
        "arrow_down" to KeyEvent.KEYCODE_DPAD_DOWN,
        "dpad_down" to KeyEvent.KEYCODE_DPAD_DOWN,
        "left" to KeyEvent.KEYCODE_DPAD_LEFT,
        "←" to KeyEvent.KEYCODE_DPAD_LEFT,
        "arrow_left" to KeyEvent.KEYCODE_DPAD_LEFT,
        "dpad_left" to KeyEvent.KEYCODE_DPAD_LEFT,
        "right" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "→" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "arrow_right" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "dpad_right" to KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    private val symbols: Map<Char, Int> = mapOf(
        '.' to KeyEvent.KEYCODE_PERIOD,
        ',' to KeyEvent.KEYCODE_COMMA,
        '-' to KeyEvent.KEYCODE_MINUS,
        '=' to KeyEvent.KEYCODE_EQUALS,
        '[' to KeyEvent.KEYCODE_LEFT_BRACKET,
        ']' to KeyEvent.KEYCODE_RIGHT_BRACKET,
        '\\' to KeyEvent.KEYCODE_BACKSLASH,
        ';' to KeyEvent.KEYCODE_SEMICOLON,
        '\'' to KeyEvent.KEYCODE_APOSTROPHE,
        '/' to KeyEvent.KEYCODE_SLASH,
        '`' to KeyEvent.KEYCODE_GRAVE,
        '@' to KeyEvent.KEYCODE_AT,
        '+' to KeyEvent.KEYCODE_PLUS,
        '*' to KeyEvent.KEYCODE_STAR,
        '#' to KeyEvent.KEYCODE_POUND,
    )

    private val shiftableSymbolKeys = setOf(
        KeyEvent.KEYCODE_COMMA, KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_GRAVE,
        KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_LEFT_BRACKET,
        KeyEvent.KEYCODE_RIGHT_BRACKET, KeyEvent.KEYCODE_BACKSLASH, KeyEvent.KEYCODE_SEMICOLON,
        KeyEvent.KEYCODE_APOSTROPHE, KeyEvent.KEYCODE_SLASH,
    )

    val arrowKeyCodes = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    /** Resolves "a", "1", "/", "Space", "F5", "Up", "PgDn", "MEDIA_PLAY" … to a key code (0 = unknown). */
    fun resolveKeyCode(name: String): Int {
        if (name.isNotEmpty() && name.isBlank()) return KeyEvent.KEYCODE_SPACE
        val clean = name.trim()
        if (clean.isEmpty()) return KeyEvent.KEYCODE_UNKNOWN

        if (clean.length == 1) {
            val c = clean[0]
            if (c in '0'..'9') return KeyEvent.KEYCODE_0 + (c - '0')
            val lower = c.lowercaseChar()
            if (lower in 'a'..'z') return KeyEvent.KEYCODE_A + (lower - 'a')
            symbols[c]?.let { return it }
        }

        val normalized = clean.lowercase().replace(' ', '_')
        aliases[normalized]?.let { return it }
        return KeyEvent.keyCodeFromString("KEYCODE_" + normalized.uppercase())
    }

    fun isValid(name: String): Boolean = resolveKeyCode(name) != KeyEvent.KEYCODE_UNKNOWN

    fun isLetter(keyCode: Int): Boolean = keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z

    /** Keys whose character changes with Shift (letters, digits, punctuation). */
    fun isShiftable(keyCode: Int): Boolean =
        isLetter(keyCode) || keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 || keyCode in shiftableSymbolKeys

    /** The character a key produces on the virtual keyboard map, or null if it doesn't type anything. */
    fun charFor(keyCode: Int, metaState: Int): String? {
        val ch = try {
            KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).get(keyCode, metaState)
        } catch (_: Exception) {
            0
        }
        if (ch == 0 || (ch and KeyCharacterMap.COMBINING_ACCENT) != 0) return null
        val s = String(Character.toChars(ch))
        if (s.length == 1 && s[0].isISOControl() && s[0] != '\t' && s[0] != '\n') return null
        return s
    }
}