import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Commit {
    private final String tree;
    private final String parent;
    private final String author;
    private final String date;
    private final String message;

    public Commit(String tree, String parent, String author, String date, String message) {
        this.tree = tree;
        this.parent = parent;
        this.author = author;
        this.date = date;
        this.message = message;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("tree: ").append(tree).append("\n");
        if (parent != null && !parent.isEmpty()) {
            sb.append("parent: ").append(parent).append("\n");
        }
        sb.append("author: ").append(author).append("\n");
        sb.append("date: ").append(date).append("\n");
        sb.append("message: ").append(message).append("\n");
        return sb.toString();
    }

    public static class CommitBuilder {
        private String tree;
        private String parent;
        private String author;
        private String message;

        public CommitBuilder setTree(String tree) {
            this.tree = tree;
            return this;
        }

        public CommitBuilder setParent(String parent) {
            this.parent = parent;
            return this;
        }

        public CommitBuilder setAuthor(String author) {
            this.author = author;
            return this;
        }

        public CommitBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Commit build() {
            if (tree == null || author == null || message == null) {
                throw new IllegalStateException("Tree, author, and message are required fields");
            }
            String date = new SimpleDateFormat("MMM d, yyyy HH:mm:ss").format(new Date());
            return new Commit(tree, parent, author, date, message);
        }
    }

    public void createCommit(String author, String message, String repoPath) 
            throws IOException, NoSuchAlgorithmException {
        Blob blob = new Blob();
        String rootTreeHash = blob.createRootTree(new File(repoPath).getParent(), repoPath);
        String parentCommit = blob.getCurrentCommitHash(repoPath);
        Commit commit = new CommitBuilder()
            .setTree(rootTreeHash)
            .setParent(parentCommit)
            .setAuthor(author)
            .setMessage(message)
            .build();
        String commitContent = commit.toString();
        String commitHash = Blob.generateUniqueFileName("tree_content:" + commitContent);
        File objectsDir = new File(repoPath, "git/objects");
        File commitFile = new File(objectsDir, commitHash);
        
        if (!commitFile.exists()) {
            byte[] content = commitContent.getBytes();
            Files.write(commitFile.toPath(), content);
            System.out.println("Commit created: " + commitHash);
        }
        blob.updateHead(commitHash, repoPath);
    }
    
}
