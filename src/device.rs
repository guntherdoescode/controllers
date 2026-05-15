// this is for the virtual ps controller device

use anyhow::Result;
use evdev::uinput::VirtualDevice;
use evdev::{AbsInfo, AbsoluteAxisCode, AttributeSet, BusType, EventType, InputEvent, InputId, KeyCode, UinputAbsSetup};

use crate::protocol::{ButtonMask, GamepadState};

pub struct PsControllerDevice {
    device: evdev::uinput::VirtualDevice,
}

impl PsControllerDevice {
    // create the virtual device
    pub fn create() -> Result<Self> {
        let id = InputId::new(BusType::BUS_USB, 0x054C, 0x0CE6, 0x0100);

        let axis_stick = UinputAbsSetup::new(
            AbsoluteAxisCode::ABS_X,
            AbsInfo::new(32768, 0, 65535, 0, 4096, 0),
        );
        let axis_stick_y = UinputAbsSetup::new(
            AbsoluteAxisCode::ABS_Y,
            AbsInfo::new(32768, 0, 65535, 0, 4096, 0),
        );
        let axis_rstick_x = UinputAbsSetup::new(
            AbsoluteAxisCode::ABS_RX,
            AbsInfo::new(32768, 0, 65535, 0, 4096, 0),
        );
        let axis_rstick_y = UinputAbsSetup::new(
            AbsoluteAxisCode::ABS_RY,
            AbsInfo::new(32768, 0, 65535, 0, 4096, 0),
        );
        let axis_trigger_l = UinputAbsSetup::new(
            AbsoluteAxisCode::ABS_Z,
            AbsInfo::new(0, 0, 255, 0, 0, 0),
        );
        let axis_trigger_r = UinputAbsSetup::new(
            AbsoluteAxisCode::ABS_RZ,
            AbsInfo::new(0, 0, 255, 0, 0, 0),
        );
        let axis_dpad_x = UinputAbsSetup::new(
            AbsoluteAxisCode::ABS_HAT0X,
            AbsInfo::new(0, -1, 1, 0, 0, 0),
        );
        let axis_dpad_y = UinputAbsSetup::new(
            AbsoluteAxisCode::ABS_HAT0Y,
            AbsInfo::new(0, -1, 1, 0, 0, 0),
        );

        let mut keys = AttributeSet::<KeyCode>::new();
        keys.insert(KeyCode::BTN_SOUTH);
        keys.insert(KeyCode::BTN_EAST);
        keys.insert(KeyCode::BTN_WEST);
        keys.insert(KeyCode::BTN_NORTH);
        keys.insert(KeyCode::BTN_TL);
        keys.insert(KeyCode::BTN_TR);
        keys.insert(KeyCode::BTN_TL2);
        keys.insert(KeyCode::BTN_TR2);
        keys.insert(KeyCode::BTN_SELECT);
        keys.insert(KeyCode::BTN_START);
        keys.insert(KeyCode::BTN_MODE);
        keys.insert(KeyCode::BTN_THUMBL);
        keys.insert(KeyCode::BTN_THUMBR);

        let device = VirtualDevice::builder()?
            .name("PlayStation Controller")
            .input_id(id)
            .with_absolute_axis(&axis_stick)?
            .with_absolute_axis(&axis_stick_y)?
            .with_absolute_axis(&axis_rstick_x)?
            .with_absolute_axis(&axis_rstick_y)?
            .with_absolute_axis(&axis_trigger_l)?
            .with_absolute_axis(&axis_trigger_r)?
            .with_absolute_axis(&axis_dpad_x)?
            .with_absolute_axis(&axis_dpad_y)?
            .with_keys(&keys)?
            .build()?;

        Ok(Self { device })
    }

    // emit events to the virtual device
    pub fn emit(&mut self, state: &GamepadState) -> Result<()> {
        let events = self.build_events(state);
        self.device.emit(&events)?;
        Ok(())
    }

    // helper to map stick values to axis
    fn stick_to_axis(value: i16) -> i32 {
        (value as i32 + 32768).clamp(0, 65535)
    }

    // helper to create key events
    fn key_event(code: u16, pressed: bool) -> InputEvent {
        InputEvent::new(EventType::KEY.0, code, if pressed { 1 } else { 0 })
    }

    // helper to create abs events
    fn abs_event(code: u16, value: i32) -> InputEvent {
        InputEvent::new(EventType::ABSOLUTE.0, code, value)
    }

    // build input events from gamepad state
    fn build_events(&self, state: &GamepadState) -> Vec<InputEvent> {
        let mut events = Vec::with_capacity(20);

        events.push(Self::key_event(KeyCode::BTN_SOUTH.0, state.buttons.contains(ButtonMask::CROSS)));
        events.push(Self::key_event(KeyCode::BTN_EAST.0, state.buttons.contains(ButtonMask::CIRCLE)));
        events.push(Self::key_event(KeyCode::BTN_WEST.0, state.buttons.contains(ButtonMask::SQUARE)));
        events.push(Self::key_event(KeyCode::BTN_NORTH.0, state.buttons.contains(ButtonMask::TRIANGLE)));
        events.push(Self::key_event(KeyCode::BTN_TL.0, state.buttons.contains(ButtonMask::L1)));
        events.push(Self::key_event(KeyCode::BTN_TR.0, state.buttons.contains(ButtonMask::R1)));
        events.push(Self::key_event(KeyCode::BTN_TL2.0, state.buttons.contains(ButtonMask::L2)));
        events.push(Self::key_event(KeyCode::BTN_TR2.0, state.buttons.contains(ButtonMask::R2)));
        events.push(Self::key_event(KeyCode::BTN_SELECT.0, state.buttons.contains(ButtonMask::SHARE)));
        events.push(Self::key_event(KeyCode::BTN_START.0, state.buttons.contains(ButtonMask::OPTIONS)));
        events.push(Self::key_event(KeyCode::BTN_MODE.0, state.buttons.contains(ButtonMask::PS)));
        events.push(Self::key_event(KeyCode::BTN_THUMBL.0, state.buttons.contains(ButtonMask::L3)));
        events.push(Self::key_event(KeyCode::BTN_THUMBR.0, state.buttons.contains(ButtonMask::R3)));

        events.push(Self::abs_event(AbsoluteAxisCode::ABS_X.0, Self::stick_to_axis(state.left_stick_x)));
        events.push(Self::abs_event(AbsoluteAxisCode::ABS_Y.0, Self::stick_to_axis(state.left_stick_y)));
        events.push(Self::abs_event(AbsoluteAxisCode::ABS_RX.0, Self::stick_to_axis(state.right_stick_x)));
        events.push(Self::abs_event(AbsoluteAxisCode::ABS_RY.0, Self::stick_to_axis(state.right_stick_y)));
        events.push(Self::abs_event(AbsoluteAxisCode::ABS_Z.0, state.left_trigger as i32));
        events.push(Self::abs_event(AbsoluteAxisCode::ABS_RZ.0, state.right_trigger as i32));

        let dpad_x = if state.buttons.contains(ButtonMask::DPAD_RIGHT) {
            1
        } else if state.buttons.contains(ButtonMask::DPAD_LEFT) {
            -1
        } else {
            0
        };
        let dpad_y = if state.buttons.contains(ButtonMask::DPAD_DOWN) {
            1
        } else if state.buttons.contains(ButtonMask::DPAD_UP) {
            -1
        } else {
            0
        };

        events.push(Self::abs_event(AbsoluteAxisCode::ABS_HAT0X.0, dpad_x));
        events.push(Self::abs_event(AbsoluteAxisCode::ABS_HAT0Y.0, dpad_y));

        events
    }
}
