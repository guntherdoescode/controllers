mod device;
mod discovery;
mod protocol;
mod server;

use anyhow::Result;
use clap::Parser;
use tracing::info;
use tracing_subscriber::EnvFilter;

// this is the main entry point, where it all begins
#[derive(Parser)]
#[command(name = "controllers", version, about = "High-performance virtual PlayStation controller server")]
struct Cli {
    #[arg(short, long, default_value = "0.0.0.0:42069")]
    bind: String,

    #[arg(short = 'n', long, default_value = "controllers")]
    hostname: String,

    #[arg(short, long, default_value_t = 42069)]
    port: u16,

    #[arg(short, long)]
    no_mdns: bool,
}

// main function
#[tokio::main]
async fn main() -> Result<()> {
    // setup logging so we can debug
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::from_default_env())
        .init();

    let cli = Cli::parse();

    info!("Controlle.rs v{} starting up", env!("CARGO_PKG_VERSION"));

    // create the virtual controller
    let mut controller = device::PsControllerDevice::create()?;
    info!("virtual PlayStation controller device created");

    if !cli.no_mdns {
        match discovery::MdnsService::new() {
            Ok(mdns) => {
                if let Err(e) = mdns.register(&cli.hostname, cli.port) {
                    tracing::warn!("failed to register mDNS service: {e}");
                }
                std::mem::forget(mdns);
            }
            Err(e) => {
                tracing::warn!("failed to start mDNS service: {e}");
            }
        }
    }

    // start the server
    server::run_server(&mut controller, &cli.bind).await
}
