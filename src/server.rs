use anyhow::Result;
use tokio::net::UdpSocket;
use tracing::{error, info, warn};

use crate::device::PsControllerDevice;
use crate::protocol::GamepadState;

// this udp server handles packets

// run the server
pub async fn run_server(device: &mut PsControllerDevice, bind_addr: &str) -> Result<()> {
    // bind the socket and start listening
    let socket = UdpSocket::bind(bind_addr).await?;
    info!("Listening on {}", socket.local_addr()?);

    let mut buf = vec![0u8; 65535];

    loop {
        // receive data, check if valid
        let (len, addr) = socket.recv_from(&mut buf).await?;
        let data = &buf[..len];

        match GamepadState::decode(data) {
            Ok(state) => {
                // emit events to the virtual device
                if let Err(e) = device.emit(&state) {
                    error!("failed to emit events: {e}");
                }
            }
            Err(e) => {
                // log invalid packets
                warn!("invalid packet from {addr}: {e}");
            }
        }
    }
}
