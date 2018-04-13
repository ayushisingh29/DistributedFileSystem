package rmi;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class ListenerThread<T> implements Runnable {

    ServerSocket serverSocket = null;
    private static ListenerThread listenerThread = null;
    boolean askedToClose = false;
    boolean isStopped = false;
    private Skeleton<T> skel;
    private T server;

    public ListenerThread(T server, Skeleton<T> skel) {
        this.skel=skel;
        this.setServer(server);
    }

    public void setServer(T server) {
        this.server = server;
    }

    public void setSoc(ServerSocket soc) {
        this.serverSocket = soc;
    }

    @Override
    public void run () {

        while (this.skel.listenerThread != null && !this.skel.listenerThread.isInterrupted()) {

            Socket clientSocket = null;
            try {

                clientSocket = this.serverSocket.accept();
                new Thread(new ServiceThread<T>(clientSocket, server)).start();
            } catch (IOException ignored) {

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }
}


