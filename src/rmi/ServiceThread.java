package rmi;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

class ServiceThread<T> implements Runnable {

    private Socket clientSocket;
    private T server;

    ServiceThread(Socket clientSocket, T server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {

            ObjectInputStream ois = new ObjectInputStream(this.clientSocket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(this.clientSocket.getOutputStream());
            String methodName = (String) ois.readObject();
            Object[] args = (Object[]) ois.readObject();

            Method m = null;
            try {
                Class<?>[] paramTypes = null;
                for (Method meth : server.getClass().getMethods()) {
                    if (meth.getName().equals(methodName)) {
                        paramTypes = meth.getParameterTypes();
                    }
                }
                m = this.server.getClass().getMethod(methodName, paramTypes);

            } catch (NoSuchMethodException | SecurityException ignored) {

            }

            try {

                assert m != null;
                m.setAccessible(true);
                Object o = m.invoke(this.server, args);
                oos.writeObject(o);

            } catch (IllegalAccessException | IllegalArgumentException ignored) {

            } catch (InvocationTargetException ex) {
                oos.writeObject(ex);

            }

        } catch (ClassNotFoundException | IOException ignored) {

        }

    }
}
