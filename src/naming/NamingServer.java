package naming;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

 <p>
 Each instance of the file
 naming server maintains the file
 file data - this is done by separate storage servers. The primary purpose of
 the naming server is to map each file name (path) to the storage server
 which hosts the file's contents.

 <p>
 The naming server provides two interfaces, <code>Service</code> and
 <code>Registration</code>, which are accessible through RMI. Storage servers
 use the <code>Registration</code> interface to inform the naming server of
 their existence. Clients use the <code>Service</code> interface to perform
 most file
 provides details on the methods supported.

 <p>
 Stubs for accessing the naming server must typically be created by directly
 specifying the remote network address. To make this possible, the client and
 registration interfaces are available at well-known ports defined in
 <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{
    /** Creates the naming server object.

     <p>
     The naming server is not started.
     */
    Skeleton serviceSkeleton;
    Skeleton registerSkeleton;

    ArrayList<Path> deleteFiles = new ArrayList<>();
    ArrayList<Path> myPaths = new ArrayList<>();
    HashMap<Path,Command> pathCommandStubMap = new HashMap<>();
    HashMap<Command,Storage> commandStorageMap = new HashMap<>();
    String currentRoot;
    volatile ConcurrentHashMap<Path, ReadWriteLock> lockMap;
    volatile ConcurrentHashMap<Path,Integer> pathReadCount;
    volatile ConcurrentHashMap<Path,Boolean> pathReplicated;


    public NamingServer()
    {
        try {
            currentRoot = new File( "." ).getCanonicalPath();
        } catch (IOException e) {

        }
        File f = new File(currentRoot+"/dummyRoot");
        f.mkdir();
        this.currentRoot = f.getPath();


        InetSocketAddress serviceAddress = new InetSocketAddress("localhost", 6000);
        InetSocketAddress registerAddress = new InetSocketAddress("localhost", 6001);

        serviceSkeleton =  new Skeleton(Service.class,this, serviceAddress);
        registerSkeleton = new Skeleton(Registration.class,this, registerAddress);
        pathCommandStubMap = new HashMap<>();
        commandStorageMap = new HashMap<>();
        lockMap = new ConcurrentHashMap<>();
        pathReadCount = new ConcurrentHashMap<>();
        pathReplicated = new ConcurrentHashMap<>();


    }

    /**
     * Delete a file or a directory and its children.
     * @param file The directory to delete.
     * @throws IOException Exception when problem occurs during deleting the directory.
     */
    private static void deleteChild(File file) throws IOException {

        for (File childFile : file.listFiles()) {

            if (childFile.isDirectory()) {
                deleteChild(childFile);
            } else {
                if (!childFile.delete()) {
                    throw new IOException();
                }
            }
        }

        if (!file.delete()) {
            throw new IOException();
        }
    }


    /** Starts the naming server.

     <p>
     After this method is called, it is possible to access the client and
     registration interfaces of the naming server remotely.

     @throws RMIException If either of the two skeletons, for the client or
     registration server interfaces, could not be
     started. The user should not attempt to start the
     server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        startSkeleton();
    }

    private void startSkeleton() throws RMIException {
        serviceSkeleton.start();
        registerSkeleton.start();
    }

    /** Stops the naming server.

     <p>
     This method commands both the client and registration interface
     skeletons to stop. It attempts to interrupt as many of the threads that
     are executing naming server code as possible. After this method is
     called, the naming server is no longer accessible remotely. The naming
     server should not be restarted.
     */
    public void stop()
    {


        serviceSkeleton.stop();
        registerSkeleton.stop();
        this.pathCommandStubMap.clear();
        this.commandStorageMap.clear();
        try {

            deleteChild(new File(this.currentRoot));

        } catch (IOException e) {

        }
        stopped(null);

    }

    /** Indicates that the server has completely shut down.

     <p>
     This method should be overridden for error reporting and application
     exit purposes. The default implementation does nothing.

     @param cause The cause for the shutdown, or <code>null</code> if the
     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }


    @Override
    public void lock(Path path, boolean exclusive) throws FileNotFoundException
    {
        if(path ==  null) {

            throw new NullPointerException();
        }

        File f = new File(this.currentRoot + path.toString());

        if(!f.exists()) {
            throw new FileNotFoundException();
        }

        if(!exclusive) {
            if(this.pathReadCount.containsKey(path)) {
                int count  = this.pathReadCount.get(path);
                if(count >= 20) {

                    Storage cstub = this.commandStorageMap.get(this.pathCommandStubMap.get(path));
                    for(Map.Entry<Command, Storage> s : this.commandStorageMap.entrySet()) {
                        if(s.getKey() != this.pathCommandStubMap.get(path)) {
                            try {

                                (s.getKey()).copy(path,cstub);
                            } catch (RMIException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    count = 1;
                    this.pathReplicated.put(path, true);
                    this.pathReadCount.put(path, count);

                    Path directory = path.parent();

                    while(!directory.toString().equals("/") && !directory.toString().equals("")) {
                        this.pathReplicated.put(directory, true);
                        directory = directory.parent();
                    }

                } else {
                    this.pathReadCount.put(path,  count+ 1);
                }
            } else {
                this.pathReadCount.put(path, 1);
            }
        }
        else if(this.pathReplicated.containsKey(path) && this.pathReplicated.get(path))
        {
            Command cstub = this.pathCommandStubMap.get(path);
            for(Map.Entry<Command,Storage> s : this.commandStorageMap.entrySet()) {
                if(s.getKey() != cstub) {
                    try {
                        s.getKey().delete(path);
                    } catch (RMIException e) {
                        e.printStackTrace();
                    }
                }
            }
            this.pathReplicated.put(path, false);
        }

        Path directory = path.toString().equals("/")?path : path.parent();

        while(!directory.toString().equals("/") && !directory.toString().equals("")) {
            ReadWriteLock lockObj = lockMap.containsKey(directory) ? lockMap.get(directory): new ReadWriteLock();
            try {
                lockObj.lockRead();
                lockMap.putIfAbsent(directory,lockObj);
            } catch (InterruptedException e) {

            }
            directory = directory.parent();
        }

        ReadWriteLock lockObj = lockMap.containsKey(path) ? lockMap.get(path): new ReadWriteLock();
        try {
            if (exclusive) {
                lockObj.lockWrite();
                lockMap.putIfAbsent(path, lockObj);
            } else {
                lockObj.lockRead();
                lockMap.putIfAbsent(path,lockObj);
            }
        }catch (InterruptedException e) {

        }

        if(!path.toString().equals("/")) {

            lockObj = lockMap.containsKey(new Path("/")) ? lockMap.get(new Path("/")): new ReadWriteLock();

            try {

                lockObj.lockRead();
                lockMap.putIfAbsent(new Path("/"),lockObj);

            }

            catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void unlock(Path path, boolean exclusive)
    {

        if(path ==  null) {
            throw new NullPointerException();
        }


        File f = new File(this.currentRoot + path.toString());

        if(!f.exists()) {
            throw new IllegalArgumentException();
        }

        Path directory = path.toString().equals("/")?path : path.parent();

        while(!directory.toString().equals("/")) {


            if(directory.toString().equals("")) {
                break;
            }

            ReadWriteLock unlockObj = lockMap.get(directory);

            if(unlockObj.readers > 0) {
                unlockObj.unlockRead();
            }

            directory = directory.parent();

        }

        if(!path.toString().equals("/"))
        {
            if(exclusive){


                ReadWriteLock lockObj = null;

                lockObj = lockMap.get(path);

                try {

                    if(lockObj.writers > 0) {


                        lockObj.unlockWrite();

                    }

                }

                catch (InterruptedException e) {

                }

            }

            else {
                ReadWriteLock lockObj = lockMap.get(path);
                if(lockObj.readers > 0) {

                    lockObj.unlockRead();

                }
            }
        }

        ReadWriteLock lockObj = lockMap.get(new Path("/"));

        try {
            if (exclusive && path.toString().equals("/")) {
                if(lockObj.writers > 0) {
                    lockObj.unlockWrite();
                }

            } else {
                if(lockObj.readers > 0) {
                    lockObj.unlockRead();
                }
            }
        }
        catch (InterruptedException e) {
        }
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        if(path == null) {
            throw new NullPointerException();
        }
        if(!this.pathCommandStubMap.containsKey(path) && !path.toString().equals("/")) {
            throw new FileNotFoundException();
        }

        File f = new File(this.currentRoot + path.toString());
        if(f.isDirectory()) {
            return true;
        }
        return false;
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {

        if(directory == null) {
            throw new NullPointerException();
        }
        String dirString = "";

        if(!this.pathCommandStubMap.containsKey(directory) && !directory.toString().equals("/")) {
            throw  new FileNotFoundException();
        }

        if(directory.toString().equals("/")) {
            dirString = this.currentRoot;
        }
        else {
            dirString = this.currentRoot + directory.toString();
        }

        File dir = new File(dirString);

        if(!dir.exists()) {
            throw new FileNotFoundException();
        }

        if(dir.isFile()) {
            throw new FileNotFoundException();
        }



        try {

            String p = dir.getPath();

            File parentPath = new File(p);

            List<String> files = listFiles(parentPath,parentPath);


            return files.toArray(new String[0]);


        } catch (Exception e) {
        }

        return null;
    }

    static List<String> listFiles(File parent, File folder) {
        List<String> lstFiles = new ArrayList<String>();
        if (folder.isDirectory()) {

            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    lstFiles.add(file.getName());
                }
            }
        }

        return lstFiles;
    }

    @Override
    public boolean createFile(Path file)
            throws RMIException, FileNotFoundException
    {
        if(file == null) {
            throw new NullPointerException();
        }

        File f = new File(this.currentRoot + file.toString());
        if(f.exists()) {
            return false;
        }

        if(this.commandStorageMap.isEmpty()) {
            throw new IllegalStateException();
        }

        File parentDir = new File(this.currentRoot + file.parent().toString());

        if(parentDir.exists() && !parentDir.isFile()) {
            try {
                if(f.createNewFile()) {

                    Map.Entry<Path, Command> entry = this.pathCommandStubMap.entrySet().iterator().next();

                    Command altStub = entry.getValue();

                    Command cstub = this.pathCommandStubMap.get(file.parent()) == null ? altStub : this.pathCommandStubMap.get(file.parent());



                    if(cstub.create(file)) {
                        this.pathCommandStubMap.put(file, cstub);
                        return true;
                    }
                    return false;
                } else {
                    return false;
                }
            } catch (IOException e) {

            }
        } else {
            throw new FileNotFoundException();
        }
        return false;
    }

    @Override
    public boolean createDirectory(Path file) throws FileNotFoundException
    {
        if(file == null) {
            throw new NullPointerException();
        }

        File dir = new File(this.currentRoot + file.toString());
        if(dir.exists()) {
            return false;
        }

        File parentDir = new File(this.currentRoot + file.parent().toString());

        if(parentDir.exists() && !parentDir.isFile()) {
            if(dir.mkdir()) {
                Command cstub = this.pathCommandStubMap.get(file.parent());
                this.pathCommandStubMap.put(file, cstub);
                return true;
            } else {
                return false;
            }
        } else {
            throw new FileNotFoundException();
        }

    }


    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {

        if(path == null) {
            throw new NullPointerException();
        }
        if(!this.pathCommandStubMap.containsKey(path)) {
            throw new FileNotFoundException();

        }

        for(Map.Entry<Command,Storage> s : this.commandStorageMap.entrySet()) {
            try {
                s.getKey().delete(path);
            } catch (RMIException e) {
                e.printStackTrace();
                return false;
            }
        }
        this.pathReplicated.remove(path);

        File f = new File(this.currentRoot + path.toString());
        if(f.exists()) {
            f.delete();
        }
        return true;
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {

        if(file == null) {
            throw new NullPointerException();
        }

        if(!this.pathCommandStubMap.containsKey(file)) {
            throw new FileNotFoundException();
        }

        File f = new File(this.currentRoot + file.toString());

        if(f.isDirectory()) {
            throw new FileNotFoundException();
        }

        return (this.commandStorageMap.get(this.pathCommandStubMap.get(file)));
    }


    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {

        this.deleteFiles = new ArrayList<>();

        if(client_stub == null || command_stub == null || files == null) {
            throw new NullPointerException();
        }

        if(this.commandStorageMap.containsKey(command_stub)) {
            throw new IllegalStateException();
        }

        this.commandStorageMap.put(command_stub,client_stub);

        for(Path p: files) {
            try {

                if(this.pathCommandStubMap.containsKey(p)) {

                    this.deleteFiles.add(p);

                }
                else {

                    createLocalFile(p);
                    this.pathCommandStubMap.put(p, command_stub);
                    if(p.toString().equals("/")) {
                        continue;
                    }
                    Path directory = p.parent();

                    while(!directory.toString().equals("/") && !this.pathCommandStubMap.containsKey(directory) && directory.toString().length() > 0) {
                        this.pathCommandStubMap.put(directory, command_stub);
                        directory = directory.parent();
                    }

                }
                list(new Path("/"));

            }
            catch (Exception e) {

            }
        }

        return this.deleteFiles.toArray(new Path[0]);
    }

    public synchronized boolean createLocalFile(Path file)
    {

        if(file.pathList.size() != 1 ) {

            String dirPath  = this.currentRoot + file.parent().toString();

            String fileName = file.last();
            File parentDir  = new File(dirPath);

            if(!parentDir.exists()) {
                boolean pdc = parentDir.mkdirs();
            }

            File newFile = new File(parentDir.getPath() +"/"+ fileName);

            if(newFile.exists()) {
                return false;
            }

            try{
                newFile.createNewFile();
                return true;
            }

            catch (IOException e){

                return false;
            }
        }

        else {
            if(file.pathList.get(0).equals("/")) {
                return false;
            }
        }

        return false;
    }

}
