package com.vcesar.commandrelay;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.vcesar.commandrelay.commands.CommandRelayCommand;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandRelayPlugin extends JavaPlugin {

    private int PORT;
    private String SECRET_KEY;
    private List<String> allowedIps;
    private boolean DEBUG;

    private ServerSocket server;
    private ExecutorService clientThreadPool; // <- Thread pool para clientes
    private static final int MAX_CLIENT_THREADS = 20; // Ajustable según tu servidor

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        getCommand("cr").setExecutor(new CommandRelayCommand(this));
        log(getConfig().getString("messages.activated", "Plugin activado"));

        // Inicializamos el ThreadPool
        clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENT_THREADS);
        startTCPServer();
    }

    @Override
    public void onDisable() {
        // Cerramos TCP Server
        if (server != null && !server.isClosed()) {
            try {
                server.close();
                log("🔌 TCP Server closed successfully.");
            } catch (IOException e) {
                logErrorToFile("Error closing TCP Server", e);
            } finally {
                try {
                    // Forzamos limpieza para GC
                    server = null;
                } catch (Exception ignored) {
                }
            }
        }

        // Cerramos el pool de threads
        if (clientThreadPool != null && !clientThreadPool.isShutdown()) {
            clientThreadPool.shutdownNow();
        }
    }

    private void loadConfigValues() {
        PORT = getConfig().getInt("port", 8193);
        SECRET_KEY = getConfig().getString("secret_key", "mi_token_secreto");
        allowedIps = getConfig().getStringList("allowed_ips");
        if (allowedIps == null)
            allowedIps = new ArrayList<>();
        DEBUG = getConfig().getBoolean("debug", false);
    }

    private void startTCPServer() {
        new Thread(() -> {
            try {
                if (server != null) {
                    if (!server.isClosed()) {
                        log("⚠️  TCP Server already running on port " + PORT);
                        return;

                    }
                }

                server = new ServerSocket(PORT);
                server.setSoTimeout(2000); // evita bloqueo indefinido en accept()
                log("✅  TCP Server started on port " + PORT);

                while (!server.isClosed()) {
                    try {
                        Socket socket = server.accept();
                        // Enviar manejo al pool de hilos
                        clientThreadPool.submit(() -> handleClient(socket));
                    } catch (SocketTimeoutException ignored) {
                        // Timeout normal
                    } catch (IOException e) {
                        if (!server.isClosed()) {
                            String errorMsg = "⚠️  Error accepting client: " + e.getMessage();
                            log(errorMsg);
                            logErrorToFile(errorMsg, e);
                        }
                    }
                }
            } catch (IOException e) {
                String errorMsg = "❌  Error starting TCP Server: " + e.getMessage();
                log(errorMsg);
                logErrorToFile(errorMsg, e);
            }
        }, "TCPServerThread").start();
    }

    private void logErrorToFile(String msg, Exception e) {
        File errorFile = new File(getDataFolder(), "errors.log");
        try (FileWriter fw = new FileWriter(errorFile, true);
                BufferedWriter bw = new BufferedWriter(fw)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            bw.write("[" + timestamp + "] " + msg + "\n");
            if (e != null) {
                bw.write("Stacktrace:\n");
                for (StackTraceElement ste : e.getStackTrace()) {
                    bw.write("\tat " + ste.toString() + "\n");
                }
            }
            bw.write("\n");
        } catch (IOException ioException) {
            log("Could not write in errors.log: " + ioException.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String clientIp = socket.getInetAddress().getHostAddress();

            if (!allowedIps.isEmpty() && !allowedIps.contains(clientIp)) {
                log("CConnection refused from IP not allowed: " + clientIp);
                writer.write("ERROR: IP not allowed\n");
                writer.flush();
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (DEBUG)
                    log("DEBUG: Received -> " + line);

                if (!line.contains(":")) {
                    writer.write("ERROR: Invalid format\n");
                    writer.flush();
                    return;
                }

                String[] parts = line.split(":", 2);
                if (parts.length < 2) {
                    writer.write("ERROR: Invalid format\n");
                    writer.flush();
                    return;
                }

                String token = parts[0].trim();
                String command = parts[1].trim();

                if (!SECRET_KEY.trim().equals(token)) {
                    writer.write(getConfig().getString("messages.invalid_token", "ERROR: token inválido") + "\n");
                    writer.flush();
                    if (DEBUG)
                        log("DEBUG: Invalid token from " + clientIp);
                    return;
                }

                // Validación de comando contra config.yml
                List<String> allowedCommands = getConfig().getStringList("allowed_commands");
                String commandRoot = command.split(" ")[0]; // solo la primera palabra del comando
                if (!allowedCommands.contains(commandRoot)) {
                    writer.write("ERROR: Command not allowed\n");
                    writer.flush();
                    return;
                }

                // Ejecutar comando en el hilo principal de Bukkit
                AtomicBoolean success = new AtomicBoolean(false);
                try {
                    success.set(Bukkit.getScheduler()
                            .callSyncMethod(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command))
                            .get());
                } catch (Exception e) {
                    log("Error execute command: " + e.getMessage());
                    writer.write("ERROR: failure to execute command\n");
                    writer.flush();
                    continue;
                }

                if (success.get()) {
                    writer.write(getConfig().getString("messages.command_executed", "Comando ejecutado correctamente")
                            + "\n");
                } else {
                    writer.write("ERROR: command failed\n");
                }
                writer.flush();
            }

        } catch (IOException e) {
            log("Error handling client: " + e.getMessage());
        }
    }

    private void log(String msg) {
        getLogger().info(msg);
    }

    // Método público para recargar configuración desde fuera
    public void reloadPluginConfig() {
        reloadConfig();
        loadConfigValues(); // <- sigue siendo privado, pero accesible desde aquí
        log("♻️  Config reloaded.");
    }

    // 🔍 Para consultar el estado del servidor TCP
    public ServerSocket getServerSocket() {
        return server;
    }

    public int getPort() {
        return PORT;
    }

}
