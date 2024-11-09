package org.example;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TPPlugin extends JavaPlugin {

    private HashMap<UUID, Long> teleportCooldown = new HashMap<>();
    private Location predefinedLocation = new Location(Bukkit.getWorld("world"), 100, 65, 100);
    private long COOLDOWN_PERIOD = TimeUnit.HOURS.toMillis(48);

    @Override
    public void onEnable() {
        getLogger().info("TPPlugin has been enabled.");

        // Asynchrone Methode zum Laden der Teleportdaten
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            if (new File("teleportData.txt").exists()) {
                loadTeleportData();
            } else {
                getLogger().info("No teleportData.txt file found.");
            }
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("TPPlugin has been disabled.");

        // Beende alle asynchronen Aufgaben, bevor das Plugin deaktiviert wird
        try {
            Bukkit.getScheduler().cancelTasks(this);  // Stoppt alle Aufgaben, die dieses Plugin verwenden
        } catch (Exception e) {
            getLogger().severe("Error while disabling tasks: " + e.getMessage());
        }

        // Speichere Teleport-Daten synchron
        saveTeleportData();  // Synchroner Aufruf
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cktp")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Asynchrone Verarbeitung des Befehls
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    UUID playerUUID = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    // Überprüfen, ob der Spieler bereits teleportiert wurde und ob die Wartezeit vorbei ist
                    if (teleportCooldown.containsKey(playerUUID)) {
                        long timeLeft = teleportCooldown.get(playerUUID) - currentTime;
                        if (timeLeft > 0) {
                            long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(timeLeft);
                            long hoursLeft = TimeUnit.MILLISECONDS.toHours(timeLeft);
                            long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60;

                            if (minutesLeft < 10) {
                                player.sendMessage("You have to wait: " + minutesLeft + " minutes and " + secondsLeft + " seconds, before you can teleport again.");
                            } else {
                                player.sendMessage("You have to wait: " + hoursLeft + " hours and " + (minutesLeft % 60) + " minutes, before you can teleport again.");
                            }
                            return;
                        }
                    }

                    // Überprüfe, ob die Welt existiert
                    if (predefinedLocation.getWorld() == null) {
                        player.sendMessage("The world for the teleport location is not loaded or does not exist.");
                        return;
                    }

                    // Teleportiere den Spieler
                    Bukkit.getScheduler().runTask(this, () -> {
                        player.teleport(predefinedLocation);
                        player.sendMessage("You've been teleported, Thanks to ChichiKugel!");
                    });
                    teleportCooldown.put(playerUUID, currentTime + COOLDOWN_PERIOD);
                });
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("CKSetCoords")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Überprüfen, ob der Spieler OP ist
                if (!player.isOp()) {
                    player.sendMessage("You need to be OP to use this command. Maybe ask ChichiKugel for help?");
                    return true;
                }

                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    if (args.length == 3) {
                        try {
                            double x = Double.parseDouble(args[0]);
                            double y = Double.parseDouble(args[1]);
                            double z = Double.parseDouble(args[2]);

                            predefinedLocation = new Location(Bukkit.getWorld("world"), x, y, z);
                            player.sendMessage("New Cords definied: " + x + ", " + y + ", " + z);
                        } catch (NumberFormatException e) {
                            player.sendMessage("Invalid Coordination Use: /CKSetCoords <x> <y> <z> or just /CKSetCoords to set your current position");
                        }
                    } else {
                        // Wenn keine Koordinaten übergeben wurden, verwende die aktuellen Koordinaten des Spielers
                        Location playerLocation = player.getLocation();
                        predefinedLocation = playerLocation;
                        player.sendMessage("New Cords set to: "
                                + playerLocation.getBlockX() + ", "
                                + playerLocation.getBlockY() + ", "
                                + playerLocation.getBlockZ());
                    }
                });
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("CKSetNewLimit")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Überprüfen, ob der Spieler OP ist
                if (!player.isOp()) {
                    player.sendMessage("You need to be OP to use this command. Maybe ask ChichiKugel for help?");
                    return true;
                }

                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    // Wenn kein Argument übergeben wurde, setze den Cooldown auf 48 Stunden
                    if (args.length == 0) {
                        COOLDOWN_PERIOD = TimeUnit.HOURS.toMillis(48);
                        player.sendMessage("New Cooldown set to 48 hours");
                    } else {
                        try {
                            long newCooldown = Long.parseLong(args[0]);
                            COOLDOWN_PERIOD = TimeUnit.HOURS.toMillis(newCooldown);
                            player.sendMessage("New Cooldown set to " + newCooldown + " hours");
                        } catch (NumberFormatException e) {
                            player.sendMessage("Invalid Cooldown Use: /CKSetNewLimit <hours>");
                        }
                    }
                });
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("CKResetLimit")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Überprüfen, ob der Spieler OP ist
                if (!player.isOp()) {
                    player.sendMessage("You need to be OP to use this command. Maybe ask ChichiKugel for help?");
                    return true;
                }

                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    if (args.length == 1) {
                        // Finde den Zielspieler asynchron
                        Player target = Bukkit.getPlayer(args[0]);
                        if (target != null) {
                            // Führe den Teil, der den Server beeinflusst, synchron aus
                            Bukkit.getScheduler().runTask(this, () -> {
                                teleportCooldown.put(target.getUniqueId(), 0L);
                                player.sendMessage("Cooldown for " + target.getName() + " has been reset.");
                            });
                        } else {
                            // Wenn der Spieler nicht gefunden wird, sende die Nachricht synchron
                            Bukkit.getScheduler().runTask(this, () -> {
                                player.sendMessage("Player not found.");
                            });
                        }
                    } else {
                        // Falsche Benutzung des Befehls
                        Bukkit.getScheduler().runTask(this, () -> {
                            player.sendMessage("Invalid Usage: /CKResetLimit <player>");
                        });
                    }
                });
                return true;
            }
        }

        return false;
    }

    private void saveTeleportData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("teleportData.txt"))) {
            long currentTime = System.currentTimeMillis();
            for (UUID uuid : teleportCooldown.keySet()) {
                long timeLeft = teleportCooldown.get(uuid) - currentTime;
                if (timeLeft > 0) {
                    writer.write(uuid.toString() + ":" + timeLeft);
                    writer.newLine();
                }
            }

            // Save the new Cooldown Period
            writer.write("New Cooldown Period: " + COOLDOWN_PERIOD);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Asynchrones Laden der Teleportdaten
    private void loadTeleportData() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            File file = new File("teleportData.txt");
            if (!file.exists()) return;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                long currentTime = System.currentTimeMillis();
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");

                    // Prüfen, ob es sich um die Zeile für den Cooldown handelt
                    if (line.startsWith("New Cooldown Period")) {
                        // Korrekt die Cooldown-Periode parsen
                        COOLDOWN_PERIOD = Long.parseLong(parts[1].trim());
                        getLogger().info("New Cooldown Period set to: " + COOLDOWN_PERIOD);
                    }
                    // Prüfen, ob es eine UUID ist (Standard-Format für UUID)
                    else if (parts.length == 2) {
                        try {
                            UUID uuid = UUID.fromString(parts[0]);
                            long timeLeft = Long.parseLong(parts[1]);
                            teleportCooldown.put(uuid, currentTime + timeLeft);
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("Fehler beim Parsen der UUID in teleportData.txt: " + parts[0]);
                        }
                    } else {
                        getLogger().warning("Fehlerhafte Zeile in teleportData.txt: " + line);
                    }
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                getLogger().severe("Fehler beim Laden der teleportData.txt: " + e.getMessage());
            }
        });
    }
}
