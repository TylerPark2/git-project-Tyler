import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;

public class BlobTest {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String repoPath = "/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER";
        Blob blob = new Blob();
        blob.resetTestFiles(repoPath);
        setupTestFiles(repoPath);
        Files.createDirectories(Paths.get(repoPath, "git", "objects"));
        System.out.println("\nCreating snapshot of working directory...");
        String rootTreeHash = blob.createRootTree(repoPath, repoPath);
        System.out.println("Root tree hash: " + rootTreeHash);
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
        System.out.println("\nContents of index file:");
        File indexFile = new File(repoPath + "/git/index");
        if (indexFile.exists()) {
            Files.readAllLines(indexFile.toPath()).forEach(System.out::println);
        }
    }
    
    private static void setupTestFiles(String repoPath) throws IOException {
        Files.createDirectories(Paths.get(repoPath, "dir1"));
        Files.createDirectories(Paths.get(repoPath, "dir2"));
        Files.createDirectories(Paths.get(repoPath, "dir2/subdir"));
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