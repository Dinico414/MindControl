package com.xenonware.mindcontrol

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ButtonState {
    private val _pressedKeys = MutableStateFlow<Set<Int>>(emptySet())
    val pressedKeys: StateFlow<Set<Int>> = _pressedKeys.asStateFlow()

    fun setKeyPressed(keyCode: Int, isPressed: Boolean) {
        _pressedKeys.value = if (isPressed) {
            _pressedKeys.value + keyCode
        } else {
            _pressedKeys.value - keyCode
        }
    }
}
