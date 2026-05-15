This project is Controlle.rs, it emulates a PS5 dualsense controller on linux wirelessly from any Android device.

# How it works:
Controlle.rs works by turning your Android device into a wireless PlayStation 5 controller for your Linux desktop. The app communicates with a lightweight Rust-based server running on your desktop using a custom UDP protocol. This protocol sends compact 17-byte packets containing the state of the controller, including button presses, analog stick positions, and trigger values.

The Android app captures your input in real-time, encodes it, and transmits it over Wi-Fi using your PC's IP. The server receives these packets, decodes them, and injects the input events directly into the Linux kernel using the `uinput` subsystem. This allows the virtual controller to be recognized by the operating system and games as a standard PlayStation controller.

### Features
- **Low Latency:** The system is optimized for minimal input lag, achieving under 10ms round-trip time on a 5GHz Wi-Fi network.
- **Full Controller Support:** Includes all PlayStation buttons, dual analog sticks, pressure-sensitive triggers, and even experimental motion controls using your phone's gyroscope.
- **Plug-and-Play:** The virtual controller is automatically recognized by most games and platforms, including Steam and Proton/Wine.
- **Automatic Discovery:** The app uses mDNS to find the desktop server on your local network, making setup quick and easy.

### How to Connect
1. **Start the Server:** Run the Rust server on your Linux desktop. By default, it listens on `0.0.0.0:42069`.
2. **Install the App:** Install the Android app on your phone.
3. **Connect:** Open the app, enter your desktop's IP address, and tap "Connect." The app will indicate when the connection is successful.
4. **Play:** Once connected, your phone acts as a fully functional controller.

# Tested on:
1. Minecraft Java (with a controller mod)
2. GTA: San Andreas (don't ask why I tested it on here, I just did)
3. SuperTuxKart (though, the mapping tends to be half broken, so accelerate is just the Square button. But if it runs, it runs.)

# Note to self:
Some features will obviously not work since it's still in beta so expect to see some bugs!
More info in [here](STEP_BY_STEP.md) on how to set it up.