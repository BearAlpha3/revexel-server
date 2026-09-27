package com.revexel.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends WebSocketServer {

    private final Map<WebSocket, Player> players =
            new ConcurrentHashMap<WebSocket, Player>();

    public Server(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(
            WebSocket connection,
            ClientHandshake handshake) {

        String id = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        Player player = new Player(id);

        players.put(connection, player);

        JSONObject welcome = new JSONObject();

        welcome.put("type", "welcome");
        welcome.put("id", player.id);
        welcome.put("x", player.x);
        welcome.put("y", player.y);

        connection.send(welcome.toString());

        sendWorld(connection);

        broadcastPlayers();

        System.out.println("[CONNECT] " + player.id);
    }

    @Override
    public void onClose(
            WebSocket connection,
            int code,
            String reason,
            boolean remote) {

        Player player = players.remove(connection);

        if (player == null) {
            return;
        }

        JSONObject message = new JSONObject();

        message.put("type", "leave");
        message.put("id", player.id);

        broadcast(message.toString());

        broadcastPlayers();

        System.out.println(
                "[DISCONNECT] " + player.id
        );
    }

    @Override
    public void onMessage(
            WebSocket connection,
            String message) {

        Player player = players.get(connection);

        if (player == null) {
            return;
        }

        try {

            JSONObject data =
                    new JSONObject(message);

            String type =
                    data.optString("type", "");

            if ("join".equals(type)) {

                handleJoin(
                        connection,
                        player,
                        data
                );

            } else if ("move".equals(type)) {

                handleMove(
                        player,
                        data
                );

            } else if ("ping".equals(type)) {

                JSONObject pong =
                        new JSONObject();

                pong.put(
                        "type",
                        "pong"
                );

                connection.send(
                        pong.toString()
                );
            }

        } catch (Exception e) {

            JSONObject error =
                    new JSONObject();

            error.put(
                    "type",
                    "error"
            );

            error.put(
                    "message",
                    "Invalid message"
            );

            connection.send(
                    error.toString()
            );
        }
    }

    private void handleJoin(
            WebSocket connection,
            Player player,
            JSONObject data) {

        String name =
                data.optString(
                        "name",
                        "Player"
                );

        if (name.length() > 16) {

            name =
                    name.substring(
                            0,
                            16
                    );
        }

        player.name = name;

        JSONObject joined =
                new JSONObject();

        joined.put(
                "type",
                "joined"
        );

        joined.put(
                "id",
                player.id
        );

        joined.put(
                "name",
                player.name
        );

        connection.send(
                joined.toString()
        );

        broadcastPlayers();

        System.out.println(
                "[JOIN] " +
                player.id +
                " -> " +
                player.name
        );
    }

    private void handleMove(
            Player player,
            JSONObject data) {

        double x =
                data.optDouble(
                        "x",
                        player.x
                );

        double y =
                data.optDouble(
                        "y",
                        player.y
                );

        if (x < -100000) {
            x = -100000;
        }

        if (x > 100000) {
            x = 100000;
        }

        if (y < -100000) {
            y = -100000;
        }

        if (y > 100000) {
            y = 100000;
        }

        player.x = x;
        player.y = y;

        JSONObject move =
                new JSONObject();

        move.put(
                "type",
                "player_move"
        );

        move.put(
                "id",
                player.id
        );

        move.put(
                "x",
                player.x
        );

        move.put(
                "y",
                player.y
        );

        broadcast(
                move.toString()
        );
    }

    private void sendWorld(
            WebSocket connection) {

        JSONObject world =
                new JSONObject();

        world.put(
                "type",
                "world"
        );

        world.put(
                "name",
                "Revexel"
        );

        world.put(
                "spawnX",
                0
        );

        world.put(
                "spawnY",
                0
        );

        world.put(
                "seed",
                123456789
        );

        connection.send(
                world.toString()
        );
    }

    private void broadcastPlayers() {

        JSONArray array =
                new JSONArray();

        for (Player player :
                players.values()) {

            JSONObject object =
                    new JSONObject();

            object.put(
                    "id",
                    player.id
            );

            object.put(
                    "name",
                    player.name
            );

            object.put(
                    "x",
                    player.x
            );

            object.put(
                    "y",
                    player.y
            );

            array.put(object);
        }

        JSONObject message =
                new JSONObject();

        message.put(
                "type",
                "players"
        );

        message.put(
                "players",
                array
        );

        broadcast(
                message.toString()
        );
    }

    @Override
    public void onError(
            WebSocket connection,
            Exception exception) {

        System.out.println(
                "[ERROR] " +
                exception.getMessage()
        );
    }

    @Override
    public void onStart() {

        System.out.println(
                "================================"
        );

        System.out.println(
                "REVEXEL SERVER ONLINE"
        );

        System.out.println(
                "PORT: " + getPort()
        );

        System.out.println(
                "================================"
        );
    }

    public static void main(
            String[] args) {

        int port = 10000;

        String envPort =
                System.getenv("PORT");

        if (envPort != null) {

            try {

                port =
                        Integer.parseInt(
                                envPort
                        );

            } catch (Exception ignored) {
            }
        }

        Server server =
                new Server(port);

        server.setReuseAddr(true);

        server.start();
    }

    private static class Player {

        String id;
        String name;

        double x;
        double y;

        Player(String id) {

            this.id = id;
            this.name = "Player";

            this.x = 0;
            this.y = 0;
        }
    }
    }
