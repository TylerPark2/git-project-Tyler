import java.io.File;

public class TestGitInterface {
    public static void main(String[] args) {
        GitInterface git = new GitInterfaceImpl("/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER");
        
        // Test staging
        System.out.println("Testing stage() method:");
        String testFilePath = "/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER/testfile.txt";
        git.stage(testFilePath);
        
        // Check if the file was staged (this assumes staging creates a blob in the objects directory)
        File objectsDir = new File("/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER/git/objects");
        if (objectsDir.exists() && objectsDir.isDirectory() && objectsDir.list().length > 0) {
            System.out.println("PASS: File appears to be staged successfully.");
        } else {
            System.out.println("FAIL: File doesn't appear to be staged.");
        }
        
        // Test committing
        System.out.println("\nTesting commit() method:");
        String commitHash = git.commit("Tyler", "Initial commit");
        
        if (commitHash != null && !commitHash.isEmpty()) {
            System.out.println("PASS: Commit created with hash: " + commitHash);
            
            // Verify that the commit file exists
            File commitFile = new File("/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER/git/objects/" + commitHash);
            if (commitFile.exists()) {
                System.out.println("PASS: Commit file found in objects directory.");
            } else {
                System.out.println("FAIL: Commit file not found in objects directory.");
            }
        } else {
            System.out.println("FAIL: Commit hash is null or empty.");
        }
        
        // Test checkout (not implemented)
        System.out.println("\nTesting checkout() method:");
        git.checkout("some-commit-hash");
        System.out.println("PASS: Checkout method called without errors (but not implemented).");
        
        System.out.println("\nAll tests completed.");
    }
}