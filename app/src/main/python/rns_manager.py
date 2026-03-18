import RNS
import os

def start_mesh(config_path):
    # Ensure the config directory exists in the app's internal storage
    if not os.path.exists(config_path):
        os.makedirs(config_path)

    # Programmatically create an RNS configuration
    # We tell RNS to connect to our local Kotlin Bluetooth-to-TCP bridge
    rns_config = f"""
[reticulum]
enable_transport = True
share_instance = True

[interfaces]
  [[Local BT Bridge]]
    type = TCPClientInterface
    enabled = True
    target_host = 127.0.0.1
    target_port = 50001
"""
    
    with open(config_path + "/config", "w") as f:
        f.write(rns_config)

    # Initialize Reticulum
    reticulum = RNS.Reticulum(config_path)
    return f"RNS {RNS.__version__} started on 127.0.0.1:50001"