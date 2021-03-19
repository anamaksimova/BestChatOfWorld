package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            /* Если реализовать Executors.newFixedThreadPool(10), по моей логике, если будет больше 10 клиентов они
            будут ждать свою очередь и видимо не зайдут в чат. Executors.newCachedThreadPool()не имеет ограничения
            сверху (помимо возможностей компьютера), и можно попробовать его сделать.
            Более масштабные идеи мне пока не приходят в голову. Решила попробовать  Executors.newFixedThreadPool(2),
            действительно открылось 2 окна, а третье открылось, но ввод пароля не прошел. При первоначальной реализации
            Executors.newFixedThreadPool(2)в самом клиент хэндлере (а не на Server) окна продолжали открываться и
            заходить и после 2. Почему? Переделала на нужный вариант.
             */
           // new Thread(() -> {
            server.getExecutorService().execute(() -> {
                try {
                    //установка сокет тайм аут
                    socket.setSoTimeout(120000);
                    // цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        //если команда отключиться
                        if (str.equals(Command.END)) {
                            out.writeUTF(Command.END);
                            throw new RuntimeException("Клиент захотел отключиться");
                        }
                        //если команда аутентификация
                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s", 3);
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    System.out.println("client: " + socket.getRemoteSocketAddress() +
                                            " connected with nick: " + nickname);
                                    break;
                                } else {
                                    sendMsg("Данная учетная запись уже используется");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }

                        }
                        //если команда регистрация
                        if (str.startsWith(Command.REG)) {
                            String[] token = str.split("\\s", 4);
                            if (token.length < 4) {
                                continue;
                            }
                            boolean regSuccess = server.getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (regSuccess) {
                                sendMsg(Command.REG_OK);
                                socket.setSoTimeout(0);
                            } else {
                                sendMsg(Command.REG_NO);
                            }
                        }
                    }
                    socket.setSoTimeout(0);

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                out.writeUTF(Command.END);
                                break;
                            }
                            if (str.startsWith(Command.DIRECT)) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.directMsg(this, token[1], token[2]);
                            }
                            if (str.startsWith(Command.CH_NICK)) {
                                String[] token = str.split("\\s", 2);
                                if (token.length < 2) {
                                    continue;
                                }
                                if (server.getAuthService().changeNickname(this.nickname, token[1])) {
                                    sendMsg("Вы сменили ник с " + this.nickname + " на " + token[1]);
                                    sendMsg(Command.NICKISCHANGED + token[1]);
                                    this.nickname = token[1];
                                    server.broadcastClientlist();
                                } else {
                                    sendMsg("Невозможно сменить Nickname,\nпользователь с таким Nickname уже существует");
                                }
                            }
                        } else {

                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    try {
                        out.writeUTF(Command.END);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }

                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Client disconnected: " + nickname);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //  }).start();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }
    public String getLogin() {
        return login;
    }
}