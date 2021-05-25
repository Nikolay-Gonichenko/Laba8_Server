
import Connection.MessageForClient;
import collection.MyCollection;

import java.io.*;

import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static ServerSocketChannel serverSocketChannel;

    public static void main(String[] args) {
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        ExecutorService fixedPool = Executors.newFixedThreadPool(2);

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver is not found. Include it in your library path ");
            e.printStackTrace();
        }

        String jdbcURL = "jdbc:postgresql://localhost:7887/studs";
        DataBaseWorker worker = new DataBaseWorker(jdbcURL, "s311790", "gps097");
        worker.connectToDataBase();
        MyCollection collection = new MyCollection();
        worker.fillCollection(collection);

        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(3346));

            while (true) {
                SocketChannel client = accept();
                Exchanger<MessageForClient> exchanger = new Exchanger<>();
                cachedPool.submit(new ServerForReading(client, worker, collection, exchanger));
                fixedPool.submit(new ServerForWriting(client, worker, collection, exchanger));
            }


            //cachedPool.shutdown();
            //fixedPool.shutdown();
        } catch (IOException e) {
            System.out.println("There is no such open server port");
        }


    }


    private static SocketChannel accept() {
        SocketChannel client = null;
        try {
            client = serverSocketChannel.accept();
            System.out.println("Client connected");

        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
        return client;
    }

}

