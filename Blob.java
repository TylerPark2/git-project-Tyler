import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

public class Blob {
    private static boolean COMPRESSION_ENABLED = true;

    public static String generateUniqueFileName(String filePath) throws IOException, NoSuchAlgorithmException {
        byte[] fileContent;
        if (filePath.startsWith("tree_content:")) {
            // Handle tree content directly
            fileContent = filePath.substring("tree_content:".length()).getBytes();
        } else {
            // Handle regular file content
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

    public void createBlob(String filePath, String repoPath) throws IOException, NoSuchAlgorithmException {
        String uniqueFileName = generateUniqueFileName(filePath);
        
        // Create the objects directory if it doesn't exist
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
        
        insertIntoIndex(false, uniqueFileName, filePath, repoPath);
    }

    public String createRootTree(String workingDir, String repoPath) throws IOException, NoSuchAlgorithmException {
        File rootDir = new File(workingDir);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IOException("Invalid working directory: " + workingDir);
        }
        return createTree(rootDir, repoPath);
    }

    public String createTree(File directory, String repoPath) throws IOException, NoSuchAlgorithmException {
        StringBuilder treeContent = new StringBuilder();
        File[] entries = directory.listFiles();
        
        if (entries != null) {
            for (File entry : entries) {
                if (entry.getName().equals("git")) continue; // Skip git directory
                
                String hash;
                if (entry.isDirectory()) {
                    hash = createTree(entry, repoPath);
                    treeContent.append("tree ").append(hash).append(" ").append(entry.getName()).append("\n");
                } else {
                    createBlob(entry.getAbsolutePath(), repoPath);
                    hash = generateUniqueFileName(entry.getAbsolutePath());
                    treeContent.append("blob ").append(hash).append(" ").append(entry.getName()).append("\n");
                }
            }
        }

        // Generate hash for tree content
        String treeHash = generateUniqueFileName("tree_content:" + treeContent.toString());
        
        // Save tree content
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

    private void insertIntoIndex(boolean isTree, String hash, String originalFilePath, String repoPath) throws IOException {
        File indexFile = new File(repoPath, "git/index");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile, true))) {
            String fileName = Paths.get(originalFilePath).getFileName().toString();
            writer.write((isTree ? "tree " : "blob ") + hash + " " + fileName);
            writer.newLine();
        }
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
}