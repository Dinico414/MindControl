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

    /**
     * The active controller profile, shared with the UI.
     *
     * The service owns the value (it reacts to profile-switch keys), so the UI mirrors it here
     * instead of re-reading the setting. Without this, a profile change made from a button press
     * would leave the header showing the previous profile.
     */
    private val _gamepadProfile = MutableStateFlow(1)
    val gamepadProfile: StateFlow<Int> = _gamepadProfile.asStateFlow()

    fun setGamepadProfile(profile: Int) {
        _gamepadProfile.value = profile
    }
}
