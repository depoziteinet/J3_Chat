package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ClientHandler {
    Server server;
    Socket socket = null;
    DataInputStream in;
    DataOutputStream out;

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private String nick;
    private String login;

    public ClientHandler(Server server, Socket socket) throws IOException {
        LogManager manager = LogManager.getLogManager();
        manager.readConfiguration(new FileInputStream("logging.properties"));

        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            server.getSrv().execute(() -> {
                try {
                    socket.setSoTimeout(120000);

                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/auth ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server
                                    .getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthorized(login)) {
                                    sendMsg("/authok " + newNick);
                                    nick = newNick;
                                    server.subscribe(this);
                                    logger.log(Level.SEVERE, "Клиент подключился: " + nick);
                                    socket.setSoTimeout(0);
                                    break;
                                } else {
                                    sendMsg("С этим логином уже авторизовались");
                                    logger.log(Level.SEVERE, "С этим ником уже авторизовались: " + nick);
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                                logger.log(Level.SEVERE, "Неверный логин / пароль");

                            }
                        }

                        if (str.startsWith("/reg ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }
                            boolean b = server.getAuthService()
                                    .registration(token[1],token[2],token[3]);
                            if(b){
                                sendMsg("/regresult ok");
                            }else{
                                sendMsg("/regresult failed");
                            }
                        }
                    }


                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                out.writeUTF("/end");
                                break;
                            }

                            if (str.startsWith("/w ")) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                }

                                server.privateMsg(this, token[1], token[2]);
                            }
                            if (str.startsWith("/c ")){
                                String[] token = str.split("\\s", 2);
                                if (token.length < 2) {
                                    continue;
                                }
                                SimpleAuthService.changeNickName(this.login, token[1]);
                                this.nick = token[1];
                                server.broadcastClientList();
                            }

                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                }catch (SocketTimeoutException e){
                    sendMsg("/end");
                }catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    System.out.println("Клиент отключился");
                    server.unsubscribe(this);
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }

    public String getLogin() {
        return login;
    }
}
