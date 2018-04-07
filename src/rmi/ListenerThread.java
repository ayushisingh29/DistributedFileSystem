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
    private T server;
    private ExecutorService executorService ;
    private List<Future> futureList = new ArrayList<Future>();

    private ListenerThread() {
        executorService = Executors.newCachedThreadPool();
    }

    static ListenerThread getListenerThread() {

        if(listenerThread == null) {
            listenerThread = new ListenerThread();
            return listenerThread;
        }

        return listenerThread;
    }

    public void setServer(T server) {
        this.server = server;
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    synchronized void stop(){
        this.isStopped = true;
        if(serverSocket != null) try {
            for (Future f : futureList) {
                f.get();
            }
            System.out.println("Finished all Jobs Closing");
            this.serverSocket.close();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    askedToClose = true;
                    return;
                }
            }
            Future<?> future = executorService.submit(new Thread(new ServiceThread<T>(clientSocket, server)));
            futureList.add(future);
        }
    }
}
