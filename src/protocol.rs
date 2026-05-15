use anyhow::{bail, Result};

// constants for the protocol, using magic bytes
pub const MAGIC: [u8; 2] = [0x43, 0x52];
// protocol version / just one version for now
pub const PROTOCOL_VERSION: u8 = 0x01;
// size of the packet in bytes
pub const PACKET_SIZE: usize = 17;

// bitflags for the button mask, each button is a bit
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

// gamepad state struct, holds all the data
#[derive(Debug, Clone, Copy)]
pub struct GamepadState {
    // bitmask of buttons that are currently pressed
    pub buttons: ButtonMask,
    // x and y axis values for the left stick
    pub left_stick_x: i16,
    pub left_stick_y: i16,
    // x and y axis values for the right stick
    pub right_stick_x: i16,
    pub right_stick_y: i16,
    // value of the left trigger
    pub left_trigger: u8,
    // value of the right trigger
    pub right_trigger: u8,
}

impl GamepadState {
    // decode bytes to gamepad state
    pub fn decode(buf: &[u8]) -> Result<Self> {
        // check if the buffer is at least the size of a packet
        if buf.len() < PACKET_SIZE {
            bail!("packet too short: {} < {}", buf.len(), PACKET_SIZE);
        }
        // check magic bytes
        if buf[0] != MAGIC[0] || buf[1] != MAGIC[1] {
            bail!("invalid magic");
        }
        // check protocol version
        if buf[2] != PROTOCOL_VERSION {
            bail!("unsupported version: {}", buf[2]);
        }

        // extract fields from the buffer
        let buttons = ButtonMask::from_bits_truncate(u32::from_le_bytes([buf[3], buf[4], buf[5], buf[6]]));
        let left_stick_x = i16::from_be_bytes([buf[7], buf[8]]);
        let left_stick_y = i16::from_be_bytes([buf[9], buf[10]]);
        let right_stick_x = i16::from_be_bytes([buf[11], buf[12]]);
        let right_stick_y = i16::from_be_bytes([buf[13], buf[14]]);
        let left_trigger = buf[15];
        let right_trigger = buf[16];

        // return the new gamepad state
        Ok(Self {
            buttons,
            left_stick_x,
            left_stick_y,
            right_stick_x,
            right_stick_y,
            left_trigger,
            right_trigger,
        })
    }

    // encode the gamepad state to bytes
    #[allow(dead_code)]
    pub fn encode(&self) -> [u8; PACKET_SIZE] {
        let mut buf = [0u8; PACKET_SIZE];
        // set magic bytes
        buf[0] = MAGIC[0];
        buf[1] = MAGIC[1];
        // set protocol version
        buf[2] = PROTOCOL_VERSION;
        // copy button bits
        buf[3..7].copy_from_slice(&self.buttons.bits().to_le_bytes());
        // copy stick and trigger values
        buf[7..9].copy_from_slice(&self.left_stick_x.to_be_bytes());
        buf[9..11].copy_from_slice(&self.left_stick_y.to_be_bytes());
        buf[11..13].copy_from_slice(&self.right_stick_x.to_be_bytes());
        buf[13..15].copy_from_slice(&self.right_stick_y.to_be_bytes());
        buf[15] = self.left_trigger;
        buf[16] = self.right_trigger;
        buf
    }
}
