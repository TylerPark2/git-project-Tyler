import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class GitInterfaceImpl implements GitInterface {
    private final String repoPath;
    private final Blob blob;
    private final Commit commitHandler;

    public GitInterfaceImpl(String repoPath) {
        this.repoPath = repoPath;
        this.blob = new Blob();
        this.commitHandler = new Commit(null, null, null, null, null);
    }

    @Override
    public void stage(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("Error: File does not exist: " + filePath);
                return;
            }
            
            if (file.isDirectory()) {
                System.out.println("Error: Cannot stage a directory: " + filePath);
                return;
            }
            
            blob.createBlob(filePath, repoPath);
            System.out.println("Staged: " + filePath);
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Error staging file: " + e.getMessage());
        }
    }

    @Override
    public String commit(String author, String message) {
        try {
            commitHandler.createCommit(author, message, repoPath);
            String commitHash = blob.getCurrentCommitHash(repoPath);
            System.out.println("Committed with hash: " + commitHash);
            return commitHash;
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Error creating commit: " + e.getMessage());
            return null;
        }
    }

    
}