import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class Blob {
    private static boolean COMPRESSION_ENABLED = true;

    static class IndexEntry {
        String type;  // "blob" or "tree"
        String hash;
        String path;

        public IndexEntry(String type, String hash, String path) {
            this.type = type;
            this.hash = hash;
            this.path = path;
        }

        @Override
        public String toString() {
            return String.format("%s %s %s", type, hash, path);
        }
    }

    public static String generateUniqueFileName(String filePath) throws IOException, NoSuchAlgorithmException {
        byte[] fileContent;
        if (filePath.startsWith("tree_content:")) {
            fileContent = filePath.substring("tree_content:".length()).getBytes();
        } else {
            fileContent = Files.readAllBytes(Paths.get(filePath));
        }
        fileContent = maybeCompress(fileContent);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = digest.digest(fileContent);
        StringBuilder hashString = new StringBuilder();
        for (byte b : hashBytes) {
            hashString.append(String.format("%02x", b));
        }
        return hashString.toString();
    }

    private List<IndexEntry> readIndex(String repoPath) throws IOException {
        File indexFile = new File(repoPath, "git/index");
        List<IndexEntry> entries = new ArrayList<>();
        if (!indexFile.exists()) {
            return entries;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(indexFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 3);
                if (parts.length == 3) {
                    entries.add(new IndexEntry(parts[0], parts[1], parts[2]));
                }
            }
        }
        return entries;
    }

    private void writeIndex(List<IndexEntry> entries, String repoPath) throws IOException {
        // Sort entries by path to maintain consistent order
        entries.sort(Comparator.comparing(e -> e.path));
        
        File indexFile = new File(repoPath, "git/index");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile))) {
            for (IndexEntry entry : entries) {
                writer.write(entry.toString());
                writer.newLine();
            }
        }
    }

    private String getRelativePath(File file, String workingDir) {
        Path workingPath = Paths.get(workingDir).toAbsolutePath();
        Path filePath = file.toPath().toAbsolutePath();
        return workingPath.relativize(filePath).toString().replace('\\', '/');
    }

    private boolean hasFileChanged(File file, String existingHash) throws IOException, NoSuchAlgorithmException {
        if (!file.exists()) {
            return true;
        }
        String newHash = generateUniqueFileName(file.getAbsolutePath());
        return !newHash.equals(existingHash);
    }

    public void createBlob(String filePath, String repoPath) throws IOException, NoSuchAlgorithmException {
        File file = new File(filePath);
        String relativePath = getRelativePath(file, new File(repoPath).getParent());
        String uniqueFileName = generateUniqueFileName(filePath);
        
        // Create the objects directory if it doesn't exist
        File objectsDir = new File(repoPath, "git/objects");
        if (!objectsDir.exists()) {
            if (!objectsDir.mkdirs()) {
                throw new IOException("Failed to create objects directory.");
            }
        }
        
        // Create blob file if it doesn't exist
        File blobFile = new File(objectsDir, uniqueFileName);
        if (!blobFile.exists()) {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            fileContent = maybeCompress(fileContent);
            Files.write(blobFile.toPath(), fileContent);
            System.out.println("Blob created: " + uniqueFileName);
        } else {
            System.out.println("Blob already exists: " + uniqueFileName);
        }
        
        updateIndex("blob", uniqueFileName, relativePath, repoPath);
    }

    private void updateIndex(String type, String hash, String path, String repoPath) throws IOException {
        List<IndexEntry> entries = readIndex(repoPath);
        
        // Remove existing entry with the same path
        entries = entries.stream()
            .filter(e -> !e.path.equals(path))
            .collect(Collectors.toList());
        
        // Add new entry
        entries.add(new IndexEntry(type, hash, path));
        
        // Write updated index
        writeIndex(entries, repoPath);
    }

    public String createRootTree(String workingDir, String repoPath) throws IOException, NoSuchAlgorithmException {
        File rootDir = new File(workingDir);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IOException("Invalid working directory: " + workingDir);
        }
        return createTree(rootDir, repoPath, workingDir);
    }

    public String createTree(File directory, String repoPath, String workingDir) throws IOException, NoSuchAlgorithmException {
        StringBuilder treeContent = new StringBuilder();
        File[] entries = directory.listFiles();
        
        if (entries != null) {
            // Sort entries for consistent tree hashing
            Arrays.sort(entries, Comparator.comparing(File::getName));
            
            for (File entry : entries) {
                if (entry.getName().equals("git")) continue;
                
                String relativePath = getRelativePath(entry, workingDir);
                String hash;
                
                if (entry.isDirectory()) {
                    hash = createTree(entry, repoPath, workingDir);
                    treeContent.append("tree ").append(hash).append(" ").append(entry.getName()).append("\n");
                    updateIndex("tree", hash, relativePath, repoPath);
                } else {
                    createBlob(entry.getAbsolutePath(), repoPath);
                    hash = generateUniqueFileName(entry.getAbsolutePath());
                    treeContent.append("blob ").append(hash).append(" ").append(entry.getName()).append("\n");
                }
            }
        }

        String treeHash = generateUniqueFileName("tree_content:" + treeContent.toString());
        
        File objectsDir = new File(repoPath, "git/objects");
        File treeFile = new File(objectsDir, treeHash);
        if (!treeFile.exists()) {
            byte[] content = treeContent.toString().getBytes();
            content = maybeCompress(content);
            Files.write(treeFile.toPath(), content);
            System.out.println("Tree created: " + treeHash);
        }
        
        return treeHash;
    }

    private static byte[] maybeCompress(byte[] data) throws IOException {
        if (COMPRESSION_ENABLED) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                gzipOutputStream.write(data);
            }
            return byteArrayOutputStream.toByteArray();
        }
        return data;
    }

    public void resetTestFiles(String repoPath) {
        File gitDir = new File(repoPath, "git");
        if (gitDir.exists()) {
            deleteDirectory(gitDir);
            System.out.println("Test files reset.");
        } else {
            System.out.println("No repository to reset.");
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }

    public void updateHead(String commitHash, String repoPath) throws IOException {
        File headFile = new File(repoPath, "git/HEAD");
        if (!headFile.exists()) {
            throw new IOException("HEAD file does not exist. Repository may not be initialized properly.");
        }
        
        Files.write(headFile.toPath(), commitHash.getBytes());
        System.out.println("HEAD updated to commit: " + commitHash);
    }

    public String getCurrentCommitHash(String repoPath) throws IOException {
        File headFile = new File(repoPath, "git/HEAD");
        if (!headFile.exists() || headFile.length() == 0) {
            return null;
        }
        return new String(Files.readAllBytes(headFile.toPath())).trim();
    }
}