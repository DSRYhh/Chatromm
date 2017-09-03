import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by hyh on 2017/5/3.
 */
public class Client {

    private final String QUIT = "/quit";

    private final Socket socket;
    private Writer writer;
    private Reader reader;

    private final String IOEXPECTIONHINT = "Connect to server failed.";

    public Client(String serverIP, int port) throws IOException {
        this.socket = new Socket(serverIP, port);

        try {
            writer = new OutputStreamWriter(socket.getOutputStream());
            reader = new InputStreamReader(socket.getInputStream());
        } catch (IOException e) {
            System.err.println(IOEXPECTIONHINT);
            return;
        }
    }

    public void Run() {
        new Thread(new Sender()).start();
        new Thread(new Receiver()).start();
    }

    private class Sender implements Runnable {

        public void run() {
            Scanner reader = new Scanner(System.in);
            //Writer writer;

            while (true)
            {
                String data = reader.nextLine();
                boolean result = Send(data);
                if (!result)
                {
                    try {
                        writer.close();
                        socket.close();
                    } catch (IOException e) {
                        System.err.println(IOEXPECTIONHINT);
                    }
                    return;
                }
            }
        }
    }

    private boolean Send(String data)
    {
        if (data.equals(QUIT))
        {
            try {
                writer.close();
                socket.close();
            } catch (IOException e) {
                System.err.println(IOEXPECTIONHINT);
            }
            return false;
        }
        data += (char)0;
        try {
            writer.write(data);
            writer.flush();
        } catch (IOException e) {
            System.err.println(IOEXPECTIONHINT);
            return false;
        }
        return true;
    }

    private class Receiver implements Runnable
    {

        public void run() {
            try {

                while (true)
                {
                    char buffer[] = new char[64];
                    int len;
                    StringBuffer sb = new StringBuffer();
                    String temp;
                    int index;
                    while ((len=reader.read(buffer)) != -1) {
                        temp = new String(buffer, 0, len);
                        if ((index = temp.indexOf((char)0)) != -1) {
                            sb.append(temp.substring(0, index));
                            break;
                        }
                        sb.append(new String(buffer, 0, len));
                    }
                    System.out.println(sb);

                }
            } catch (IOException e) {
                System.err.println("Connect to server failed.");
            }
        }

    }

    public static void main(String args[]) {

//        Scanner in = new Scanner(System.in);
//        String user = in.nextLine();
        try {
            Client client = new Client("localhost",8899);
//            while (!client.Login())
//            {
//                user = in.nextLine();
//                client.User = user;
//            }
            client.Run();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    }
