# CommandRelayPlugin

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](#)  
[![Version](https://img.shields.io/badge/version-1.0-blue)](#)  
[![License](https://img.shields.io/badge/license-MIT-yellow)](#)  
[![Minecraft](https://img.shields.io/badge/minecraft-1.20+-brightgreen)](#)  

**CommandRelayPlugin** is a Minecraft server plugin that allows external applications to securely send and execute commands via TCP.  
It uses a **secret token**, an **IP whitelist**, and a list of **allowed commands** to ensure safe remote command execution.

---

## ✨ Features

- 🔑 **Token authentication** for every request.
- 🌐 **IP whitelist** to control which hosts can connect.
- ✅ **Allowed commands list** to prevent unauthorized execution.
- ⚡ Built-in **TCP server** with thread pooling for multiple clients.
- 📜 **Error logging** in `errors.log`.
- 🔄 Reloadable configuration (`/cr reload`).
- 🐛 Debug mode for detailed request logging.

---

## ⚙️ Configuration (`config.yml`)

```yaml
#############################
# plugin created by vC3sar_ #
#############################

# IMPORTANT! Do not share your secret_key with anyone.
# Remember to restart the server after changing this configuration.
# Use the /cr reload command to reload the configuration without restarting the server.

# CR RELOAD DOES NOT RELOAD THE PLUGIN, ONLY THE CONFIGURATION.
# IF YOU CHANGE THE PORT, YOU MUST RESTART THE SERVER.

# Port where the plugin will listen for incoming connections.
# Make sure this port is open in your server's firewall
# and is not being used by another service.
port: 8193
# Secret token to authenticate connections between the plugin and the external API
# Change it to a secure value and keep it private
secret_key: "mi_token_secreto"

# You can define multiple allowed IPs in allowed_ips
# Use "*" to allow all IPs (not recommended for security reasons)
# Add the IP address from where your Node.js script will run
# Example:
# allowed_ips:
#   - "105.123.100.12"
allowed_ips:
  - "127.0.0.1"

# retry_attempts y retry_delay manage the reconnection attempts if the connection is lost
retry_attempts: 3
retry_delay: 1000

# You can enable debug mode to see more details in the server console
debug: true

# Messages for debug and user feedback
messages:
  activated: "CommandRelay activado."
  invalid_token: "ERROR: token inválido."
  command_executed: "Comando ejecutado correctamente."
  debug_received: "DEBUG: Comando recibido -> "


# Lista de comandos permitidos que pueden ser ejecutados desde la API externa
# No incluyas el "/" al inicio del comando
# permite solo los comandos que utilizaras para minimizar riesgos de seguridad
# ningun comando que no este en esta lista sera ejecutado
allowed_commands:
  - say
  - give
  - kick
  - ban
```

## 📡 Example TCP Sender (Node.js)

```
const net = require("net");

const PLUGIN_HOST = "localhost"; // Server IP running the plugin
const PLUGIN_PORT = 8193;        // Port defined in config.yml
const TOKEN = "my_secret_token"; // Token from config.yml

function sendCommandToPlugin(command) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection(
      { host: PLUGIN_HOST, port: PLUGIN_PORT },
      () => {
        console.log("✅ Connected to TCP plugin");
        client.write(`${TOKEN}:${command}\n`);
      }
    );

    let dataBuffer = "";
    client.on("data", (data) => {
      dataBuffer += data.toString();
    });

    client.on("end", () => {
      console.log("⬅ Plugin response:", dataBuffer.trim());
      resolve(dataBuffer.trim());
    });

    client.on("error", (err) => {
      console.error("❌ Connection error:", err.message);
      reject(err);
    });
  });
}

(async () => {
  try {
    const response = await sendCommandToPlugin("say Hello from Node.js");
    console.log("Final response:", response);
  } catch (err) {
    console.error("Connection failed:", err);
  }
})();
```
## 🔒 Security Notes

- Always use a strong secret token.

- Only add trusted IPs to allowed_ips.

- Limit allowed_commands to safe operations.

## 🛠 Commands

- /cr <info|tcpstatus|reload>   # Admin commands

## Permissions

- commandrelay.admin (default: op)