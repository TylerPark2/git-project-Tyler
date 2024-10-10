import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;

public class CommitTester {
    public static void main(String[] args) {
        try {
            String repoPath = "/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER";
            setupTestRepo(repoPath);
            String author1 = "Tyler Park <tyler@example.com>";
            String message1 = "Initial commit";
            testCreateCommit(author1, message1, repoPath);
            String author2 = "Summer Park <Summer@example.com>";
            String message2 = "Added new feature";
            testCreateCommit(author2, message2, repoPath);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void setupTestRepo(String repoPath) throws IOException {
        File repo = new File(repoPath);
        File gitDir = new File(repo, "git");
        File objectsDir = new File(gitDir, "objects");

        if (!objectsDir.exists()) {
            objectsDir.mkdirs();
            System.out.println("Test repository setup at: " + repoPath);
        }

        File headFile = new File(gitDir, "HEAD");
        if (headFile.exists()) {
            Files.delete(headFile.toPath());
        }
        headFile.createNewFile();
        System.out.println("HEAD file created/reset: " + headFile.getPath());
    }

    private static void testCreateCommit(String author, String message, String repoPath) 
            throws IOException, NoSuchAlgorithmException {
        Commit commit = new Commit(null, null, null, null, null);
        commit.createCommit(author, message, repoPath);
        File headFile = new File(repoPath + "/git/HEAD");
        String latestCommitHash = new String(Files.readAllBytes(headFile.toPath())).trim();
        System.out.println("Latest commit hash: " + latestCommitHash);
        File commitFile = new File(repoPath + "/git/objects/" + latestCommitHash);
        if (commitFile.exists()) {
            String commitContent = new String(Files.readAllBytes(commitFile.toPath()));
            System.out.println("Commit content:\n" + commitContent);
        } else {
            System.out.println("Commit file not found.");
        }
        System.out.println("\n=======================================\n");
    }
}
