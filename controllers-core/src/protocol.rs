pub const MAGIC: [u8; 2] = [0x43, 0x52];
pub const PROTOCOL_VERSION: u8 = 0x01;
pub const PACKET_SIZE: usize = 17;

bitflags::bitflags! {
    #[derive(Debug, Clone, Copy)]
    pub struct ButtonMask: u32 {
        const CROSS       = 1 << 0;
        const CIRCLE      = 1 << 1;
        const SQUARE      = 1 << 2;
        const TRIANGLE    = 1 << 3;
        const L1          = 1 << 4;
        const R1          = 1 << 5;
        const L2          = 1 << 6;
        const R2          = 1 << 7;
        const SHARE       = 1 << 8;
        const OPTIONS     = 1 << 9;
        const PS          = 1 << 10;
        const L3          = 1 << 11;
        const R3          = 1 << 12;
        const TOUCHPAD    = 1 << 13;
        const DPAD_UP     = 1 << 14;
        const DPAD_DOWN   = 1 << 15;
        const DPAD_LEFT   = 1 << 16;
        const DPAD_RIGHT  = 1 << 17;
    }
}

#[derive(Debug, Clone, Copy)]
pub struct GamepadState {
    pub buttons: u32,
    pub left_stick_x: i16,
    pub left_stick_y: i16,
    pub right_stick_x: i16,
    pub right_stick_y: i16,
    pub left_trigger: u8,
    pub right_trigger: u8,
}

pub fn encode(state: &GamepadState) -> [u8; PACKET_SIZE] {
    let mut buf = [0u8; PACKET_SIZE];
    buf[0] = MAGIC[0];
    buf[1] = MAGIC[1];
    buf[2] = PROTOCOL_VERSION;
    buf[3..7].copy_from_slice(&state.buttons.to_le_bytes());
    buf[7..9].copy_from_slice(&state.left_stick_x.to_be_bytes());
    buf[9..11].copy_from_slice(&state.left_stick_y.to_be_bytes());
    buf[11..13].copy_from_slice(&state.right_stick_x.to_be_bytes());
    buf[13..15].copy_from_slice(&state.right_stick_y.to_be_bytes());
    buf[15] = state.left_trigger;
    buf[16] = state.right_trigger;
    buf
}
