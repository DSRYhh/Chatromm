import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * Created by hyh on 2017/5/1.
 */
public class Server {

    /**
     * The port number this server use.
     */
    private int Port;
    private ServerSocket serverSocket;

    private Vector<Socket> sockets;

    /**
     * Create a chatroom server. Must call run() to run the server.
     * @param port the port number this server use
     */
    public Server(int port)
    {
        if (port < 0 || port > 65535)
        {
            throw new IllegalArgumentException();
        }
        this.Port = port;
        this.sockets = new Vector<Socket>();
    }

    /**
     * Run the server
     */
    public void Run()
    {
        try
        {
            serverSocket  = new ServerSocket(Port);
        }
        catch (IOException e) {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ex)
                {
                    ex.printStackTrace(System.err);
                }
            }
        }

        while (true)
        {
            try{
                Socket socket = serverSocket.accept();
                this.sockets.add(socket);

                ClientManager(socket);
            } catch (IOException e) {

            }
        }
    }

    private void ClientManager(final Socket socket)
    {
        new Thread(new Runnable() {
            public void run() {
                while (true)
                {
                    //System.out.println("number" + Integer.toString(sockets.size()));
                    try {
                        Reader reader = new InputStreamReader(socket.getInputStream());
                        char buffer[] = new char[64];
                        int len;
                        StringBuilder stringBuilder = new StringBuilder();
                        String temp;
                        int index;
                        while ((len=reader.read(buffer)) != -1) {
                            temp = new String(buffer, 0, len);
                            if ((index = temp.indexOf((char)0)) != -1) {
                                stringBuilder.append(temp.substring(0, index));
                                break;
                            }
                            stringBuilder.append(temp);
                        }

                        String str = stringBuilder.toString();
                        if (str.length() != 0)
                        {
                            HandleMessage(str);
                        }

                        if (len == -1)//Receive FIN, client close the connection.
                        {
                            reader.close();
                            socket.close();
                            sockets.remove(socket);
                            break;
                        }
                    }
                    catch (IOException e)
                    {

                    }
                }
            }
        }).start();//run() method call runnable.run() in main thread!
    }

    private void HandleMessage(String message)
    {
        String presetPattern = "\\/\\/(.+)";
        String commandPattern = "\\/(.+)";
        if (message.matches(presetPattern))
        {
            String preset = message.substring(2);
            HandlePreset(preset);
        }
        else if (message.matches(commandPattern))
        {
            String command = message.substring(1);
            HandleCommand(command);
        }
        else
        {
            HandleBroadcast(message);
        }
    }

    private void HandleCommand(String command)
    {
        //TODO
    }
    
    private void HandlePreset(String preset)
    {
        //TODO
    }
    
    private void HandleBroadcast(final String boardcast)
    {
        new Thread(new Runnable() {
            public void run() {
                for (Socket socket : sockets)
                {
                    try {
                        Writer writer = new OutputStreamWriter(socket.getOutputStream());
                        String data = boardcast + (char)0;
                        writer.write(data);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static void main(String args[]) {
        Server server = new Server(8899);
        server.Run();
    }

}
