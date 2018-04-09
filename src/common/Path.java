package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

 <p>
 Objects of type <code>Path</code> are used by all filesystem interfaces.
 Path objects are immutable.

 <p>
 The string representation of paths is a forward-slash-delimeted sequence of
 path components. The root directory is represented as a single forward
 slash.

 <p>
 The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
 not permitted within path components. The forward slash is the delimeter,
 and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Comparable<Path>, Serializable
{
    public String root = null;
    public ArrayList<String> pathList = new ArrayList<>();

    /** Creates a new path which represents the root directory. */
    public Path()
    {
        this.root = "/";
        this.pathList.add(root);
    }

    /** Creates a new path by appending the given component to an existing path.

     @param path The existing path.
     @param component The new component.
     @throws IllegalArgumentException If <code>component</code> includes the
     separator, a colon, or
     <code>component</code> is the empty
     string.
     */
    public Path(Path path, String component)
    {
        //System.out.println("Path and component constructor - " + path.pathList + ", " + component);

        this.root = path.root;
        this.pathList.addAll(path.pathList);
        //System.out.println("Path = " + this.pathList);

        if(component == null || component.length() == 0 || component.contains(":") || component.contains("/")) {
            //System.out.println("Component blotted");
            throw new IllegalArgumentException();
        }

        this.pathList.add(component);

        //System.out.println("Added " + component +"to path. New path - " + this.pathList);

        //throw new UnsupportedOperationException("not implemented");
    }

    /** Creates a new path from a path string.

     <p>
     The string is a sequence of components delimited with forward slashes.
     Empty components are dropped. The string must begin with a forward
     slash.

     @param path The path string.
     @throws IllegalArgumentException If the path string does not begin with
     a forward slash, or if the path
     contains a colon character.
     */
    public Path(String path)
    {

        //System.out.println("Creating path with " + path);

        if(path == null || path.length() == 0 || !path.startsWith("/") || path.contains(":")) {

            throw new IllegalArgumentException();

        }

        String []pathArray = path.split("/");

        if(this.root == null) {

            this.root = "/";
            this.pathList.add(root);

        }

        for(String s : pathArray) {
            if(s.length() != 0) {
                this.pathList.add(s);
            }
        }

        //System.out.println("Created path - " + this.pathList);
    }

    /** Returns an iterator over the components of the path.

     <p>
     The iterator cannot be used to modify the path object - the
     <code>remove</code> method is not supported.

     @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
        //System.out.println(" Returning iterator.");

        return (Collections.unmodifiableList(this.pathList.subList(1,this.pathList.size())).iterator());
    }

    /** Lists the paths of all files in a directory tree on the local
     file//System.

     @param directory The root directory of the directory tree.
     @return An array of relative paths, one for each file in the directory
     tree.
     @throws FileNotFoundException If the root directory does not exist.
     @throws IllegalArgumentException If <code>directory</code> exists but
     does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        Path paths[] = null;

        if(!directory.exists()) {
            throw new FileNotFoundException();
        }

        if(!directory.isDirectory()) {
            throw new IllegalArgumentException();
        }

        try {

            String p = directory.getCanonicalPath();

            File parentPath = new File(p);

            List<String> files = listFiles(parentPath,parentPath);

            paths = new Path[files.size()];
            int i = -1;

            for (String file : files) {
                paths[++i] = new Path(file);
                //System.out.println("List : " + new File(file).getCanonicalPath());
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }
        return paths;
    }



     private static List<String> listFiles(File parent, File folder) {
        List<String> lstFiles = new ArrayList<String>();
        if (folder.isDirectory()) {

            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        lstFiles.addAll(listFiles(parent, file));
                    } else {
                        String path = file.getPath();
                        String offset = parent.getPath();

                        path = path.substring(offset.length());
                        //System.out.println("Adding to path - " + path);
                        lstFiles.add(path);
                        //System.out.println((new File(path).exists()));
                    }
                }
            }
        }

        return lstFiles;
    }

    /** Determines whether the path represents the root directory.

     @return <code>true</code> if the path does represent the root directory,
     and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return this.pathList.size() == 1;
    }

    /** Returns the path to the parent of this path.

     @throws IllegalArgumentException If the path represents the root
     directory, and therefore has no parent.
     */
    public Path parent()
    {
        //System.out.println("Returning parent for " + this.toString());

        int size = this.pathList.size()-1;

        if(size == 0) {
            throw  new IllegalArgumentException();
        }

        Path newPath = new Path();
        newPath.root = "/";
        newPath.pathList.add(newPath.root);

        for(int i  = 0;i < size; i++) {
            newPath.pathList.add(this.pathList.get(i));
        }

        //System.out.println("Returning the parent as " + newPath.toString());
        return newPath;
    }

    /** Returns the last component in the path.

     @throws IllegalArgumentException If the path represents the root
     directory, and therefore has no last
     component.
     */
    public String last()
    {
        //System.out.println("Returning the last component for " + this.toString());
        int size = this.pathList.size();

        if(size == 1) {
            //System.out.println("No last component.");
            throw new IllegalArgumentException();
        }

        //System.out.println("Returning last component - " + this.pathList.get(size-1));
        return this.pathList.get(size-1);
    }

    /** Determines if the given path is a subpath of this path.

     <p>
     The other path is a subpath of this path if it is a prefix of this path.
     Note that by this definition, each path is a subpath of itself.

     @param other The path to be tested.
     @return <code>true</code> If and only if the other path is a subpath of
     this path.
     */
    public boolean isSubpath(Path other)
    {
        if(other == null) {
            return true;
        }

        String subPath = other.toString();

        if(this.toString().startsWith(subPath)) {
            return true;
        }

        return false;
    }

    /** Converts the path to <code>File</code> object.

     @param root The resulting <code>File</code> object is created relative
     to this directory.
     @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        if(root == null) {
            //System.out.println("Creating new file in - " + root.getPath());
            //System.out.println("to string is................." + this.toString() + "\n");
            return new File(this.toString());
        }

//        System.out.println("returning new File for " + root.getPath() + this.toString());
        return new File(root.getPath() + this.toString());

    }

    /** Compares this path to another.

     <p>
     An ordering upon <code>Path</code> objects is provided to prevent
     deadlocks between applications that need to lock multiple filesystem
     objects simultaneously. By convention, paths that need to be locked
     simultaneously are locked in increasing order.

     <p>
     Because locking a path requires locking every component along the path,
     the order is not arbitrary. For example, suppose the paths were ordered
     first by length, so that <code>/etc</code> precedes
     <code>/bin/cat</code>, which precedes <code>/etc/dfs/conf.txt</code>.

     <p>
     Now, suppose two users are running two applications, such as two
     instances of <code>cp</code>. One needs to work with <code>/etc</code>
     and <code>/bin/cat</code>, and the other with <code>/bin/cat</code> and
     <code>/etc/dfs/conf.txt</code>.

     <p>
     Then, if both applications follow the convention and lock paths in
     increasing order, the following situation can occur: the first
     application locks <code>/etc</code>. The second application locks
     <code>/bin/cat</code>. The first application tries to lock
     <code>/bin/cat</code> also, but gets blocked because the second
     application holds the lock. Now, the second application tries to lock
     <code>/etc/dfs/conf.txt</code>, and also gets blocked, because it would
     need to acquire the lock for <code>/etc</code> to do so. The two
     applications are now deadlocked.

     @param other The other path.
     @return Zero if the two paths are equal, a negative number if this path
     precedes the other path, or a positive number if this path
     follows the other path.
     */
    @Override
    public int compareTo(Path other)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Compares two paths for equality.

     <p>
     Two paths are equal if they share all the same components.

     @param other The other path.
     @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        Path otherPath = (Path)other;
        if(this.toString().equals(otherPath.toString())) {
            return true;
        }
        return false;
        //       throw new UnsupportedOperationException("not implemented");
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        return this.toString().hashCode();

    }

    /** Converts the path to a string.

     <p>
     The string may later be used as an argument to the
     <code>Path(String)</code> constructor.

     @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        String result = "";

        //System.out.println("Returning path for " + this.pathList);

        if(this.pathList.size() == 0) {
            return null;
        }

        if(this.pathList.size() == 1) {
            return "/";
        }

        for(Object res : this.pathList.toArray()) {
            if(!res.equals("/")) {
                result += "/" + res;
            }

        }

        //System.out.println("Returning path as " + result + " length = " + result.length());

        return result.trim();
    }
}
