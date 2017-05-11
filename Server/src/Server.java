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

    private String LoginRequired = "Invalid command.";
    private String DuplicatedUserName = "Name exist, please choose another name.";
    private String LoginSucceed = "You have logined";
    private String LoginBroadcast = "%s has logined";
    private String IllegalCommand = "Illegal command. Check syntax.";

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

        private Vector<String> messages = new Vector<String>();

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
                                            messages.add(LoginRequired);
                                            Server.this.Send(socket,LoginRequired);
                                        }
                                    }
                                    else
                                    {

                                        HandleMessage(ClientManager.this ,readResult.str);
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


        private boolean ClientLogin(String command, User user)
        {
            Pattern loginPattern = Pattern.compile("\\/login (.+)");
            Matcher matcher = loginPattern.matcher(command);
            if (matcher.matches())
            {

                String userName = matcher.group(1);
                user.IsLogin = true;

                if (userName.contains(" "))
                {
                    user.IsLogin = false;
                    this.messages.add("Whitespace is not allowed.");
                    Server.this.Send(user.Socket,"Whitespace is not allowed.");
                    return false;
                }

                for (ClientManager c : clients)//duplicated user name
                {
                    if (c.user.UserName == null)
                    {
                        continue;
                    }
                    if (c.user.UserName.equals(user.UserName))
                    {
                        user.IsLogin = false;
                        c.Send(DuplicatedUserName);
                        return false;
                    }
                }

                //Login succeed
                user.UserName = userName;
                for (ClientManager c : clients)
                {
                    if (c.user.UserName.equals(user.UserName))
                    {
                        c.Send(LoginSucceed);
                    }
                    else
                    {
                        c.Send(String.format(LoginBroadcast,user.UserName));
                    }
                }

                return true;
            }
            return false;
        }



        private void HandleMessage(ClientManager sender,final String message)
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
                String command = message;
                HandleCommand(sender, command);
            }
            else
            {
                HandleBroadcast(sender.user.UserName + " say to all " + message);
            }
        }

        private void HandleCommand(ClientManager sender, String command)
        {
            Pattern PrivateChatPattern = Pattern.compile("\\/to (\\S+) (.+)");
            Pattern UserQueryPattern = Pattern.compile("\\/who");
            Pattern HistoryMessagesPattern = Pattern.compile("\\/history( [0-9]+ [0-9]+)?");
            Pattern QuitPattern = Pattern.compile("\\/quit");

            Matcher PrivateChatMatcher = PrivateChatPattern.matcher(command);
            Matcher UserQueryMatcher = UserQueryPattern.matcher(command);
            Matcher HistoryMessagesMatcher = HistoryMessagesPattern.matcher(command);
            Matcher QuitMatcher = QuitPattern.matcher(command);

            if (PrivateChatMatcher.matches())
            {
                String receiverName = PrivateChatMatcher.group(1);
                ClientManager receiver = FindUser(receiverName);

                if (receiver == null)//can't find receiver
                {
                    sender.Send(String.format("Can't find user %s.",receiverName));
                }
                else
                {
                    PrivateChat(sender,receiver,PrivateChatMatcher.group(2));
                }
            }
            else if (UserQueryMatcher.matches())
            {
                StringBuilder list = new StringBuilder();
                for (ClientManager client : clients)
                {
                    list.append(client.user.UserName).append("\n");
                }
                list.append(String.format("Total online user: %d.\n", clients.size()));
                sender.Send(list.toString());
            }
            else if (HistoryMessagesMatcher.matches())
            {
                HistoryQuery(sender,HistoryMessagesMatcher);
            }
            else if (QuitMatcher.matches())
            {
                this.Quit();
            }
            else
            {
                sender.Send(IllegalCommand);
            }
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
                        client.Send(broadcast);
                    }
                }
            }).start();
        }


        private void PrivateChat(ClientManager sender, ClientManager receiver, String message)
        {
            sender.Send(String.format("You say to %s: %s",receiver.user.UserName,message));
            receiver.Send(String.format("%s say to you: %s",sender.user.UserName,message));
        }
        private void HistoryQuery(ClientManager sender, Matcher HistoryMessagesMatcher)
        {
            String index = HistoryMessagesMatcher.group(1);
            if (index == null)
            {
                StringBuilder history = new StringBuilder();
                for (String m : messages)
                {
                    history.append(m + "\n");
                }
                sender.Send(history.toString(),false);
            }
            else
            {
                index = index.trim();
                String[] indexes = index.split(" ");
                int startIndex = Integer.parseInt(indexes[0]);
                int endIndex = Integer.parseInt(indexes[1]);
                if (startIndex >= endIndex)
                {
                    int temp = startIndex;
                    startIndex = endIndex;
                    endIndex = temp;
                }

                if (startIndex < 0 || startIndex >= messages.size())
                {
                    sender.Send(IllegalCommand);
                    return;
                }
                if (endIndex < 0 || endIndex >= messages.size())
                {
                    sender.Send(IllegalCommand);
                    return;
                }

                StringBuilder history = new StringBuilder();
                for (int i = startIndex; i <= endIndex; i++)
                {
                    history.append(messages.elementAt(i) + "\n");
                }
                sender.Send(history.toString(),false);

            }
        }
        private void Quit()
        {
            clients.remove(this);
            try
            {
                this.reader.close();
                this.socket.close();
            }
            catch (Exception ignored)
            {

            }
        }

        private void Send(final String message)
        {
            this.messages.add(message);
            Server.this.Send(this.socket,message);
        }

        private void Send(final String message, boolean addToHistory)
        {
            if (addToHistory)
            {
                Send(message);
            }
            else
            {
                Server.this.Send(this.socket,message);
            }
        }
    }

    private ClientManager FindUser(String userName)
    {
        for (ClientManager c : clients)
        {
            if (c.user.UserName.equals(userName))
            {
                return c;
            }
        }

        return null;
    }

    private void Send(final Socket socket, final String message)
    {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Writer writer = new OutputStreamWriter(socket.getOutputStream());

                    writer.write(message + (char)0);
                    writer.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

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
