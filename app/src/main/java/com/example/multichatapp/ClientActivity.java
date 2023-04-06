package com.example.multichatapp;

import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity {

    EditText serverIp, smessage, clientName;
    TextView chat;
    Button button_connect, sent, button_disconnect;
    String serverIpAddress = "", msg = "", str;
    Handler handler = new Handler();
    Socket socket;

    DataInputStream in;
    DataOutputStream os;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        chat = (TextView) findViewById(R.id.chat);
        serverIp = (EditText) findViewById(R.id.server_ip);
        clientName = (EditText)findViewById(R.id.user_name);
        smessage = (EditText) findViewById(R.id.client_message);
        sent = (Button) findViewById(R.id.sent_button);
        button_connect = (Button) findViewById(R.id.button_connect);
        button_disconnect = (Button) findViewById(R.id.button_disconnect);

        sent.setEnabled(false);
        sent.setOnClickListener(v -> {
            if(smessage.getText() != null && !smessage.getText().toString().equals("")){
                Thread sentThread = new Thread(new sentMessage());
                sentThread.start();
            }
        });

        button_connect.setOnClickListener(v -> {
            if(serverIp.getText() == null || serverIp.getText().toString().equals("")){
                Toast.makeText(getApplicationContext(),"Server IP cannot be empty", Toast.LENGTH_SHORT).show();
            }
            else if(clientName.getText() == null || clientName.getText().toString().equals("")){
                Toast.makeText(getApplicationContext(),"Client Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
            else if(clientName.getText() != null && clientName.getText().toString().indexOf('@') != -1){
                Toast.makeText(getApplicationContext(),"Cannot contain '@' character", Toast.LENGTH_SHORT).show();
            }
            else {
                serverIpAddress = serverIp.getText().toString();
                Thread clientThread = new Thread(new
                        ClientThread());
                clientThread.start();
            }
        });

        button_disconnect.setEnabled(false);
        button_disconnect.setOnClickListener(v->{
            if(socket !=null){
                try {
                    button_connect.setEnabled(true);
                    serverIp.setEnabled(true);
                    clientName.setEnabled(true);
                    sent.setEnabled(false);
                    button_disconnect.setEnabled(false);
                    smessage.setEnabled(false);
                    msg = "";

                    Thread exitConnection = new Thread(new exitConnection());
                    exitConnection.start();

                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        try {
            if(socket !=null)
                socket.close();
            super.onBackPressed();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    class exitConnection implements Runnable {
        @Override
        public void run() {
            try {
                str = "/quit\n";
                handler.post(() -> {
                    chat.setText("Disconnected from server");
                });
                os.writeBytes(str);
                os.flush();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    class sentMessage implements Runnable {
        @Override
        public void run() {
            try {
                str = smessage.getText().toString() + "\n";
                msg = msg + "<"+ clientName.getText().toString() + "> " + str;
                handler.post(() -> {
                    chat.setText(msg);
                    smessage.setText("");
                });
                os.writeBytes(str);
                os.flush();

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public class ClientThread implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress serverAddr =
                        InetAddress.getByName(serverIpAddress);
                socket = new Socket(serverAddr, 10000); //create client socket
                handler.post(() ->{
                    chat.setText("Connected to server");
                    smessage.setEnabled(true);
                    sent.setEnabled(true);
                    button_disconnect.setEnabled(true);
                    button_connect.setEnabled(false);
                    serverIp.setEnabled(false);
                    clientName.setEnabled(false);
                } );

                /*******************************************
                 setup i/p streams
                 ******************************************/
                in = new DataInputStream(socket.getInputStream());
                os = new DataOutputStream(socket.getOutputStream());

                os.writeBytes(clientName.getText().toString().trim()+"\n");
                os.flush();

                String line = null;
                while (in!=null && (line = in.readLine()) != null) {
                    if (line.startsWith("/busy")) {
                        handler.post(() ->{
                            chat.setText("Server too busy. Try again later");
                            smessage.setEnabled(false);
                            sent.setEnabled(false);
                            button_disconnect.setEnabled(false);
                            button_connect.setEnabled(true);
                            serverIp.setEnabled(true);
                            clientName.setEnabled(true);
                        });
                        break;
                    }

                    msg = msg + "<Server> " + line + "\n";
                    handler.post(() -> chat.setText(msg));
                }

                if(in!=null) in.close();
                if(socket!=null) socket.close();

            } catch (IOException e) {
                handler.post(() -> Toast.makeText(getApplicationContext(),"Connection Error", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}