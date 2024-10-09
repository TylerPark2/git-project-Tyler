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
        String type;  // either a "blob" or "tree"
        String hash;
        String path;
        boolean deleted;

        public IndexEntry(String type, String hash, String path) {
            this.type = type;
            this.hash = hash;
            this.path = path;
            this.deleted = false;
        }

        public IndexEntry(String type, String hash, String path, boolean deleted) {
            this.type = type;
            this.hash = hash;
            this.path = path;
            this.deleted = deleted;
        }

        @Override
        public String toString() {
            return String.format("%s %s %s%s", type, hash, path, deleted ? " deleted" : "");
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
                String[] parts = line.split(" ", 4); // Changed to handle deletion status
                if (parts.length >= 3) {
                    boolean deleted = parts.length == 4 && parts[3].equals("deleted");
                    entries.add(new IndexEntry(parts[0], parts[1], parts[2], deleted));
                }
            }
        }
        return entries;
    }

    private void writeIndex(List<IndexEntry> entries, String repoPath) throws IOException {
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
        File objectsDir = new File(repoPath, "git/objects");
        if (!objectsDir.exists()) {
            if (!objectsDir.mkdirs()) {
                throw new IOException("Failed to create objects directory.");
            }
        }
        
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
        
        entries = entries.stream()
            .filter(e -> !e.path.equals(path))
            .collect(Collectors.toList());
        
        entries.add(new IndexEntry(type, hash, path));
        
        writeIndex(entries, repoPath);
    }

    public String createRootTree(String workingDir, String repoPath) throws IOException, NoSuchAlgorithmException {
        checkForDeletedFiles(workingDir, repoPath);
        
        File rootDir = new File(workingDir);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IOException("Invalid working directory: " + workingDir);
        }
        return createTree(rootDir, repoPath, workingDir);
    }

    private void checkForDeletedFiles(String workingDir, String repoPath) throws IOException {
        List<IndexEntry> entries = readIndex(repoPath);
        Set<String> existingFiles = new HashSet<>();
        collectExistingFiles(new File(workingDir), workingDir, existingFiles);
        boolean hasChanges = false;
        for (IndexEntry entry : entries) {
            if (!entry.deleted && !existingFiles.contains(entry.path)) {
                entry.deleted = true;
                hasChanges = true;
                System.out.println("Marked as deleted: " + entry.path);
            }
        }
        
        if (hasChanges) {
            writeIndex(entries, repoPath);
        }
    }

    private void collectExistingFiles(File directory, String workingDir, Set<String> existingFiles) {
        if (directory.getName().equals("git")) return;
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("git")) continue;
                
                String relativePath = getRelativePath(file, workingDir);
                if (file.isFile()) {
                    existingFiles.add(relativePath);
                } else if (file.isDirectory()) {
                    existingFiles.add(relativePath);
                    collectExistingFiles(file, workingDir, existingFiles);
                }
            }
        }
    }

    public void removeFromIndex(String filePath, String repoPath) throws IOException {
        List<IndexEntry> entries = readIndex(repoPath);
        String relativePath = getRelativePath(new File(filePath), new File(repoPath).getParent());
        
        boolean found = false;
        for (IndexEntry entry : entries) {
            if (entry.path.equals(relativePath)) {
                entry.deleted = true;
                found = true;
                System.out.println("Marked as deleted in index: " + relativePath);
                break;
            }
        }
        
        if (found) {
            writeIndex(entries, repoPath);
        } else {
            System.out.println("File not found in index: " + relativePath);
        }
    }

    public void cleanIndex(String repoPath) throws IOException {
        List<IndexEntry> entries = readIndex(repoPath);
        List<IndexEntry> cleanedEntries = entries.stream()
            .filter(entry -> !entry.deleted)
            .collect(Collectors.toList());
        
        if (cleanedEntries.size() < entries.size()) {
            writeIndex(cleanedEntries, repoPath);
            System.out.println("Cleaned " + (entries.size() - cleanedEntries.size()) + " deleted entries from index");
        }
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