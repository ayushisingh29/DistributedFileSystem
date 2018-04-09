package rmi;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/** RMI skeleton

    <p>
    A skeleton encapsulates a multithreaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parametrized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
*/
public class Skeleton<T>
{
    
       private Class<T> c; 
       private  T server ;
       private InetSocketAddress address;  //this is the address
       public InetSocketAddress addressRef;
       public threadListener listenerThread; //listener thread, which spins service threads
       public ServerSocket socket= null; //this is a server socket created to listen to the requests
       public Thread thread = null; 
    /** Creates a <code>Skeleton</code> with no initial server address. The
        address will be determined by the system when <code>start</code> is
        called. Equivalent to using <code>Skeleton(null)</code>.

        <p>
        This constructor is for skeletons that will not be used for
        bootstrapping RMI - those that therefore do not require a well-known
        port.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server)
    {
        if(c == null || server == null){ //check if the interface is null or the server implementing interface is null
            throw new NullPointerException("interface or the server is null\n");
        }
        
         if(!c.isInterface()){ //Validate if C is interface or not
             throw new Error("Invalid interface\n");
         }
         
         if(!isInterfaceSyntaxCorrect(c)){ //check if the interface syntax is correct.
            throw new Error("Invalid interface\n");
         }
        this.c=c; //the interface
        this.server=server; //the server
        Random rand= new Random(); //since the address here is going to be null, in such case assign a random port
        int start=7000;           //to Inetsocketaddress address. 
        int end=30000;
        int randPort=rand.nextInt(end-start) + end;
           try {
               this.address= new InetSocketAddress(InetAddress.getLocalHost(),randPort);
               this.addressRef=this.address;
           } catch (UnknownHostException ex) {
               Logger.getLogger(Skeleton.class.getName()).log(Level.SEVERE, null, ex);
           }
    }

    /** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address) 
    {
        
        if(c == null || server == null){
            throw new NullPointerException("interface or the server is null\n");
        }
        
        if(!c.isInterface()){
             throw new Error("not an interface, provide correct value\n");
         }
        
        if(!isInterfaceSyntaxCorrect(c)){ //check if the interface syntax is correct.
            throw new Error("Invalid interface\n");
         }
         
        this.c=c;
        this.server=server;
        this.address=address;
        if(this.address == null){ //if the address is null create a new inetsocketaddress using random port
        Random rand= new Random();
        int start=7000;
        int end=30000;
        int randPort=rand.nextInt(end-start) + end;
            try {
                this.address= new InetSocketAddress(InetAddress.getLocalHost(),randPort);
                this.addressRef=this.address;
            } catch (UnknownHostException ex) {
                Logger.getLogger(Skeleton.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause)
    {
    }

    
   
    /** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception)
    {
        return false;
    }

    
    public InetSocketAddress findAddress(){
        return this.address;
    }
    
     public boolean isInterfaceSyntaxCorrect(Class c){
         for(Method method: c.getDeclaredMethods()){ //from the interface pull all the methods and iterate one by one
             for (Class excep: method.getExceptionTypes()){ //fetch the exception type defined for the declared method
                if(excep.getName().contains("RMIException")){// check if the exception contains RMIException, if yes then return true 
                    return true;                             //stating that the interface is correct else return false
                }
             }   
         }
         return false;
    }
    
    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
    }

    /** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socketSrv cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
     */
    public synchronized void start() throws RMIException
    {
     
        if(thread == null){ // this is used because start() shouldn't be called again once it is in the process of execution
        try{
            socket= new ServerSocket(this.address.getPort());// creates a server socket bound to the port in the argument 
        }catch(Exception ex){
            throw new RMIException("Error creating sockets\n");
        }
        listenerThread= new threadListener(this, this.server, this.c, this.address, socket, true);//listener thread object is created
        thread= new Thread(listenerThread);//listener thread is created
        thread.start();//listener thread started
        }
      
    }

    /** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
     */
    public synchronized void stop() 
    {
          if(thread != null){ //stop is only executed if the server is in start state
              try {
                  socket.close(); //close the socket
                  listenerThread.stopExeService();// stop executor service, which will wait for all the service 
              } catch (IOException ex) {          //thread to process it's execution before closing 
                  throw new Error("Error closing socket" + ex);
              }
              thread=null; //initialize the listener thread to null
              this.stopped(null);
          }
    }
       
}

 class threadListener<T> extends Thread{
     
     private Skeleton<T> skeleton; //skeleton object for referring skeleton class
     private T server; //server object to refer server class
     private Class<T> c;// interface object
     private ServerSocket socket;//Opened socket object
     InetSocketAddress address;// address to be bounded
     public Thread thread = null;
     ExecutorService exeService ; //executor service to control threads
     List<Future> serviceThreadExeList = new ArrayList<Future>();//Represent list of result from asynchronous computation 
     int port;
     public threadListener(Skeleton<T> skeleton, T server, Class<T> c, InetSocketAddress address, ServerSocket socket, boolean isSocketOpen){
         this.skeleton=skeleton;
         this.server=server;
         this.c=c;
         this.address=address;
         this.socket=socket;
         exeService=Executors.newCachedThreadPool();
     }
     //When the stop is called from skeleton this function informs service threads to wait for it's current
     //tasks to complete before ending service thread
     public synchronized void stopExeService(){
         for(Future tasks : serviceThreadExeList) //gets the list of all tasks(service thread tasks) under exectution 
                {
             try {
                 tasks.get(); //waits for each tasks to be completed before the service thread ends.
             } catch (InterruptedException ex) {
                 System.out.println("Service thread wait state interrupted" + ex.getMessage());
             } catch (ExecutionException ex) {
                System.out.println("Error during execution of wait task request" + ex.getMessage());
             }
                    
                }
         
     }
     
     @Override
     public void run(){
         while(skeleton.thread!= null && !skeleton.thread.isInterrupted()){// process request from client until listener thread is running
             try {
                 if(!this.socket.isClosed()){
                 Socket incomingRequest= this.socket.accept(); //
                 Future curServiceThread = exeService.submit(new Thread(new incomingRequestThread(this.skeleton, incomingRequest, this.server, c)));//creates service threads for client
                 serviceThreadExeList.add(curServiceThread);//add each of the service threads task to the list for informing it to wait and completes it's execution in case server is stopped
                 }
             } catch (Exception ignored) {
               
             }
         }
       
     } 
}


class incomingRequestThread<T> extends Thread{

    private Skeleton skeleton;
    private Socket socket;
    private T server;
    private Class<T> c;
    public incomingRequestThread(Skeleton skeleton, Socket socket, T server, Class<T> c) {
        this.skeleton=skeleton;
        this.socket=socket;
        this.server=server;
        this.c=c;
    }
  
    @Override
      public void run(){
        try {
            ObjectOutputStream output;
            ObjectInputStream input;
            Object response=null;
            input= new ObjectInputStream(this.socket.getInputStream()); //create the input stream
            output= new ObjectOutputStream(this.socket.getOutputStream()); // create output stream
             String methodName=(String) input.readObject(); //fetch method to be executed from input stream
             Object[] list=(Object[]) input.readObject(); //fetch the argument
             Class[] parameterType =  (Class[])input.readObject();
             Method met = null;
              try {
                       met= this.server.getClass().getMethod(methodName, parameterType); //fetch the method given the class, method name string and parameter type
                      
              } catch (NoSuchMethodException ex) {  //return error if no method or security excpetion is thrown
                 ex.printStackTrace();
              } catch (SecurityException ex) {
                 ex.printStackTrace();
              }
             
              try {
                  response= met.invoke(server, list);//invoke method on the server object and get the response
                  output.writeObject(response);//write the response on output objecct stream
                 
              }
              catch (IllegalArgumentException | IllegalAccessException ex) {
                  Logger.getLogger(incomingRequestThread.class.getName()).log(Level.SEVERE, null, ex);
              } catch (InvocationTargetException ex) {
                  output.writeObject(ex);
              }
                 
        } catch (EOFException ex) {
           
        } 
        catch (IOException ex) {
            Logger.getLogger(incomingRequestThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(incomingRequestThread.class.getName()).log(Level.SEVERE, null, ex);
        }
          
     }
}
