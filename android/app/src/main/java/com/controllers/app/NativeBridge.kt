package com.controllers.app

object NativeBridge {
    // this loads the native lib, hope it doesn't crash
    init {
        System.loadLibrary("controllers_core")
    }

    // connect to the native side
    external fun connect(addr: String, port: Int): Boolean

    // disconnect from native side
    external fun disconnect()

    // check if its connected
    external fun isConnected(): Boolean

    // send the gamepad state
    external fun sendGamepadState(
        buttons: Int,
        leftStickX: Short,
        leftStickY: Short,
        rightStickX: Short,
        rightStickY: Short,
        leftTrigger: Byte,
        rightTrigger: Byte
    ): Boolean
}
