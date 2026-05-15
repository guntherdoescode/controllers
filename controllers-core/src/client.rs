use std::net::UdpSocket;
use std::sync::Mutex;

use crate::protocol;

// static socket, shared across threads
static SOCKET: Mutex<Option<UdpSocket>> = Mutex::new(None);

// connect to the server with udp
pub fn connect(addr: &str) -> Result<(), String> {
    let socket = UdpSocket::bind("0.0.0.0:0").map_err(|e| format!("bind: {e}"))?;
    socket.connect(addr).map_err(|e| format!("connect: {e}"))?;
    socket.set_nonblocking(false).map_err(|e| format!("set_nonblocking: {e}"))?;

    let mut guard = SOCKET.lock().map_err(|e| format!("lock: {e}"))?;
    *guard = Some(socket);
    Ok(())
}

// disconnect, just clear the socket
pub fn disconnect() {
    if let Ok(mut guard) = SOCKET.lock() {
        *guard = None;
    }
}

// check if its connected
pub fn is_connected() -> bool {
    SOCKET.lock().map(|g| g.is_some()).unwrap_or(false)
}

// send the gamepad state, hope it doesn't drop packets
pub fn send(
    buttons: u32,
    left_stick_x: i16,
    left_stick_y: i16,
    right_stick_x: i16,
    right_stick_y: i16,
    left_trigger: u8,
    right_trigger: u8,
) -> Result<(), String> {
    let guard = SOCKET.lock().map_err(|e| format!("lock: {e}"))?;
    let socket = guard.as_ref().ok_or("not connected")?;

    let state = protocol::GamepadState {
        buttons,
        left_stick_x,
        left_stick_y,
        right_stick_x,
        right_stick_y,
        left_trigger,
        right_trigger,
    };

    let buf = protocol::encode(&state);
    socket.send(&buf).map_err(|e| format!("send: {e}"))?;
    Ok(())
}
