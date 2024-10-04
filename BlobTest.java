import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;

public class BlobTest {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // Define the path to your project where the test files will be created
        String repoPath = "/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER/testRepo";
        
        // Create a Blob instance
        Blob blob = new Blob();
        
        // Reset the test files (delete if they exist from previous runs)
        blob.resetTestFiles(repoPath);
        
        // Setup test files and directories in the repo
        setupTestFiles(repoPath);
        
        // Create git directory structure
        Files.createDirectories(Paths.get(repoPath, "git", "objects"));
        
        // Create the root tree for the working directory and print the root hash
        System.out.println("\nCreating snapshot of working directory...");
        String rootTreeHash = blob.createRootTree(repoPath, repoPath);
        System.out.println("Root tree hash: " + rootTreeHash);
        
        // Print the contents of the objects directory
        System.out.println("\nContents of objects directory:");
        File objectsDir = new File(repoPath + "/git/objects");
        if (objectsDir.exists()) {
            String[] objects = objectsDir.list();
            if (objects != null) {
                for (String object : objects) {
                    System.out.println(" - " + object);
                }
            }
        }
        
        // Print the contents of the index file
        System.out.println("\nContents of index file:");
        File indexFile = new File(repoPath + "/git/index");
        if (indexFile.exists()) {
            Files.readAllLines(indexFile.toPath()).forEach(System.out::println);
        }
        
        blob.resetTestFiles(repoPath);
    }

    // Test setup: Create sample files and directories for the test
    private static void setupTestFiles(String repoPath) throws IOException {
        // Create directories
        Files.createDirectories(Paths.get(repoPath, "dir1"));
        Files.createDirectories(Paths.get(repoPath, "dir2"));
        Files.createDirectories(Paths.get(repoPath, "dir2/subdir"));

        // Creating sample files
        Files.write(Paths.get(repoPath, "file1.txt"), "Content of file 1".getBytes());
        Files.write(Paths.get(repoPath, "dir1/file2.txt"), "Content of file 2".getBytes());
        Files.write(Paths.get(repoPath, "dir2/file3.txt"), "Content of file 3".getBytes());
        Files.write(Paths.get(repoPath, "dir2/subdir/file4.txt"), "Content of file 4".getBytes());

        System.out.println("Test files created:");
        System.out.println(" - file1.txt");
        System.out.println(" - dir1/file2.txt");
        System.out.println(" - dir2/file3.txt");
        System.out.println(" - dir2/subdir/file4.txt");
    }
}