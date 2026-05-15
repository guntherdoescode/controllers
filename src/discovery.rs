use std::collections::HashMap;

use anyhow::Result;
use mdns_sd::{ServiceDaemon, ServiceInfo};
use tracing::info;

const SERVICE_TYPE: &str = "_controllers._udp.local.";
const SERVICE_NAME: &str = "Controlle.rs Server";

// this is for mdns, service discovery
pub struct MdnsService {
    daemon: ServiceDaemon,
}

impl MdnsService {
    // create a new mdns service
    pub fn new() -> Result<Self> {
        let daemon = ServiceDaemon::new()?;
        Ok(Self { daemon })
    }

    // register the service, and announce it
    pub fn register(&self, hostname: &str, port: u16) -> Result<()> {
        let mut props = HashMap::new();
        props.insert("device".to_string(), "PlayStation Controller".to_string());
        props.insert("vendor".to_string(), "Sony".to_string());
        props.insert("protocol".to_string(), "1.0".to_string());

        let service = ServiceInfo::new(
            SERVICE_TYPE,
            SERVICE_NAME,
            hostname,
            "",
            port,
            Some(props),
        )?;

        self.daemon.register(service)?;
        info!("mDNS service registered: {SERVICE_NAME} on port {port}");
        Ok(())
    }

    // shutdown the service, cleanup
    #[allow(dead_code)]
    pub fn shutdown(self) {
        drop(self.daemon);
    }
}
