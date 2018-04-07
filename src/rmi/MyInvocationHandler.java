package rmi;


import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.Arrays;

public class MyInvocationHandler implements InvocationHandler,Serializable{

    String host;
    int port;

    public MyInvocationHandler(int port, String host) {
        this.port = port;
        this.host = host;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String str = "";

        // Check for Equals
        if(method.getName().equals("equals")) {
            if(proxy == null || args[0] == null) {
                return false;
            }
            return checkEquals((Proxy) proxy, (Proxy)args[0]);
        }

        // Check for HashCode
        if(method.getName().equals("hashCode")) {
            return checkHashCode(proxy);
        }

        // Check for toString
        if(method.getName().equals("toString")) {
            return checktoString(proxy);
        }

            if(isValidMethod((Proxy)proxy, method)) {
                try {

                    if(method == null)
                        return null;

                    Socket socket = new Socket(host,port);

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream ois  = new ObjectInputStream(socket.getInputStream());

                    oos.writeObject(method.getName());
                    if(args != null && !socket.isOutputShutdown() && args.length != 0){
                        oos.writeObject(args);
                        socket.getOutputStream().flush();
                    }
                    else if (!socket.isOutputShutdown()) {
                        oos.writeObject(null);
                        socket.getOutputStream().flush();
                    }

                    Object o = ois.readObject();

                    if(o instanceof Exception)
                    {
                        Exception ex = (Exception) ((Exception) o).getCause();
                        throw ex;
                    }

                    socket.close();
                    return o;
                }
                catch (Exception ex) {
                    if(ex instanceof FileNotFoundException) {
                        throw ex;
                    }
                    throw new RMIException("RMI Exception");
                }
            }
            else {
                throw new RMIException("Not a valid method");
            }
    }

    private String checktoString(Object proxy) {
        String s = "";
        MyInvocationHandler i1 = (MyInvocationHandler)Proxy.getInvocationHandler(proxy);

        Class[] proxyInterfaces = proxy.getClass().getInterfaces();
        for(Class c : proxyInterfaces) {
            s +="\n" +  c.getName();
        }

        s+= "\nhost = " + i1.host.toString() + "\nport = " + String.valueOf(i1.port);

        return s;
    }

    private int checkHashCode(Object proxy) {
        String s = "";
        MyInvocationHandler i1 = (MyInvocationHandler)Proxy.getInvocationHandler(proxy);

        Class[] proxyInterfaces = proxy.getClass().getInterfaces();
        for(Class c : proxyInterfaces) {
            s +="\n" +  c.getName();
        }

        s+= "\nhost = " + i1.host.toString() + "\nport = " + String.valueOf(i1.port);

        return s.hashCode();
    }


    private boolean checkEquals(Proxy proxy, Proxy arg) {
        Class[] proxyInterfaces = proxy.getClass().getInterfaces();
        Class[] argsInterfaces  = arg.getClass().getInterfaces();
        for(Class c : proxyInterfaces) {
            if(!(Arrays.asList(argsInterfaces).contains(c))) {
                return false;
            }
        }
        MyInvocationHandler i1 = (MyInvocationHandler)Proxy.getInvocationHandler(proxy);
        MyInvocationHandler i2 = (MyInvocationHandler)Proxy.getInvocationHandler(arg);
        if((i1.host.equals(i2.host)) && (i1.port == i2.port)) {
            return true;
        }

        return false;
    }

    /**
     * Function to check if interface being used is Remote.
     */
    private boolean isValidMethod(Proxy proxy, Method m) {
        Class[] interfaces = proxy.getClass().getInterfaces();
        for( Class iface : interfaces) {
            Method[] methods = iface.getMethods();
            for (Method method : methods) {
                for (Class<?> exception : method.getExceptionTypes()) {
                    if (exception.getName().contains("rmi.RMIException")) {
                        if(m.getName().toString().equals(method.getName().toString())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
