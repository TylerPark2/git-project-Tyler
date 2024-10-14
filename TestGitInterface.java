import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestGitInterface {
    private static final String REPO_PATH = "/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER";
    private static final String TEST_FILE_1 = "test_file_1.txt";
    private static final String TEST_FILE_2 = "test_file_2.txt";
    
    public static void main(String[] args) {
        GitInterface git = new GitInterfaceImpl(REPO_PATH);
        
        try {
            setupTestEnvironment();
            System.out.println("Testing stage method:");
            testStage(git);
            System.out.println("\nTesting commit method:");
            testCommit(git);
            cleanupTestEnvironment();
            
        } catch (IOException e) {
            System.err.println("Error during test: " + e.getMessage());
        }
    }
    
    private static void setupTestEnvironment() throws IOException {
        Files.write(Paths.get(REPO_PATH, TEST_FILE_1), "Test content 1".getBytes());
        Files.write(Paths.get(REPO_PATH, TEST_FILE_2), "Test content 2".getBytes());
    }
    
    private static void testStage(GitInterface git) {
        git.stage(Paths.get(REPO_PATH, TEST_FILE_1).toString());
        System.out.println("Staged " + TEST_FILE_1);
        verifyStaging(TEST_FILE_1);
        git.stage(Paths.get(REPO_PATH, TEST_FILE_2).toString());
        System.out.println("Staged " + TEST_FILE_2);
        verifyStaging(TEST_FILE_2);
    }
    
    private static void verifyStaging(String fileName) {
        File indexFile = new File(REPO_PATH, "git/index");
        if (indexFile.exists()) {
            try {
                String indexContent = new String(Files.readAllBytes(indexFile.toPath()));
                if (indexContent.contains(fileName)) {
                    System.out.println("Verification: " + fileName + " is in the index.");
                } else {
                    System.out.println("Verification Failed: " + fileName + " is not in the index.");
                }
            } catch (IOException e) {
                System.err.println("Error reading index file: " + e.getMessage());
            }
        } else {
            System.out.println("Verification Failed: Index file does not exist.");
        }
    }
    
    private static void testCommit(GitInterface git) {
        String author = "Test Author";
        String message = "Test commit message";
        
        String commitHash = git.commit(author, message);
        
        if (commitHash != null && !commitHash.isEmpty()) {
            System.out.println("Commit created with hash: " + commitHash);
            verifyCommit(commitHash);
        } else {
            System.out.println("Commit failed: No hash returned.");
        }
    }
    
    private static void verifyCommit(String commitHash) {
        File commitFile = new File(REPO_PATH, "git/objects/" + commitHash);
        if (commitFile.exists()) {
            System.out.println("Verification: Commit file exists in objects directory.");
        } else {
            System.out.println("Verification Failed: Commit file does not exist in objects directory.");
        }
        
        File headFile = new File(REPO_PATH, "git/HEAD");
        if (headFile.exists()) {
            try {
                String headContent = new String(Files.readAllBytes(headFile.toPath())).trim();
                if (headContent.equals(commitHash)) {
                    System.out.println("Verification: HEAD points to the new commit.");
                } else {
                    System.out.println("Verification Failed: HEAD does not point to the new commit.");
                }
            } catch (IOException e) {
                System.err.println("Error reading HEAD file: " + e.getMessage());
            }
        } else {
            System.out.println("Verification Failed: HEAD file does not exist.");
        }
    }
    
    private static void cleanupTestEnvironment() throws IOException {
        Files.deleteIfExists(Paths.get(REPO_PATH, TEST_FILE_1));
        Files.deleteIfExists(Paths.get(REPO_PATH, TEST_FILE_2));
    }
}