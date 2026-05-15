mod client;
mod protocol;

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jbyte, jint, jshort};
use jni::JNIEnv;

// this is the native bridge, connects kotlin to rust

// connect function, called from java
#[no_mangle]
pub extern "system" fn Java_com_controllers_app_NativeBridge_connect(
    mut env: JNIEnv,
    _class: JClass,
    addr: JString,
    port: jint,
) -> jboolean {
    let addr_str: String = match env.get_string(&addr) {
        Ok(s) => s.into(),
        Err(e) => {
            log::error!("failed to get addr string: {e}");
            return 0;
        }
    };

    let full_addr = format!("{addr_str}:{port}");
    match client::connect(&full_addr) {
        Ok(()) => {
            log::info!("connected to {full_addr}");
            1
        }
        Err(e) => {
            log::error!("connection failed: {e}");
            0
        }
    }
}

// disconnect function, called from java
#[no_mangle]
pub extern "system" fn Java_com_controllers_app_NativeBridge_disconnect(
    _env: JNIEnv,
    _class: JClass,
) {
    client::disconnect();
    log::info!("disconnected");
}

// send gamepad state, called from java
#[no_mangle]
pub extern "system" fn Java_com_controllers_app_NativeBridge_sendGamepadState(
    _env: JNIEnv,
    _class: JClass,
    buttons: jint,
    left_stick_x: jshort,
    left_stick_y: jshort,
    right_stick_x: jshort,
    right_stick_y: jshort,
    left_trigger: jbyte,
    right_trigger: jbyte,
) -> jboolean {
    match client::send(
        buttons as u32,
        left_stick_x as i16,
        left_stick_y as i16,
        right_stick_x as i16,
        right_stick_y as i16,
        left_trigger as u8,
        right_trigger as u8,
    ) {
        Ok(()) => 1,
        Err(e) => {
            log::error!("send failed: {e}");
            0
        }
    }
}

// check connection status, called from java
#[no_mangle]
pub extern "system" fn Java_com_controllers_app_NativeBridge_isConnected(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    match client::is_connected() {
        true => 1,
        false => 0,
    }
}
