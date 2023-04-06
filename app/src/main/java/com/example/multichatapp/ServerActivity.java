package com.example.multichatapp;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerActivity extends AppCompatActivity {

    Button buttonSend;
    EditText clientMessage;
    TextView chat, displayStatus;
    String str, msg = "";
    int serverPort = 10000;
    Socket socketClient;
    Handler handler = new Handler();
    WifiManager wifiManager;

    private static final int maxClientsCount = 5;
    private static final ClientThread[] clientThreads = new ClientThread[maxClientsCount];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        clientMessage = (EditText) findViewById(R.id.client_message);
        chat = (TextView) findViewById(R.id.chat);
        displayStatus = (TextView)findViewById(R.id.display_status);
        displayStatus.setText("Server hosted on " + ip);

        Thread serverThread = new Thread(new ServerThread());
        serverThread.start();

        buttonSend = (Button) findViewById(R.id.button_send);
        buttonSend.setEnabled(false);
        buttonSend.setOnClickListener(v -> {

            if(clientMessage.getText() != null && !clientMessage.getText().toString().trim().equals("")){
                Thread sendThread = new Thread(new SendMessage());
                sendThread.start();
            }
        });
    }

    @Override
    public void onBackPressed() {
        try {
            if(socketClient != null)
                socketClient.close();
            super.onBackPressed();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    class SendMessage implements Runnable {
        @Override
        public void run() {
            try {
                String msgFromServer = clientMessage.getText().toString().trim();

                /* If the message is private sent it to the given client. */
                if (msgFromServer.startsWith("@")) {
                    String[] words = msgFromServer.split("\\s", 2);
                    if (words.length > 1 && words[1] != null) {
                        words[1] = words[1].trim();
                        if (!words[1].isEmpty()) {
                            synchronized (this) {
                                for (int i = 0; i < maxClientsCount; i++) {
                                    if (clientThreads[i] != null && clientThreads[i].clientName != null && clientThreads[i].clientName.equals(words[0])) {
                                        clientThreads[i].os.writeBytes(words[1]+ "\n");
                                        clientThreads[i].os.flush();
                                        break;
                                    }
                                }

                                msg = msg + "<Server> "+ msgFromServer +"\n";

                                handler.post(()->{
                                    chat.setText(msg);
                                    clientMessage.setText("");
                                });
                            }
                        }
                    }
                } else {
                    /* The message is public, broadcast it to all clients. */
                    synchronized (this) {

                        str = msgFromServer + "\n";

                        boolean noClientsConnected = true;
                        for (int i = 0; i < maxClientsCount; i++) {
                            if (clientThreads[i] != null) {
                                noClientsConnected = false;
                                clientThreads[i].os.writeBytes(str);
                                clientThreads[i].os.flush();
                            }
                        }

                        if(noClientsConnected){
                            msg = msg + "No clients connected\n";

                            handler.post(()->{
                                chat.setText(msg);
                                clientMessage.setText("");
                            });
                        } else {
                            msg = msg + "<Server> "+ str;

                            handler.post(()->{
                                chat.setText(msg);
                                clientMessage.setText("");
                            });
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println(e);

            }
        }
    }

    public class ServerThread implements Runnable {
        @Override
        public void run() {
            try {

                ServerSocket serverSocket = new ServerSocket(serverPort);

                while (true) {
                    socketClient = serverSocket.accept();
                    handler.post(() ->{
                        if(!clientMessage.isEnabled()) clientMessage.setEnabled(true);
                        if(!buttonSend.isEnabled()) buttonSend.setEnabled(true);
                    } );

                    int i = 0;
                    for (i = 0; i < maxClientsCount; i++) {
                        if (clientThreads[i] == null) {
                            (clientThreads[i] = new ClientThread(socketClient, clientThreads)).start();
                            break;
                        }
                    }
                    if (i == maxClientsCount) {
                        DataOutputStream os = new DataOutputStream(socketClient.getOutputStream());
                        str = "/busy\n";
                        os.writeBytes(str);
                        os.flush();
                        os.close();
                        socketClient.close();
                    }
                }

            } catch (Exception e) {
                System.out.println(e);

            }
        }
    }

    public class ClientThread extends Thread{
        private String clientName = null;
        private DataInputStream is = null;
        private DataOutputStream os = null;
        private Socket clientSocket = null;
        private final ClientThread[] threads;
        private int maxClientsCount;


        public ClientThread(Socket clientSocket, ClientThread[] threads) {
            this.clientSocket = clientSocket;
            this.threads = threads;
            maxClientsCount = threads.length;
        }

        public void run() {
            int maxClientsCount = this.maxClientsCount;
            ClientThread[] threads = this.threads;

            try {
                /*
                 * Create input and output streams for this client.
                 */
                is = new DataInputStream(clientSocket.getInputStream());
                os = new DataOutputStream(clientSocket.getOutputStream());

                String name = is.readLine();

                synchronized (this) {
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] != null && threads[i] == this) {
                            clientName = "@" + name;
                            break;
                        }
                    }

                    handler.post(()->{
                        msg = msg + "*** A new client " + name + " joined!!! ***"+"\n";
                        chat.setText(msg);
                    });
                }
                /* Start the conversation. */
                while (true) {
                    String line = is.readLine();

                    if (line.startsWith("/quit")) {
                        break;
                    }

                    msg = msg + "<" + name + "> " + line + "\n";
                    handler.post(() -> chat.setText(msg));
                }

                synchronized (this) {
                    handler.post(()->{
                        msg = msg + "*** Client " + name + " left!!! ***"+"\n";
                        chat.setText(msg);
                    });
                }

                /*
                 * Clean up. Set the current thread variable to null so that a new client
                 * could be accepted by the server.
                 */
                synchronized (this) {
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] == this) {
                            threads[i] = null;
                        }
                    }
                }
                /*
                 * Close the output stream, close the input stream, close the socket.
                 */
                is.close();
                os.close();
                clientSocket.close();
            } catch (IOException e) {
            }
        }
    }
}