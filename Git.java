import java.io.*;
public class Git {
    public static void main (String [] args) {
        String repoPath = "/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER";
        Git git = new Git();
        git.initRepo(repoPath);

    }

    public void initRepo(String path) {
        File git = new File(path, "git");
        git.mkdirs();
        
        if (git.exists()) {
            File objectsDir = new File(git, "objects");
            File indexFile = new File(git, "index");
            File headFile = new File(git, "HEAD");
            if (objectsDir.exists() && indexFile.exists() && headFile.exists()) {
                System.out.println("Git Repository already exists");
                return;
            }
        }
    
        git.mkdirs();
        File objectsDir = new File(git, "objects");
        objectsDir.mkdirs(); 
    
        File indexFile = new File(git, "index");
        File headFile = new File(git, "HEAD");  // Create HEAD file
        
        try {
            indexFile.createNewFile();
            headFile.createNewFile();  // Initialize empty HEAD file
            System.out.println("Initialized empty Git repository in " + git.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error creating index or HEAD file: " + e.getMessage());
        }
    }
    
}