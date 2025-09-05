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
port: 8193
secret_key: "my_secret_token"
allowed_ips:
  - "127.0.0.1"
debug: false
allowed_commands:
  - say
  - give
messages:
  activated: "Plugin activated"
  invalid_token: "ERROR: invalid token"
  command_executed: "Command executed successfully"
```

```
📡 Example TCP Sender (Node.js)
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