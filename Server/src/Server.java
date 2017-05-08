import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hyh on 2017/5/1.
 */
public class Server {

    /**
     * The port number this server use.
     */
    private int Port;
    private ServerSocket serverSocket;

    private Vector<ClientManager> clients;

    private String LoginRequired = "Login required.";

    /**
     * Create a chatroom server. Must call run() to run the server.
     * @param port the port number this server use
     */
    private Server(int port)
    {
        if (port < 0 || port > 65535)
        {
            throw new IllegalArgumentException();
        }
        this.Port = port;
        this.clients = new Vector<ClientManager>();
    }

    /**
     * Run the server
     */
    private void Run()
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
            try
            {
                Socket socket = serverSocket.accept();

                ClientManager client = new ClientManager(socket);
                client.Run();
                //this.clients.add(client);
            }
            catch (IOException e) {
                break;
            }
        }
    }


    enum ReaderStatus{
        Success,
        Failed,
        Closed
    }


    private class ClientManager
    {
        private Socket socket;
        private Reader reader;
        private User user;

        ClientManager(Socket socket)
        {
            try
            {
                this.socket = socket;
                this.reader = new InputStreamReader(socket.getInputStream());

                this.user = new User();
                user.Socket = socket;

                clients.add(this);
            }
            catch (IOException e)
            {

            }
        }

        public void Run()
        {
            new Thread(new Receiver()).start();
        }

        private class Receiver implements Runnable
        {
            public void run()
            {
                while (true)
                {
                    try {
                        ReadResult readResult = Read(reader);

                        switch (readResult.status)
                        {

                            case Success:
                                if (readResult.str.length() != 0)
                                {
                                    if (!user.IsLogin)
                                    {
                                        user.IsLogin = ClientLogin(readResult.str,user);
                                        if (!user.IsLogin)
                                        {
                                            Send(socket,LoginRequired);
                                        }
                                    }
                                    else
                                    {
                                        HandleMessage(user,readResult.str);
                                    }
                                }
                                break;
                            case Failed:
                                throw new IOException();
                            case Closed:
                                reader.close();
                                socket.close();
                                clients.remove(ClientManager.this);
                                break;
                        }

                    }
                    catch (IOException e)
                    {
                        break;
                    }
                }
            }


            ReadResult Read(Reader reader)
            {
                ReadResult res = new ReadResult();
                try
                {
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

                    if (len == -1)
                    {
                        res.status = ReaderStatus.Closed;
                        return res;
                    }
                    else {
                        res.status = ReaderStatus.Success;
                        res.str = stringBuilder.toString();
                        return res;
                    }
                }
                catch (IOException e)
                {
                    res.status = ReaderStatus.Failed;
                    return res;
                }
            }

            private class ReadResult
            {
                private ReaderStatus status;
                private String str = null;
            }
        }



        private void HandleMessage(User sender,String message)
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
                message = sender.UserName + " say to all " + message;
                HandleBroadcast(message);
            }
        }

        private boolean ClientLogin(String command, User user)
        {
            Pattern loginPattern = Pattern.compile("\\/login (.+)");
            Matcher matcher = loginPattern.matcher(command);
            if (matcher.matches())
            {

                user.UserName = matcher.group(1);
                user.IsLogin = true;

                Send(user.Socket,"Login as " + user.UserName);

                return true;
            }
            return false;
        }

        private void HandleCommand(String command)
        {
            //TODO
        }

        private void HandlePreset(String preset)
        {
            //TODO
        }

        private void HandleBroadcast(final String broadcast)
        {
            new Thread(new Runnable() {
                public void run() {
                    for (ClientManager client : clients)
                    {
                        Socket socket = client.socket;
                        Send(socket,broadcast);
                    }
                }
            }).start();
        }
    }

    private boolean Send(Socket socket, String message)
    {
        try {
            Writer writer = new OutputStreamWriter(socket.getOutputStream());

            message += (char)0;
            writer.write(message);
            writer.flush();

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    private class User
    {
        Socket Socket;
        String UserName;
        boolean IsLogin = false;
    }

    public static void main(String args[]) {
        Server server = new Server(8899);
        server.Run();
    }

}
