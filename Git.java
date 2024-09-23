import java.io.*;
public class Git {
    public static void main (String [] args) {
        String repoPath = "/Users/User/Desktop/HTCS_Projects/";
        Git git = new Git();
        git.initRepo(repoPath);

    }

    public void initRepo(String path) {
        File git = new File (path, "git");
        git.mkdirs();
        if (git.exists()) {
            File objectsDir = new File(git, "objects");
            File indexFile = new File(git, "index");
            if (objectsDir.exists() && indexFile.exists()) {
                System.out.println("Git Repository already exists");
                return;
            }
        }

        git.mkdirs();
        File objectsDir = new File(git, "objects");
        objectsDir.mkdirs(); 

        File indexFile = new File(git, "index");
        try {
            indexFile.createNewFile();
        } catch (IOException e) {
            System.err.println("Error creating index file: " + e.getMessage());
        }

        System.out.println("Initialized empty Git repository in " + git.getAbsolutePath());
    }
}


        
